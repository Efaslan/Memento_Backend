package com.emiraslan.memento.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MedicationScheduleRequestDto {

    @NotNull(message = "PATIENT_ID_REQUIRED")
    private Integer patientUserId;

    @NotBlank(message = "MEDICATION_NAME_REQUIRED")
    @Size(max = 100, message = "MEDICATION_NAME_TOO_LONG")
    private String medicationName;

    @NotBlank(message = "DOSAGE_REQUIRED")
    @Size(max = 50, message = "DOSAGE_TOO_LONG")
    private String dosage;

    @Size(max = 255, message = "NOTES_TOO_LONG")
    private String notes;

    @NotNull(message = "START_DATE_REQUIRED")
    private LocalDate startDate;

    // nullable in case isPrn = true
    private LocalDate endDate;

    @NotNull(message = "IS_PRN_REQUIRED")
    private Boolean isPrn;

    // nullable in case isPrn = true
    @Schema(type = "array", example = "[\"09:00\", \"21:00\"]")
    private List<LocalTime> times;
}