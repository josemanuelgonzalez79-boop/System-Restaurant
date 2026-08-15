package com.rest.restaurantsystem.catalog.modifier;

import com.rest.restaurantsystem.catalog.product.ProductResponse;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModifierGroupService {

    private final ModifierGroupRepository repository;
    private final ModifierOptionRepository optionRepository;
    private final ProductService productService;

    public ModifierGroupService(
            ModifierGroupRepository repository,
            ModifierOptionRepository optionRepository,
            ProductService productService
    ) {
        this.repository = repository;
        this.optionRepository = optionRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<ModifierGroupResponse> findAll(Long productId) {
        List<ModifierGroup> groups = productId == null
                ? repository.findAllByOrderByProductIdAscSortOrderAscNameAsc()
                : repository.findByProductIdOrderBySortOrderAscNameAsc(productId);
        Map<Long, List<ModifierOption>> optionsByGroup = groups.isEmpty()
                ? Map.of()
                : optionRepository
                        .findByGroupIdInOrderByGroupIdAscSortOrderAscNameAsc(
                                groups.stream().map(ModifierGroup::getId).toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(ModifierOption::getGroupId));
        return groups.stream()
                .map(group -> toResponse(group, optionsByGroup.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ModifierGroupResponse findById(Long id) {
        ModifierGroup group = getEntity(id);
        return toResponse(group, optionRepository.findByGroupIdOrderBySortOrderAscNameAsc(id));
    }

    @Transactional
    public ModifierGroupResponse create(ModifierGroupRequest request) {
        validate(request, null);
        ModifierGroup group = repository.save(new ModifierGroup(request));
        return toResponse(group, List.of());
    }

    @Transactional
    public ModifierGroupResponse update(Long id, ModifierGroupRequest request) {
        validate(request, id);
        ModifierGroup group = getEntity(id);
        group.update(request);
        return toResponse(group, optionRepository.findByGroupIdOrderBySortOrderAscNameAsc(id));
    }

    @Transactional
    public ModifierGroupResponse changeActive(Long id, boolean active) {
        ModifierGroup group = getEntity(id);
        if (active) {
            validateConfigurable(group);
        }
        group.setActive(active);
        return toResponse(group, optionRepository.findByGroupIdOrderBySortOrderAscNameAsc(id));
    }

    ModifierGroup getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el grupo de modificadores solicitado."
                ));
    }

    void validateConfigurable(ModifierGroup group) {
        productService.findConfigurableById(group.getProductId());
    }

    private void validate(ModifierGroupRequest request, Long currentId) {
        productService.findConfigurableById(request.productId());
        if (request.minSelections() > request.maxSelections()) {
            throw new BadRequestException(
                    "La selección mínima no puede ser mayor que la selección máxima."
            );
        }
        boolean exists = currentId == null
                ? repository.existsByProductIdAndNameIgnoreCase(
                        request.productId(),
                        request.name().trim()
                )
                : repository.existsByProductIdAndNameIgnoreCaseAndIdNot(
                        request.productId(),
                        request.name().trim(),
                        currentId
                );
        if (exists) {
            throw new ConflictException("Ese producto ya tiene un grupo con el mismo nombre.");
        }
    }

    private ModifierGroupResponse toResponse(ModifierGroup group, List<ModifierOption> options) {
        ProductResponse product = productService.findById(group.getProductId());
        return new ModifierGroupResponse(
                group.getId(),
                group.getProductId(),
                product.name(),
                group.getName(),
                group.getMinSelections(),
                group.getMaxSelections(),
                group.getSortOrder(),
                group.isActive(),
                group.getVersion(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                options.stream().map(option -> ModifierOptionResponse.from(option, group)).toList()
        );
    }
}
