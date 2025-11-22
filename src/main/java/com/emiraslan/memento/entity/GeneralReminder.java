package com.emiraslan.memento.entity;

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
@Table(name = "GeneralReminders")
public class GeneralReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Integer reminderId;

    // Owner of the reminder(the patient)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    // Creator of the reminder(doctor, relative, or the patient themselves)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = true)
    private User creator;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "reminder_time", nullable = false)
    private LocalDateTime reminderTime;

    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "recurrence_rule", length = 100)
    private String recurrenceRule;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;
}
