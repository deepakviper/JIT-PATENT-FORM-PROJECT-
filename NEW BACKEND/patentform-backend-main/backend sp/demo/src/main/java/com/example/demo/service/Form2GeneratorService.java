package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class Form2GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(Form2GeneratorService.class);

    // ------------------------------------------------------------------
    // MAIN ENTRY POINT
    // ------------------------------------------------------------------

    public byte[] generateForm2(PatentFormResponse data, byte[] sourceFileBytes) {

        System.out.println("========== FORM 2 GENERATE DEBUG ==========");
        System.out.println("Principal: " + (data.getPrincipal() != null ? data.getPrincipal().getName() : "NULL"));
        System.out.println("Inventors count: " + (data.getInventors() != null ? data.getInventors().size() : 0));
        System.out.println("Source file available: " + (sourceFileBytes != null ? "YES (" + sourceFileBytes.length + " bytes)" : "NO"));
        System.out.println("============================================");

        ClassPathResource resource = new ClassPathResource("Form2,main .docx");

        if (!resource.exists()) {
            logger.error("❌ Form2,main .docx not found in resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream();
             XWPFDocument targetDoc = new XWPFDocument(is)) {

            // 1. Text placeholders
            Map<String, String> textReplacements = buildTextReplacementsMap(data);

            for (XWPFParagraph paragraph : new ArrayList<>(targetDoc.getParagraphs())) {
                replaceTextPlaceholders(paragraph, textReplacements);
            }

            if (targetDoc.getTables() != null) {
                for (XWPFTable table : targetDoc.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                replaceTextPlaceholders(paragraph, textReplacements);
                            }
                        }
                    }
                }
            }

            // 2. If source file provided → copy Description + Abstract with images
            if (sourceFileBytes != null && sourceFileBytes.length > 0) {
                try (XWPFDocument sourceDoc = new XWPFDocument(new ByteArrayInputStream(sourceFileBytes))) {

                    // Description
                    copySectionFromSource(targetDoc, sourceDoc, "{description}",
                            "(?i)^\\s*DESCRIPTION\\s*:?\\s*$",
                            "(?i)^\\s*(CLAIMS|WE CLAIM|I CLAIM)\\s*:?\\s*$");

                    // Claims — smart: use source if available, else auto-generate
                    handleClaimsSection(targetDoc, sourceDoc, data);

                    // Abstract
                    copySectionFromSource(targetDoc, sourceDoc, "{abstract}",
                            "(?i)^\\s*ABSTRACT\\s*:?\\s*$",
                            "(?i)^\\s*DESCRIPTION\\s*:?\\s*$");
                }
            } else {
                fallbackPlainTextInjection(targetDoc, "{description}", data.getDescriptionXml());
                fallbackPlainTextInjection(targetDoc, "{claims}",      data.getClaimsXml());
                fallbackPlainTextInjection(targetDoc, "{abstract}",    data.getAbstractXml());
            }

            // 3. Write output
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                targetDoc.write(bos);
                return bos.toByteArray();
            }

        } catch (Exception e) {
            logger.error("❌ CRITICAL ERROR DURING FORM 2 GENERATION", e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    // ------------------------------------------------------------------
    // CLAIMS HANDLER — smart routing
    // ------------------------------------------------------------------

    private void handleClaimsSection(XWPFDocument targetDoc, XWPFDocument sourceDoc, PatentFormResponse data) {
        // Check if source has CLAIMS section
        boolean sourceHasClaims = doesSourceHaveClaimsSection(sourceDoc);

        if (sourceHasClaims) {
            System.out.println("✅ Source has CLAIMS section — using it directly");
            copySectionFromSource(targetDoc, sourceDoc, "{claims}",
                    "(?i)^\\s*(CLAIMS|WE CLAIM|I CLAIM)\\s*:?\\s*$",
                    null);
        } else {
            System.out.println("ℹ️ Source has NO CLAIMS section — auto-generating from Workflow Methodology");
            generateAutoClaims(targetDoc, sourceDoc, data);
        }
    }

    private boolean doesSourceHaveClaimsSection(XWPFDocument sourceDoc) {
        Pattern claimsHeading = Pattern.compile("(?i)^\\s*(CLAIMS|WE CLAIM|I CLAIM)\\s*:?\\s*$");
        for (IBodyElement el : sourceDoc.getBodyElements()) {
            if (el instanceof XWPFParagraph) {
                String text = ((XWPFParagraph) el).getText();
                if (text != null && claimsHeading.matcher(text).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // AUTO-GENERATE CLAIMS from Workflow Methodology section
    // ------------------------------------------------------------------

    private void generateAutoClaims(XWPFDocument targetDoc, XWPFDocument sourceDoc, PatentFormResponse data) {
        XWPFParagraph targetPlaceholder = findParagraphContainingPlaceholder(targetDoc, "{claims}");
        if (targetPlaceholder == null) {
            System.out.println("❌ {claims} placeholder not found in target");
            return;
        }

        // Extract workflow methodology stages from source
        List<String> stages = extractWorkflowStages(sourceDoc);
        System.out.println("   → Extracted " + stages.size() + " workflow stages");

        String title = data.getTitleOfInvention() != null
                ? data.getTitleOfInvention()
                : "the invention";

        // Build claims list
        List<ClaimEntry> claims = new ArrayList<>();

        // Claim 1 — preamble about invention
        claims.add(new ClaimEntry(
                "The patent disclosure covers Novel System, Design and Method of "
                        + title
                        + " as described above in Fig 1 & 2. The operational methodology of the invention consists of the following stages:",
                new ArrayList<>()
        ));

        // Claims 2-N — stages
        for (String stage : stages) {
            ClaimEntry entry = parseStageAsClaim(stage);
            if (entry != null) {
                claims.add(entry);
            }
        }

        // ---- Now insert them into target ----

        // Preamble: "I/We Claim,"
        String preamble = (data.getInventors() != null && data.getInventors().size() > 1)
                ? "We Claim,"
                : "I/We Claim,";

        insertParagraph(targetDoc, targetPlaceholder, preamble, true, false, 12);
        insertParagraph(targetDoc, targetPlaceholder, "", false, false, 11);

        // Numbered claims
        int claimNumber = 1;
        for (ClaimEntry entry : claims) {
            String claimText = claimNumber + ". " + entry.text;
            insertParagraph(targetDoc, targetPlaceholder, claimText, false, false, 11);

            // Sub-bullet points (if any)
            for (String bullet : entry.subBullets) {
                insertParagraph(targetDoc, targetPlaceholder, "  • " + bullet, false, false, 11);
            }

            // Blank line between claims
            insertParagraph(targetDoc, targetPlaceholder, "", false, false, 11);

            claimNumber++;
        }

        // Remove placeholder
        int pos = targetDoc.getPosOfParagraph(targetPlaceholder);
        if (pos >= 0) {
            targetDoc.removeBodyElement(pos);
        }

        System.out.println("✅ Auto-generated " + claims.size() + " claims");
    }

    private List<String> extractWorkflowStages(XWPFDocument sourceDoc) {
        List<String> stages = new ArrayList<>();

        Pattern startPattern = Pattern.compile("(?i)^\\s*WORKFLOW\\s+METHODOLOGY\\s*:?\\s*$");
        Pattern endPattern = Pattern.compile("(?i)^\\s*(EXEMPLARY|UNIQUENESS|IMPLEMENTATION\\s+SCENARIO|REFERENCES)\\s*.*");

        boolean capturing = false;
        StringBuilder currentStage = null;

        for (IBodyElement el : sourceDoc.getBodyElements()) {
            if (!(el instanceof XWPFParagraph)) continue;

            String text = ((XWPFParagraph) el).getText();
            if (text == null) text = "";
            String trimmed = text.trim();

            if (!capturing) {
                if (startPattern.matcher(trimmed).find()) {
                    capturing = true;
                }
                continue;
            } else {
                if (endPattern.matcher(trimmed).find()) {
                    if (currentStage != null && currentStage.length() > 0) {
                        stages.add(currentStage.toString().trim());
                    }
                    break;
                }

                if (trimmed.isEmpty()) continue;

                // Detect stage boundary: "Stage 1:", "Stage 2:", etc.
                if (trimmed.matches("(?i)^Stage\\s+\\d+\\s*:.*")) {
                    // Save previous stage
                    if (currentStage != null && currentStage.length() > 0) {
                        stages.add(currentStage.toString().trim());
                    }
                    // Start new stage — strip "Stage N:" prefix
                    String cleaned = trimmed.replaceFirst("(?i)^Stage\\s+\\d+\\s*:\\s*", "");
                    currentStage = new StringBuilder(cleaned);
                } else if (currentStage != null) {
                    // Continuation lines (like sub-bullets)
                    currentStage.append("\n").append(trimmed);
                } else {
                    // Text before first "Stage N:" — start a stage-less block
                    if (currentStage == null) {
                        currentStage = new StringBuilder(trimmed);
                    }
                }
            }
        }

        // Add final stage if we didn't hit end pattern
        if (capturing && currentStage != null && currentStage.length() > 0) {
            stages.add(currentStage.toString().trim());
        }

        return stages;
    }

    /**
     * Parses a stage text into main claim text + optional sub-bullets.
     * Example input:
     *   "Risk Classification: Risks are classified into:
     *    Low Risk
     *    Moderate Risk
     *    High Risk
     *    Critical Risk"
     *
     * Returns:
     *   text = "Risk Classification: Risks are classified into:"
     *   subBullets = ["Low Risk", "Moderate Risk", "High Risk", "Critical Risk"]
     */
    private ClaimEntry parseStageAsClaim(String stageText) {
        if (stageText == null || stageText.isBlank()) return null;

        String[] lines = stageText.split("\n");
        String mainText = lines[0].trim();
        List<String> subBullets = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                subBullets.add(line);
            }
        }

        return new ClaimEntry(mainText, subBullets);
    }

    private static class ClaimEntry {
        String text;
        List<String> subBullets;

        ClaimEntry(String text, List<String> subBullets) {
            this.text = text;
            this.subBullets = subBullets;
        }
    }

    private void insertParagraph(XWPFDocument doc, XWPFParagraph beforeThis,
                                 String text, boolean bold, boolean italic, int fontSize) {
        XWPFParagraph newPara = doc.insertNewParagraph(beforeThis.getCTP().newCursor());
        XWPFRun run = newPara.createRun();
        run.setText(text);
        run.setFontFamily("Times New Roman");
        run.setFontSize(fontSize);
        run.setColor("000000");
        run.setBold(bold);
        run.setItalic(italic);
    }

    // ------------------------------------------------------------------
    // Copy a section from source to target — preserves images/formatting
    // ------------------------------------------------------------------

    private void copySectionFromSource(XWPFDocument targetDoc, XWPFDocument sourceDoc,
                                       String placeholder, String startPatternStr, String endPatternStr) {

        XWPFParagraph targetPlaceholderPara = findParagraphContainingPlaceholder(targetDoc, placeholder);
        if (targetPlaceholderPara == null) {
            System.out.println("❌ Placeholder " + placeholder + " NOT FOUND in template");
            return;
        }

        Pattern startPattern = Pattern.compile(startPatternStr);
        Pattern endPattern = endPatternStr != null ? Pattern.compile(endPatternStr) : null;

        System.out.println("✅ Copying " + placeholder + " section from source doc...");

        boolean capturing = false;
        int copiedCount = 0;

        for (IBodyElement el : sourceDoc.getBodyElements()) {
            if (el instanceof XWPFParagraph) {
                XWPFParagraph sourcePara = (XWPFParagraph) el;
                String text = sourcePara.getText();
                if (text == null) text = "";

                if (!capturing) {
                    if (startPattern.matcher(text).find()) {
                        capturing = true;
                    }
                    continue;
                } else {
                    if (endPattern != null && endPattern.matcher(text).find()) {
                        break;
                    }

                    try {
                        copyParagraphToTarget(sourcePara, targetDoc, targetPlaceholderPara);
                        copiedCount++;
                    } catch (Exception ex) {
                        System.out.println("   ⚠️ Failed to copy paragraph: " + ex.getMessage());
                    }
                }
            } else if (el instanceof XWPFTable && capturing) {
                try {
                    copyTableToTarget((XWPFTable) el, targetDoc, targetPlaceholderPara);
                    copiedCount++;
                } catch (Exception ex) {
                    System.out.println("   ⚠️ Failed to copy table: " + ex.getMessage());
                }
            }
        }

        System.out.println("   → Total copied: " + copiedCount + " elements");

        int pos = targetDoc.getPosOfParagraph(targetPlaceholderPara);
        if (pos >= 0) {
            targetDoc.removeBodyElement(pos);
        }
    }

    private void copyParagraphToTarget(XWPFParagraph sourcePara, XWPFDocument targetDoc,
                                       XWPFParagraph beforeThisPara) throws Exception {

        XWPFParagraph newPara = targetDoc.insertNewParagraph(beforeThisPara.getCTP().newCursor());

        if (sourcePara.getCTP().getPPr() != null) {
            newPara.getCTP().setPPr((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr)
                    sourcePara.getCTP().getPPr().copy());
        }

        for (XWPFRun sourceRun : sourcePara.getRuns()) {
            XWPFRun newRun = newPara.createRun();

            String text = sourceRun.getText(0);
            if (text != null) {
                newRun.setText(text);
            }

            String fontFamily = sourceRun.getFontFamily();
            if (fontFamily != null) newRun.setFontFamily(fontFamily);

            int fontSize = sourceRun.getFontSize();
            if (fontSize > 0) newRun.setFontSize(fontSize);

            newRun.setBold(sourceRun.isBold());
            newRun.setItalic(sourceRun.isItalic());

            String color = sourceRun.getColor();
            if (color != null) newRun.setColor(color);

            if (sourceRun.getUnderline() != null && sourceRun.getUnderline() != UnderlinePatterns.NONE) {
                newRun.setUnderline(sourceRun.getUnderline());
            }

            // Copy embedded images
            for (XWPFPicture picture : sourceRun.getEmbeddedPictures()) {
                try {
                    XWPFPictureData pictureData = picture.getPictureData();
                    byte[] imageBytes = pictureData.getData();
                    String fileName = pictureData.getFileName();
                    int pictureType = pictureData.getPictureType();

                    int widthEmu = 5000000;
                    int heightEmu = 4000000;

                    newRun.addPicture(
                            new ByteArrayInputStream(imageBytes),
                            pictureType,
                            fileName != null ? fileName : "image",
                            widthEmu,
                            heightEmu
                    );

                    System.out.println("      🖼️ Copied image: " + fileName);
                } catch (Exception ex) {
                    System.out.println("      ⚠️ Failed to copy image: " + ex.getMessage());
                }
            }
        }
    }

    private void copyTableToTarget(XWPFTable sourceTable, XWPFDocument targetDoc,
                                   XWPFParagraph beforeThisPara) throws Exception {
        XWPFTable newTable = targetDoc.insertNewTbl(beforeThisPara.getCTP().newCursor());
        newTable.getCTTbl().set(sourceTable.getCTTbl().copy());
    }

    // ------------------------------------------------------------------
    // FALLBACK — Plain text injection (no source file case)
    // ------------------------------------------------------------------

    private void fallbackPlainTextInjection(XWPFDocument document, String placeholder, String xmlContent) {
        String plainText = getPlainTextFromXml(xmlContent);
        XWPFParagraph target = findParagraphContainingPlaceholder(document, placeholder);

        if (target == null) return;

        if (plainText == null || plainText.trim().isEmpty()) {
            clearParagraphText(target);
            return;
        }

        String[] lines = plainText.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            XWPFParagraph newPara = document.insertNewParagraph(target.getCTP().newCursor());
            XWPFRun run = newPara.createRun();
            run.setText(line.trim());
            run.setFontFamily("Times New Roman");
            run.setFontSize(11);
            run.setColor("000000");

            if (isLikelyHeading(line.trim())) {
                run.setBold(true);
                run.setFontSize(12);
            }
        }

        int pos = document.getPosOfParagraph(target);
        if (pos >= 0) document.removeBodyElement(pos);
    }

    private String getPlainTextFromXml(String xmlContent) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) return "";

        String DELIMITER = "|||ELEMENT_SEPARATOR|||";
        String[] fragments = xmlContent.split(Pattern.quote(DELIMITER));

        StringBuilder result = new StringBuilder();

        for (String fragment : fragments) {
            if (fragment == null || fragment.trim().isEmpty()) continue;

            String xml = fragment;
            if (xml.startsWith("P::")) xml = xml.substring(3);
            else if (xml.startsWith("T::")) xml = xml.substring(3);

            String plainText = xml.replaceAll("<[^>]+>", "").trim();
            plainText = plainText
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&apos;", "'");

            if (!plainText.isEmpty()) {
                result.append(plainText).append("\n");
            }
        }

        return result.toString().trim();
    }

    private boolean isLikelyHeading(String line) {
        if (line == null || line.isEmpty()) return false;
        if (line.length() > 100) return false;

        boolean isAllCaps = line.equals(line.toUpperCase());
        boolean matchesHeadingPattern =
                line.matches("(?i)^[0-9]+\\.?\\s*[A-Z].*")
                        || line.matches("(?i)^(TECHNICAL FIELD|BACKGROUND|OBJECTIVES|SUMMARY|DESCRIPTION|CLAIMS|ABSTRACT|SYSTEM ARCHITECTURE|WORKFLOW METHODOLOGY|EXEMPLARY IMPLEMENTATION|UNIQUENESS|REFERENCES).*")
                        || (isAllCaps && line.length() < 60);

        return matchesHeadingPattern;
    }

    // ------------------------------------------------------------------
    // TEXT PLACEHOLDERS
    // ------------------------------------------------------------------

    private Map<String, String> buildTextReplacementsMap(PatentFormResponse data) {
        Map<String, String> map = new HashMap<>();

        map.put("{title}", nullSafe(data.getTitleOfInvention()));

        if (data.getApplicant() != null) {
            PatentFormResponse.ApplicantDTO applicant = data.getApplicant();
            map.put("{applicantName}",        nullSafe(applicant.getName()));
            map.put("{applicantNationality}", nullSafe(applicant.getNationality(), "Indian"));

            if (applicant.getAddress() != null) {
                PatentFormResponse.AddressDTO addr = applicant.getAddress();
                String fullAddress =
                        (notBlank(addr.getStreet())  ? addr.getStreet()  + ", " : "")
                                + (notBlank(addr.getCity())    ? addr.getCity()    + ", " : "")
                                + (notBlank(addr.getState())   ? addr.getState()   + ", " : "")
                                + (notBlank(addr.getCountry()) ? addr.getCountry() : "India")
                                + (notBlank(addr.getPincode()) ? " - " + addr.getPincode() : "");
                map.put("{applicantAddress}", fullAddress);
            } else {
                map.put("{applicantAddress}", "");
            }
        } else {
            map.put("{applicantName}", "");
            map.put("{applicantNationality}", "Indian");
            map.put("{applicantAddress}", "");
        }

        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        String formattedDate = "Dated this " + day + getOrdinalSuffix(day) + " "
                + today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
                + " " + today.format(DateTimeFormatter.ofPattern("yyyy"));
        map.put("{date}", formattedDate);

        if (data.getPrincipal() != null && notBlank(data.getPrincipal().getName())) {
            map.put("{principal}", data.getPrincipal().getName());
        } else {
            map.put("{principal}", "");
        }

        if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.getInventors().size(); i++) {
                if (i > 0) sb.append("\t");
                String name = data.getInventors().get(i).getName();
                sb.append(name != null ? name.toUpperCase() : "");
            }
            map.put("{inventor_names}", sb.toString());
        } else {
            map.put("{inventor_names}", "");
        }

        return map;
    }

    private void replaceTextPlaceholders(XWPFParagraph paragraph, Map<String, String> replacements) {
        if (paragraph == null) return;
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

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

        XWPFRun baseRun = runs.get(0);
        String fontFamily = baseRun.getFontFamily();
        Double fontSize = baseRun.getFontSizeAsDouble();
        boolean isBold = baseRun.isBold();
        boolean isItalic = baseRun.isItalic();
        String color = baseRun.getColor();

        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        String[] lines = fullText.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            String[] tabParts = lines[li].split("\t", -1);
            for (int ti = 0; ti < tabParts.length; ti++) {
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(tabParts[ti]);
                if (fontFamily != null) newRun.setFontFamily(fontFamily);
                if (fontSize != null && fontSize > 0) newRun.setFontSize(fontSize);
                newRun.setBold(isBold);
                newRun.setItalic(isItalic);
                if (color != null) newRun.setColor(color);

                if (ti < tabParts.length - 1) newRun.addTab();
            }
            if (li < lines.length - 1) {
                XWPFRun brRun = paragraph.createRun();
                if (fontFamily != null) brRun.setFontFamily(fontFamily);
                brRun.addBreak();
            }
        }
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private XWPFParagraph findParagraphContainingPlaceholder(XWPFDocument document, String placeholder) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text != null && text.contains(placeholder)) {
                return paragraph;
            }
        }
        return null;
    }

    private void clearParagraphText(XWPFParagraph paragraph) {
        List<XWPFRun> runs = paragraph.getRuns();
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
    }

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

    private String nullSafe(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}