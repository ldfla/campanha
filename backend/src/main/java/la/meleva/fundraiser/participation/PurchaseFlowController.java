package la.meleva.fundraiser.participation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferService;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferService.OfferView;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fluxo de compra com reserva temporária (DEMO). Expõe as ofertas definidas no servidor,
 * inicia a reserva de números e só confirma a atribuição após o pagamento mock conciliado.
 */
@RestController
@RequestMapping("/participations/campaigns/{campaignId}")
public class PurchaseFlowController {

    private final PurchaseOfferService offers;
    private final ReservationCheckoutService checkout;

    PurchaseFlowController(PurchaseOfferService offers, ReservationCheckoutService checkout) {
        this.offers = offers;
        this.checkout = checkout;
    }

    @GetMapping("/offers")
    @PreAuthorize("isAuthenticated()")
    public List<OfferView> listOffers(@PathVariable String campaignId) {
        return offers.list(campaignId);
    }

    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public OfferView createOffer(@AuthenticationPrincipal AppUserDetails principal,
                                 @PathVariable String campaignId,
                                 @Valid @RequestBody CreateOfferRequest request) {
        return offers.create(principal.userId(), campaignId, request.quantity(), request.totalCents());
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public CheckoutResponse start(@AuthenticationPrincipal AppUserDetails principal,
                                  @PathVariable String campaignId,
                                  @Valid @RequestBody CheckoutRequest request) {
        return CheckoutResponse.from(checkout.start(principal.userId(), campaignId, request.idempotencyKey(),
                request.offerId(), request.entityId(), request.referralCode()));
    }

    @PostMapping("/checkout/{requestPublicId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void confirm(@AuthenticationPrincipal AppUserDetails principal,
                        @PathVariable String campaignId,
                        @PathVariable String requestPublicId) {
        checkout.confirm(principal.userId(), campaignId, requestPublicId);
    }

    public record CreateOfferRequest(@Min(1) @Max(100) int quantity, @Positive long totalCents) { }

    public record CheckoutRequest(
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String idempotencyKey,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String offerId,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String entityId,
            @Nullable @Size(max = 20) String referralCode) { }

    public record CheckoutResponse(String requestPublicId, Instant expiresAt, List<String> numbers,
                                   String orderPublicId, long amountCents, String currency, boolean simulation) {
        public CheckoutResponse {
            numbers = List.copyOf(numbers);
        }

        static CheckoutResponse from(ReservationCheckoutService.CheckoutView view) {
            return new CheckoutResponse(view.reservation().requestPublicId(), view.reservation().expiresAt(),
                    view.reservation().numbers(), view.orderPublicId(), view.amountCents(), view.currency(),
                    view.simulation());
        }
    }
}
