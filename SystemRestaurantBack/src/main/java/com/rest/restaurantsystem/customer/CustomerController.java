package com.rest.restaurantsystem.customer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) { this.service = service; }

    @GetMapping
    public List<CustomerResponse> search(@RequestParam(defaultValue = "") String search,
                                         @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.search(search, includeArchived);
    }

    @GetMapping("/{id}")
    public CustomerResponse find(@PathVariable Long id) { return service.find(id); }

    @GetMapping("/{id}/history")
    public CustomerHistoryResponse history(@PathVariable Long id) { return service.history(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerSaveRequest request,
                                   Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerSaveRequest request,
                                   Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/active")
    public CustomerResponse changeActive(@PathVariable Long id,
                                         @Valid @RequestBody CustomerStatusRequest request,
                                         Authentication authentication) {
        return service.changeActive(id, request, authentication.getName());
    }
}
