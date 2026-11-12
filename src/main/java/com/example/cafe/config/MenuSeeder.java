package com.example.cafe.config;

import com.example.cafe.service.MenuItem;
import com.example.cafe.service.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * menus 표가 비어 있으면 기본 메뉴를 넣음 (가상 데이터). 한 개라도 있으면 아무것도 안 함.
 * 앱 여러 개가 동시에 넣으려 하면 이름 유일 제약으로 하나만 성공 → 나머지는 로그만 남기고 계속 뜸.
 */
@Component
public class MenuSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuSeeder.class);

    private final MenuItemRepository repository;

    public MenuSeeder(MenuItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        try {
            repository.saveAll(List.of(
                    MenuItem.create("아메리카노", 4500),
                    MenuItem.create("카페라테", 5000),
                    MenuItem.create("바닐라라테", 5500),
                    MenuItem.create("콜드브루", 5000),
                    MenuItem.create("녹차라테", 5500)));
            log.info("기본 메뉴 5개 넣음");
        } catch (RuntimeException e) {
            log.warn("기본 메뉴 넣기 건너뜀 : {}", e.getMessage());
        }
    }
}
