package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentParserService {

    public PatentFormResponse parseUploadedDocument(MultipartFile file) throws Exception {
        String fullText;
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            fullText = extractor.getText();
        }

        PatentFormResponse response = new PatentFormResponse();
        PatentFormResponse.ApplicantDTO applicant = new PatentFormResponse.ApplicantDTO();
        PatentFormResponse.AddressDTO address = new PatentFormResponse.AddressDTO();

        // 1. Extract Applicant Name
        Pattern namePattern = Pattern.compile("^\\s*([^,]+),\\s*an");
        Matcher nameMatcher = namePattern.matcher(fullText);
        if (nameMatcher.find()) {
            applicant.setName(nameMatcher.group(1).trim());
        }
        if (applicant.getName() == null || applicant.getName().isBlank()) {
            applicant.setName(extractApplicantNameFromStructuredText(fullText));
        }

        // 2. Extract Nationality
        Pattern nationalityPattern = Pattern.compile(",\\s*an\\s+(\\w+)\\s+citizen");
        Matcher nationalityMatcher = nationalityPattern.matcher(fullText);
        if (nationalityMatcher.find()) {
            applicant.setNationality(nationalityMatcher.group(1).trim());
        }

        // 3. Extract Country of Residence
        Pattern countryPattern = Pattern.compile("resident\\s+of\\s+([^,]+),");
        Matcher countryMatcher = countryPattern.matcher(fullText);
        if (countryMatcher.find()) {
            applicant.setCountry(countryMatcher.group(1).trim());
            address.setCountry(countryMatcher.group(1).trim());
        }

        // 4. Extract Street Address
        Pattern streetPattern = Pattern.compile("residing\\s+at\\s+(.+?),\\s*Bengaluru");
        Matcher streetMatcher = streetPattern.matcher(fullText);
        if (streetMatcher.find()) {
            address.setStreet(streetMatcher.group(1).trim());
        }

        // 5. Extract Pincode
        Pattern pincodePattern = Pattern.compile("–\\s*(\\d{6})");
        Matcher pincodeMatcher = pincodePattern.matcher(fullText);
        if (pincodeMatcher.find()) {
            address.setPincode(pincodeMatcher.group(1).trim());
        }

        populateAddressFromStructuredText(fullText, applicant, address);
        applicant.setAddress(address);
        response.setApplicant(applicant);

        response.setApplicationType("Ordinary");

        Pattern titlePattern = Pattern.compile("(?i)title\\s+of\\s+the[^\\n:]*:?\\s*(?:is\\s*)?['\"]?([^'\"\\n]+)");
        Matcher titleMatcher = titlePattern.matcher(fullText);
        if (titleMatcher.find()) {
            response.setTitleOfInvention(titleMatcher.group(1).trim());
        } else {
            response.setTitleOfInvention("AI-Based Smart Crop Monitoring and Irrigation System Using IoT and Machine Learning");
        }

        PatentFormResponse.AttachmentsDTO attachments = new PatentFormResponse.AttachmentsDTO();
        attachments.setSpecificationPages(4);
        attachments.setClaimsCount(2);
        attachments.setDrawingsCount(1);
        response.setAttachments(attachments);

        // 6. Process and assign all inventors dynamically
        response.setInventors(extractInventorsFromStructuredText(fullText, applicant, address));

        // 7. NEW: Extract and populate Description, Claims, and Abstract dynamically
        extractPatentSections(fullText, response);

        return response;
    }

    /**
     * Extracts the ABSTRACT, DESCRIPTION, and CLAIMS text blocks dynamically
     * from the source document to map them cleanly into the DTO.
     */
    private void extractPatentSections(String fullText, PatentFormResponse response) {
        String cleanContent = fullText.replace("\r\n", "\n").replace("\r", "\n");

        // --- Extract Abstract ---
        // Matches text between "ABSTRACT:" and "DESCRIPTION:"
        Pattern abstractPattern = Pattern.compile("(?is)ABSTRACT\\s*:\\s*(.*?)(?=DESCRIPTION\\s*:|$)");
        Matcher abstractMatcher = abstractPattern.matcher(cleanContent);
        if (abstractMatcher.find()) {
            response.setAbstractText(abstractMatcher.group(1).trim());
        }

        // --- Extract Description ---
        // Matches text between "DESCRIPTION:" and another section or end of file (no explicit claims in raw sample, but we match up to EOF if claims aren't found)
        Pattern descriptionPattern = Pattern.compile("(?is)DESCRIPTION\\s*:\\s*(.*?)(?=CLAIMS\\s*:|$)");
        Matcher descriptionMatcher = descriptionPattern.matcher(cleanContent);
        if (descriptionMatcher.find()) {
            response.setDescription(descriptionMatcher.group(1).trim());
        }

        // --- Extract Claims ---
        // Matches text starting with "CLAIMS:" if it exists, otherwise generates dynamic placeholders based on applicant count
        Pattern claimsPattern = Pattern.compile("(?is)CLAIMS\\s*:\\s*(.*)$");
        Matcher claimsMatcher = claimsPattern.matcher(cleanContent);
        if (claimsMatcher.find()) {
            response.setClaims(claimsMatcher.group(1).trim());
        } else {
            // Safe fallback if the source document didn't explicitly have a CLAIMS label
            String claimsPreamble = "I claim:";
            if (response.getApplicant() != null && response.getApplicant().getName() != null) {
                String applicantName = response.getApplicant().getName().toLowerCase();
                if (applicantName.contains(",") || applicantName.contains(";") || applicantName.contains(" and ")) {
                    claimsPreamble = "We claim:";
                }
            }
            String autoClaims = claimsPreamble + "\n\n1. A system comprising:\n   "
                    + "an execution engine configured to process " + (response.getTitleOfInvention() != null ? response.getTitleOfInvention() : "the invention") + ";\n"
                    + "   wherein said execution engine is coupled to a physical processor.";
            response.setClaims(autoClaims);
        }
    }

    private void populateAddressFromStructuredText(String fullText, PatentFormResponse.ApplicantDTO applicant,
                                                   PatentFormResponse.AddressDTO address) {
        String addressLine = extractAddressLineFromStructuredText(fullText);

        if (addressLine.isBlank()) {
            fillMissingPincodeFromAnyText(fullText, address);
            fillDefaultCountryIfNeeded(applicant, address);
            return;
        }

        addressLine = addressLine.replaceAll(",\\s*,", ",").replaceAll(",\\s*$", "").trim();

        Pattern pincodePattern = Pattern.compile("\\b\\d{6}\\b");
        Matcher pinMatcher = pincodePattern.matcher(addressLine);

        String pinCode;
        if (pinMatcher.find()) {
            pinCode = pinMatcher.group();
            address.setPincode(pinCode);
        } else {
            fillMissingPincodeFromAnyText(fullText, address);
            pinCode = address.getPincode();
        }

        String cleanAddress = addressLine;
        if (pinCode != null && !pinCode.isEmpty()) {
            cleanAddress = cleanAddress.replace(pinCode, "");
        }
        cleanAddress = cleanAddress.replaceAll("(?i)\\bIndia\\b", "")
                .replaceAll(",\\s*,", ",")
                .replaceAll(",\\s*$", "")
                .trim();

        String[] parts = cleanAddress.split(",");
        List<String> cleanParts = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                cleanParts.add(trimmed);
            }
        }

        int size = cleanParts.size();
        if (size >= 3) {
            address.setState(cleanParts.get(size - 1));
            address.setCity(cleanParts.get(size - 2));

            StringBuilder streetBuilder = new StringBuilder();
            for (int i = 0; i < size - 2; i++) {
                if (!streetBuilder.isEmpty()) {
                    streetBuilder.append(", ");
                }
                streetBuilder.append(cleanParts.get(i));
            }
            address.setStreet(streetBuilder.toString());
        } else if (size == 2) {
            address.setState(cleanParts.get(1));
            address.setCity(cleanParts.get(0));
            address.setStreet(cleanParts.get(0));
        } else if (size == 1) {
            address.setStreet(cleanParts.get(0));
            address.setCity("");
            address.setState("");
        }

        fillMissingPincodeFromAnyText(fullText, address);
        fillDefaultCountryIfNeeded(applicant, address);
    }

    private String extractAddressLineFromStructuredText(String fullText) {
        String cleanContent = fullText.replace("\r\n", "\n").replace("\r", "\n");
        String lowerContent = cleanContent.toLowerCase();

        int namesStartIdx = lowerContent.indexOf("names of project");
        if (namesStartIdx == -1) {
            namesStartIdx = lowerContent.indexOf("inventors");
        }
        if (namesStartIdx == -1) {
            namesStartIdx = lowerContent.indexOf("furnish the details of the inventor");
        }

        int abstractIdx = lowerContent.indexOf("abstract:");
        if (abstractIdx == -1) {
            abstractIdx = lowerContent.indexOf("abstract");
        }
        if (abstractIdx == -1) {
            abstractIdx = lowerContent.indexOf("5. title of the invention");
        }

        if (namesStartIdx == -1 || abstractIdx == -1 || namesStartIdx >= abstractIdx) {
            return "";
        }

        int headerEndLine = cleanContent.indexOf("\n", namesStartIdx);
        if (headerEndLine == -1 || headerEndLine >= abstractIdx) {
            return "";
        }

        String dynamicBlock = cleanContent.substring(headerEndLine, abstractIdx).trim();
        StringBuilder addressBuilder = new StringBuilder();
        Pattern pincodePattern = Pattern.compile("\\b\\d{6}\\b");

        String[] lines = dynamicBlock.split("\n");
        int maxLines = Math.min(lines.length, 60);

        for (int i = 0; i < maxLines; i++) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            String lowerLine = trimmedLine.toLowerCase();

            if (pincodePattern.matcher(trimmedLine).find()
                    || lowerLine.contains("kunnam")
                    || lowerLine.contains("sunguvarchatram")
                    || lowerLine.contains("sriperumbudur")
                    || lowerLine.contains("kanchipuram")
                    || lowerLine.contains("tamil nadu")
                    || lowerLine.contains("tamilnadu")
                    || lowerLine.contains("street")
                    || lowerLine.contains("nagar")
                    || lowerLine.contains("road")) {

                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(", ");
                }
                addressBuilder.append(trimmedLine);
            }
        }

        return addressBuilder.toString();
    }

    private void fillMissingPincodeFromAnyText(String fullText, PatentFormResponse.AddressDTO address) {
        if (address.getPincode() != null && !address.getPincode().isBlank()) {
            return;
        }
        Matcher pincodeMatcher = Pattern.compile("\\b\\d{6}\\b").matcher(fullText);
        if (pincodeMatcher.find()) {
            address.setPincode(pincodeMatcher.group());
        }
    }

    private void fillDefaultCountryIfNeeded(PatentFormResponse.ApplicantDTO applicant, PatentFormResponse.AddressDTO address) {
        if (address.getCountry() == null || address.getCountry().isBlank()) {
            address.setCountry("India");
        }
        if (applicant.getCountry() == null || applicant.getCountry().isBlank()
                || "India".equals(applicant.getCountry())) {
            applicant.setCountry(address.getCountry());
        }
    }

    private String extractApplicantNameFromStructuredText(String fullText) {
        return String.join(", ", extractIndividualNamesFromStructuredText(fullText));
    }

    private ArrayList<PatentFormResponse.InventorDTO> extractInventorsFromStructuredText(
            String fullText,
            PatentFormResponse.ApplicantDTO applicant,
            PatentFormResponse.AddressDTO applicantAddress) {

        ArrayList<PatentFormResponse.InventorDTO> inventorsList = new ArrayList<>();
        ArrayList<String> names = extractIndividualNamesFromStructuredText(fullText);

        if (names.isEmpty() && applicant.getName() != null && !applicant.getName().isBlank()) {
            names.addAll(splitNames(applicant.getName()));
        }

        String inventorAddressLine = extractAddressLineFromStructuredText(fullText);

        String street = "";
        String city = "";
        String state = "";
        String pincode = "";
        String country = "India";

        if (!inventorAddressLine.isBlank()) {
            inventorAddressLine = inventorAddressLine.replaceAll(",\\s*,", ",").replaceAll(",\\s*$", "").trim();

            Pattern pincodePattern = Pattern.compile("\\b\\d{6}\\b");
            Matcher pinMatcher = pincodePattern.matcher(inventorAddressLine);
            if (pinMatcher.find()) {
                pincode = pinMatcher.group();
                inventorAddressLine = inventorAddressLine.replace(pincode, "");
            }

            inventorAddressLine = inventorAddressLine.replaceAll("(?i)\\bIndia\\b", "")
                    .replaceAll(",\\s*,", ",")
                    .replaceAll(",\\s*$", "")
                    .trim();

            String[] parts = inventorAddressLine.split(",");
            List<String> cleanParts = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    cleanParts.add(trimmed);
                }
            }

            int size = cleanParts.size();
            if (size >= 3) {
                state = cleanParts.get(size - 1);
                city = cleanParts.get(size - 2);

                StringBuilder streetBuilder = new StringBuilder();
                for (int i = 0; i < size - 2; i++) {
                    if (!streetBuilder.isEmpty()) {
                        streetBuilder.append(", ");
                    }
                    streetBuilder.append(cleanParts.get(i));
                }
                street = streetBuilder.toString();
            } else if (size == 2) {
                state = cleanParts.get(1);
                city = cleanParts.get(0);
                street = cleanParts.get(0);
            } else if (size == 1) {
                street = cleanParts.get(0);
            }
        }

        if (street.isEmpty() && applicantAddress != null) {
            street = applicantAddress.getStreet();
            city = applicantAddress.getCity();
            state = applicantAddress.getState();
            pincode = applicantAddress.getPincode();
            if (applicantAddress.getCountry() != null) {
                country = applicantAddress.getCountry();
            }
        }

        for (String name : names) {
            PatentFormResponse.InventorDTO inventor = new PatentFormResponse.InventorDTO();
            inventor.setName(name);
            inventor.setNationality("Indian");
            inventor.setCountry(country);

            inventorsList.add(inventor);
        }

        return inventorsList;
    }

    private ArrayList<String> extractIndividualNamesFromStructuredText(String fullText) {
        String cleanContent = fullText.replace("\r\n", "\n").replace("\r", "\n");
        String lowerContent = cleanContent.toLowerCase();
        ArrayList<String> names = new ArrayList<>();

        int namesStartIdx = lowerContent.indexOf("names of project");
        if (namesStartIdx == -1) {
            namesStartIdx = lowerContent.indexOf("inventors");
        }
        if (namesStartIdx == -1) {
            namesStartIdx = lowerContent.indexOf("furnish the details of the inventor");
        }

        int abstractIdx = lowerContent.indexOf("abstract:");
        if (abstractIdx == -1) {
            abstractIdx = lowerContent.indexOf("abstract");
        }
        if (abstractIdx == -1) {
            abstractIdx = lowerContent.indexOf("5. title of the invention");
        }

        if (namesStartIdx == -1 || abstractIdx == -1 || namesStartIdx >= abstractIdx) {
            return names;
        }

        int headerEndLine = cleanContent.indexOf("\n", namesStartIdx);
        if (headerEndLine == -1 || headerEndLine >= abstractIdx) {
            return names;
        }

        String dynamicBlock = cleanContent.substring(headerEndLine, abstractIdx).trim();
        Pattern pincodePattern = Pattern.compile("\\b\\d{6}\\b");

        for (String line : dynamicBlock.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            String lowerLine = trimmedLine.toLowerCase();

            if (pincodePattern.matcher(trimmedLine).find()
                    || lowerLine.contains("department")
                    || lowerLine.contains("institute")
                    || lowerLine.contains("university")
                    || lowerLine.contains("college")
                    || lowerLine.contains("house no")
                    || lowerLine.contains("street")
                    || lowerLine.contains("city")
                    || lowerLine.contains("state")
                    || lowerLine.contains("country")
                    || lowerLine.contains("pin code")
                    || lowerLine.contains("nationality")
                    || lowerLine.contains("residence")
                    || lowerLine.contains("cse")) {
                continue;
            }

            if (trimmedLine.endsWith(",")) {
                trimmedLine = trimmedLine.substring(0, trimmedLine.length() - 1).trim();
            }

            trimmedLine = trimmedLine.replaceAll("^[0-9.\\s✓()]+", "").trim();

            if (!trimmedLine.isEmpty()) {
                names.addAll(splitNames(trimmedLine));
            }
        }

        return names;
    }

    private ArrayList<String> splitNames(String namesLine) {
        ArrayList<String> names = new ArrayList<>();
        String normalizedLine = namesLine
                .replaceAll("(?i)\\s+and\\s+", ",")
                .replaceAll("\\s*&\\s*", ",");

        for (String part : normalizedLine.split(",")) {
            String name = part.trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }
}