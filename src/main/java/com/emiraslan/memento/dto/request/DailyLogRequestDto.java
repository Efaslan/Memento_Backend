package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogRequestDto {

    @Size(max = 255, message = "DESCRIPTION_TOO_LONG")
    private String description;

    @Range(min = 0, max = 5000, message = "QUANTITY_ML_MIN_0_MAX_5000")
    private Integer quantityMl;
}
