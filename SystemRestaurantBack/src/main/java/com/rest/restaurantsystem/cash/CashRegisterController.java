package com.rest.restaurantsystem.cash;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cash-registers")
public class CashRegisterController {

    private final CashRegisterService service;

    public CashRegisterController(CashRegisterService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public ResponseEntity<CashRegisterSummaryResponse> findCurrent(
            @RequestParam Long branchId,
            Authentication authentication
    ) {
        return service.findCurrent(branchId, authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<CashRegisterSummaryResponse> findHistory(
            @RequestParam Long branchId,
            Authentication authentication
    ) {
        return service.findHistory(branchId, authentication.getName());
    }

    @GetMapping("/{id}")
    public CashRegisterSummaryResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.findById(id, authentication.getName());
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CashRegisterSummaryResponse open(
            @Valid @RequestBody CashRegisterOpenRequest request,
            Authentication authentication
    ) {
        return service.open(request, authentication.getName());
    }

    @PostMapping("/{sessionId}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public CashRegisterSummaryResponse addMovement(
            @PathVariable Long sessionId,
            @Valid @RequestBody CashMovementCreateRequest request,
            Authentication authentication
    ) {
        return service.addMovement(sessionId, request, authentication.getName());
    }

    @PatchMapping("/{sessionId}/movements/{movementId}/void")
    public CashRegisterSummaryResponse voidMovement(
            @PathVariable Long sessionId,
            @PathVariable Long movementId,
            @Valid @RequestBody CashMovementVoidRequest request,
            Authentication authentication
    ) {
        return service.voidMovement(
                sessionId,
                movementId,
                request,
                authentication.getName()
        );
    }

    @PostMapping("/{sessionId}/close")
    public CashRegisterSummaryResponse close(
            @PathVariable Long sessionId,
            @Valid @RequestBody CashRegisterCloseRequest request,
            Authentication authentication
    ) {
        return service.close(sessionId, request, authentication.getName());
    }
}
