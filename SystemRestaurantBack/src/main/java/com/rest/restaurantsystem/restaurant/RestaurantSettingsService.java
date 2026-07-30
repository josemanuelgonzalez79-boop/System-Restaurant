package com.rest.restaurantsystem.restaurant;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

@Service
public class RestaurantSettingsService {

    private final RestaurantSettingsRepository repository;

    public RestaurantSettingsService(RestaurantSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RestaurantSettingsResponse get() {
        return RestaurantSettingsResponse.from(findSettings());
    }

    @Transactional
    public RestaurantSettingsResponse update(RestaurantSettingsRequest request) {
        validateTimezone(request.timezone());
        RestaurantSettings settings = findSettings();
        settings.update(request);
        return RestaurantSettingsResponse.from(settings);
    }

    private RestaurantSettings findSettings() {
        return repository.findById(RestaurantSettings.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la configuración inicial del restaurante."
                ));
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new BadRequestException("La zona horaria indicada no es válida.");
        }
    }
}
