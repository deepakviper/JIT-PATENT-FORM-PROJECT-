package com.example.demo.service;

import com.example.demo.dto.PatentFormResponse;

public class PatentOfficeHelper {

    public static String determineBranch(PatentFormResponse data) {
        if (data == null || data.getApplicant() == null) {
            return "Chennai";
        }
        
        String city = "";
        String state = "";
        
        if (data.getApplicant().getAddress() != null) {
            if (data.getApplicant().getAddress().getCity() != null) {
                city = data.getApplicant().getAddress().getCity().toLowerCase().trim();
            }
            if (data.getApplicant().getAddress().getState() != null) {
                state = data.getApplicant().getAddress().getState().toLowerCase().trim();
            }
        }
        
        String combined = city + " " + state;
        
        // Mumbai Jurisdiction
        if (combined.contains("maharashtra") || combined.contains("mumbai") || combined.contains("pune") || 
            combined.contains("gujarat") || combined.contains("ahmedabad") || combined.contains("goa") || 
            combined.contains("madhya pradesh") || combined.contains("chhattisgarh")) {
            return "Mumbai";
        }
        
        // Delhi Jurisdiction
        if (combined.contains("delhi") || combined.contains("haryana") || combined.contains("punjab") || 
            combined.contains("rajasthan") || combined.contains("jaipur") || combined.contains("uttar pradesh") || 
            combined.contains("noida") || combined.contains("uttarakhand") || combined.contains("himachal")) {
            return "Delhi";
        }
        
        // Kolkata Jurisdiction
        if (combined.contains("west bengal") || combined.contains("kolkata") || combined.contains("bihar") || 
            combined.contains("odisha") || combined.contains("orissa") || combined.contains("jharkhand") || 
            combined.contains("assam")) {
            return "Kolkata";
        }
        
        // Default to Chennai for southern states (Tamil Nadu, Karnataka, Kerala, Andhra Pradesh, Telangana)
        return "Chennai";
    }
}
