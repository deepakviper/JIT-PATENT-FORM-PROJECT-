package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentGeneratorService {

    public byte[] processAndGenerateForm(String rawInputText) throws Exception {
        PatentFormResponse data = extractDataFromRawText(rawInputText);
        return generateFilledForm1(data);
    }

    public PatentFormResponse extractDataFromRawText(String fileContent) {
        PatentFormResponse response = new PatentFormResponse();
        PatentFormResponse.ApplicantDTO applicant = response.getApplicant();

        String cleanContent = fileContent.replace("\r\n", "\n").replace("\r", "\n");
        String lowerContent = cleanContent.toLowerCase();

        // 1. TITLE EXTRACTION
        Pattern titlePattern = Pattern.compile(
                "(?i)title of the[^\\n:]*:\\s*(.*?)\\s*(?=\\n\\s*name|\\n\\s*abstract|$)",
                Pattern.DOTALL
        );
        Matcher titleMatcher = titlePattern.matcher(cleanContent);
        if (titleMatcher.find()) {
            response.setTitleOfInvention(titleMatcher.group(1).trim().replace("\n", " "));
        }

        // 2. MULTI-LINE NAMES & ADDRESS EXTRACTION
        int namesStartIdx = lowerContent.indexOf("names of project");
        if (namesStartIdx == -1) {
            namesStartIdx = lowerContent.indexOf("inventors");
        }
        int abstractIdx = lowerContent.indexOf("abstract:");

        if (namesStartIdx != -1 && abstractIdx != -1) {
            int headerEndLine = cleanContent.indexOf("\n", namesStartIdx);
            if (headerEndLine != -1 && headerEndLine < abstractIdx) {
                String dynamicBlock = cleanContent.substring(headerEndLine, abstractIdx).trim();
                String[] lines = dynamicBlock.split("\n");

                List<String> individualNames = new ArrayList<>();
                String addressLine = "";
                Pattern pincodePattern = Pattern.compile("\\b\\d{6}\\b");

                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) continue;

                    Matcher pinMatcher = pincodePattern.matcher(trimmedLine);

                    if (pinMatcher.find()
                            || trimmedLine.toLowerCase().contains("kunnam")
                            || trimmedLine.toLowerCase().contains("sunguvarchatram")) {
                        addressLine = trimmedLine;
                    } else if (!trimmedLine.toLowerCase().contains("department")
                            && !trimmedLine.toLowerCase().contains("institute")
                            && !trimmedLine.toLowerCase().contains("university")
                            && !trimmedLine.toLowerCase().contains("college")
                            && !trimmedLine.toLowerCase().contains("cse")) {

                        if (trimmedLine.endsWith(",")) {
                            trimmedLine = trimmedLine.substring(0, trimmedLine.length() - 1).trim();
                        }
                        individualNames.add(trimmedLine);
                    }
                }

                applicant.setName(String.join(", ", individualNames));

                PatentFormResponse.AddressDTO address = applicant.getAddress();
                String street = "";
                String city = "";
                String state = "";
                String pincode = "631604";
                String country = "India";

                if (!addressLine.isEmpty()) {
                    String[] addressParts = addressLine.split(",");
                    for (int i = 0; i < addressParts.length; i++) {
                        addressParts[i] = addressParts[i].trim();
                    }

                    if (addressParts.length >= 4) {
                        StringBuilder streetBuilder = new StringBuilder();
                        for (int i = 0; i < addressParts.length - 3; i++) {
                            if (!streetBuilder.isEmpty()) streetBuilder.append(", ");
                            streetBuilder.append(addressParts[i]);
                        }
                        street = streetBuilder.toString();
                        city = addressParts[addressParts.length - 3];
                        state = addressParts[addressParts.length - 2];

                        String tailSegment = addressParts[addressParts.length - 1];
                        Matcher finalPinMatcher = pincodePattern.matcher(tailSegment);
                        if (finalPinMatcher.find()) {
                            pincode = finalPinMatcher.group();
                        }
                    } else {
                        street = addressLine;
                    }

                    address.setStreet(street);
                    address.setCity(city);
                    address.setState(state);
                    address.setPincode(pincode);
                    address.setCountry(country);

                    applicant.setNationality("Indian");
                    applicant.setCountry(country);
                }

                List<PatentFormResponse.InventorDTO> inventorsList = new ArrayList<>();
                for (String name : individualNames) {
                    PatentFormResponse.InventorDTO inventor = new PatentFormResponse.InventorDTO();
                    inventor.setName(name);
                    inventor.setNationality("Indian");
                    inventor.setCountry(country);
                    inventorsList.add(inventor);
                }
                response.setInventors(inventorsList);
            }
        }
        return response;
    }

    public byte[] generateFilledForm1(PatentFormResponse data) throws Exception {

        System.out.println("📥 BACKEND RECEIVED INVENTORS: " +
                (data.getInventors() != null ? data.getInventors().size() : 0));
        if (data.getInventors() != null) {
            for (int i = 0; i < data.getInventors().size(); i++) {
                System.out.println("📥 INVENTOR " + (i + 1) + ": " +
                        data.getInventors().get(i).getName());
            }
        }
        ClassPathResource resource = new ClassPathResource("Form1mai.docx");

        if (!resource.exists()) {
            System.out.println("❌ ERROR: Form1mai.docx was not found inside resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            // 1. Process standalone paragraphs (outside tables)
            if (document.getParagraphs() != null) {
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    processParagraph(paragraph, data, null);
                }
            }

            // 2. Process all tables with hybrid inventor-aware routing
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    processInventorAwareTable(table, data);
                }
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                document.write(bos);
                return bos.toByteArray();
            }

        } catch (Exception e) {
            System.out.println("❌ CRITICAL ERROR DURING DOC GENERATION:");
            e.printStackTrace();
            return new byte[0];
        }
    }

    // -------------------------------------------------------------------------
    // TABLE PROCESSING
    // -------------------------------------------------------------------------

    private void processInventorAwareTable(XWPFTable table, PatentFormResponse data) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows == null || rows.isEmpty()) return;

        int nameRowIndex   = findRowIndexContainingToken(rows, "{{INV_NAME}}");
        int streetRowIndex = findRowIndexContainingToken(rows, "{{INV_STREET}}");

        boolean hasInventorNameToken       = nameRowIndex != -1;
        boolean isUnifiedSingleRowTemplate = hasInventorNameToken
                && streetRowIndex != -1
                && streetRowIndex == nameRowIndex;

        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);

            if (isUnifiedSingleRowTemplate && r == nameRowIndex) {
                r = processUnifiedInventorRow(table, r, row, data);
            } else {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        processParagraph(paragraph, data, null);
                    }
                }
            }
        }
    }

    private int processUnifiedInventorRow(XWPFTable table, int rowIndex,
                                          XWPFTableRow sourceRow, PatentFormResponse data) {
        List<PatentFormResponse.InventorDTO> inventors = data.getInventors();

        if (inventors == null || inventors.isEmpty()) {
            for (XWPFTableCell cell : sourceRow.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    processParagraph(p, data, null);
                }
            }
            return rowIndex;
        }

        String pristineRowXml = sourceRow.getCtRow().xmlText();
        int insertIndex = rowIndex;

        for (int i = 0; i < inventors.size(); i++) {
            XWPFTableRow targetRow;

            if (i == 0) {
                targetRow = sourceRow;
            } else {
                insertIndex++;
                targetRow = table.insertNewTableRow(insertIndex);

                try {
                    CTRow clonedCtRow = CTRow.Factory.parse(pristineRowXml);
                    targetRow.getCtRow().set(clonedCtRow);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deep-clone inventor row XML", e);
                }

                targetRow = new XWPFTableRow(targetRow.getCtRow(), table);
                table.getRows().set(insertIndex, targetRow);
            }

            for (XWPFTableCell cell : targetRow.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    processParagraph(p, data, inventors.get(i));
                }
            }
        }

        return insertIndex;
    }

    private int findRowIndexContainingToken(List<XWPFTableRow> rows, String token) {
        for (int i = 0; i < rows.size(); i++) {
            for (XWPFTableCell cell : rows.get(i).getTableCells()) {
                String text = cell.getText();
                if (text != null && text.contains(token)) return i;
            }
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // PARAGRAPH PROCESSING
    // -------------------------------------------------------------------------

    private void processParagraph(XWPFParagraph paragraph,
                                  PatentFormResponse data,
                                  PatentFormResponse.InventorDTO specificInventor) {

        if (paragraph == null
                || paragraph.getText() == null
                || paragraph.getText().trim().isEmpty()) {
            return;
        }

        // 1. General meta tokens
        replaceTextInParagraph(paragraph, "{{TITLE}}", data.getTitleOfInvention());
        replaceTextInParagraph(paragraph, "{{APPLICATION_TYPE}}", data.getApplicationType());

        // 2. Applicant placeholders
        if (data.getApplicant() != null) {
            PatentFormResponse.ApplicantDTO applicant = data.getApplicant();

            replaceTextInParagraph(paragraph, "{{APP_NAME}}",    applicant.getName());
            replaceTextInParagraph(paragraph, "{{NATIONALITY}}", applicant.getNationality());
            replaceTextInParagraph(paragraph, "{{RES_CO}}",      applicant.getCountry());

            if (applicant.getAddress() != null) {
                PatentFormResponse.AddressDTO address = applicant.getAddress();
                replaceTextInParagraph(paragraph, "{{HOUSE_NO}}", "Department of CSE");
                replaceTextInParagraph(paragraph, "{{STREET}}",   address.getStreet());
                replaceTextInParagraph(paragraph, "{{CITY}}",     address.getCity());
                replaceTextInParagraph(paragraph, "{{STATE}}",    address.getState());
                replaceTextInParagraph(paragraph, "{{COUNTRY}}",  address.getCountry());
                replaceTextInParagraph(paragraph, "{{PINCODE}}",  address.getPincode());
            }
        }

        // 3. Single-inventor row mapping (Unified / cloned-row route)
        if (specificInventor != null) {
            replaceTextInParagraph(paragraph, "{{INV_NAME}}",
                    specificInventor.getName());
            replaceTextInParagraph(paragraph, "{{INV_NAT}}",
                    specificInventor.getNationality() != null ? specificInventor.getNationality() : "Indian");
            replaceTextInParagraph(paragraph, "{{INV_COUNTRY}}",
                    specificInventor.getCountry() != null ? specificInventor.getCountry() : "India");
        }
        // 4. Split-template fallback — stitch all inventors into one cell (vertical)
        else if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            List<PatentFormResponse.InventorDTO> inventors = data.getInventors();
            StringBuilder namesBuilder         = new StringBuilder();
            StringBuilder nationalitiesBuilder = new StringBuilder();
            StringBuilder countriesBuilder     = new StringBuilder();

            for (int i = 0; i < inventors.size(); i++) {
                PatentFormResponse.InventorDTO inventor = inventors.get(i);
                if (i > 0) {
                    namesBuilder.append("\n");
                    nationalitiesBuilder.append("\n");
                    countriesBuilder.append("\n");
                }
                namesBuilder.append(inventor.getName() != null ? inventor.getName() : "");
                nationalitiesBuilder.append(inventor.getNationality() != null ? inventor.getNationality() : "Indian");
                countriesBuilder.append(inventor.getCountry() != null ? inventor.getCountry() : "India");
            }

            replaceTextInParagraph(paragraph, "{{INV_NAME}}",    namesBuilder.toString());
            replaceTextInParagraph(paragraph, "{{INV_NAT}}",     nationalitiesBuilder.toString());
            replaceTextInParagraph(paragraph, "{{INV_COUNTRY}}", countriesBuilder.toString());
        }

        // 5. Shared inventor address placeholders
        if (data.getApplicant() != null && data.getApplicant().getAddress() != null) {
            PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();
            replaceTextInParagraph(paragraph, "{{INV_HOUSE_NO}}",     "Department of CSE");
            replaceTextInParagraph(paragraph, "{{INV_STREET}}",       addr.getStreet());
            replaceTextInParagraph(paragraph, "{{INV_CITY}}",         addr.getCity());
            replaceTextInParagraph(paragraph, "{{INV_STATE}}",        addr.getState());
            replaceTextInParagraph(paragraph, "{{INV_COUNTRY_ADDR}}", addr.getCountry());
            replaceTextInParagraph(paragraph, "{{INV_PIN}}",          addr.getPincode());
        }

        // 6. Principal details — from frontend input
        if (data.getPrincipal() != null) {
            PatentFormResponse.PrincipalDTO principal = data.getPrincipal();

            replaceTextInParagraph(paragraph, "{{SERVICE_NAME}}",   principal.getName());
            replaceTextInParagraph(paragraph, "{{SERVICE_TEL}}",    principal.getTelephone());
            replaceTextInParagraph(paragraph, "{{SERVICE_MOBILE}}", principal.getMobile());
            replaceTextInParagraph(paragraph, "{{SERVICE_FAX}}",    principal.getFax());
            replaceTextInParagraph(paragraph, "{{SERVICE_EMAIL}}",  principal.getEmail());

            if (data.getApplicant() != null && data.getApplicant().getAddress() != null) {
                PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();

                String fullPostalAddress = principal.getName()
                        + (addr.getStreet()  != null && !addr.getStreet().isEmpty()  ? ", " + addr.getStreet()  : "")
                        + (addr.getCity()    != null && !addr.getCity().isEmpty()    ? ", " + addr.getCity()    : "")
                        + (addr.getState()   != null && !addr.getState().isEmpty()   ? ", " + addr.getState()   : "")
                        + (addr.getCountry() != null && !addr.getCountry().isEmpty() ? ", " + addr.getCountry() : "")
                        + (addr.getPincode() != null && !addr.getPincode().isEmpty() ? " - " + addr.getPincode() : "");

                replaceTextInParagraph(paragraph, "{{SERVICE_ADDRESS}}", fullPostalAddress);
            }
        }

        // 7. Auto-generated date
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();

        replaceTextInParagraph(paragraph, "{{DAY}}",
                day + getDayOrdinalSuffix(day));
        replaceTextInParagraph(paragraph, "{{MONTH}}",
                today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)));
        replaceTextInParagraph(paragraph, "{{YEAR}}",
                today.format(DateTimeFormatter.ofPattern("yyyy")));

        // 8. ✅ Inventor signature block — HORIZONTAL format with tab spacing
        // 8. Inventor signature block
        if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            StringBuilder signaturesBuilder = new StringBuilder();
            List<PatentFormResponse.InventorDTO> inventors = data.getInventors();

            for (int i = 0; i < inventors.size(); i++) {
                if (i > 0) signaturesBuilder.append("\t");
                String name = inventors.get(i).getName();
                signaturesBuilder.append(name != null ? name.toUpperCase() : "");
            }

            // ✅ Add this debug log
            System.out.println("✅ INVENTOR SIGNATURES: " + signaturesBuilder.toString());
            System.out.println("✅ INVENTOR COUNT: " + inventors.size());

            replaceTextInParagraph(paragraph, "{{INVENTOR_NAME}}", signaturesBuilder.toString());
        }


        // 9. Document metadata — attachments
        if (data.getAttachments() != null) {
            PatentFormResponse.AttachmentsDTO attachments = data.getAttachments();
            replaceTextInParagraph(paragraph, "{{PAGES}}",
                    String.valueOf(attachments.getSpecificationPages()));
            replaceTextInParagraph(paragraph, "{{CLAIMS}}",
                    String.valueOf(attachments.getClaimsCount()));
        }
    }

    // -------------------------------------------------------------------------
    // RUN-LEVEL PLACEHOLDER REPLACEMENT
    // ✅ Updated to handle both \n (line breaks) and \t (tab spacing)
    // -------------------------------------------------------------------------

    private void replaceTextInParagraph(XWPFParagraph paragraph,
                                        String targetToken,
                                        String replacementValue) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // Stitch all runs into one string to detect tokens split across runs
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) sb.append(text);
        }

        String fullText = sb.toString();
        if (!fullText.contains(targetToken)) return;

        String valueToUse  = replacementValue != null ? replacementValue : "";
        String updatedText = fullText.replace(targetToken, valueToUse);

        // Preserve base run formatting from the first run
        XWPFRun baseRun  = runs.get(0);
        String  fontName = baseRun.getFontFamily() != null ? baseRun.getFontFamily() : "Arial";
        int     fontSize = baseRun.getFontSize() > 0      ? baseRun.getFontSize()    : 11;
        boolean isBold   = baseRun.isBold();
        String  color    = baseRun.getColor();

        // Remove all existing runs cleanly
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // ✅ Split on \n first for line breaks
        String[] lineParts = updatedText.split("\n", -1);
        for (int i = 0; i < lineParts.length; i++) {
            String linePart = lineParts[i];

            // ✅ Split each line on \t for horizontal tab spacing
            String[] tabParts = linePart.split("\t", -1);
            for (int t = 0; t < tabParts.length; t++) {
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(tabParts[t]);
                newRun.setFontFamily(fontName);
                if (fontSize > 0) newRun.setFontSize(fontSize);
                newRun.setBold(isBold);
                if (color != null) newRun.setColor(color);

                // ✅ Add real Word tab character after each segment except the last
                if (t < tabParts.length - 1) {
                    newRun.addTab();
                }
            }

            // ✅ Add real Word line break between lines except after the last line
            if (i < lineParts.length - 1) {
                XWPFRun breakRun = paragraph.createRun();
                breakRun.setFontFamily(fontName);
                breakRun.addBreak();
            }
        }
    }

    // -------------------------------------------------------------------------
    // HELPER — ORDINAL SUFFIX
    // -------------------------------------------------------------------------

    private String getDayOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        switch (day % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
}