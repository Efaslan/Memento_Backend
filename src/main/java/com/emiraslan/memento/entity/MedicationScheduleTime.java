package com.emiraslan.memento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MedicationScheduleTimes")
public class MedicationScheduleTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_id")
    private Integer timeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false) // FK pointing to a schedule.
    private MedicationSchedule schedule;

    // The time assigned to the schedule. Nullable to cover isPrn = 1 medicines.
    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;
}