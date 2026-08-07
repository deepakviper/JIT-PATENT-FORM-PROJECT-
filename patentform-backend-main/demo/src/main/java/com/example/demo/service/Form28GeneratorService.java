package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
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
public class Form28GeneratorService {

    public byte[] generateForm28(PatentFormResponse data) {

        try {

            ClassPathResource resource = new ClassPathResource("FORM 28.docx");

            if (!resource.exists()) {
                throw new RuntimeException("FORM 28.docx not found inside src/main/resources");
            }

            try (InputStream inputStream = resource.getInputStream();
                    XWPFDocument document = new XWPFDocument(inputStream);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                Map<String, String> replacements = buildReplacementMap(data);

                replaceParagraphs(document, replacements);
                replaceTables(document, replacements);

                document.write(outputStream);

                return outputStream.toByteArray();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Form-28", e);
        }
    }

    private Map<String, String> buildReplacementMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();
        LocalDate today = LocalDate.now();

        String applicantName = "Jeppiaar Institute of Technology";
        if (data != null &&
                data.getApplicant() != null &&
                data.getApplicant().getName() != null &&
                !data.getApplicant().getName().trim().isEmpty()) {
            applicantName = data.getApplicant().getName().trim();
        }

        String applicantNationality = "Indian";
        if (data != null && data.getApplicant() != null && data.getApplicant().getNationality() != null) {
            applicantNationality = data.getApplicant().getNationality().trim();
        }

        String signatoryName = applicantName;
        if (data != null && data.getApplicant() != null && data.getApplicant().getAddress() != null 
                && data.getApplicant().getAddress().getPrincipalName() != null 
                && !data.getApplicant().getAddress().getPrincipalName().isBlank()) {
            signatoryName = data.getApplicant().getAddress().getPrincipalName().trim();
        }

        map.put("{applicantName}", applicantName);
        map.put("{signatureName}", applicantName);

        map.put("{currentDay}", String.valueOf(today.getDayOfMonth()));
        map.put("{currentMonth}", today.format(DateTimeFormatter.ofPattern("MMMM")));
        map.put("{currentYear}", String.valueOf(today.getYear()));

        String resolvedBranch = PatentOfficeHelper.determineBranch(data);
        map.put("{patentOfficeBranch}", resolvedBranch);

        String applicantAddressStr = "";
        if (data != null && data.getApplicant() != null && data.getApplicant().getAddress() != null) {
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
        String detailsStr = applicantAddressStr + ", Nationality: " + applicantNationality;
        map.put("{applicantAddress}", detailsStr);
        map.put("{applicationNo}", "N/A");
        map.put("{patentNo}", "N/A");

        // Literal template replacements
        map.put("Dr. J. VENU GOPALA KRISHNAN", applicantName);
        map.put("(Indian)", "(" + applicantNationality + ")");
        map.put("Chennai", resolvedBranch);

        // Signatory box replacements
        map.put("{Principal Name}", signatoryName);
        map.put("{desination}", "Principal");
        map.put("{clg name}", applicantName);
        map.put("{name}", signatoryName);

        String suffixDate = today.getDayOfMonth() + getDayOfMonthSuffix(today.getDayOfMonth()) + " " + today.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        map.put("{day}", suffixDate);

        return map;
    }

    private String getDayOfMonthSuffix(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }

    private void replaceParagraphs(XWPFDocument document,
            Map<String, String> replacements) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceParagraph(paragraph, replacements);
        }
    }

    private void replaceTables(XWPFDocument document,
            Map<String, String> replacements) {
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceParagraph(paragraph, replacements);
                    }
                }
            }
        }
    }

    private void replaceParagraph(XWPFParagraph paragraph,
            Map<String, String> replacements) {
        String text = paragraph.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (text.contains(entry.getKey())) {
                text = text.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
                changed = true;
            }
        }

        if (changed) {
            XWPFRun baseRun = paragraph.getRuns().isEmpty() ? null : paragraph.getRuns().get(0);
            String fontFamily = baseRun != null ? baseRun.getFontFamily() : "Arial";
            Double fontSizeVal = baseRun != null ? baseRun.getFontSizeAsDouble() : 11.0;
            int fontSize = (fontSizeVal != null && fontSizeVal > 0) ? fontSizeVal.intValue() : 11;
            boolean isBold = baseRun != null && baseRun.isBold();
            String color = baseRun != null ? baseRun.getColor() : null;

            while (paragraph.getRuns().size() > 0) {
                paragraph.removeRun(0);
            }

            XWPFRun newRun = paragraph.createRun();
            newRun.setText(text);
            newRun.setFontFamily(fontFamily);
            newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
            if (color != null) {
                newRun.setColor(color);
            }
        }
    }
}