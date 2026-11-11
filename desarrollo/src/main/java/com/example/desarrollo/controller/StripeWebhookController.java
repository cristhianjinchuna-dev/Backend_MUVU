package com.example.desarrollo.controller;

import com.example.desarrollo.service.PagoPublicidadService;
import com.example.desarrollo.service.stripe.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stripe")
public class StripeWebhookController {

    private final PagoPublicidadService pagoPublicidadService;
    private final StripeService stripeService;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirEvento(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank() || webhookSecret.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Firma de webhook Stripe inválida");
            return ResponseEntity.badRequest().build();
        }

        if (!"payment_intent.succeeded".equals(event.getType())) {
            return ResponseEntity.ok().build();
        }

        try {
            String intentId = com.google.gson.JsonParser.parseString(
                    event.getDataObjectDeserializer().getRawJson()).getAsJsonObject().get("id").getAsString();
            pagoPublicidadService.registrarPagoConfirmado(stripeService.obtenerIntento(intentId));
            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            log.error("No se pudo recuperar PaymentIntent del evento Stripe {}", event.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            log.error("No se pudo procesar el evento Stripe {}", event.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
