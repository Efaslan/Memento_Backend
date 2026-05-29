package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsentRequest {
    @NotNull(message = "Onay türü boş bırakılamaz.")
    private ConsentType consentType;

    @NotBlank(message = "Döküman versiyonu boş bırakılamaz.")
    private String documentVersion;

    @NotNull(message = "Onay durumu (true/false) belirtilmelidir.")
    private Boolean isAccepted;
}
