package com.example.desarrollo.service.stripe;

import com.example.desarrollo.exceptions.PaymentException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StripeServiceTest {

    private final StripeService stripeService = new StripeService();

    @Test
    void crearIntentoPublicidad_cobraPENEnCentimosYAsociaMetadata() throws Exception {
        PaymentIntent intent = mock(PaymentIntent.class);
        try (MockedStatic<PaymentIntent> api = mockStatic(PaymentIntent.class)) {
            api.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenReturn(intent);

            assertSame(intent, stripeService.crearIntentoPublicidad(7L, 12L, 30.0));

            ArgumentCaptor<PaymentIntentCreateParams> captor = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
            api.verify(() -> PaymentIntent.create(captor.capture()));
            assertEquals(3000L, captor.getValue().getAmount());
            assertEquals("pen", captor.getValue().getCurrency());
            assertEquals(List.of("card"), captor.getValue().getPaymentMethodTypes());
            assertEquals("7", captor.getValue().getMetadata().get("habitacionId"));
            assertEquals("12", captor.getValue().getMetadata().get("arrendadorId"));
        }
    }

    @Test
    void validarPagoPublicidad_rechazaImporteMonedaEstadoOMetadataIncorrectos() {
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getStatus()).thenReturn("succeeded");
        when(intent.getCurrency()).thenReturn("pen");
        when(intent.getAmount()).thenReturn(3000L);
        when(intent.getMetadata()).thenReturn(Map.of("habitacionId", "7", "arrendadorId", "12", "monto", "30.0"));

        assertDoesNotThrow(() -> stripeService.validarPagoPublicidad(intent, 7L, 12L, 30.0));
        assertThrows(PaymentException.class, () -> stripeService.validarPagoPublicidad(intent, 7L, 12L, 54.0));
    }
}
