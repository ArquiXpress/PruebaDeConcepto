package com.arquixpress.marketplace.payments;

public record PaymentGatewayResult(PaymentStatus status, String externalReference, String message) {
    public static PaymentGatewayResult approved(String reference) {
        return new PaymentGatewayResult(PaymentStatus.APPROVED, reference, "Pago aprobado");
    }

    public static PaymentGatewayResult rejected(String reference) {
        return new PaymentGatewayResult(PaymentStatus.REJECTED, reference, "Pago rechazado");
    }

    public static PaymentGatewayResult pending(String message) {
        return new PaymentGatewayResult(PaymentStatus.PENDING, null, message);
    }
}
