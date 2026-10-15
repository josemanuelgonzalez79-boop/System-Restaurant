package com.rest.restaurantsystem.promotion;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.structure.BranchService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PromotionServiceTest {
    private final PromotionService service = new PromotionService(
            mock(PromotionRepository.class), mock(BranchService.class));

    @Test
    void calculatesDiscountAtBackendPrecision() {
        assertThat(service.calculate(DiscountType.PERCENT,
                new BigDecimal("12.50"), new BigDecimal("199.99")))
                .isEqualByComparingTo("25.00");
    }

    @Test
    void rejectsFreeOrderAndExcessivePercentage() {
        assertThrows(BadRequestException.class,
                () -> service.calculate(DiscountType.FIXED,
                        new BigDecimal("30.00"), new BigDecimal("30.00")));
        assertThrows(BadRequestException.class,
                () -> service.calculate(DiscountType.PERCENT,
                        new BigDecimal("90.01"), new BigDecimal("100.00")));
        assertThrows(BadRequestException.class,
                () -> service.calculate(DiscountType.FIXED,
                        new BigDecimal("91.00"), new BigDecimal("100.00")));
    }
}
