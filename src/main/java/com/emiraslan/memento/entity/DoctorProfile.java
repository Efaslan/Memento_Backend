package com.emiraslan.memento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DoctorProfiles")
public class DoctorProfile {

    @Id
    @Column(name = "doctor_user_id")
    private Integer doctorUserId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Needed when the PK is also a FK because we don't generate the PK value, we just find it
    @JoinColumn(name = "doctor_user_id") // FK
    private User doctor;

    @Column(name = "specialization", length = 100)
    private String specialization;

    @Column(name = "hospital_name", length = 200)
    private String hospitalName;

    @Column(name = "title", length = 50)
    private String title;
}
