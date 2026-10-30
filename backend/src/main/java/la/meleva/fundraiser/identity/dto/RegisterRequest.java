package la.meleva.fundraiser.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String firstName,

        @NotBlank(message = "Sobrenome é obrigatório")
        @Size(min = 2, max = 150, message = "Sobrenome deve ter entre 2 e 150 caracteres")
        String surname,

        @NotBlank(message = "Número de WhatsApp é obrigatório")
        @Pattern(regexp = "^\\+?[1-9]\\d{10,14}$", message = "Número de telefone inválido")
        String phoneNumber
) {}
