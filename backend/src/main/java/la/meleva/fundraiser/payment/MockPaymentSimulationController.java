package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulação local de liquidação do PSP mock. Existe **apenas** no profile `local` e exige
 * `SYSTEM_ADMIN`; nunca é exposto em produção. Marca a cobrança simulada do pedido como paga e
 * reconcilia o pedido, permitindo demonstrar o checkout de ponta a ponta em sandbox.
 */
@RestController
@RequestMapping("/payments/mock")
@Profile("local")
public class MockPaymentSimulationController {

    private final MockPaymentProvider provider;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final OrderService orderService;

    MockPaymentSimulationController(MockPaymentProvider provider, OrderRepository orders,
                                    PaymentRepository payments, OrderService orderService) {
        this.provider = provider;
        this.orders = orders;
        this.payments = payments;
        this.orderService = orderService;
    }

    /** Marca o pedido simulado como pago no PSP mock e reconcilia (idempotente por pedido). */
    @PostMapping("/orders/{orderPublicId}/settle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void settle(@PathVariable String orderPublicId) {
        var order = orders.findByPublicId(orderPublicId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var payment = payments.findByOrderId(order.getId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        provider.setState(payment.getProviderChargeId(), PaymentProvider.ChargeState.PAID);
        orderService.reconcile(payment.getId());
    }
}
