package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Form2GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form2GeneratorService.class);

    /**
     * Generates the byte array for Form 2 (Complete Specification) using Apache POI
     * @param data The parsed patent data structure
     * @return Generated .docx byte array
     */
    public byte[] generateForm2(PatentFormResponse data) {
        ClassPathResource resource = new ClassPathResource("Form2_Template.docx");

        if (!resource.exists()) {
            logger.error("Form2_Template.docx was not found inside resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream(); XWPFDocument document = new XWPFDocument(is)) {

            Map<String, String> replacements = buildReplacementsMap(data);

            // 1. Process standalone paragraphs
            if (document.getParagraphs() != null) {
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    replacePlaceholdersInParagraph(paragraph, replacements);
                }
            }

            // 2. Process table cells
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                replacePlaceholdersInParagraph(paragraph, replacements);
                            }
                        }
                    }
                }
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                document.write(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            logger.error("CRITICAL ERROR DURING FORM 2 GENERATION", e);
            return new byte[0];
        }
    }

    /**
     * Maps the template placeholder tokens to the values dynamically extracted in the DTO
     */
    private Map<String, String> buildReplacementsMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();

        // 1. Title
        map.put("{title}", data.getTitleOfInvention() != null ? data.getTitleOfInvention() : "");

        // 2. Applicant Info
        if (data.getApplicant() != null) {
            PatentFormResponse.ApplicantDTO applicant = data.getApplicant();
            map.put("{applicantName}", applicant.getName() != null ? applicant.getName() : "");
            map.put("{applicantNationality}", applicant.getNationality() != null ? applicant.getNationality() : "Indian");

            if (applicant.getAddress() != null) {
                PatentFormResponse.AddressDTO applicantAddress = applicant.getAddress();
                String baseInstitution = "Department of CSE, Jeppiaar Institute of Technology, ";

                String fullAddress = baseInstitution
                        + (applicantAddress.getStreet() != null && !applicantAddress.getStreet().isEmpty() ? applicantAddress.getStreet() + ", " : "")
                        + (applicantAddress.getCity() != null && !applicantAddress.getCity().isEmpty() ? applicantAddress.getCity() + ", " : "")
                        + (applicantAddress.getState() != null && !applicantAddress.getState().isEmpty() ? applicantAddress.getState() + ", " : "")
                        + (applicantAddress.getCountry() != null && !applicantAddress.getCountry().isEmpty() ? applicantAddress.getCountry() : "India")
                        + " - " + (applicantAddress.getPincode() != null && !applicantAddress.getPincode().isEmpty() ? applicantAddress.getPincode() : "631604");

                map.put("{applicantAddress}", fullAddress);
            } else {
                map.put("{applicantAddress}", "Department of CSE, Jeppiaar Institute of Technology, India - 631604");
            }
        } else {
            map.put("{applicantName}", "");
            map.put("{applicantNationality}", "Indian");
            map.put("{applicantAddress}", "");
        }

        // 3. Dynamic Description Section
        String descriptionText = (data.getDescription() != null && !data.getDescription().trim().isEmpty())
                ? data.getDescription()
                : "No description provided.";
        map.put("{description}", descriptionText);

        // 4. Dynamic Claims Section
        String claimsText = (data.getClaims() != null && !data.getClaims().trim().isEmpty())
                ? data.getClaims()
                : "No claims provided.";
        map.put("{claims}", claimsText);

        // 5. Dynamic Abstract Section
        String abstractTextValue = (data.getAbstractText() != null && !data.getAbstractText().trim().isEmpty())
                ? data.getAbstractText()
                : "No abstract provided.";
        map.put("{abstract}", abstractTextValue);

        return map;
    }

    private void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        if (paragraph == null) return;
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        StringBuilder consolidatedText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) {
                consolidatedText.append(text);
            }
        }

        String paragraphText = consolidatedText.toString();
        boolean replacedAny = false;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String target = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            if (paragraphText.contains(target)) {
                paragraphText = paragraphText.replace(target, value);
                replacedAny = true;
            }
        }

        if (replacedAny) {
            XWPFRun baseRun = runs.get(0);
            String fontFamily = baseRun.getFontFamily();
            Double fontSize = baseRun.getFontSizeAsDouble();
            boolean isBold = baseRun.isBold();
            boolean isItalic = baseRun.isItalic();
            String color = baseRun.getColor();

            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            XWPFRun newRun = paragraph.createRun();
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (fontSize != null && fontSize > 0) newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
            newRun.setItalic(isItalic);
            if (color != null) newRun.setColor(color);

            String[] lines = paragraphText.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                newRun.setText(lines[i]);
                if (i < lines.length - 1) {
                    newRun.addCarriageReturn();
                }
            }
        }
    }
}