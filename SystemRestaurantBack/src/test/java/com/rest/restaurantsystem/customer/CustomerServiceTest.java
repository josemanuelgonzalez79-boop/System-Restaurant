package com.rest.restaurantsystem.customer;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository customers;
    @Mock CustomerEventRepository events;
    @Mock JdbcTemplate jdbc;
    @InjectMocks CustomerService service;

    @Test
    void normalizedTelephonePreventsDuplicateBeforeSaving() {
        when(customers.existsByPhoneKeyAndIdNot("6671234567", -1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(
                new CustomerSaveRequest("Ana", "667 123-4567", null, null, null, null), "caja"));
        verify(customers, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStaleVersionWithoutLosingNewPreferences() {
        Customer current = new Customer(new CustomerSaveRequest("Ana", null, null,
                "Mesa tranquila", null, null), "caja", null);
        when(customers.findLocked(4L)).thenReturn(Optional.of(current));

        assertThrows(ConflictException.class, () -> service.update(4L,
                new CustomerSaveRequest("Ana", null, null, "Mesa ruidosa", null, 2L), "gerente"));
        assertEquals("Mesa tranquila", current.getPreferences());
    }

    @Test
    void archiveKeepsCustomerAndRecordsActor() {
        Customer current = new Customer(new CustomerSaveRequest("Ana", null, null,
                null, null, null), "caja", null);
        ReflectionTestUtils.setField(current, "id", 4L);
        when(customers.findLocked(4L)).thenReturn(Optional.of(current));
        when(customers.findById(4L)).thenReturn(Optional.of(current));
        when(customers.saveAndFlush(current)).thenReturn(current);

        CustomerResponse result = service.changeActive(4L,
                new CustomerStatusRequest(false, 0L), "gerente");

        assertFalse(result.active());
        verify(events).save(any(CustomerEvent.class));
        assertThrows(BadRequestException.class, () -> service.requireActive(4L));
    }
}
