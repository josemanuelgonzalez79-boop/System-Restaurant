package com.rest.restaurantsystem.reservation;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) { this.service = service; }

    @GetMapping
    public List<ReservationResponse> findDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findDay(date);
    }

    @GetMapping("/{id}")
    public ReservationResponse find(@PathVariable Long id) { return service.find(id); }

    @GetMapping("/{id}/history")
    public List<ReservationEventResponse> history(@PathVariable Long id) { return service.history(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationSaveRequest request,
                                      Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public ReservationResponse update(@PathVariable Long id,
                                      @Valid @RequestBody ReservationSaveRequest request,
                                      Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id,
                                      @Valid @RequestBody ReservationActionRequest request,
                                      Authentication authentication) {
        return service.cancel(id, request, authentication.getName());
    }

    @PostMapping("/{id}/no-show")
    public ReservationResponse noShow(@PathVariable Long id,
                                      @Valid @RequestBody ReservationActionRequest request,
                                      Authentication authentication) {
        return service.noShow(id, request, authentication.getName());
    }
}
