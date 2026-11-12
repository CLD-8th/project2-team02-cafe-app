package com.example.cafe.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 메뉴 한 개. 데이터베이스 표 menus 와 1:1. 직원이 추가 · 수정 · 판매 중지함.
 *
 * 삭제는 행을 지우지 않고 available=false (판매 중지) — 실수로 지워도 되살릴 수 있음.
 * 주문은 주문 당시 이름 · 가격을 따로 저장하므로 메뉴가 바뀌어도 지난 주문은 그대로.
 */
@Entity(name = "MenuItem")
@Table(name = "menus")
public class MenuItem {

    public static final int MAX_NAME = 30;
    public static final int MIN_PRICE = 100;
    public static final int MAX_PRICE = 100_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = MAX_NAME)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private Instant createdAt;

    /** 사진의 저장소 key (menus/{id}/{uuid}.jpg). 사진이 없으면 null */
    @Column(length = 200)
    private String imageKey;

    protected MenuItem() {
        // JPA 용
    }

    public static MenuItem create(String name, int price) {
        MenuItem m = new MenuItem();
        m.rename(name);
        m.changePrice(price);
        m.available = true;
        m.createdAt = Instant.now();
        return m;
    }

    public void rename(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME) {
            throw new IllegalArgumentException("name 은 1~" + MAX_NAME + "자");
        }
        this.name = trimmed;
    }

    public void changePrice(int price) {
        if (price < MIN_PRICE || price > MAX_PRICE) {
            throw new IllegalArgumentException("price 는 " + MIN_PRICE + "~" + MAX_PRICE + " 사이");
        }
        this.price = price;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getImageKey() { return imageKey; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public boolean isAvailable() { return available; }
}
