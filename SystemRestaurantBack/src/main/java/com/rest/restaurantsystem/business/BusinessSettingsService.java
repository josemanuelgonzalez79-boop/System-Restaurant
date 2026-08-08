package com.rest.restaurantsystem.business;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

@Service
public class BusinessSettingsService {

    private final BusinessSettingsRepository repository;

    public BusinessSettingsService(BusinessSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BusinessSettingsResponse get() {
        return BusinessSettingsResponse.from(findSettings());
    }

    @Transactional
    public BusinessSettingsResponse update(BusinessSettingsRequest request) {
        validateTimezone(request.timezone());
        BusinessSettings settings = findSettings();
        settings.update(request);
        return BusinessSettingsResponse.from(settings);
    }

    private BusinessSettings findSettings() {
        return repository.findById(BusinessSettings.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la configuración inicial del negocio."
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
