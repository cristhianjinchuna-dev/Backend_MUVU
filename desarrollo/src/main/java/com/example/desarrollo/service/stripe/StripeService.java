package com.example.desarrollo.service.stripe;

import com.example.desarrollo.exceptions.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public PaymentIntent crearIntentoPublicidad(Long habitacionId, Long arrendadorId, Double monto) throws StripeException {
        long montoEnCentimos = Math.round(monto * 100);
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montoEnCentimos)
                .setCurrency("pen")
                .addPaymentMethodType("card")
                .putMetadata("habitacionId", String.valueOf(habitacionId))
                .putMetadata("arrendadorId", String.valueOf(arrendadorId))
                .putMetadata("monto", String.valueOf(monto))
                .build();
        return PaymentIntent.create(params);
    }

    public PaymentIntent obtenerIntento(String paymentIntentId) throws StripeException {
        PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                .addExpand("payment_method")
                .build();
        return PaymentIntent.retrieve(paymentIntentId, params, null);
    }

    public void validarPagoPublicidad(PaymentIntent intent, Long habitacionId, Long arrendadorId, Double monto) {
        if (intent.getMetadata() == null) {
            throw new PaymentException("El intento de Stripe no contiene metadatos de publicidad");
        }
        String montoMetadata = intent.getMetadata().get("monto");
        long montoEsperado = Math.round(monto * 100);
        if (!"succeeded".equals(intent.getStatus())
                || !"pen".equalsIgnoreCase(intent.getCurrency())
                || intent.getAmount() == null
                || intent.getAmount() != montoEsperado
                || !String.valueOf(habitacionId).equals(intent.getMetadata().get("habitacionId"))
                || !String.valueOf(arrendadorId).equals(intent.getMetadata().get("arrendadorId"))
                || montoMetadata == null
                || Double.compare(monto, Double.parseDouble(montoMetadata)) != 0) {
            throw new PaymentException("El pago de Stripe no coincide con el plan solicitado");
        }
    }
}
