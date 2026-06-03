package com.ecommerce.ecommerce_backend.controller;

import com.ecommerce.ecommerce_backend.dto.request.CartRequest;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.CartResponse;
import com.ecommerce.ecommerce_backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /cart
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCart();
        return ResponseEntity.ok(
                ApiResponse.success("Cart fetched successfully", cart));
    }

    // POST /cart/add
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartRequest request) {
        CartResponse cart = cartService.addToCart(request);
        return ResponseEntity.ok(
                ApiResponse.success("Item added to cart", cart));
    }

    // PUT /cart/update/{cartItemId}
    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartRequest request) {
        CartResponse cart = cartService.updateCartItem(cartItemId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Cart updated successfully", cart));
    }

    // DELETE /cart/remove/{cartItemId}
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long cartItemId) {
        CartResponse cart = cartService.removeFromCart(cartItemId);
        return ResponseEntity.ok(
                ApiResponse.success("Item removed from cart", cart));
    }

    // DELETE /cart/clear
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(
                ApiResponse.success("Cart cleared successfully"));
    }
}