package com.example.desarrollo.service;

import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.PagoPublicidadResponseDTO;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.exceptions.PaymentException;
import com.example.desarrollo.model.Arrendador;
import com.example.desarrollo.model.Habitacion;
import com.example.desarrollo.model.PagoPublicidad;
import com.example.desarrollo.model.Rol;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.PagoPublicidadRepository;
import com.example.desarrollo.service.stripe.StripeService;
import com.stripe.exception.ApiConnectionException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoPublicidadServiceTest {

    @Mock private PagoPublicidadRepository pagoPublicidadRepository;
    @Mock private HabitacionRepository habitacionRepository;
    @Spy private StripeService stripeService = new StripeService();
    @Spy private ModelMapper modelMapper = new ModelMapper();
    @Mock private UsuarioService usuarioService;
    @Mock private ApplicationEventPublisher publisher;
    @InjectMocks private PagoPublicidadService pagoPublicidadService;

    private Habitacion habitacion;

    @BeforeEach
    void setUp() {
        Arrendador arrendador = new Arrendador();
        arrendador.setId(2L);
        arrendador.setRol(Rol.ARRENDADOR);
        arrendador.setCorreo("rosa@muvu.com");
        arrendador.setNombre("Rosa");
        habitacion = new Habitacion();
        habitacion.setId(1L);
        habitacion.setDireccion("Av. Grau 100");
        habitacion.setArrendador(arrendador);
        lenient().when(habitacionRepository.findById(1L)).thenReturn(Optional.of(habitacion));
    }

    @Test
    void crearIntento_validaTitularYPlanAntesDeLlamarStripe() throws Exception {
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_test");
        when(intent.getClientSecret()).thenReturn("secret_test");
        doReturn(intent).when(stripeService).crearIntentoPublicidad(1L, 2L, 30.0);

        var response = pagoPublicidadService.crearIntento(1L, 30.0);
        assertEquals("pi_test", response.getPaymentIntentId());
        assertEquals("secret_test", response.getClientSecret());
        verify(stripeService).crearIntentoPublicidad(1L, 2L, 30.0);
    }

    @Test
    void crearIntento_montoInvalido_noLlamaStripe() throws Exception {
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        assertThrows(InvalidOperationException.class, () -> pagoPublicidadService.crearIntento(1L, 40.0));
        verify(stripeService, never()).crearIntentoPublicidad(any(), any(), any());
    }

    @Test
    void crearIntento_habitacionAjena_lanzaForbidden() throws Exception {
        when(usuarioService.getIdUsuarioActual()).thenReturn(99L);
        assertThrows(ForbiddenException.class, () -> pagoPublicidadService.crearIntento(1L, 30.0));
        verify(stripeService, never()).crearIntentoPublicidad(any(), any(), any());
    }

    @Test
    void crearIntento_stripeFalla_lanzaPaymentException() throws Exception {
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        doThrow(new ApiConnectionException("sin conexión"))
                .when(stripeService).crearIntentoPublicidad(1L, 2L, 30.0);
        assertThrows(PaymentException.class, () -> pagoPublicidadService.crearIntento(1L, 30.0));
    }

    @Test
    void pagoConfirmado_registraYDestacaSoloTrasEstadoSucceeded() {
        when(pagoPublicidadRepository.findByStripePaymentIntentId("pi_ok")).thenReturn(Optional.empty());
        when(pagoPublicidadRepository.save(any(PagoPublicidad.class))).thenAnswer(inv -> inv.getArgument(0));
        PaymentIntent intent = paymentIntent("pi_ok", "succeeded");

        PagoPublicidadResponseDTO response = pagoPublicidadService.registrarPagoConfirmado(intent);

        assertEquals(1L, response.getHabitacionId());
        assertEquals(30.0, response.getMonto());
        assertEquals(com.example.desarrollo.model.MetodoPago.APPLE_PAY, response.getMetodoPago());
        assertTrue(habitacion.getEsDestacada());
        verify(pagoPublicidadRepository).save(argThat(p -> "pi_ok".equals(p.getStripePaymentIntentId())));
        verify(publisher).publishEvent(any(NotificacionCorreoEvent.class));
    }

    @Test
    void pagoConfirmado_noSucceededNoPersiste() {
        when(pagoPublicidadRepository.findByStripePaymentIntentId("pi_wait")).thenReturn(Optional.empty());
        assertThrows(PaymentException.class,
                () -> pagoPublicidadService.registrarPagoConfirmado(paymentIntent("pi_wait", "processing")));
        assertFalse(Boolean.TRUE.equals(habitacion.getEsDestacada()));
        verify(pagoPublicidadRepository, never()).save(any());
    }

    @Test
    void webhookDuplicado_devuelvePagoExistenteSinDuplicar() {
        PagoPublicidad existing = pago(LocalDate.now().plusDays(10));
        when(pagoPublicidadRepository.findByStripePaymentIntentId("pi_ok")).thenReturn(Optional.of(existing));
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_ok");

        PagoPublicidadResponseDTO response = pagoPublicidadService.registrarPagoConfirmado(intent);

        assertEquals(existing.getId(), response.getId());
        verify(pagoPublicidadRepository, never()).save(any());
    }

    @Test
    void listarTodos_devuelvePagosDelArrendador() {
        when(usuarioService.getIdUsuarioActual()).thenReturn(2L);
        when(pagoPublicidadRepository.findByHabitacionArrendadorId(2L)).thenReturn(List.of(pago(LocalDate.now())));
        assertEquals(1, pagoPublicidadService.listarTodos().size());
    }

    @Test
    void desactivarPublicidadesVencidas_quitaElDestacado() {
        habitacion.setEsDestacada(true);
        when(pagoPublicidadRepository.findByFechaFinBefore(any(LocalDate.class)))
                .thenReturn(List.of(pago(LocalDate.now().minusDays(1))));
        pagoPublicidadService.desactivarPublicidadesVencidas();
        assertFalse(habitacion.getEsDestacada());
        verify(habitacionRepository).save(habitacion);
    }

    private PaymentIntent paymentIntent(String id, String status) {
        PaymentIntent intent = mock(PaymentIntent.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("habitacionId", "1");
        metadata.put("arrendadorId", "2");
        metadata.put("monto", "30.0");
        when(intent.getId()).thenReturn(id);
        lenient().when(intent.getStatus()).thenReturn(status);
        lenient().when(intent.getCurrency()).thenReturn("pen");
        lenient().when(intent.getAmount()).thenReturn(3000L);
        lenient().when(intent.getMetadata()).thenReturn(metadata);
        PaymentMethod paymentMethod = mock(PaymentMethod.class);
        PaymentMethod.Card card = mock(PaymentMethod.Card.class);
        PaymentMethod.Card.Wallet wallet = mock(PaymentMethod.Card.Wallet.class);
        lenient().when(intent.getPaymentMethodObject()).thenReturn(paymentMethod);
        lenient().when(paymentMethod.getCard()).thenReturn(card);
        lenient().when(card.getWallet()).thenReturn(wallet);
        lenient().when(wallet.getType()).thenReturn("apple_pay");
        return intent;
    }

    private PagoPublicidad pago(LocalDate fechaFin) {
        PagoPublicidad pago = new PagoPublicidad();
        pago.setId(4L);
        pago.setHabitacion(habitacion);
        pago.setMonto(30.0);
        pago.setStripePaymentIntentId("pi_ok");
        pago.setFechaInicio(fechaFin.minusDays(30));
        pago.setFechaFin(fechaFin);
        return pago;
    }
}
