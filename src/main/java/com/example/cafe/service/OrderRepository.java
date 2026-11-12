package com.example.cafe.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** orders 표 접근. 메서드 이름이 곧 쿼리 (Spring Data JPA). 최근 주문이 위 · 최대 100건 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 직원 — 전체
    List<Order> findTop100ByOrderByIdDesc();

    List<Order> findTop100ByStatusOrderByIdDesc(OrderStatus status);

    // 손님 — 내 주문만
    List<Order> findTop100ByCustomerIdOrderByIdDesc(Long customerId);

    List<Order> findTop100ByCustomerIdAndStatusOrderByIdDesc(Long customerId, OrderStatus status);
}
