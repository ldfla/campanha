package la.meleva.fundraiser.identity.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record VerifyContactRequest(@NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phoneNumber, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {}
