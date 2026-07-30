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
public class Form5GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form5GeneratorService.class);

    public byte[] generateForm5(PatentFormResponse data) {
        ClassPathResource resource = new ClassPathResource("Form-[5].docx");

        if (!resource.exists()) {
            logger.error("❌ CRITICAL: Form-[5].docx was NOT found inside src/main/resources/");
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
            logger.error("❌ CRITICAL EXCEPTION DURING PROCESSING:", e);
            return new byte[0];
        }
    }

    private Map<String, String> buildReplacementsMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 1. Section 1: Name of Applicant Stacking
        String primaryApplicant = "";
        if (data.getApplicant() != null && data.getApplicant().getName() != null) {
            primaryApplicant = data.getApplicant().getName().replaceAll(",\\s*", "\n").trim();
        }
        map.put("{applicantName}", primaryApplicant.isEmpty() ? "N/A" : primaryApplicant);
        map.put("{titleOfInvention}", data.getTitleOfInvention() != null ? data.getTitleOfInvention() : "N/A");

        // 2. Section 2: Smart Formatting Strategy for Multi-Inventors
        String namesOnly = "N/A";
        String nationalitiesOnly = "Indian";
        String addressesOnly = "N/A";

        if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            // Collect names sequentially as per usual
            namesOnly = data.getInventors().stream()
                    .map(inv -> inv.getName() != null ? inv.getName() : "")
                    .collect(Collectors.joining(", "));

            // Deduplicate Nationalities: If all are the same, just print it once.
            java.util.Set<String> uniqueNationalities = data.getInventors().stream()
                    .map(inv -> inv.getNationality() != null ? inv.getNationality() : "Indian")
                    .collect(Collectors.toSet());
            nationalitiesOnly = String.join(", ", uniqueNationalities);

            // Dynamically compile the complete structural institutional address line
            String sharedAddressStr = "N/A";
            if (data.getApplicant() != null && data.getApplicant().getAddress() != null) {
                PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();

                // 💡 FIX: Explicitly prepend the institution title before stitching the structural address parts
                sharedAddressStr = "Jeppiaar Institute of Technology (JIT), "
                        + (addr.getStreet() != null && !addr.getStreet().isEmpty() ? addr.getStreet() : "Kunnam, Sunguvarchatram")
                        + (addr.getCity() != null ? ", " + addr.getCity() : ", Sriperumbudur")
                        + (addr.getState() != null ? ", " + addr.getState() : ", Tamil Nadu")
                        + (addr.getPincode() != null ? " - " + addr.getPincode() : " - 631604")
                        + (addr.getCountry() != null ? ", " + addr.getCountry() : ", India");

                // Clean up any double-comma scenarios safely
                sharedAddressStr = sharedAddressStr.replaceAll(",\\s*,", ",").trim();
            }

            addressesOnly = sharedAddressStr;
        }

        map.put("{inventorNames}", namesOnly);
        map.put("{inventorNationalities}", nationalitiesOnly);
        map.put("{inventorAddresses}", addressesOnly);

        // Standard Application Metadata Placeholders
        map.put("{baseApplicationNo}", "____________________");
        map.put("{baseApplicationDate}", "____________________");

        // 3. Current Execution Splits
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
        map.put("{patentOfficeBranch}", "Chennai");

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

            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            paragraph.setSpacingBefore(0);
            paragraph.setSpacingAfter(0);

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