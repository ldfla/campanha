package la.meleva.fundraiser.privacy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Direitos do titular sobre os próprios dados (LGPD) e CPF para recebimento de prêmio. */
@RestController
@RequestMapping("/privacy/me")
public class PrivacyController {

    private final PrivacyService service;
    private final CpfService cpfs;

    PrivacyController(PrivacyService service, CpfService cpfs) {
        this.service = service;
        this.cpfs = cpfs;
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public PrivacyService.ExportView export(@AuthenticationPrincipal AppUserDetails principal) {
        return service.export(principal.userId());
    }

    @PostMapping("/anonymize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void anonymize(@AuthenticationPrincipal AppUserDetails principal) {
        service.anonymize(principal.userId());
    }

    /** CPF é informado apenas quando o titular vai receber o prêmio. */
    @PutMapping("/cpf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void registerCpf(@AuthenticationPrincipal AppUserDetails principal,
                            @Valid @RequestBody CpfRequest request) {
        cpfs.register(principal.userId(), request.cpf());
    }

    @GetMapping("/cpf")
    @PreAuthorize("isAuthenticated()")
    public CpfView maskedCpf(@AuthenticationPrincipal AppUserDetails principal) {
        return new CpfView(cpfs.masked(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.name())));
    }

    public record CpfRequest(@NotBlank String cpf) { }

    public record CpfView(String maskedCpf) { }
}
