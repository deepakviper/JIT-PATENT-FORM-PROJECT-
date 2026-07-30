package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class Form28GeneratorService {

    public byte[] generateForm28(PatentFormResponse data) {

        try {

            ClassPathResource resource = new ClassPathResource("Form-28.docx");

            if (!resource.exists()) {
                throw new RuntimeException("Form-28.docx not found inside src/main/resources");
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

        map.put("{applicantName}", applicantName);
        map.put("{signatureName}", applicantName);

        map.put("{currentDay}",
                String.valueOf(today.getDayOfMonth()));

        map.put("{currentMonth}",
                today.format(DateTimeFormatter.ofPattern("MMMM")));

        map.put("{currentYear}",
                String.valueOf(today.getYear()));

        map.put("{patentOfficeBranch}", "Chennai");

        String applicantAddressStr = "";
        String applicantNationality = "Indian";
        if (data != null && data.getApplicant() != null) {
            applicantNationality = data.getApplicant().getNationality() != null ? data.getApplicant().getNationality() : "Indian";
            if (data.getApplicant().getAddress() != null) {
                PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();
                applicantAddressStr = (addr.getStreet() != null && !addr.getStreet().isEmpty() ? addr.getStreet() + ", " : "")
                        + (addr.getCity() != null && !addr.getCity().isEmpty() ? addr.getCity() + ", " : "")
                        + (addr.getState() != null && !addr.getState().isEmpty() ? addr.getState() + ", " : "")
                        + (addr.getCountry() != null && !addr.getCountry().isEmpty() ? addr.getCountry() : "India")
                        + " - " + (addr.getPincode() != null && !addr.getPincode().isEmpty() ? addr.getPincode() : "");
            }
        }
        String detailsStr = applicantAddressStr + ", Nationality: " + applicantNationality;
        map.put("{applicantAddress}", detailsStr);
        map.put("{applicationNo}", "N/A");
        map.put("{patentNo}", "N/A");

        return map;
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

        // Match sequence of Unicode 8230 (\u2026) or dots
        String regex = "[\u2026.]{3,}";

        if (text.contains("I/We")) {
            text = text.replaceAll("I/We\\s*" + regex, "I/We {applicantName}");
            changed = true;
        }
        if (text.matches("^" + regex + "$")) {
            text = "{applicantAddress}";
            changed = true;
        }
        if (text.contains("application no.")) {
            text = text.replaceAll("application no.\\s*" + regex, "application no. {applicationNo}");
            text = text.replaceAll("patent no" + regex, "patent no. {patentNo}");
            changed = true;
        }
        if (text.contains("Dated this")) {
            text = text.replaceAll("Dated this\\s*" + regex, "Dated this {currentDay}");
            text = text.replaceAll("day of\\s*" + regex, "day of {currentMonth}");
            text = text.replaceAll("20" + regex, "20{currentYear}");
            text = text.replaceAll("Signature\\s*" + regex, "Signature {signatureName}");
            changed = true;
        }
        if (text.contains("(Name)")) {
            text = text.replaceAll("\\(Name\\)\\s*" + regex, "(Name) {signatureName}");
            changed = true;
        }
        if (text.contains("(Designation)")) {
            text = text.replaceAll("\\(Designation\\)\\s*" + regex, "(Designation) Small Entity / Startup");
            changed = true;
        }
        if (text.contains("At")) {
            text = text.replaceAll("At\\s*" + regex, "At {patentOfficeBranch}");
            changed = true;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {

            if (text.contains(entry.getKey())) {

                text = text.replace(entry.getKey(), entry.getValue());

                changed = true;
            }

        }

        if (changed) {

            while (paragraph.getRuns().size() > 0) {
                paragraph.removeRun(0);
            }

            XWPFRun run = paragraph.createRun();
            run.setText(text);

        }

    }

}