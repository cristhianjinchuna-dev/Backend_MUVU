package com.example.desarrollo.service;

import com.example.desarrollo.Events.NotificacionCorreoEvent;
import com.example.desarrollo.dto.PagoPublicidadIntentResponseDTO;
import com.example.desarrollo.dto.PagoPublicidadResponseDTO;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.exceptions.PaymentException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.Arrendador;
import com.example.desarrollo.model.Habitacion;
import com.example.desarrollo.model.Mail;
import com.example.desarrollo.model.MetodoPago;
import com.example.desarrollo.model.PagoPublicidad;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.PagoPublicidadRepository;
import com.example.desarrollo.service.stripe.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoPublicidadService {

    private final PagoPublicidadRepository pagoPublicidadRepository;
    private final HabitacionRepository habitacionRepository;
    private final StripeService stripeService;
    private final ModelMapper modelMapper;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher publisher;

    public PagoPublicidadIntentResponseDTO crearIntento(Long habitacionId, Double monto) {
        Habitacion habitacion = buscarHabitacion(habitacionId);
        Long arrendadorId = usuarioService.getIdUsuarioActual();
        validarPropietario(habitacion, arrendadorId);
        calcularFechaFin(LocalDate.now(), monto);

        try {
            PaymentIntent intent = stripeService.crearIntentoPublicidad(habitacionId, arrendadorId, monto);
            return new PagoPublicidadIntentResponseDTO(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            log.error("No se pudo crear el intento de pago para la habitación {}", habitacionId, e);
            throw new PaymentException("No se pudo iniciar el pago en Stripe", e);
        }
    }

    @Transactional
    public PagoPublicidadResponseDTO confirmarDesdeCliente(String paymentIntentId) {
        try {
            PaymentIntent intent = stripeService.obtenerIntento(paymentIntentId);
            Long arrendadorId = usuarioService.getIdUsuarioActual();
            if (intent.getMetadata() == null
                    || !String.valueOf(arrendadorId).equals(intent.getMetadata().get("arrendadorId"))) {
                throw new ForbiddenException("Este pago no pertenece al arrendador autenticado");
            }
            return registrarPagoConfirmado(intent);
        } catch (StripeException e) {
            log.error("No se pudo verificar el intento de pago {}", paymentIntentId, e);
            throw new PaymentException("No se pudo verificar el pago en Stripe", e);
        }
    }

    @Transactional
    public PagoPublicidadResponseDTO registrarPagoConfirmado(PaymentIntent intent) {
        PagoPublicidad existente = pagoPublicidadRepository.findByStripePaymentIntentId(intent.getId()).orElse(null);
        if (existente != null) return modelMapper.map(existente, PagoPublicidadResponseDTO.class);

        try {
            Long habitacionId = Long.valueOf(intent.getMetadata().get("habitacionId"));
            Long arrendadorId = Long.valueOf(intent.getMetadata().get("arrendadorId"));
            Double monto = Double.valueOf(intent.getMetadata().get("monto"));
            Habitacion habitacion = buscarHabitacion(habitacionId);
            validarPropietario(habitacion, arrendadorId);
            stripeService.validarPagoPublicidad(intent, habitacionId, arrendadorId, monto);

            LocalDate fechaInicio = LocalDate.now();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, monto);
            PagoPublicidad pago = new PagoPublicidad();
            pago.setHabitacion(habitacion);
            pago.setMonto(monto);
            pago.setMetodoPago(detectarMetodoPagoWallet(intent));
            pago.setStripePaymentIntentId(intent.getId());
            pago.setFechaInicio(fechaInicio);
            pago.setFechaFin(fechaFin);
            pago = pagoPublicidadRepository.save(pago);

            habitacion.setEsDestacada(true);
            habitacionRepository.save(habitacion);
            log.info("Pago de Stripe {} registrado para habitación {}", intent.getId(), habitacionId);

            Arrendador arrendador = habitacion.getArrendador();
            publisher.publishEvent(new NotificacionCorreoEvent(this, Mail.para(arrendador.getCorreo(),
                    "MuvU: pago de publicidad confirmado",
                    "Hola " + arrendador.getNombre() + ", recibimos tu pago de S/ " + monto
                            + ". Tu habitación en " + habitacion.getDireccion() + " estará destacada hasta el " + fechaFin + ".")));
            return modelMapper.map(pago, PagoPublicidadResponseDTO.class);
        } catch (NumberFormatException | NullPointerException e) {
            throw new PaymentException("Los datos asociados al pago de Stripe no son válidos", e);
        }
    }

    private MetodoPago detectarMetodoPagoWallet(PaymentIntent intent) {
        PaymentMethod paymentMethod = intent.getPaymentMethodObject();
        if (paymentMethod == null || paymentMethod.getCard() == null) {
            throw new PaymentException("El pago confirmado no corresponde a una tarjeta admitida");
        }

        PaymentMethod.Card.Wallet wallet = paymentMethod.getCard().getWallet();
        if (wallet == null) return MetodoPago.TARJETA;

        String walletType = wallet.getType();
        if ("apple_pay".equals(walletType)) return MetodoPago.APPLE_PAY;
        if ("google_pay".equals(walletType)) return MetodoPago.GOOGLE_PAY;
        throw new PaymentException("El wallet utilizado no esta permitido para publicidad");
    }

    public List<PagoPublicidadResponseDTO> listarTodos() {
        return pagoPublicidadRepository.findByHabitacionArrendadorId(usuarioService.getIdUsuarioActual()).stream()
                .map(pago -> modelMapper.map(pago, PagoPublicidadResponseDTO.class))
                .toList();
    }

    private Habitacion buscarHabitacion(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada con ID: " + id));
    }

    private void validarPropietario(Habitacion habitacion, Long arrendadorId) {
        if (!habitacion.getArrendador().getId().equals(arrendadorId)) {
            throw new ForbiddenException("Solo el dueño puede pagar publicidad para esta habitación");
        }
    }

    private LocalDate calcularFechaFin(LocalDate inicio, Double monto) {
        if (Double.compare(monto, 30.0) == 0) return inicio.plusDays(30);
        if (Double.compare(monto, 54.0) == 0) return inicio.plusDays(60);
        throw new InvalidOperationException("Monto no permitido. Solo son 2 opciones disponibles");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void desactivarPublicidadesVencidas() {
        List<PagoPublicidad> pagosVencidos = pagoPublicidadRepository.findByFechaFinBefore(LocalDate.now());
        for (PagoPublicidad pago : pagosVencidos) {
            Habitacion habitacion = pago.getHabitacion();
            if (Boolean.TRUE.equals(habitacion.getEsDestacada())) {
                habitacion.setEsDestacada(false);
                habitacionRepository.save(habitacion);
                log.info("Publicidad vencida: la habitación {} dejó de estar destacada", habitacion.getId());
            }
        }
    }
}
