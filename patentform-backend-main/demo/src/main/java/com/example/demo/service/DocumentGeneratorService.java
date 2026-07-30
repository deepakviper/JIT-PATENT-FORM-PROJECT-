package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentGeneratorService {

    public byte[] processAndGenerateForm(String rawInputText) throws Exception {
        PatentFormResponse data = extractDataFromRawText(rawInputText);
        return generateFilledForm1(data);
    }

    public PatentFormResponse extractDataFromRawText(String fileContent) {
        PatentFormResponse response = new PatentFormResponse();
        PatentFormResponse.ApplicantDTO applicant = response.getApplicant();

        String cleanContent = fileContent.replace("\r\n", "\n").replace("\r", "\n");
        String lowerContent = cleanContent.toLowerCase();

        // 1. TITLE EXTRACTION
        Pattern titlePattern = Pattern
                .compile("(?i)title of the[^\\n:]*:\\s*(.*?)\\s*(?=\\n\\s*name|\\n\\s*abstract|$)", Pattern.DOTALL);
        Matcher titleMatcher = titlePattern.matcher(cleanContent);
        if (titleMatcher.find()) {
            response.setTitleOfInvention(titleMatcher.group(1).trim().replace("\n", " "));
        }

        // 2. MULTI-LINE NAMES & ADDRESS EXTRACTION
        int namesStartIdx = lowerContent.indexOf("names of project");
        if (namesStartIdx == -1) {
            namesStartIdx = lowerContent.indexOf("inventors");
        }
        int abstractIdx = lowerContent.indexOf("abstract:");

        if (namesStartIdx != -1 && abstractIdx != -1) {
            int headerEndLine = cleanContent.indexOf("\n", namesStartIdx);
            if (headerEndLine != -1 && headerEndLine < abstractIdx) {
                String dynamicBlock = cleanContent.substring(headerEndLine, abstractIdx).trim();
                String[] lines = dynamicBlock.split("\n");

                List<String> individualNames = new ArrayList<>();
                String addressLine = "";
                Pattern pincodePattern = Pattern.compile("\\b\\d{6}\\b");

                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty())
                        continue;

                    Matcher pinMatcher = pincodePattern.matcher(trimmedLine);

                    // Identify address line
                    if (pinMatcher.find() || trimmedLine.toLowerCase().contains("kunnam") ||
                            trimmedLine.toLowerCase().contains("sunguvarchatram")) {
                        addressLine = trimmedLine;
                    }
                    // Collect names safely
                    else if (!trimmedLine.toLowerCase().contains("department") &&
                            !trimmedLine.toLowerCase().contains("institute") &&
                            !trimmedLine.toLowerCase().contains("university") &&
                            !trimmedLine.toLowerCase().contains("college") &&
                            !trimmedLine.toLowerCase().contains("cse")) {

                        if (trimmedLine.endsWith(",")) {
                            trimmedLine = trimmedLine.substring(0, trimmedLine.length() - 1).trim();
                        }
                        individualNames.add(trimmedLine);
                    }
                }

                // 1. Map Names to the Applicant
                applicant.setName(String.join(", ", individualNames));

                // 2. Parse and Map Address directly
                PatentFormResponse.AddressDTO address = applicant.getAddress();
                String street = "";
                String city = "";
                String state = "";
                String pincode = "631604";
                String country = "India";

                if (!addressLine.isEmpty()) {
                    String[] addressParts = addressLine.split(",");
                    for (int i = 0; i < addressParts.length; i++) {
                        addressParts[i] = addressParts[i].trim();
                    }

                    if (addressParts.length >= 4) {
                        StringBuilder streetBuilder = new StringBuilder();
                        for (int i = 0; i < addressParts.length - 3; i++) {
                            if (!streetBuilder.isEmpty())
                                streetBuilder.append(", ");
                            streetBuilder.append(addressParts[i]);
                        }
                        street = streetBuilder.toString();
                        city = addressParts[addressParts.length - 3];
                        state = addressParts[addressParts.length - 2];

                        String tailSegment = addressParts[addressParts.length - 1];
                        Matcher finalPinMatcher = pincodePattern.matcher(tailSegment);
                        if (finalPinMatcher.find()) {
                            pincode = finalPinMatcher.group();
                        }
                    } else {
                        street = addressLine;
                    }

                    address.setStreet(street);
                    address.setCity(city);
                    address.setState(state);
                    address.setPincode(pincode);
                    address.setCountry(country);

                    applicant.setNationality("Indian");
                    applicant.setCountry(country);
                }

                // 3. Populate Inventor list
                List<PatentFormResponse.InventorDTO> inventorsList = new ArrayList<>();
                for (String name : individualNames) {
                    PatentFormResponse.InventorDTO inventor = new PatentFormResponse.InventorDTO();
                    inventor.setName(name);
                    inventor.setNationality("Indian");
                    inventor.setCountry(country);
                    inventorsList.add(inventor);
                }
                response.setInventors(inventorsList);
            }
        }
        return response;
    }

    public byte[] generateFilledForm1(PatentFormResponse data) throws Exception {
        ClassPathResource resource = new ClassPathResource("Form1_Template.docx");

        if (!resource.exists()) {
            System.out.println("\u274C ERROR: Form1_Template.docx was not found inside resources/");
            return new byte[0];
        }

        try (InputStream is = resource.getInputStream(); XWPFDocument document = new XWPFDocument(is)) {

            // 1. Process non-table standalone paragraphs
            if (document.getParagraphs() != null) {
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    processParagraph(paragraph, data, null);
                }
            }

            // 2. Process Tables with Hybrid Fallback Routing (Handles dynamic rows OR split
            // tables)
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    processInventorAwareTable(table, data);
                }
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                document.write(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            System.out.println("\u274C CRITICAL ERROR DURING DOC GENERATION:");
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Routes a single table through the hybrid Split / Unified inventor-table
     * strategy.
     *
     * SPLIT TEMPLATE (the real Form1 layout): {{INV_NAME}} lives in one physical
     * row
     * (often the top of a vMerge block spanning the address rows below it), while
     * {{INV_STREET}} / {{INV_CITY}} / etc. live in *different* physical rows. In
     * this
     * case we NEVER clone rows — we stitch every inventor's
     * name/nationality/country
     * into the single existing cell using addBreak(), and let the (shared) address
     * fields populate once from the applicant/institution address. This is the only
     * safe strategy when a vMerge block is involved, since cloning a row that
     * participates in a vMerge group would require cloning and re-flagging the
     * entire
     * merged group (restart/continue) to avoid corrupting the grid.
     *
     * UNIFIED TEMPLATE (a template variant, not what Form1 actually uses today): if
     * {{INV_NAME}} and {{INV_STREET}} are found in the SAME physical row, we assume
     * each inventor gets their own full row, and we clone that row per inventor
     * using
     * a deep XML copy captured BEFORE any text replacement happens.
     */
    private void processInventorAwareTable(XWPFTable table, PatentFormResponse data) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows == null || rows.isEmpty())
            return;

        int nameRowIndex = findRowIndexContainingToken(rows, "{{INV_NAME}}");
        int streetRowIndex = findRowIndexContainingToken(rows, "{{INV_STREET}}");

        boolean hasInventorNameToken = nameRowIndex != -1;
        boolean isUnifiedSingleRowTemplate = hasInventorNameToken
                && streetRowIndex != -1
                && streetRowIndex == nameRowIndex;

        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);

            if (isUnifiedSingleRowTemplate && r == nameRowIndex) {
                r = processUnifiedInventorRow(table, r, row, data);
            } else {
                // Split-template row (or any non-inventor row): process in place,
                // never clone. Multi-inventor stitching happens inside processParagraph.
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        processParagraph(paragraph, data, null);
                    }
                }
            }
        }
    }

    private int processUnifiedInventorRow(XWPFTable table, int rowIndex, XWPFTableRow sourceRow,
            PatentFormResponse data) {
        List<PatentFormResponse.InventorDTO> inventors = data.getInventors();

        if (inventors == null || inventors.isEmpty()) {
            for (XWPFTableCell cell : sourceRow.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    processParagraph(p, data, null);
                }
            }
            return rowIndex;
        }

        // Snapshot the pristine row XML BEFORE any replacement mutates it.
        String pristineRowXml = sourceRow.getCtRow().xmlText();

        int insertIndex = rowIndex;

        for (int i = 0; i < inventors.size(); i++) {
            XWPFTableRow targetRow;

            if (i == 0) {
                targetRow = sourceRow;
            } else {
                insertIndex++;
                targetRow = table.insertNewTableRow(insertIndex);

                try {
                    CTRow clonedCtRow = CTRow.Factory.parse(pristineRowXml);
                    targetRow.getCtRow().set(clonedCtRow);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deep-clone inventor row XML", e);
                }

                // CRITICAL FIX: table.insertNewTableRow() builds its XWPFTableRow's
                // internal cell list from whatever XML existed at construction time
                // (i.e. empty, since the row was brand new). Overwriting the CTRow
                // afterwards does NOT refresh that cached cell list, so
                // targetRow.getTableCells() would silently return an empty list and
                // every cloned row's placeholders would never get replaced. Rebuilding
                // the wrapper from the now-populated CTRow and swapping it back into
                // the table's live row list fixes this without touching any XML.
                targetRow = new XWPFTableRow(targetRow.getCtRow(), table);
                table.getRows().set(insertIndex, targetRow);
            }

            for (XWPFTableCell cell : targetRow.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    processParagraph(p, data, inventors.get(i));
                }
            }
        }

        return insertIndex; // Skip scanning the rows we just created
    }

    /**
     * Returns the index of the first row in {@code rows} whose cell text contains
     * {@code token}, or -1 if no row contains it.
     */
    private int findRowIndexContainingToken(List<XWPFTableRow> rows, String token) {
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText();
                if (text != null && text.contains(token)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void processParagraph(XWPFParagraph paragraph, PatentFormResponse data,
            PatentFormResponse.InventorDTO specificInventor) {
        if (paragraph == null || paragraph.getText() == null || paragraph.getText().trim().isEmpty()) {
            return;
        }

        // 1. General Meta Tokens
        replaceTextInParagraph(paragraph, "{{TITLE}}", data.getTitleOfInvention());
        replaceTextInParagraph(paragraph, "{{APPLICATION_TYPE}}", data.getApplicationType());

        // 2. Map Applicant Placeholders
        if (data.getApplicant() != null) {
            PatentFormResponse.ApplicantDTO applicant = data.getApplicant();

            replaceTextInParagraph(paragraph, "{{APP_NAME}}", applicant.getName());
            replaceTextInParagraph(paragraph, "{{NATIONALITY}}", applicant.getNationality());
            replaceTextInParagraph(paragraph, "{{RES_CO}}", applicant.getCountry()); // UPDATED: {{RESIDENCE_COUNTRY}}
                                                                                     // -> {{RES_CO}}

            if (applicant.getAddress() != null) {
                PatentFormResponse.AddressDTO address = applicant.getAddress();

                replaceTextInParagraph(paragraph, "{{HOUSE_NO}}", "Department of CSE");
                replaceTextInParagraph(paragraph, "{{STREET}}", address.getStreet());
                replaceTextInParagraph(paragraph, "{{CITY}}", address.getCity());
                replaceTextInParagraph(paragraph, "{{STATE}}", address.getState());
                replaceTextInParagraph(paragraph, "{{COUNTRY}}", address.getCountry());
                replaceTextInParagraph(paragraph, "{{PINCODE}}", address.getPincode());
            }
        }

        // 3. Dynamic Single-Inventor Row Mapping (used only by the Unified/cloned-row
        // route)
        if (specificInventor != null) {
            replaceTextInParagraph(paragraph, "{{INV_NAME}}", specificInventor.getName());
            replaceTextInParagraph(paragraph, "{{INV_NAT}}",
                    specificInventor.getNationality() != null ? specificInventor.getNationality() : "Indian");
            replaceTextInParagraph(paragraph, "{{INV_COUNTRY}}",
                    specificInventor.getCountry() != null ? specificInventor.getCountry() : "India");
        }
        // 4. Split-template fallback: stitch every inventor's name/nationality/country
        // into the same existing cell (no row cloning) using physical line breaks.
        else if (data.getInventors() != null && !data.getInventors().isEmpty()) {
            List<PatentFormResponse.InventorDTO> inventors = data.getInventors();
            StringBuilder namesBuilder = new StringBuilder();
            StringBuilder nationalitiesBuilder = new StringBuilder();
            StringBuilder countriesBuilder = new StringBuilder();

            for (int i = 0; i < inventors.size(); i++) {
                PatentFormResponse.InventorDTO inventor = inventors.get(i);
                if (i > 0) {
                    namesBuilder.append("\n");
                    nationalitiesBuilder.append("\n");
                    countriesBuilder.append("\n");
                }
                namesBuilder.append(inventor.getName());
                nationalitiesBuilder.append(inventor.getNationality() != null ? inventor.getNationality() : "Indian");
                countriesBuilder.append(inventor.getCountry() != null ? inventor.getCountry() : "India");
            }

            replaceTextInParagraph(paragraph, "{{INV_NAME}}", namesBuilder.toString());
            replaceTextInParagraph(paragraph, "{{INV_NAT}}", nationalitiesBuilder.toString());
            replaceTextInParagraph(paragraph, "{{INV_COUNTRY}}", countriesBuilder.toString());
        }

        // 5. Shared/global address placeholders for the inventor block. These are
        // filled once (not per-inventor) because the template only has a single
        // occurrence of each address token - all inventors share the institution's
        // address in this form.
        if (data.getApplicant() != null && data.getApplicant().getAddress() != null) {
            PatentFormResponse.AddressDTO addr = data.getApplicant().getAddress();
            replaceTextInParagraph(paragraph, "{{INV_HOUSE_NO}}", "Department of CSE");
            replaceTextInParagraph(paragraph, "{{INV_STREET}}", addr.getStreet());
            replaceTextInParagraph(paragraph, "{{INV_CITY}}", addr.getCity());
            replaceTextInParagraph(paragraph, "{{INV_STATE}}", addr.getState());
            replaceTextInParagraph(paragraph, "{{INV_COUNTRY_ADDR}}", addr.getCountry());
            replaceTextInParagraph(paragraph, "{{INV_PIN}}", addr.getPincode());
        }

        // 6. Map Section 7: Address for Service of Applicant in India
        if (data.getApplicant() != null && data.getApplicant().getAddress() != null) {
            PatentFormResponse.AddressDTO sharedAddress = data.getApplicant().getAddress();

            replaceTextInParagraph(paragraph, "{{SERVICE_NAME}}", "Dr. J. VENU GOPALA KRISHNAN");

            String fullPostalAddress = "Principal, Jeppiaar Institute of Technology (JIT), "
                    + (sharedAddress.getStreet() != null ? sharedAddress.getStreet() + ", "
                            : "Kunnam, Sunguvarchatram, ")
                    + (sharedAddress.getCity() != null ? sharedAddress.getCity() + ", " : "Sriperumbudur, ")
                    + (sharedAddress.getState() != null ? sharedAddress.getState() + ", " : "Tamil Nadu, ")
                    + (sharedAddress.getCountry() != null ? sharedAddress.getCountry() : "India")
                    + " - " + (sharedAddress.getPincode() != null ? sharedAddress.getPincode() : "631 604");

            replaceTextInParagraph(paragraph, "{{SERVICE_ADDRESS}}", fullPostalAddress);
            replaceTextInParagraph(paragraph, "{{SERVICE_TEL}}", "+91- 044-27159000");
            replaceTextInParagraph(paragraph, "{{SERVICE_MOBILE}}", "74012 22007");
            replaceTextInParagraph(paragraph, "{{SERVICE_FAX}}", "+91- 044-27159006");
            replaceTextInParagraph(paragraph, "{{SERVICE_EMAIL}}", "principal@jeppiaarinstitute.org");
        }

        // 7. Map Document Metadata Attachments
        if (data.getAttachments() != null) {
            PatentFormResponse.AttachmentsDTO attachments = data.getAttachments();
            replaceTextInParagraph(paragraph, "{{PAGES}}", String.valueOf(attachments.getSpecificationPages()));
            replaceTextInParagraph(paragraph, "{{CLAIMS}}", String.valueOf(attachments.getClaimsCount()));
        }
    }

    /**
     * Replaces a token within a paragraph, first stitching together any runs Word
     * split the token across (spellcheck/autocorrect commonly fragments
     * "{{INV_NAME}}" into 2-3 runs). Multi-line replacement values are split on
     * "\n"
     * and rejoined using addBreak() (a real <w:br/>), never a literal newline
     * character, since Word will not render "\n" typed into run text as a line
     * break.
     */
    private void replaceTextInParagraph(XWPFParagraph paragraph, String targetToken, String replacementValue) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty())
            return;

        // Stitch tokens split across Apache POI Runs safely
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null)
                sb.append(text);
        }

        String fullText = sb.toString();
        if (!fullText.contains(targetToken))
            return;

        String valueToUse = replacementValue != null ? replacementValue : "";
        String updatedText = fullText.replace(targetToken, valueToUse);

        // Retain original template font family, size, color, and weights
        XWPFRun baseRun = runs.get(0);
        String fontName = baseRun.getFontFamily() != null ? baseRun.getFontFamily() : "Arial";
        int fontSize = baseRun.getFontSize() > 0 ? baseRun.getFontSize() : 11;
        boolean isBold = baseRun.isBold();
        String color = baseRun.getColor();

        // Clear split run fragments cleanly
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // Safe multiline replacement wrapper
        String[] lineParts = updatedText.split("\n");
        for (int i = 0; i < lineParts.length; i++) {
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(lineParts[i]);
            newRun.setFontFamily(fontName);
            if (fontSize > 0)
                newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
            if (color != null)
                newRun.setColor(color);

            // Add hard line breaks for multiline segments safely without breaking cell
            // structures
            if (i < lineParts.length - 1) {
                newRun.addBreak();
            }
        }
    }
}