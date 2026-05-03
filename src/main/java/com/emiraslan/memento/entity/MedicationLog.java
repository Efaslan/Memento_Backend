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
@Table(name = "medication_logs", indexes = {
        // patient -> taken_at, for finding logs of a patient from e.g. last 7 days
        @Index(name = "idx_medlog_patient_date", columnList = "patient_user_id, taken_at"),

        // for CRON job NOT EXISTS query,
        // time_id -> taken_at, finds if a log was taken for a time_id -> today
        @Index(name = "idx_medlog_schedule_date", columnList = "schedule_time_id, taken_at")
})
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
    @Column(name = "taken_at")
    @Builder.Default
    private LocalDateTime takenAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING) // TAKEN, LATE_DOSE, SKIPPED
    @Column(name = "status", length = 10)
    private MedicationStatus status;
}