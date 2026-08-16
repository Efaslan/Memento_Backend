package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogRequestDto {

    @NotNull(message = "DESCRIPTION_CANNOT_BE_NULL")
    @Size(max = 1000, message = "DESCRIPTION_TOO_LONG")
    private String description;

    @NotNull(message = "IS_MANUAL_EDITING_CANNOT_BE_NULL")
    private Boolean isManualEditing;
}
