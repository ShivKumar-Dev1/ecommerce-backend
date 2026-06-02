package com.ecommerce.ecommerce_backend.repository;

import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders for a specific user
    List<Order> findByUser(User user);

    // Get all orders for a specific user (with pagination)
    Page<Order> findByUser(User user, Pageable pageable);

    // Get all orders by user id
    List<Order> findByUserId(Long userId);

    // Get orders by status
    List<Order> findByStatus(Order.OrderStatus status);
}
