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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Form9GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form9GeneratorService.class);

    public byte[] generateForm9(PatentFormResponse data) {
        ClassPathResource resource = new ClassPathResource("form 9.docx");

        if (!resource.exists()) {
            logger.error("❌ CRITICAL: form 9.docx was NOT found inside src/main/resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream(); XWPFDocument document = new XWPFDocument(is)) {

            Map<String, String> replacements = buildReplacementsMap(data);

            // Process standalone paragraphs
            if (document.getParagraphs() != null) {
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    replacePlaceholdersInParagraph(paragraph, replacements);
                }
            }

            // Process table cells
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

        String applicantName = "Jeppiaar Institute of Technology";
        if (data != null && data.getApplicant() != null && data.getApplicant().getName() != null
                && !data.getApplicant().getName().trim().isEmpty()) {
            applicantName = data.getApplicant().getName().trim();
        }

        map.put("{applicantName}", applicantName);
        map.put("{signatureName}", applicantName);
        map.put("{currentDay}", String.valueOf(today.getDayOfMonth()));
        map.put("{currentMonth}", today.format(DateTimeFormatter.ofPattern("MMMM")));
        map.put("{currentYear}", String.valueOf(today.getYear()));
        map.put("{patentOfficeBranch}", "Chennai");

        String applicantAddressStr = "";
        String applicantNationality = "Indian";
        if (data != null && data.getApplicant() != null) {
            applicantNationality = data.getApplicant().getNationality() != null ? data.getApplicant().getNationality() : "Indian";
            if (data.getApplicant().getAddress() != null) {
                PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();
                List<String> parts = new ArrayList<>();
                if (addr.getHouseNo() != null && !addr.getHouseNo().isBlank()) parts.add(addr.getHouseNo().trim());
                if (addr.getStreet() != null && !addr.getStreet().isBlank()) parts.add(addr.getStreet().trim());
                if (addr.getAreaLocality() != null && !addr.getAreaLocality().isBlank()) parts.add(addr.getAreaLocality().trim());
                if (addr.getVillageTown() != null && !addr.getVillageTown().isBlank()) parts.add(addr.getVillageTown().trim());
                if (addr.getCity() != null && !addr.getCity().isBlank()) parts.add(addr.getCity().trim());
                if (addr.getDistrict() != null && !addr.getDistrict().isBlank()) parts.add(addr.getDistrict().trim());
                if (addr.getState() != null && !addr.getState().isBlank()) parts.add(addr.getState().trim());
                if (addr.getCountry() != null && !addr.getCountry().isBlank()) parts.add(addr.getCountry().trim());
                
                applicantAddressStr = String.join(", ", parts);
                if (addr.getPincode() != null && !addr.getPincode().isBlank()) {
                    if (!applicantAddressStr.isEmpty()) {
                        applicantAddressStr += " - " + addr.getPincode().trim();
                    } else {
                        applicantAddressStr = addr.getPincode().trim();
                    }
                }
            }
        }
        map.put("{applicantAddress}", applicantAddressStr);
        map.put("{applicantNationality}", applicantNationality);
        map.put("{applicationNo}", "N/A");
        map.put("{applicationDate}", "N/A");

        return map;
    }

    private void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        String paragraphText = paragraph.getText();
        if (paragraphText == null || paragraphText.isEmpty()) return;

        boolean updated = false;

        // Pre-process Form 9 dots/ellipses to standard curly brace placeholders
        if (paragraphText.contains("l/We") || paragraphText.contains("I/We")) {
            paragraphText = paragraphText.replaceAll("(?i)(l/We\\s+l\\s+[\\s\u2026.]{2,}|l/We\\s+[\\s\u2026.]{2,})", "I/We {applicantName}, residing at {applicantAddress}, nationality: {applicantNationality}");
            updated = true;
        }
        if (paragraphText.contains("Patent application No")) {
            paragraphText = "Patent application No: {applicationNo}              dated {applicationDate}";
            updated = true;
        }
        if (paragraphText.contains("under section 11A(2)")) {
            paragraphText = paragraphText.replaceAll("^[\\s\u2026.]+", "");
            updated = true;
        }
        if (paragraphText.contains("day of")) {
            // Replaces "..day of .. 20" or similar
            paragraphText = paragraphText.replaceAll("[\\s\u2026.]*day of", "{currentDay} day of");
            paragraphText = paragraphText.replaceAll("of\\s*[\\s\u2026.]*\\s*20[\\s\u2026.]*", "of {currentMonth} 20{currentYear}");
            updated = true;
        }
        if (paragraphText.contains("Signature")) {
            paragraphText = paragraphText.replaceAll("Signature\\s*[\\s\u2026.]+\\s*\\d*", "Signature: {signatureName}");
            updated = true;
        }
        if (paragraphText.contains("At .") || paragraphText.trim().endsWith("At .")) {
            paragraphText = paragraphText.replaceAll("At\\s*[\\s\u2026.]+", "At {patentOfficeBranch}");
            updated = true;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (paragraphText.contains(entry.getKey())) {
                paragraphText = paragraphText.replace(entry.getKey(), entry.getValue());
                updated = true;
            }
        }

        if (updated) {
            while (paragraph.getRuns().size() > 0) {
                paragraph.removeRun(0);
            }
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(paragraphText);
        }
    }
}
