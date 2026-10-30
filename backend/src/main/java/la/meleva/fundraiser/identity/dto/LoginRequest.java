package la.meleva.fundraiser.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Número de WhatsApp é obrigatório")
        @Pattern(regexp = "^\\+?[1-9]\\d{10,14}$", message = "Número de telefone inválido")
        String phoneNumber
) {}
