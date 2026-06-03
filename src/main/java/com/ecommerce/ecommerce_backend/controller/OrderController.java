package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.request.OrderRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponse;
import com.ecommerce.ecommerce_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // POST /orders  — Place order from cart
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody OrderRequest request) {

        OrderResponse order = orderService.placeOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Order placed successfully", order));
    }

    // GET /orders  — Get my orders (User)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<OrderResponse> orders = orderService.getMyOrders(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Orders fetched successfully", orders));
    }

    // GET /orders/{id}  — Get single order
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id) {

        OrderResponse order = orderService.getOrderById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order fetched successfully", order));
    }

    // GET /orders/all  — Get all orders (Admin only)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<OrderResponse> orders = orderService.getAllOrders(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("All orders fetched successfully", orders));
    }

    // PUT /orders/{id}/status  — Update order status (Admin only)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        OrderResponse order = orderService.updateOrderStatus(id, status);

        return ResponseEntity.ok(
                ApiResponse.success("Order status updated successfully", order));
    }

    // PUT /orders/{id}/cancel  — Cancel order (User)
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id) {

        OrderResponse order = orderService.cancelOrder(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled successfully", order));
    }
}