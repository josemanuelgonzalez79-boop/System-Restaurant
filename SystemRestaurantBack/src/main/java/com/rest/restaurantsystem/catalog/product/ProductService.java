package com.rest.restaurantsystem.catalog.product;

import com.rest.restaurantsystem.catalog.category.Category;
import com.rest.restaurantsystem.catalog.category.CategoryService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository repository, CategoryService categoryService) {
        this.repository = repository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Long categoryId) {
        List<Product> products = categoryId == null
                ? repository.findAllByOrderByNameAsc()
                : repository.findByCategoryIdOrderByNameAsc(categoryId);

        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        validateUniqueSku(request.sku(), null);
        Category category = getActiveCategory(request.categoryId());
        return ProductResponse.from(repository.save(new Product(request, category)));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        validateUniqueSku(request.sku(), id);
        Category category = getActiveCategory(request.categoryId());
        Product product = getEntity(id);
        product.update(request, category);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeActive(Long id, boolean active) {
        Product product = getEntity(id);
        product.setActive(active);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeAvailability(Long id, boolean available) {
        Product product = getEntity(id);
        if (!product.isActive() && available) {
            throw new BadRequestException("No puedes marcar como disponible un producto inactivo.");
        }
        product.setAvailable(available);
        return ProductResponse.from(product);
    }

    private Product getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el producto solicitado."));
    }

    private Category getActiveCategory(Long categoryId) {
        Category category = categoryService.getEntity(categoryId);
        if (!category.isActive()) {
            throw new BadRequestException("La categoría seleccionada está inactiva.");
        }
        return category;
    }

    private void validateUniqueSku(String rawSku, Long currentProductId) {
        String sku = normalizeSku(rawSku);
        if (sku == null) {
            return;
        }

        boolean exists = currentProductId == null
                ? repository.existsBySkuIgnoreCase(sku)
                : repository.existsBySkuIgnoreCaseAndIdNot(sku, currentProductId);
        if (exists) {
            throw new ConflictException("Ya existe un producto con ese SKU.");
        }
    }

    private String normalizeSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim();
    }
}
