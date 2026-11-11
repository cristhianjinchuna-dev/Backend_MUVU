package com.example.desarrollo.controller;

import com.example.desarrollo.service.PagoPublicidadService;
import com.example.desarrollo.service.stripe.StripeService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StripeWebhookControllerTest {

    private final PagoPublicidadService pagoPublicidadService = mock(PagoPublicidadService.class);
    private final StripeService stripeService = mock(StripeService.class);

    @Test
    void sinFirmaRechazaElWebhook() {
        StripeWebhookController controller = controller("whsec_test");

        assertEquals(400, controller.recibirEvento("{}", null).getStatusCode().value());
        verifyNoInteractions(pagoPublicidadService, stripeService);
    }

    @Test
    void firmaInvalidaRechazaElWebhook() {
        StripeWebhookController controller = controller("whsec_test");

        assertEquals(400, controller.recibirEvento("{}", "t=123,v1=invalid").getStatusCode().value());
        verifyNoInteractions(pagoPublicidadService, stripeService);
    }

    private StripeWebhookController controller(String secret) {
        StripeWebhookController controller = new StripeWebhookController(pagoPublicidadService, stripeService);
        ReflectionTestUtils.setField(controller, "webhookSecret", secret);
        return controller;
    }
}
