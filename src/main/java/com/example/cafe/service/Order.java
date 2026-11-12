package com.example.cafe.service;

import com.example.cafe.common.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 주문 한 건. 데이터베이스 표 orders 와 1:1.
 *
 * 표 이름이 order 가 아닌 이유 : ORDER 는 SQL 예약어.
 * 엔티티 이름을 CafeOrder 로 둔 이유 : Order 는 JPQL 예약어(ORDER BY)와 겹침.
 */
@Entity(name = "CafeOrder")
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_customer", columnList = "customerId")
})
public class Order {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문한 메뉴의 id. 메뉴는 지우지 않고 판매 중지만 하므로 항상 남아 있음 */
    @Column(nullable = false)
    private Long menuId;

    /** 주문 당시 이름 · 가격. 나중에 메뉴가 바뀌어도 지난 주문은 그대로 */
    @Column(nullable = false, length = MenuItem.MAX_NAME)
    private String menuName;

    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    /** 주문한 회원. 손님은 자기 주문만 보므로 이 값으로 거름 */
    @Column(nullable = false)
    private Long customerId;

    /** 직원 화면 표시용. username 은 바뀌지 않으므로 함께 저장(조인 없이 목록 표시) */
    @Column(nullable = false, length = 20)
    private String customerName;

    protected Order() {
        // JPA 용
    }

    /** 새 주문은 항상 RECEIVED 로 시작 */
    public static Order create(MenuItem menu, int quantity, CurrentUser customer) {
        if (!menu.isAvailable()) {
            throw new IllegalArgumentException("판매 중지된 메뉴: " + menu.getName());
        }
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("quantity 는 " + MIN_QUANTITY + "~" + MAX_QUANTITY + " 사이여야 함: " + quantity);
        }
        Order o = new Order();
        o.menuId = menu.getId();
        o.menuName = menu.getName();
        o.unitPrice = menu.getPrice();
        o.quantity = quantity;
        o.status = OrderStatus.RECEIVED;
        o.createdAt = Instant.now();
        o.customerId = customer.id();
        o.customerName = customer.username();
        return o;
    }

    /** 허용하지 않는 전이면 409 */
    public void changeStatus(OrderStatus next) {
        if (!status.canChangeTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
        this.status = next;
    }

    public Long getId() { return id; }
    public Long getMenuId() { return menuId; }
    public String getMenuName() { return menuName; }
    public int getUnitPrice() { return unitPrice; }
    public int getTotalPrice() { return unitPrice * quantity; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }

    public boolean isOwnedBy(CurrentUser user) {
        return customerId.equals(user.id());
    }
}
