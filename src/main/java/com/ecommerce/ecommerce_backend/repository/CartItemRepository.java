package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Find a specific item in a cart by product
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // Get all items in a cart
    List<CartItem> findByCart(Cart cart);

    // Delete all items in a cart (used when order is placed)
    void deleteByCart(Cart cart);

    // Check if product exists in cart
    boolean existsByCartAndProduct(Cart cart, Product product);
}
