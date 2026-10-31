package la.meleva.fundraiser.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/payments")
public class PaymentController {
    private final OrderService orders;
    public PaymentController(OrderService orders){this.orders=orders;}
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('PARTICIPANT')")
    public OrderService.OrderReceipt create(@AuthenticationPrincipal AppUserDetails principal,@Valid @RequestBody CreateOrderRequest request){return orders.createOrder(request.campaignId(),principal.userId(),request.amountCents(),request.idempotencyKey());}
    @PostMapping("/webhooks/mock") @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public WebhookResponse webhook(@Valid @RequestBody WebhookRequest request){return new WebhookResponse(orders.receiveWebhook(request.payload(),request.signature()));}
    public record CreateOrderRequest(@NotNull Long campaignId,@Positive long amountCents,@NotBlank String idempotencyKey) {}
    public record WebhookRequest(@NotBlank String payload,@NotBlank String signature) {}
    public record WebhookResponse(boolean processed) {}
}
