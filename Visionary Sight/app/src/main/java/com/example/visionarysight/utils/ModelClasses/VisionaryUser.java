package com.example.visionarysight.utils.ModelClasses;

import java.io.Serializable;
import com.google.firebase.Timestamp;

public class VisionaryUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fullName;
    private String email;
    private String firstName;
    private String familyName;
    private Timestamp registrationDateTime;

    public VisionaryUser() {}

    public VisionaryUser(String fullName, String email, String firstName, String familyName, Timestamp registrationDateTime) {
        this.fullName = fullName;
        this.email = email;
        this.firstName = firstName;
        this.familyName = familyName;
        this.registrationDateTime = registrationDateTime;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Timestamp getRegistrationDateTime() {
        return registrationDateTime;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public void setRegistrationDateTime(Timestamp registrationDateTime) {
        this.registrationDateTime = registrationDateTime;
    }
}
