package com.rest.restaurantsystem.customer;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomerService {
    private final CustomerRepository customers;
    private final CustomerEventRepository events;
    private final JdbcTemplate jdbc;

    public CustomerService(CustomerRepository customers, CustomerEventRepository events, JdbcTemplate jdbc) {
        this.customers = customers;
        this.events = events;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String search, boolean includeArchived) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (term.length() > 80) throw new BadRequestException("La búsqueda debe tener máximo 80 caracteres.");
        String digits = term.replaceAll("[^0-9]", "");
        return customers.search(term, digits, includeArchived,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "name", "id")))
                .map(CustomerResponse::from).getContent();
    }

    @Transactional(readOnly = true)
    public CustomerResponse find(Long id) { return CustomerResponse.from(get(id)); }

    @Transactional(readOnly = true)
    public CustomerHistoryResponse history(Long id) {
        get(id);
        List<CustomerVisitResponse> visits = jdbc.query("""
                SELECT r.id AS reservation_id, r.order_id, r.status AS state,
                       'DINE_IN' AS mode, p.name AS point_name, r.start_at AS visit_at
                FROM reservations r JOIN service_points p ON p.id = r.service_point_id
                WHERE r.customer_id = ?
                UNION ALL
                SELECT NULL::bigint, o.id, o.status, o.service_mode, p.name, o.opened_at
                FROM restaurant_orders o LEFT JOIN service_points p ON p.id = o.service_point_id
                WHERE o.customer_id = ?
                  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.order_id = o.id
                                  AND r.customer_id = ?)
                ORDER BY visit_at DESC LIMIT 50
                """, (rs, row) -> new CustomerVisitResponse(
                rs.getObject("reservation_id", Long.class), rs.getObject("order_id", Long.class),
                rs.getString("state"), rs.getString("mode"), rs.getString("point_name"),
                rs.getTimestamp("visit_at").toInstant()), id, id, id);
        return new CustomerHistoryResponse(visits,
                events.findTop50ByCustomerIdOrderByOccurredAtDescIdDesc(id).stream()
                        .map(CustomerHistoryResponse.CustomerAuditResponse::from).toList());
    }

    @Transactional
    public CustomerResponse create(CustomerSaveRequest request, String username) {
        String key = phoneKey(request.phone());
        if (key != null && customers.existsByPhoneKeyAndIdNot(key, -1L)) {
            throw new ConflictException("Ese teléfono ya pertenece a otra ficha. Búscala para evitar duplicados.");
        }
        try {
            Customer saved = customers.saveAndFlush(new Customer(request, username, key));
            events.save(new CustomerEvent(saved.getId(), "CREATED", username));
            return CustomerResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatePhone();
        }
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerSaveRequest request, String username) {
        Customer current = locked(id);
        version(current, request.expectedVersion());
        String key = phoneKey(request.phone());
        if (key != null && customers.existsByPhoneKeyAndIdNot(key, id)) throw duplicatePhone();
        current.update(request, username, key);
        try {
            Customer saved = customers.saveAndFlush(current);
            events.save(new CustomerEvent(id, "UPDATED", username));
            return CustomerResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatePhone();
        }
    }

    @Transactional
    public CustomerResponse changeActive(Long id, CustomerStatusRequest request, String username) {
        Customer current = locked(id);
        version(current, request.expectedVersion());
        if (current.isActive() == request.active()) return CustomerResponse.from(current);
        current.setActive(request.active(), username);
        Customer saved = customers.saveAndFlush(current);
        events.save(new CustomerEvent(id, request.active() ? "RESTORED" : "ARCHIVED", username));
        return CustomerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse requireActive(Long id) {
        Customer current = get(id);
        if (!current.isActive()) {
            throw new BadRequestException("La ficha está archivada. Reactívala para usarla en un pedido nuevo.");
        }
        return CustomerResponse.from(current);
    }

    private Customer get(Long id) {
        return customers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la ficha del cliente."));
    }

    private Customer locked(Long id) {
        return customers.findLocked(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la ficha del cliente."));
    }

    private void version(Customer current, Long expected) {
        if (expected == null || expected != current.getVersion()) {
            throw new ConflictException("La ficha cambió en otra tablet. Actualiza antes de guardar.");
        }
    }

    private String phoneKey(String phone) {
        if (phone == null || phone.isBlank()) return null;
        if (!phone.matches("[+()0-9 .-]+")) {
            throw new BadRequestException("El teléfono solo puede contener números y separadores habituales.");
        }
        String key = phone.replaceAll("[^0-9]", "");
        if (key.length() < 7 || key.length() > 15) {
            throw new BadRequestException("El teléfono debe contener entre 7 y 15 dígitos.");
        }
        return key;
    }

    private ConflictException duplicatePhone() {
        return new ConflictException("Ese teléfono ya pertenece a otra ficha. Búscala para evitar duplicados.");
    }
}
