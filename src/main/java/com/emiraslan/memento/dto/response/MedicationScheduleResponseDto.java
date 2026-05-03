package com.emiraslan.memento.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationScheduleResponseDto {
    private Integer scheduleId;
    private Integer patientUserId;
    private Integer doctorUserId;
    private String doctorName; // For showing the doctor's name on mobile
    private String medicationName;
    private String dosage;
    private String notes;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isPrn;
    private Boolean isActive;

    private List<TimeInfoDto> times;

    // Time information from MedicationSchedulesTime. We combine the two tables in one dto class.
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeInfoDto{
        private Integer timeId;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime time;
    }
}