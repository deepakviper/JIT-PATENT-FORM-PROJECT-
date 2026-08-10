package com.example.demo.controller;

import com.example.demo.dto.PatentFormResponse;
import com.example.demo.service.DocumentGeneratorService;
import com.example.demo.service.DocumentParserService;
import com.example.demo.service.Form2GeneratorService;
import com.example.demo.service.Form3GeneratorService;
import com.example.demo.service.Form5GeneratorService;
import com.example.demo.service.Form9GeneratorService; // ✅ NEW
import com.example.demo.service.Form28GeneratorService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/patent")
@CrossOrigin(origins = "http://localhost:5173")
public class PatentController {

    @Autowired
    private DocumentParserService parserService;

    @Autowired
    private DocumentGeneratorService generatorService;

    @Autowired
    private Form2GeneratorService form2Service;

    @Autowired
    private Form3GeneratorService form3Service;

    @Autowired
    private Form5GeneratorService form5Service;

    @Autowired
    private Form9GeneratorService form9Service; // ✅ NEW
    @Autowired
    private Form28GeneratorService form28Service;

    // Reusable JSON mapper — ignores unknown fields to prevent crashes
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ------------------------------------------------------------------
    // /parse — Extract patent data from uploaded document
    // ------------------------------------------------------------------
    @PostMapping("/parse")
    public ResponseEntity<PatentFormResponse> parsePatentDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            PatentFormResponse responseData = parserService.parseUploadedDocument(file);
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ------------------------------------------------------------------
    // /download — Generate filled patent form
    //   - data (required)        → JSON blob of PatentFormResponse
    //   - sourceFile (optional)  → source .docx (used only by Form 2)
    // ------------------------------------------------------------------
    @PostMapping(value = "/download", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> downloadFilledForm(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "sourceFile", required = false) MultipartFile sourceFile,
            @RequestParam(value = "formType", defaultValue = "form1") String formType) {

        try {
            PatentFormResponse finalData = objectMapper.readValue(dataJson, PatentFormResponse.class);

            byte[] documentBytes;
            String filename;

            System.out.println("========== DOWNLOAD REQUEST ==========");
            System.out.println("Form type: " + formType);
            System.out.println("Source file received: " + (sourceFile != null ? sourceFile.getOriginalFilename() : "NONE"));
            System.out.println("======================================");

            // ✅ Added Form 9 to routing
            if ("form9".equalsIgnoreCase(formType)) {
                documentBytes = form9Service.generateForm9(finalData);
                filename = "Filled_Patent_Form_9.docx";

            } else if ("form5".equalsIgnoreCase(formType)) {
                documentBytes = form5Service.generateForm5(finalData);
                filename = "Filled_Patent_Form_5.docx";

            } else if ("form3".equalsIgnoreCase(formType)) {
                documentBytes = form3Service.generateForm3(finalData);
                filename = "Filled_Patent_Form_3.docx";

            } else if ("form2".equalsIgnoreCase(formType)) {
                byte[] sourceFileBytes = (sourceFile != null && !sourceFile.isEmpty())
                        ? sourceFile.getBytes()
                        : null;
                documentBytes = form2Service.generateForm2(finalData, sourceFileBytes);
                filename = "Filled_Patent_Form_2.docx";

            }
            else if ("form28".equalsIgnoreCase(formType)) {
                documentBytes = form28Service.generateForm28(finalData);
                filename = "Filled_Patent_Form_28.docx";
            }else {
                documentBytes = generatorService.generateFilledForm1(finalData);
                filename = "Filled_Patent_Form_1.docx";
            }

            if (documentBytes == null || documentBytes.length == 0) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(documentBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}