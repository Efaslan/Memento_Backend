package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedLocationRequestDto {

    @NotBlank(message = "LOCATION_NAME_REQUIRED")
    @Size(max = 100, message = "LOCATION_NAME_TOO_LONG")
    private String locationName;

    @NotNull(message = "LATITUDE_REQUIRED")
    @Digits(integer = 3, fraction = 6, message = "INVALID_LATITUDE_FORMAT")
    private BigDecimal latitude;

    @NotNull(message = "LONGITUDE_REQUIRED")
    @Digits(integer = 3, fraction = 6, message = "INVALID_LONGITUDE_FORMAT")
    private BigDecimal longitude;

    // optional
    @Size(max = 255, message = "ADDRESS_DETAILS_TOO_LONG")
    private String addressDetails;
}