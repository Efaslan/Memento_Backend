package com.emiraslan.memento.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class MedicationScheduleDto {
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

    // Time information from MedicationSchedulesTime. We combine the two tables in one dto class.
    @JsonFormat(pattern = "HH:mm")
    @ArraySchema(schema = @Schema(
            type = "string",
            pattern = "HH:mm",
            example = "09:00"
    ))
    private List<LocalTime> times;
}