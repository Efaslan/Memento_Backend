package com.emiraslan.memento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileDto {
    private Integer doctorUserId;
    // additional info from User
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    // from DoctorProfile
    private String specialization;
    private String hospitalName;
    private String title;
}