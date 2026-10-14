package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository repository;

    @InjectMocks
    private BranchService service;

    @Test
    void updatesDetailsOfTheOnlyLocalRestaurant() {
        Branch restaurant = restaurant();
        when(repository.findAll()).thenReturn(List.of(restaurant));

        service.updateLocalDetails("  Mi Restaurante  ", "  Centro  ", "  5551234  ", "  America/Mazatlan  ");

        assertEquals("Mi Restaurante", restaurant.getName());
        assertEquals("Centro", restaurant.getAddress());
        assertEquals("5551234", restaurant.getPhone());
        assertEquals("America/Mazatlan", restaurant.getTimezone());
    }

    @Test
    void clearsOptionalContactDetails() {
        Branch restaurant = restaurant();
        when(repository.findAll()).thenReturn(List.of(restaurant));

        service.updateLocalDetails("Restaurante", " ", null, "America/Mazatlan");

        assertNull(restaurant.getAddress());
        assertNull(restaurant.getPhone());
    }

    @Test
    void reportsMissingLocalRestaurant() {
        when(repository.findAll()).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateLocalDetails("Restaurante", null, null, "America/Mazatlan"));
    }

    @Test
    void findsExistingRestaurantByInternalId() {
        Branch restaurant = restaurant();
        when(repository.findById(1L)).thenReturn(Optional.of(restaurant));

        assertEquals("Restaurante", service.findById(1L).name());
    }

    private Branch restaurant() {
        return new Branch(new BranchRequest(
                "PRINCIPAL", "Restaurante", null, null, "America/Mazatlan", 0));
    }
}
