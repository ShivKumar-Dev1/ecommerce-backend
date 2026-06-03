package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.request.OrderRequest;
import com.ecommerce.ecommerce_backend.dto.response.OrderItemResponse;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponse;
import com.ecommerce.ecommerce_backend.entity.*;
import com.ecommerce.ecommerce_backend.exception.ApiException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    // ── GET CURRENT LOGGED IN USER ────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    // ── PLACE ORDER ───────────────────────────────────────
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User user = getCurrentUser();

        // Get user's cart
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new ApiException("Cart is empty", HttpStatus.BAD_REQUEST));

        // Check cart is not empty
        if (cart.getCartItems().isEmpty()) {
            throw new ApiException(
                    "Cannot place order with empty cart",
                    HttpStatus.BAD_REQUEST);
        }

        // Calculate total price and validate stock
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            // Check stock availability
            if (product.getStock() < cartItem.getQuantity()) {
                throw new ApiException(
                        "Not enough stock for product: " + product.getName()
                                + ". Available: " + product.getStock(),
                        HttpStatus.BAD_REQUEST);
            }

            totalPrice = totalPrice.add(
                    product.getPrice().multiply(
                            BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setTotalPrice(totalPrice);
        order.setStatus(Order.OrderStatus.PENDING);

        // Create order items and add directly to order
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            // Add item directly to order list
            order.getOrderItems().add(orderItem);

            // Reduce product stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // Save order with items together (cascade will save items)
        Order savedOrder = orderRepository.save(order);

        // Flush and clear JPA first level cache
        entityManager.flush();
        entityManager.clear();

        // Clear cart after order placed
        cartItemRepository.deleteByCart(cart);

        // Fetch fresh from DB
        Order freshOrder = orderRepository.findById(savedOrder.getId())
                .orElseThrow();

        return mapToResponse(freshOrder);
    }

    // ── GET MY ORDERS (User) ──────────────────────────────
    public Page<OrderResponse> getMyOrders(int page, int size) {
        User user = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        return orderRepository.findByUser(user, pageable)
                .map(this::mapToResponse);
    }

    // ── GET SINGLE ORDER ──────────────────────────────────
    public OrderResponse getOrderById(Long orderId) {
        User user = getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId));

        // Check order belongs to current user (unless admin)
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            throw new ApiException(
                    "Access denied", HttpStatus.FORBIDDEN);
        }

        return mapToResponse(order);
    }

    // ── GET ALL ORDERS (Admin only) ───────────────────────
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        return orderRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    // ── UPDATE ORDER STATUS (Admin only) ──────────────────
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId));

        try {
            Order.OrderStatus newStatus =
                    Order.OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    "Invalid status. Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED",
                    HttpStatus.BAD_REQUEST);
        }

        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    // ── CANCEL ORDER (User) ───────────────────────────────
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId));

        // Check order belongs to current user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }

        // Can only cancel PENDING orders
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new ApiException(
                    "Only PENDING orders can be cancelled",
                    HttpStatus.BAD_REQUEST);
        }

        // Restore product stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    // ── MAP Entity → Response DTO ─────────────────────────
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .items(items)
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        BigDecimal subtotal = item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(subtotal)
                .build();
    }
}