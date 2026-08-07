package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Form3GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form3GeneratorService.class);

    public byte[] generateForm3(PatentFormResponse data) {
        ClassPathResource resource = new ClassPathResource("Form-3.docx");

        if (!resource.exists()) {
            logger.error("❌ CRITICAL: Form-3.docx was NOT found inside src/main/resources/");
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
                            String cellText = cell.getText().toLowerCase();
                            if (cellText.contains("controller of patents") && !cellText.contains("undertake")) {
                                String branch = replacements.get("{patentOfficeBranch}");
                                cleanControllerAddressCell(cell, branch);
                                continue;
                            }
                            
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
            logger.error("❌ CRITICAL EXCEPTION DURING PROCESSING:", e);
            return new byte[0];
        }
    }

    private Map<String, String> buildReplacementsMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 1. Primary Applicant Names (stacked vertically via newline)
        String primaryApplicant = "";
        if (data.getApplicant() != null && data.getApplicant().getName() != null) {
            primaryApplicant = data.getApplicant().getName().replaceAll(",\\s*", "\n").trim();
        }
        map.put("{applicantName}", primaryApplicant.isEmpty() ? "N/A" : primaryApplicant);

        // 2. Extract and format the Inventor Names and Details cleanly
        String inventorNamesOnly = "N/A";
        String inventorFullDetails = "N/A";

        if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            // Extract just the names (e.g., "Merlin Shiny J\nVaidegi D")
            inventorNamesOnly = data.getInventors().stream()
                    .map(inv -> inv.getName())
                    .collect(Collectors.joining("\n"));

            // Extract full details with nationality and address
            inventorFullDetails = data.getInventors().stream()
                    .map(inv -> inv.getName() + " (" + inv.getNationality() + ", Resident of " + inv.getCountry() + ")")
                    .collect(Collectors.joining("\n"));
        }

        // Set the standard joint applicant block to the full details
        map.put("{jointApplicants}", inventorFullDetails);

        // 💡 3. THE FIX: Map the Assignee tags to display the INVENTORS' data instead!
        map.put("{assigneeName}", inventorNamesOnly);
        map.put("{assigneeDetails}", inventorFullDetails);
        map.put("{assigneeAddress}", inventorFullDetails); // Backwards compatibility fallback

        // Standard formatting fallbacks
        map.put("{baseApplicationNo}", "____________________");
        map.put("{baseApplicationDate}", "____________________");
        map.put("{country}", "N/A");
        map.put("{appDate}", "N/A");
        map.put("{fAppNo}", "N/A");
        map.put("{appStatus}", "N/A");
        map.put("{pubDate}", "N/A");
        map.put("{grantDate}", "N/A");

        // 4. Execution Dates
        map.put("{currentDay}", String.valueOf(today.getDayOfMonth()));
        map.put("{currentMonth}", today.format(DateTimeFormatter.ofPattern("MMMM")));
        map.put("{currentYear}", String.valueOf(today.getYear()).substring(Math.max(0, String.valueOf(today.getYear()).length() - 2)));

        String signName = "N/A";
        if (data.getApplicant() != null && data.getApplicant().getName() != null) {
            String[] splitNames = data.getApplicant().getName().split(",");
            if (splitNames.length > 0) {
                signName = splitNames[0].trim();
            }
        }
        map.put("{signatureName}", signName);
        map.put("{patentOfficeBranch}", PatentOfficeHelper.determineBranch(data));

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

        // 1. Clean up and format the Date block
        if (paragraphText.contains("Dated this") || paragraphText.toLowerCase().contains("dated this")) {
            String day = replacements.get("{currentDay}");
            String month = replacements.get("{currentMonth}");
            String year = replacements.get("{currentYear}");
            if (day != null && month != null && year != null) {
                if (year.length() == 2) {
                    year = "20" + year;
                }
                
                // Replace 20{currentYear} first to avoid 202026 duplication
                paragraphText = paragraphText.replace("20{currentYear}", year);
                paragraphText = paragraphText.replace("{currentDay}", day);
                paragraphText = paragraphText.replace("{currentMonth}", month);
                paragraphText = paragraphText.replace("{currentYear}", year);
                
                // Replace raw underscore line: "Dated this______day of_______20"
                paragraphText = paragraphText.replaceAll("(?i)Dated this\\s*[_\\s]+day of\\s*[_\\s]+20\\d*", "Dated this " + day + " day of " + month + ", " + year);
                paragraphText = paragraphText.replaceAll("(?i)Dated this\\s*[_\\s]*day of\\s*[_\\s]*20", "Dated this " + day + " day of " + month + ", " + year);
                replacedAny = true;
            }
        }

        // 2. Apply standard map replacements
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (paragraphText.contains(entry.getKey())) {
                paragraphText = paragraphText.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
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

            // Clear existing layout fragments completely
            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            // Zero out standard margins to avoid vertical swelling inside the table cell
            paragraph.setSpacingBefore(0);
            paragraph.setSpacingAfter(0);

            XWPFRun newRun = paragraph.createRun();
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (fontSize != null && fontSize > 0) newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
            newRun.setItalic(isItalic);
            if (color != null) newRun.setColor(color);

            // Split text blocks dynamically by the newline identifier
            String[] lines = paragraphText.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                newRun.setText(lines[i]);
                if (i < lines.length - 1) {
                    newRun.addCarriageReturn(); // Breaks layout vertically within the same container block
                }
            }
        }
    }

    private void cleanControllerAddressCell(XWPFTableCell cell, String branch) {
        if (branch == null || branch.isEmpty()) {
            branch = "Chennai";
        }
        
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }
        
        XWPFParagraph p1 = cell.getParagraphs().get(0);
        while (p1.getRuns().size() > 0) {
            p1.removeRun(0);
        }
        XWPFRun r1 = p1.createRun();
        r1.setFontFamily("Times New Roman");
        r1.setFontSize(12);
        r1.setText("To");
        
        XWPFParagraph p2 = cell.addParagraph();
        p2.setSpacingBefore(0);
        p2.setSpacingAfter(0);
        XWPFRun r2 = p2.createRun();
        r2.setFontFamily("Times New Roman");
        r2.setFontSize(12);
        r2.setText("The Controller of Patents");
        
        XWPFParagraph p3 = cell.addParagraph();
        p3.setSpacingBefore(0);
        p3.setSpacingAfter(0);
        XWPFRun r3 = p3.createRun();
        r3.setFontFamily("Times New Roman");
        r3.setFontSize(12);
        r3.setText("The Patent Office, at " + branch);
    }
}