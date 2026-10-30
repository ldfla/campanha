package la.meleva.fundraiser.payment;

public enum OrderStatus {

    CREATED,
    AWAITING_PAYMENT,
    CONFIRMED,
    RECONCILED,
    CANCELLED,
    EXPIRED,
    REFUND_PENDING,
    REFUNDED
}
