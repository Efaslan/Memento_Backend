package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.MedicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MedicationLogs")
public class MedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_log_id")
    private Integer medicationLogId;

    // FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_time_id", nullable = false)
    private MedicationScheduleTime scheduleTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    // The timestamp when the patient physically takes their medicine.
    @Column(name = "taken_at", columnDefinition = "DATETIME2 DEFAULT GETDATE()")
    @Builder.Default
    private LocalDateTime takenAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING) // TAKEN, LATE_DOSE, SKIPPED
    @Column(name = "status", length = 50)
    private MedicationStatus status;
}