package com.example.cafe.service;

import com.example.cafe.common.ForbiddenException;
import com.example.cafe.common.InvalidStatusTransitionException;
import com.example.cafe.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 생성 · 조회 · 상태 변경 · 목록.
 *
 *   손님  주문 생성 · 내 주문 조회/목록 · 내 주문을 RECEIVED 일 때만 취소
 *   직원  전체 조회/목록 · 전이표대로 상태 변경 (docs/domain.md)
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final MenuService menuService;

    public OrderService(OrderRepository repository, MenuService menuService) {
        this.repository = repository;
        this.menuService = menuService;
    }

    /** 없는 메뉴 id 는 400 (주문 자체가 없는 게 아니므로 404 가 아님) */
    public Order create(Long menuId, int quantity, CurrentUser user) {
        if (menuId == null) {
            throw new IllegalArgumentException("menuId 가 비어 있음");
        }
        MenuItem menu;
        try {
            menu = menuService.get(menuId);
        } catch (NotFoundException e) {
            throw new IllegalArgumentException("없는 메뉴: " + menuId);
        }
        return repository.save(Order.create(menu, quantity, user));
    }

    @Transactional(readOnly = true)
    public Order get(long id, CurrentUser user) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("없는 주문: " + id));
        if (!user.isStaff() && !order.isOwnedBy(user)) {
            throw new ForbiddenException("다른 손님의 주문");
        }
        return order;
    }

    public Order changeStatus(long id, OrderStatus next, CurrentUser user) {
        Order order = get(id, user);
        if (!user.isStaff()) {
            if (next != OrderStatus.CANCELED) {
                throw new ForbiddenException("손님은 취소만 할 수 있음");
            }
            if (order.getStatus() != OrderStatus.RECEIVED) {
                throw new InvalidStatusTransitionException(order.getStatus(), next);
            }
        }
        order.changeStatus(next);
        return order;
    }

    /** status 가 null 이면 전체. 손님은 자기 주문만 */
    @Transactional(readOnly = true)
    public List<Order> list(OrderStatus status, CurrentUser user) {
        if (user.isStaff()) {
            return status == null
                    ? repository.findTop100ByOrderByIdDesc()
                    : repository.findTop100ByStatusOrderByIdDesc(status);
        }
        return status == null
                ? repository.findTop100ByCustomerIdOrderByIdDesc(user.id())
                : repository.findTop100ByCustomerIdAndStatusOrderByIdDesc(user.id(), status);
    }
}
