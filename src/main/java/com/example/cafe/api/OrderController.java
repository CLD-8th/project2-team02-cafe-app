package com.example.cafe.api;

import com.example.cafe.api.dto.ChangeStatusRequest;
import com.example.cafe.api.dto.CreateOrderRequest;
import com.example.cafe.api.dto.OrderResponse;
import com.example.cafe.service.Order;
import com.example.cafe.service.OrderService;
import com.example.cafe.service.OrderStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 주문 API 4개. 모두 로그인 필요 (Authorization: Bearer <token>).
 *   POST  /api/orders              생성
 *   GET   /api/orders/{id}         조회
 *   PATCH /api/orders/{id}/status  상태 변경
 *   GET   /api/orders?status=      목록
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateOrderRequest req) {
        if (req.quantity() == null) {
            throw new IllegalArgumentException("quantity 가 비어 있음");
        }
        Order order = service.create(req.menuId(), req.quantity(), CurrentUsers.from(jwt));
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return OrderResponse.from(service.get(id, CurrentUsers.from(jwt)));
    }

    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                      @RequestBody ChangeStatusRequest req) {
        return OrderResponse.from(service.changeStatus(id, OrderStatus.parse(req.status()), CurrentUsers.from(jwt)));
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String status) {
        OrderStatus filter = (status == null || status.isBlank()) ? null : OrderStatus.parse(status);
        return service.list(filter, CurrentUsers.from(jwt)).stream().map(OrderResponse::from).toList();
    }
}
