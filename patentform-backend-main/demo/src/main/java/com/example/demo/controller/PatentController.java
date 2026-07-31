package com.example.demo.controller;

import com.example.demo.dto.PatentFormResponse;
import com.example.demo.service.DocumentGeneratorService;
import com.example.demo.service.DocumentParserService;
import com.example.demo.service.Form2GeneratorService;
import com.example.demo.service.Form3GeneratorService;
import com.example.demo.service.Form5GeneratorService; // 1. Added Form 5 import
import com.example.demo.service.Form28GeneratorService;
import com.example.demo.service.Form9GeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/patent")
@CrossOrigin(origins = "*") // Connects seamlessly with your React app
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
    private Form5GeneratorService form5Service; // 2. Added Form 5 service injection

    @Autowired
    private Form28GeneratorService form28Service;

    @Autowired
    private Form9GeneratorService form9Service;

    // Your existing parse endpoint
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

    // UPDATED DOWNLOAD ENDPOINT FOR FORM 1, FORM 2, FORM 3, AND FORM 5
    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadFilledForm(
            @RequestBody PatentFormResponse finalData,
            @RequestParam(value = "formType", defaultValue = "form1") String formType) {
        try {
            byte[] documentBytes;
            String filename;

            // 3. Dynamically routes based on formType including the new Form 5 handler
            if ("form5".equalsIgnoreCase(formType)) {
                documentBytes = form5Service.generateForm5(finalData);
                filename = "Filled_Patent_Form_5.docx";
            } else if ("form3".equalsIgnoreCase(formType)) {
                documentBytes = form3Service.generateForm3(finalData);
                filename = "Filled_Patent_Form_3.docx";
            } else if ("form2".equalsIgnoreCase(formType)) {
                documentBytes = form2Service.generateForm2(finalData);
                filename = "Filled_Patent_Form_2.docx";
            } else if ("form28".equalsIgnoreCase(formType)) {
                documentBytes = form28Service.generateForm28(finalData);
                filename = "Filled_Patent_Form_28.docx";
            } else if ("form9".equalsIgnoreCase(formType)) {
                documentBytes = form9Service.generateForm9(finalData);
                filename = "Filled_Patent_Form_9.docx";
            } else {
                documentBytes = generatorService.generateFilledForm1(finalData);
                filename = "Filled_Patent_Form_1.docx";
            }

            // If the template doesn't exist yet, return a clear error status
            if (documentBytes == null || documentBytes.length == 0) {
                return ResponseEntity.notFound().build();
            }

            // Set the HTTP headers dynamically using the determined filename
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(documentBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}