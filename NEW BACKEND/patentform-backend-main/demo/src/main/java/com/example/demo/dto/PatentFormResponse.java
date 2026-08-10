package com.example.demo.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PatentFormResponse {

    private String applicationType = "Ordinary";
    private ApplicantDTO applicant = new ApplicantDTO();
    private PrincipalDTO principal = new PrincipalDTO();
    private List<InventorDTO> inventors = new ArrayList<>();
    private String titleOfInvention;
    private AttachmentsDTO attachments = new AttachmentsDTO();

    // ✅ Existing plain-text fields (used by Form 1, Form 3, Form 5)
    private String description = "";
    private String claims = "";
    private String abstractText = "";

    // ✅ NEW: XML block fields (used by Form 2 — preserves formatting, images, tables)
    private String descriptionXml = "";
    private String claimsXml = "";
    private String abstractXml = "";

    // ✅ ✅ ✅ NEW FIELDS FOR FORM 28 (Option B)
    private String role = "";        // {role}
    private String clgName = "";     // {clg_name}

    // ------------------------------------------------------------------
    // Applicant Details
    // ------------------------------------------------------------------
    @Data
    public static class ApplicantDTO {
        private String name = "";
        private String nationality = "Indian";
        private String country = "India";
        private AddressDTO address = new AddressDTO();
    }

    // ------------------------------------------------------------------
    // Address Details
    // ------------------------------------------------------------------
    @Data
    public static class AddressDTO {
        private String houseNo = "";
        private String street = "";
        private String city = "";
        private String district = "";
        private String state = "";
        private String country = "";
        private String pincode = "";
    }

    // ------------------------------------------------------------------
    // Principal Details (from frontend)
    // ------------------------------------------------------------------
    @Data
    public static class PrincipalDTO {
        private String name = "";
        private String designation = "";
        private String telephone = "";
        private String mobile = "";
        private String fax = "";
        private String email = "";
    }

    // ------------------------------------------------------------------
    // Inventor Details
    // ------------------------------------------------------------------
    @Data
    public static class InventorDTO {
        private String name;
        private String nationality = "Indian";
        private String country = "India";
    }

    // ------------------------------------------------------------------
    // Attachments Metadata
    // ------------------------------------------------------------------
    @Data
    public static class AttachmentsDTO {
        private int specificationPages;
        private int claimsCount;
        private int drawingsCount;
    }
}