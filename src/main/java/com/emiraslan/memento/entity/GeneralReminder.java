package com.emiraslan.memento.entity;

import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.RecurrenceRule;
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
@Table(name = "general_reminders", indexes = {
        // for monthly calendar view, patient -> reminder time
        @Index(name = "idx_reminder_patient_time", columnList = "patient_user_id, reminder_time"),

        // for the CRON job to find due reminders, just reminder time
        @Index(name = "idx_reminder_time_only", columnList = "reminder_time")
})
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
    @JoinColumn(name = "creator_user_id")
    private User creator;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "reminder_time", nullable = false)
    private LocalDateTime reminderTime;

    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_rule", length = 10)
    private RecurrenceRule recurrenceRule;
}
