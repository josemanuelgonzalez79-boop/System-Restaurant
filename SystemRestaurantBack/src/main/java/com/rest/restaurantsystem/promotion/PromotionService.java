package com.rest.restaurantsystem.promotion;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.structure.BranchService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class PromotionService {
    private final PromotionRepository promotions;
    private final BranchService branches;

    public PromotionService(PromotionRepository promotions, BranchService branches) {
        this.promotions = promotions;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> findAll(boolean availableOnly) {
        LocalDate today = localToday();
        return promotions.findAllByOrderByNameAscIdAsc().stream()
                .filter(p -> !availableOnly || p.isActive()
                        && !today.isBefore(p.getStartsOn()) && !today.isAfter(p.getEndsOn()))
                .map(PromotionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PromotionResponse requireEligible(Long id, BigDecimal subtotal) {
        Promotion promotion = promotions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la promoción."));
        LocalDate today = localToday();
        if (!promotion.isActive() || today.isBefore(promotion.getStartsOn())
                || today.isAfter(promotion.getEndsOn())) {
            throw new BadRequestException("La promoción no está vigente en este restaurante.");
        }
        if (subtotal.compareTo(promotion.getMinimumSubtotal()) < 0) {
            throw new BadRequestException("El subtotal no alcanza el mínimo de la promoción.");
        }
        return PromotionResponse.from(promotion);
    }

    @Transactional
    public PromotionResponse create(PromotionSaveRequest request, String username) {
        String code = validate(request);
        if (promotions.existsByCodeAndIdNot(code, -1L)) throw duplicateCode();
        try {
            return PromotionResponse.from(promotions.saveAndFlush(new Promotion(request, code, username)));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCode();
        }
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionSaveRequest request, String username) {
        String code = validate(request);
        Promotion current = promotions.findLocked(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la promoción."));
        if (request.expectedVersion() == null || request.expectedVersion() != current.getVersion()) {
            throw new ConflictException("La promoción cambió en otra tablet. Actualiza antes de guardar.");
        }
        if (promotions.existsByCodeAndIdNot(code, id)) throw duplicateCode();
        current.update(request, code, username);
        try {
            return PromotionResponse.from(promotions.saveAndFlush(current));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCode();
        }
    }

    public BigDecimal calculate(DiscountType type, BigDecimal value, BigDecimal subtotal) {
        if (type == null || value == null || subtotal == null || subtotal.signum() <= 0
                || value.signum() <= 0 || value.scale() > 2
                || type == DiscountType.PERCENT && value.compareTo(new BigDecimal("90.00")) > 0) {
            throw new BadRequestException("El descuento debe ser positivo y no superar el 90 %.");
        }
        BigDecimal amount = type == DiscountType.PERCENT
                ? subtotal.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.UNNECESSARY);
        if (amount.compareTo(subtotal.multiply(new BigDecimal("0.90"))
                .setScale(2, RoundingMode.HALF_UP)) > 0) {
            throw new BadRequestException("El descuento no puede superar el 90 % del subtotal.");
        }
        if (amount.signum() <= 0 || amount.compareTo(subtotal) >= 0) {
            throw new BadRequestException("El descuento debe dejar un total a pagar de al menos $0.01.");
        }
        return amount;
    }

    private String validate(PromotionSaveRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{3,24}")) {
            throw new BadRequestException("Usa una clave de 3 a 24 letras, números, guiones o guion bajo.");
        }
        if (request.endsOn().isBefore(request.startsOn())) {
            throw new BadRequestException("La fecha final debe ser igual o posterior a la inicial.");
        }
        if (request.discountType() == DiscountType.PERCENT
                && request.discountValue().compareTo(new BigDecimal("90.00")) > 0) {
            throw new BadRequestException("El porcentaje máximo permitido es 90 %.");
        }
        return code;
    }

    private LocalDate localToday() {
        String timezone = branches.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Configura primero el restaurante local."))
                .timezone();
        return LocalDate.now(ZoneId.of(timezone));
    }

    private ConflictException duplicateCode() {
        return new ConflictException("Ya existe otra promoción con esa clave.");
    }
}
