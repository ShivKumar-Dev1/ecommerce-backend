package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.request.CartRequest;
import com.ecommerce.ecommerce_backend.dto.response.CartItemResponse;
import com.ecommerce.ecommerce_backend.dto.response.CartResponse;
import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.ApiException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.CartItemRepository;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── GET CURRENT LOGGED IN USER ────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    // ── GET OR CREATE CART FOR USER ───────────────────────
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // ── GET CART ──────────────────────────────────────────
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        return mapToResponse(cart);
    }

    // ── ADD ITEM TO CART ──────────────────────────────────
    @Transactional
    public CartResponse addToCart(CartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        // Find product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", request.getProductId()));

        // Check stock availability
        if (product.getStock() < request.getQuantity()) {
            throw new ApiException(
                    "Not enough stock. Available: " + product.getStock(),
                    HttpStatus.BAD_REQUEST);
        }

        // Check if product already in cart
        Optional<CartItem> existingItem =
                cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            // Update quantity if already exists
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            // Check stock for updated quantity
            if (product.getStock() < newQuantity) {
                throw new ApiException(
                        "Not enough stock. Available: " + product.getStock(),
                        HttpStatus.BAD_REQUEST);
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Add new item to cart
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }

        // Refresh cart from DB
        Cart updatedCart = cartRepository.findById(cart.getId())
                .orElseThrow();
        return mapToResponse(updatedCart);
    }

    // ── UPDATE CART ITEM QUANTITY ─────────────────────────
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, CartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item not found"));

        // Make sure item belongs to current user's cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ApiException("Unauthorized", HttpStatus.FORBIDDEN);
        }

        // Check stock
        if (item.getProduct().getStock() < request.getQuantity()) {
            throw new ApiException(
                    "Not enough stock. Available: " + item.getProduct().getStock(),
                    HttpStatus.BAD_REQUEST);
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
        return mapToResponse(updatedCart);
    }

    // ── REMOVE ITEM FROM CART ─────────────────────────────
    @Transactional
    public CartResponse removeFromCart(Long cartItemId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item not found"));

        // Make sure item belongs to current user's cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ApiException("Unauthorized", HttpStatus.FORBIDDEN);
        }

        cartItemRepository.delete(item);

        Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
        return mapToResponse(updatedCart);
    }

    // ── CLEAR ENTIRE CART ─────────────────────────────────
    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCart(cart);
    }

    // ── MAP Entity → Response DTO ─────────────────────────
    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems()
                .stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        // Calculate total price
        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total items count
        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .build();
    }

    private CartItemResponse mapItemToResponse(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(item.getProduct().getImageUrl())
                .productPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}