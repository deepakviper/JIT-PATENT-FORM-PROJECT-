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
public class Form3GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form3GeneratorService.class);

    // ------------------------------------------------------------------
    // MAIN ENTRY POINT
    // ------------------------------------------------------------------

    public byte[] generateForm3(PatentFormResponse data) {

        ClassPathResource resource = new ClassPathResource("FORM3main.docx");

        if (!resource.exists()) {
            logger.error("❌ CRITICAL: Form-3.docx was NOT found inside src/main/resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            Map<String, String> replacements = buildReplacementsMap(data);

            // 1. Process standalone paragraphs
            if (document.getParagraphs() != null) {
                for (XWPFParagraph paragraph : new ArrayList<>(document.getParagraphs())) {
                    replacePlaceholders(paragraph, replacements);
                }
            }

            // 2. Process table cells (and NESTED tables!)
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    replaceInTable(table, replacements);
                }
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                document.write(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            logger.error("❌ CRITICAL EXCEPTION DURING FORM 3 PROCESSING:", e);
            return new byte[0];
        }
    }

    // ------------------------------------------------------------------
    // RECURSIVE METHOD TO FIX THE TABLE ISSUE
    // ------------------------------------------------------------------
    private void replaceInTable(XWPFTable table, Map<String, String> replacements) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {

                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replacePlaceholders(paragraph, replacements);
                }

                for (XWPFTable nestedTable : cell.getTables()) {
                    replaceInTable(nestedTable, replacements);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // BUILD PLACEHOLDER MAP
    // ------------------------------------------------------------------

    private Map<String, String> buildReplacementsMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();

        // 1. {principal} - Name of the Principal
        String principalName = "";
        if (data.getPrincipal() != null && notBlank(data.getPrincipal().getName())) {
            principalName = data.getPrincipal().getName();
        }
        map.put("{principal}", principalName);

        // 2. {role} - Designation of the Principal
        String role = "";
        if (data.getPrincipal() != null && notBlank(data.getPrincipal().getDesignation())) {
            role = data.getPrincipal().getDesignation();
        }
        // Fallback just in case the frontend sends bad data again
        if (role.isEmpty()) role = "Principal";
        map.put("{role}", role);

        // 3. {college_name} - Standard casing
        String clgName = "";
        if (data.getApplicant() != null && notBlank(data.getApplicant().getName())) {
            clgName = data.getApplicant().getName();
        }
        map.put("{college_name}", clgName);

        // 4. {COLLEGE_NAME} - UPPERCASE for the Assignee section
        map.put("{COLLEGE_NAME}", clgName.toUpperCase());

        // 5. {address} - Multi-line formatted address
        map.put("{address}", buildMultiLineAddress(data));

        // 6. {date} - Format: 10th August 2026
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        String formattedDate = day + getOrdinalSuffix(day) + " "
                + today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
                + " " + today.format(DateTimeFormatter.ofPattern("yyyy"));
        map.put("{date}", formattedDate);

        return map;
    }

    // ------------------------------------------------------------------
    // BUILD MULTI-LINE ADDRESS
    // ------------------------------------------------------------------

    private String buildMultiLineAddress(PatentFormResponse data) {
        if (data.getApplicant() == null || data.getApplicant().getAddress() == null) {
            return "";
        }

        PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();
        StringBuilder sb = new StringBuilder();

        List<String> line1 = new ArrayList<>();
        if (notBlank(addr.getHouseNo())) line1.add(addr.getHouseNo());
        if (notBlank(addr.getStreet())) line1.add(addr.getStreet());
        if (!line1.isEmpty()) sb.append(String.join(", ", line1));

        List<String> line2 = new ArrayList<>();
        if (notBlank(addr.getCity())) line2.add(addr.getCity());
        if (notBlank(addr.getDistrict())) line2.add(addr.getDistrict());
        if (!line2.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(String.join(", ", line2));
        }

        if (notBlank(addr.getState())) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(addr.getState());
        }

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
    // PLACEHOLDER REPLACER (Handles multiline vertical breaks correctly)
    // ------------------------------------------------------------------

    private void replacePlaceholders(XWPFParagraph paragraph, Map<String, String> replacements) {
        if (paragraph == null) return;
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) sb.append(text);
        }

        String fullText = sb.toString();
        boolean replaced = false;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (fullText.contains(entry.getKey())) {
                fullText = fullText.replace(entry.getKey(), entry.getValue());
                replaced = true;
            }
        }

        if (!replaced) return;

        XWPFRun baseRun = runs.get(0);
        String fontFamily = baseRun.getFontFamily();
        Double fontSize = baseRun.getFontSizeAsDouble();
        boolean isBold = baseRun.isBold();
        boolean isItalic = baseRun.isItalic();
        String color = baseRun.getColor();

        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // Split on newline (\n) so Word interprets vertical stacks properly
        String[] lines = fullText.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(lines[i]);

            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (fontSize != null && fontSize > 0) newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
            newRun.setItalic(isItalic);
            if (color != null) newRun.setColor(color);

            // Important: Use addBreak() for real line breaks in Word
            if (i < lines.length - 1) {
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

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}