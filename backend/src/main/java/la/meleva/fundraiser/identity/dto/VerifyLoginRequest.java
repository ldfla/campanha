package la.meleva.fundraiser.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyLoginRequest(
        @NotBlank(message = "Número de WhatsApp é obrigatório")
        @Pattern(regexp = "^\\+?[1-9]\\d{10,14}$", message = "Número de telefone inválido")
        String phoneNumber,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Código deve ter 6 dígitos") String code
) {}
