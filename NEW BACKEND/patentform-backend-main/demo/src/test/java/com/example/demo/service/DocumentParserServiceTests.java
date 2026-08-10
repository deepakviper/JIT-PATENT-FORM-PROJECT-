package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import java.io.InputStream;

public class DocumentParserServiceTests {

    @Test
    public void testParser() throws Exception {
        DocumentParserService parserService = new DocumentParserService();
        ClassPathResource resource = new ClassPathResource("Form2,main .docx");
        try (InputStream is = resource.getInputStream()) {
            MockMultipartFile file = new MockMultipartFile("file", "Form2,main .docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", is);
            PatentFormResponse response = parserService.parseUploadedDocument(file);
            System.out.println("Abstract Text Length: " + (response.getAbstractText() != null ? response.getAbstractText().length() : 0));
            System.out.println("Description XML Length: " + (response.getDescriptionXml() != null ? response.getDescriptionXml().length() : 0));
            System.out.println("Claims XML Length: " + (response.getClaimsXml() != null ? response.getClaimsXml().length() : 0));
        }
    }
}
