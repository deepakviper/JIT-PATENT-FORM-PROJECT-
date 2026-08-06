package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
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
                // Loop through a copy since we will be inserting new paragraphs
                List<XWPFParagraph> parasCopy = new ArrayList<>(document.getParagraphs());
                for (XWPFParagraph paragraph : parasCopy) {
                    replacePlaceholdersInParagraphWithLayout(document, paragraph, replacements);
                }
            }

            // 2. Process table cells
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            List<XWPFParagraph> cellParasCopy = new ArrayList<>(cell.getParagraphs());
                            for (XWPFParagraph paragraph : cellParasCopy) {
                                replacePlaceholdersInParagraphWithLayout(document, paragraph, replacements);
                            }
                        }
                    }
                }
            }

            // 3. Append saved drawings
            appendSavedDrawings(document);

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
                List<String> parts = new ArrayList<>();
                if (applicantAddress.getHouseNo() != null && !applicantAddress.getHouseNo().isBlank()) parts.add(applicantAddress.getHouseNo().trim());
                if (applicantAddress.getStreet() != null && !applicantAddress.getStreet().isBlank()) parts.add(applicantAddress.getStreet().trim());
                if (applicantAddress.getAreaLocality() != null && !applicantAddress.getAreaLocality().isBlank()) parts.add(applicantAddress.getAreaLocality().trim());
                if (applicantAddress.getVillageTown() != null && !applicantAddress.getVillageTown().isBlank()) parts.add(applicantAddress.getVillageTown().trim());
                if (applicantAddress.getCity() != null && !applicantAddress.getCity().isBlank()) parts.add(applicantAddress.getCity().trim());
                if (applicantAddress.getDistrict() != null && !applicantAddress.getDistrict().isBlank()) parts.add(applicantAddress.getDistrict().trim());
                if (applicantAddress.getState() != null && !applicantAddress.getState().isBlank()) parts.add(applicantAddress.getState().trim());
                if (applicantAddress.getCountry() != null && !applicantAddress.getCountry().isBlank()) parts.add(applicantAddress.getCountry().trim());
                
                String fullAddress = String.join(", ", parts);
                if (applicantAddress.getPincode() != null && !applicantAddress.getPincode().isBlank()) {
                    if (!fullAddress.isEmpty()) {
                        fullAddress += " - " + applicantAddress.getPincode().trim();
                    } else {
                        fullAddress = applicantAddress.getPincode().trim();
                    }
                }
                map.put("{applicantAddress}", fullAddress);
            } else {
                map.put("{applicantAddress}", "");
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

    private void replacePlaceholdersInParagraphWithLayout(XWPFDocument document, XWPFParagraph paragraph, Map<String, String> replacements) {
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

        if (paragraphText.contains("{description}")) {
            replaceWithMultipleParagraphs(document, paragraph, replacements.get("{description}"));
            return;
        } else if (paragraphText.contains("{claims}")) {
            replaceWithMultipleParagraphs(document, paragraph, replacements.get("{claims}"));
            return;
        } else if (paragraphText.contains("{abstract}")) {
            replaceWithMultipleParagraphs(document, paragraph, replacements.get("{abstract}"));
            return;
        }

        replacePlaceholdersInParagraph(paragraph, replacements);
    }

    private void replaceWithMultipleParagraphs(XWPFDocument document, XWPFParagraph targetPara, String text) {
        if (text == null) text = "";
        String[] lines = text.split("\n");
        XWPFParagraph currentPara = targetPara;
        
        while (currentPara.getRuns().size() > 0) {
            currentPara.removeRun(0);
        }

        boolean firstLineWritten = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            
            XWPFParagraph activePara;
            if (!firstLineWritten) {
                activePara = currentPara;
                firstLineWritten = true;
            } else {
                XmlCursor cursor = currentPara.getCTP().newCursor();
                if (cursor.toNextSibling()) {
                    activePara = document.insertNewParagraph(cursor);
                } else {
                    activePara = document.createParagraph();
                }
                activePara.setStyle(targetPara.getStyle());
                activePara.setSpacingAfter(targetPara.getSpacingAfter());
                activePara.setSpacingBefore(targetPara.getSpacingBefore());
            }
            
            XWPFRun run = activePara.createRun();
            run.setFontFamily("Times New Roman");
            run.setFontSize(12);
            
            if (isHeading(line)) {
                run.setBold(true);
            }
            
            run.setText(line);
            currentPara = activePara;
        }
    }

    private boolean isHeading(String line) {
        String trimmed = line.trim();
        if (trimmed.length() > 80) {
            return false;
        }
        
        String clean = trimmed.replaceAll("^[0-9.\\s]+", "").replaceAll("[:.\\s]+$", "").trim();
        if (clean.isEmpty()) {
            return false;
        }
        
        boolean allCaps = clean.matches("^[A-Z0-9\\s&(),/\\-]+$") && clean.chars().anyMatch(Character::isLetter);
        if (allCaps) {
            return true;
        }
        
        String lower = clean.toLowerCase();
        return lower.equals("field of the invention")
                || lower.equals("field of invention")
                || lower.equals("background of the invention")
                || lower.equals("background of invention")
                || lower.equals("object of the invention")
                || lower.equals("summary of the invention")
                || lower.equals("summary of invention")
                || lower.equals("detailed description")
                || lower.equals("detailed description of the invention")
                || lower.equals("brief description of drawings")
                || lower.equals("brief description of the drawings")
                || lower.equals("claims")
                || lower.equals("abstract");
    }

    private void appendSavedDrawings(XWPFDocument document) {
        java.io.File dir = new java.io.File("temp_images");
        if (!dir.exists()) {
            return;
        }
        java.io.File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        XWPFParagraph headingPara = document.createParagraph();
        headingPara.setSpacingBefore(480);
        headingPara.setSpacingAfter(240);
        XWPFRun headingRun = headingPara.createRun();
        headingRun.setFontFamily("Times New Roman");
        headingRun.setFontSize(14);
        headingRun.setBold(true);
        headingRun.setText("DRAWINGS / FIGURES");

        for (java.io.File f : files) {
            String name = f.getName().toLowerCase();
            int picType = XWPFDocument.PICTURE_TYPE_PNG;
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                picType = XWPFDocument.PICTURE_TYPE_JPEG;
            } else if (name.endsWith(".gif")) {
                picType = XWPFDocument.PICTURE_TYPE_GIF;
            }

            XWPFParagraph imgPara = document.createParagraph();
            imgPara.setAlignment(ParagraphAlignment.CENTER);
            imgPara.setSpacingAfter(240);
            
            XWPFRun imgRun = imgPara.createRun();
            
            try (java.io.InputStream is = new java.io.FileInputStream(f)) {
                // Word drawing size standard dimensions (400px wide, 300px high in EMUs)
                imgRun.addPicture(is, picType, f.getName(), 3810000, 2857500);
            } catch (Exception e) {
                logger.error("Failed to insert drawing: " + f.getName(), e);
            }

            XWPFParagraph captionPara = document.createParagraph();
            captionPara.setAlignment(ParagraphAlignment.CENTER);
            captionPara.setSpacingAfter(240);
            XWPFRun captionRun = captionPara.createRun();
            captionRun.setFontFamily("Times New Roman");
            captionRun.setFontSize(10);
            captionRun.setItalic(true);
            
            String figureNumber = f.getName().replaceAll("^image_", "").replaceAll("\\.[a-zA-Z0-9]+$", "");
            try {
                int num = Integer.parseInt(figureNumber) + 1;
                captionRun.setText("Figure " + num);
            } catch (Exception e) {
                captionRun.setText(f.getName());
            }
        }
    }
}