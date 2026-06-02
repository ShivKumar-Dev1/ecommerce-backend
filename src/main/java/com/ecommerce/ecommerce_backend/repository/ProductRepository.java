package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Get all products by category (with pagination)
    Page<Product> findByCategory(String category, Pageable pageable);

    // Search products by name (case-insensitive, with pagination)
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Search by name AND filter by category
    Page<Product> findByNameContainingIgnoreCaseAndCategory(
            String name, String category, Pageable pageable);

    // Get products within a price range
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);

    // Check if product name already exists
    boolean existsByName(String name);
}
