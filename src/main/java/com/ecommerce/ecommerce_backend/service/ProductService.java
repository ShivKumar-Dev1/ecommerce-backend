package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.request.ProductRequest;
import com.ecommerce.ecommerce_backend.dto.response.ProductResponse;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // ── GET ALL PRODUCTS (with pagination & sorting) ──────
    public Page<ProductResponse> getAllProducts(
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    // ── SEARCH PRODUCTS ───────────────────────────────────
    public Page<ProductResponse> searchProducts(
            String keyword, String category,
            int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        // Both keyword and category provided
        if (keyword != null && !keyword.isEmpty()
                && category != null && !category.isEmpty()) {
            return productRepository
                    .findByNameContainingIgnoreCaseAndCategory(
                            keyword, category, pageable)
                    .map(this::mapToResponse);
        }

        // Only keyword provided
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository
                    .findByNameContainingIgnoreCase(keyword, pageable)
                    .map(this::mapToResponse);
        }

        // Only category provided
        if (category != null && !category.isEmpty()) {
            return productRepository
                    .findByCategory(category, pageable)
                    .map(this::mapToResponse);
        }

        // Nothing provided — return all
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    // ── GET SINGLE PRODUCT ────────────────────────────────
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", id));
        return mapToResponse(product);
    }

    // ── ADD PRODUCT (Admin only) ──────────────────────────
    public ProductResponse addProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .build();

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    // ── UPDATE PRODUCT (Admin only) ───────────────────────
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    // ── DELETE PRODUCT (Admin only) ───────────────────────
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", id));
        productRepository.delete(product);
    }

    // ── MAP Entity → Response DTO ─────────────────────────
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
