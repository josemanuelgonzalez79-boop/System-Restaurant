package com.rest.restaurantsystem.catalog.category;

import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getEntity(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una categoría con ese nombre.");
        }
        return CategoryResponse.from(repository.save(new Category(request)));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe otra categoría con ese nombre.");
        }
        Category category = getEntity(id);
        category.update(request);
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse changeActive(Long id, boolean active) {
        Category category = getEntity(id);
        category.setActive(active);
        return CategoryResponse.from(category);
    }

    public Category getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría solicitada."));
    }
}
