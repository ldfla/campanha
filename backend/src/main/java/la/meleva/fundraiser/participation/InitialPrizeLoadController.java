package la.meleva.fundraiser.participation;

import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carga inicial de prêmios. A leitura (export/hash) é pública para transparência — o arquivo
 * contém apenas número, prêmio e timestamp, sem dados pessoais.
 */
@RestController
@RequestMapping("/participations/campaigns/{campaignId}/numbers/prize-load")
public class InitialPrizeLoadController {

    private final InitialPrizeLoadService service;

    InitialPrizeLoadController(InitialPrizeLoadService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public InitialPrizeLoadService.LoadView load(@AuthenticationPrincipal AppUserDetails principal,
                                                 @PathVariable String campaignId) {
        return service.load(principal.userId(), campaignId);
    }

    @GetMapping
    public InitialPrizeLoadService.LoadView current(@PathVariable String campaignId) {
        return service.current(campaignId);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@PathVariable String campaignId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=numeros-premiados.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(service.exportCsv(campaignId));
    }

    @GetMapping("/verify")
    public InitialPrizeLoadService.VerifyView verify(@PathVariable String campaignId) {
        return service.verify(campaignId);
    }
}
