package com.example.desarrollo.service;

import lombok.extern.slf4j.Slf4j;
import com.example.desarrollo.Events.ActualizacionHabitacionesEvent;
import com.example.desarrollo.dto.HabitacionDetailDTO;
import com.example.desarrollo.dto.HabitacionRequestDTO;
import com.example.desarrollo.dto.HabitacionResponseDTO;
import com.example.desarrollo.dto.ImagenResponseDTO;
import com.example.desarrollo.exceptions.ForbiddenException;
import com.example.desarrollo.exceptions.InvalidOperationException;
import com.example.desarrollo.exceptions.ResourceNotFoundException;
import com.example.desarrollo.model.Arrendador;
import com.example.desarrollo.model.Habitacion;
import com.example.desarrollo.model.Universidad;
import com.example.desarrollo.repository.ArrendadorRepository;
import com.example.desarrollo.repository.HabitacionRepository;
import com.example.desarrollo.repository.ImagenRepository;
import com.example.desarrollo.repository.UniversidadRepository;
import com.google.maps.model.LatLng;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitacionService {

    private final GoogleMapsService googleMapsService;
    private final HabitacionRepository habitacionRepository;
    private final ImagenRepository imagenRepository;
    private final ModelMapper modelMapper;
    private final ArrendadorRepository arrendadorRepository;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher publisher;
    private final UniversidadRepository universidadRepository;

    // Búsqueda por cercanía, filtro de radio y ordenamiento
    public Page<HabitacionResponseDTO> findCercanas(Long universidadId, Double radioKm, Pageable pageable) {
        // 1. Obtener la universidad de referencia
        Universidad universidad = universidadRepository.findById(universidadId)
                .orElseThrow(() -> new ResourceNotFoundException("Universidad no encontrada con ID: " + universidadId));

        // 2. Obtener todas las habitaciones
        List<Habitacion> todas = habitacionRepository.findAll();

        // 3. Mapear a DTO, calcular distancia Haversine, filtrar por radio y ordenar
        List<HabitacionResponseDTO> filtradas = todas.stream()
                .filter(h -> h.getLatitud() != null && h.getLongitud() != null)
                .map(habitacion -> {
                    double distancia = calcularHaversine(
                            universidad.getLatitud(), universidad.getLongitud(),
                            habitacion.getLatitud(), habitacion.getLongitud()
                    );
                    HabitacionResponseDTO dto = modelMapper.map(habitacion, HabitacionResponseDTO.class);
                    dto.setDistanciaKm(Math.round(distancia * 100.0) / 100.0); // Redondeo a 2 decimales
                    return dto;
                })
                .filter(dto -> dto.getDistanciaKm() <= radioKm)
                // Ordenar: 1° Destacadas arriba (true antes que false), 2° Por menor distanciaKm
                .sorted(Comparator.comparing(HabitacionResponseDTO::getEsDestacada, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HabitacionResponseDTO::getDistanciaKm))
                .toList();
        log.debug("Cercanas a la universidad {} en {} km: {} resultados", universidadId, radioKm, filtradas.size());

        // 4. Paginación manual de la lista filtrada
        int inicio = (int) pageable.getOffset();
        int fin = Math.min((inicio + pageable.getPageSize()), filtradas.size());

        if (inicio > filtradas.size()) {
            return new PageImpl<>(List.of(), pageable, filtradas.size());
        }

        List<HabitacionResponseDTO> sublista = filtradas.subList(inicio, fin);
        return new PageImpl<>(sublista, pageable, filtradas.size());
    }

    // Métod0 privado para el cálculo matemático de Haversine
    private double calcularHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio aproximado de la Tierra en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Create (POST)
    @Transactional
    public HabitacionResponseDTO save(HabitacionRequestDTO habitacionRequestDTO) {
        validarParCoordenadas(habitacionRequestDTO);
        Arrendador yo = arrendadorRepository.findById(usuarioService.getIdUsuarioActual())
                .orElseThrow(() -> new ResourceNotFoundException("Arrendador no encontrado"));

        if (!Boolean.TRUE.equals(yo.getVerificado())) {
            log.warn("Arrendador {} intentó publicar sin estar verificado", yo.getId());
            throw new ForbiddenException("Tu cuenta aún no ha sido verificada por un administrador");
        }

        Habitacion newHabitacion = modelMapper.map(habitacionRequestDTO, Habitacion.class);
        newHabitacion.setArrendador(yo);

        aplicarCoordenadas(newHabitacion, habitacionRequestDTO);
        newHabitacion = habitacionRepository.save(newHabitacion);
        log.info("Habitación {} publicada por el arrendador {}", newHabitacion.getId(), yo.getId());
        publisher.publishEvent(new ActualizacionHabitacionesEvent(this, yo.getId()));
        return modelMapper.map(newHabitacion, HabitacionResponseDTO.class);
    }

    // Read (GET)
    public HabitacionDetailDTO findById(Long id) {
        Habitacion habitacion = habitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada con ID: " + id));
        HabitacionDetailDTO dto = modelMapper.map(habitacion, HabitacionDetailDTO.class);
        dto.setImagenes(habitacion.getImagenes().stream()
                .map(img -> new ImagenResponseDTO(img.getId(), img.getUrl(), habitacion.getId()))
                .toList());
        return dto;
    }

    // Read all paginado (GET)
    // Regla de negocio: las habitaciones destacadas (pagaron publicidad) salen primero
    public Page<HabitacionResponseDTO> findAll(Pageable pageable) {
        Pageable destacadasPrimero = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("esDestacada").descending());
        return habitacionRepository.findAll(destacadasPrimero)
                .map(h -> modelMapper.map(h, HabitacionResponseDTO.class));
    }

    // Habitaciones de un arrendador (su panel), las más recientes primero
    public Page<HabitacionResponseDTO> findByArrendador(Long arrendadorId, Pageable pageable) {
        if (!arrendadorRepository.existsById(arrendadorId)) {
            throw new ResourceNotFoundException("Arrendador no encontrado con ID: " + arrendadorId);
        }
        Pageable masRecientes = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("id").descending());
        return habitacionRepository.findByArrendadorId(arrendadorId, masRecientes)
                .map(h -> modelMapper.map(h, HabitacionResponseDTO.class));
    }

    // Delete (DELETE)
    @Transactional
    public void deleteById(Long id) {
        Habitacion habitacion = buscarPropia(id);
        habitacionRepository.delete(habitacion);
        log.info("Habitación {} eliminada", id);
        publisher.publishEvent(new ActualizacionHabitacionesEvent(this, habitacion.getArrendador().getId()));
    }

    @Transactional
    public HabitacionResponseDTO update(Long id, HabitacionRequestDTO dto) {
        validarParCoordenadas(dto);
        Habitacion habitacion = buscarPropia(id);

        if (dto.getLatitud() != null) {
            habitacion.setLatitud(dto.getLatitud());
            habitacion.setLongitud(dto.getLongitud());
        } else if (!habitacion.getDireccion().equalsIgnoreCase(dto.getDireccion())) {
            LatLng coords = googleMapsService.obtenerCoordenadas(dto.getDireccion());
            habitacion.setLatitud(coords.lat);
            habitacion.setLongitud(coords.lng);
        }
        habitacion.setDireccion(dto.getDireccion());
        habitacion.setPrecio(dto.getPrecio());
        habitacion.setArea(dto.getArea());
        log.info("Habitación {} actualizada", id);

        return modelMapper.map(habitacion, HabitacionResponseDTO.class);
    }

    private void aplicarCoordenadas(Habitacion habitacion, HabitacionRequestDTO dto) {
        if (dto.getLatitud() != null) {
            habitacion.setLatitud(dto.getLatitud());
            habitacion.setLongitud(dto.getLongitud());
            return;
        }
        LatLng coords = googleMapsService.obtenerCoordenadas(habitacion.getDireccion());
        habitacion.setLatitud(coords.lat);
        habitacion.setLongitud(coords.lng);
    }

    private void validarParCoordenadas(HabitacionRequestDTO dto) {
        if ((dto.getLatitud() == null) != (dto.getLongitud() == null)) {
            throw new InvalidOperationException("La latitud y longitud deben enviarse juntas");
        }
    }

    // Busca la habitación y valida que sea del arrendador logueado
    private Habitacion buscarPropia(Long id) {
        Habitacion habitacion = habitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada con ID: " + id));

        Long miId = usuarioService.getIdUsuarioActual();
        if (!habitacion.getArrendador().getId().equals(miId)) {
            log.warn("Usuario {} intentó modificar la habitación {} sin ser el dueño", miId, id);
            throw new ForbiddenException("Solo el dueño puede modificar esta habitación");
        }
        return habitacion;
    }
}
