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
import java.util.*;

@Service
public class Form9GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form9GeneratorService.class);

    // ------------------------------------------------------------------
    // MAIN ENTRY POINT
    // ------------------------------------------------------------------

    public byte[] generateForm9(PatentFormResponse data) {

        System.out.println("========== FORM 9 GENERATE DEBUG ==========");
        System.out.println("Principal: " + (data.getPrincipal() != null ? data.getPrincipal().getName() : "NULL"));
        System.out.println("Inventors count: " + (data.getInventors() != null ? data.getInventors().size() : 0));
        System.out.println("Title: " + data.getTitleOfInvention());
        System.out.println("============================================");

        ClassPathResource resource = new ClassPathResource("form 9main.docx");

        if (!resource.exists()) {
            logger.error("❌ form 9main.docx not found in resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            Map<String, String> replacements = buildReplacementsMap(data);

            // 1. Replace placeholders in standalone paragraphs
            for (XWPFParagraph paragraph : new ArrayList<>(document.getParagraphs())) {
                replacePlaceholders(paragraph, replacements);
            }

            // 2. Replace placeholders inside all table cells (critical for Section 1)
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                replacePlaceholders(paragraph, replacements);
                            }
                        }
                    }
                }
            }

            // 3. Write output
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                document.write(bos);
                return bos.toByteArray();
            }

        } catch (Exception e) {
            logger.error("❌ CRITICAL ERROR DURING FORM 9 GENERATION", e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    // ------------------------------------------------------------------
    // Build placeholder map
    // ------------------------------------------------------------------

    private Map<String, String> buildReplacementsMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();

        // 1. Title
        map.put("{title}", nullSafe(data.getTitleOfInvention()));

        // 2. Nationality — from applicant
        String nationality = "Indian";
        if (data.getApplicant() != null && notBlank(data.getApplicant().getNationality())) {
            nationality = data.getApplicant().getNationality();
        }
        map.put("{nation}", nationality);

        // 3. Address (multi-line vertical format)
        String address = buildMultiLineAddress(data);
        map.put("{inv_address}", address);

        // 4. Date — today's date in "09th August 2026" format
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        String formattedDate = day + getOrdinalSuffix(day) + " "
                + today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
                + " " + today.format(DateTimeFormatter.ofPattern("yyyy"));
        map.put("{date}", formattedDate);

        // 5. Principal
        if (data.getPrincipal() != null && notBlank(data.getPrincipal().getName())) {
            map.put("{principal}", data.getPrincipal().getName());
        } else {
            map.put("{principal}", "");
        }

        // 6. Inventor names — VERTICAL (each on new line) for table
        if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            StringBuilder verticalNames = new StringBuilder();
            for (int i = 0; i < data.getInventors().size(); i++) {
                if (i > 0) verticalNames.append("\n");
                String name = data.getInventors().get(i).getName();
                verticalNames.append(name != null ? name.toUpperCase() : "");
            }
            map.put("{inventor_names_vertical}", verticalNames.toString());

            // 7. Inventor names — HORIZONTAL (comma-separated) for sentence
            StringBuilder horizontalNames = new StringBuilder();
            for (int i = 0; i < data.getInventors().size(); i++) {
                if (i > 0) horizontalNames.append(", ");
                String name = data.getInventors().get(i).getName();
                horizontalNames.append(name != null ? name.toUpperCase() : "");
            }
            map.put("{inventor_names_horizontal}", horizontalNames.toString());
        } else {
            map.put("{inventor_names_vertical}", "");
            map.put("{inventor_names_horizontal}", "");
        }

        return map;
    }

    /**
     * Builds multi-line address for {inv_address} placeholder.
     * Example output:
     *   Kunnam, Sunguvarchatram
     *   Sriperumbudur
     *   Tamil Nadu
     *   India - 631604
     */
    private String buildMultiLineAddress(PatentFormResponse data) {
        if (data.getApplicant() == null || data.getApplicant().getAddress() == null) {
            return "";
        }

        PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();
        StringBuilder sb = new StringBuilder();

        // Line 1: Street (if present)
        if (notBlank(addr.getStreet())) {
            sb.append(addr.getStreet());
        }

        // Line 2: City
        if (notBlank(addr.getCity())) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(addr.getCity());
        }

        // Line 3: State
        if (notBlank(addr.getState())) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(addr.getState());
        }

        // Line 4: Country - Pincode
        StringBuilder countryLine = new StringBuilder();
        if (notBlank(addr.getCountry())) {
            countryLine.append(addr.getCountry());
        } else {
            countryLine.append("India");
        }
        if (notBlank(addr.getPincode())) {
            countryLine.append(" - ").append(addr.getPincode());
        }

        if (sb.length() > 0) sb.append("\n");
        sb.append(countryLine);

        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Placeholder replacer — handles multi-line values (\n) properly
    // ------------------------------------------------------------------

    private void replacePlaceholders(XWPFParagraph paragraph, Map<String, String> replacements) {
        if (paragraph == null) return;
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // Merge all runs into one string to detect split placeholders
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) sb.append(text);
        }

        String fullText = sb.toString();
        boolean replacedAny = false;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (fullText.contains(entry.getKey())) {
                fullText = fullText.replace(entry.getKey(), entry.getValue());
                replacedAny = true;
            }
        }

        if (!replacedAny) return;

        // Preserve first run formatting
        XWPFRun baseRun = runs.get(0);
        String fontFamily = baseRun.getFontFamily();
        Double fontSize = baseRun.getFontSizeAsDouble();
        boolean isBold = baseRun.isBold();
        boolean isItalic = baseRun.isItalic();
        String color = baseRun.getColor();

        // Remove all existing runs
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // Split on \n and create real Word line breaks
        String[] lines = fullText.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(lines[li]);
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (fontSize != null && fontSize > 0) newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
            newRun.setItalic(isItalic);
            if (color != null) newRun.setColor(color);

            // Add real Word line break between lines
            if (li < lines.length - 1) {
                newRun.addBreak();
            }
        }
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private String getOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}