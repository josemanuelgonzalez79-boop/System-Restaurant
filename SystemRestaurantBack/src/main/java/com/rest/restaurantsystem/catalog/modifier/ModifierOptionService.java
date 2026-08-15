package com.rest.restaurantsystem.catalog.modifier;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModifierOptionService {

    private final ModifierOptionRepository repository;
    private final ModifierGroupService groupService;

    public ModifierOptionService(
            ModifierOptionRepository repository,
            ModifierGroupService groupService
    ) {
        this.repository = repository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public List<ModifierOptionResponse> findAll(Long groupId) {
        List<ModifierOption> options = groupId == null
                ? repository.findAllByOrderByGroupIdAscSortOrderAscNameAsc()
                : repository.findByGroupIdOrderBySortOrderAscNameAsc(groupId);
        return options.stream()
                .map(option -> ModifierOptionResponse.from(
                        option,
                        groupService.getEntity(option.getGroupId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ModifierOptionResponse findById(Long id) {
        ModifierOption option = getEntity(id);
        return ModifierOptionResponse.from(option, groupService.getEntity(option.getGroupId()));
    }

    @Transactional
    public ModifierOptionResponse create(ModifierOptionRequest request) {
        ModifierGroup group = validate(request, null);
        return ModifierOptionResponse.from(repository.save(new ModifierOption(request)), group);
    }

    @Transactional
    public ModifierOptionResponse update(Long id, ModifierOptionRequest request) {
        ModifierGroup group = validate(request, id);
        ModifierOption option = getEntity(id);
        option.update(request);
        return ModifierOptionResponse.from(option, group);
    }

    @Transactional
    public ModifierOptionResponse changeActive(Long id, boolean active) {
        ModifierOption option = getEntity(id);
        ModifierGroup group = groupService.getEntity(option.getGroupId());
        if (active) {
            if (!group.isActive()) {
                throw new BadRequestException("Activa primero el grupo de modificadores.");
            }
            groupService.validateConfigurable(group);
        }
        option.setActive(active);
        return ModifierOptionResponse.from(option, group);
    }

    private ModifierGroup validate(ModifierOptionRequest request, Long currentId) {
        ModifierGroup group = groupService.getEntity(request.groupId());
        if (!group.isActive()) {
            throw new BadRequestException("El grupo debe estar activo para agregar opciones.");
        }
        groupService.validateConfigurable(group);
        boolean exists = currentId == null
                ? repository.existsByGroupIdAndNameIgnoreCase(
                        request.groupId(),
                        request.name().trim()
                )
                : repository.existsByGroupIdAndNameIgnoreCaseAndIdNot(
                        request.groupId(),
                        request.name().trim(),
                        currentId
                );
        if (exists) {
            throw new ConflictException("Ese grupo ya tiene una opción con el mismo nombre.");
        }
        return group;
    }

    private ModifierOption getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la opción de modificador solicitada."
                ));
    }
}
