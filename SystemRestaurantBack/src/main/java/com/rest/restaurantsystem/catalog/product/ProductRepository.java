package com.rest.restaurantsystem.catalog.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}
