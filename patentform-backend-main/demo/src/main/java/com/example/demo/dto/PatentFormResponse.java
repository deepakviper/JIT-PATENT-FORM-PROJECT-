package com.example.demo.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PatentFormResponse {
    private String applicationType = "Ordinary";
    private ApplicantDTO applicant = new ApplicantDTO();
    private List<InventorDTO> inventors = new ArrayList<>();
    private String titleOfInvention;
    private AttachmentsDTO attachments = new AttachmentsDTO();

    // 💡 ADD THESE THREE FIELDS TO CAPTURE THE ACTUAL PARSED TEXT FROM USER UPLOADS:
    private String description = "";
    private String claims = "";
    private String abstractText = "";

    @Data
    public static class ApplicantDTO {
        private String name = "";
        private String nationality = "Indian";
        private String country = "India";
        private AddressDTO address = new AddressDTO();
    }

    @Data
    public static class AddressDTO {
        private String street = "";
        private String city = "";
        private String district = "";
        private String state = "";
        private String country = "";
        private String pincode = "";
        private String houseNo = "";
        private String principalName = "";
        private String telephone = "";
        private String mobile = "";
        private String fax = "";
        private String email = "";
    }

    @Data
    public static class InventorDTO {
        private String name;
        private String nationality = "Indian";
        private String country = "India";
    }

    @Data
    public static class AttachmentsDTO {
        private int specificationPages;
        private int claimsCount;
        private int drawingsCount;
    }
}