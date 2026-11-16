package com.example.cafe.service;

import com.example.cafe.common.NotFoundException;
import com.example.cafe.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * 메뉴 조회 · 관리.
 *   누구나  판매 중인 메뉴 목록
 *   직원    전체 목록(판매 중지 포함) · 추가 · 수정 · 판매 중지/재개
 * 직원 여부는 SecurityConfig(쓰기) · 컨트롤러(전체 목록)에서 확인함.
 */
@Service
@Transactional
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final MenuItemRepository repository;
    private final ImageStorage storage;

    public MenuService(MenuItemRepository repository, ImageStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<MenuItem> list(boolean includeUnavailable) {
        return includeUnavailable
                ? repository.findAllByOrderByIdAsc()
                : repository.findAllByAvailableTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public MenuItem get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("없는 메뉴: " + id));
    }

    public MenuItem create(String name, Integer price) {
        if (price == null) {
            throw new IllegalArgumentException("price 가 비어 있음");
        }
        MenuItem item = MenuItem.create(name, price);
        if (repository.existsByName(item.getName())) {
            throw new IllegalArgumentException("이미 있는 메뉴: " + item.getName());
        }
        return repository.save(item);
    }

    /** 넘어온 값만 바꿈 (null 은 그대로) */
    public MenuItem update(long id, String name, Integer price, Boolean available) {
        MenuItem item = get(id);
        if (name != null) {
            item.rename(name);
            if (repository.existsByNameAndIdNot(item.getName(), id)) {
                throw new IllegalArgumentException("이미 있는 메뉴: " + item.getName());
            }
        }
        if (price != null) {
            item.changePrice(price);
        }
        if (available != null) {
            item.setAvailable(available);
        }
        return item;
    }

    /** 삭제 = 판매 중지. 행은 남김 */
    public void discontinue(long id) {
        get(id).setAvailable(false);
    }

    /**
     * 사진 교체. 새 사진을 먼저 저장하고 DB 를 바꾼 뒤 옛 사진을 지움
     * — 중간에 실패해도 메뉴가 없는 사진을 가리키지 않게.
     */
    public MenuItem changeImage(long id, MultipartFile file) throws IOException {
        MenuItem item = get(id);
        String ext = ImageValidator.extensionOf(file);
        String key = "menus/" + id + "/" + UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            storage.save(key, in, file.getSize(), file.getContentType());
        }
        String old = item.getImageKey();
        item.setImageKey(key);
        if (old != null) {
            try {
                storage.delete(old);
            } catch (IOException | RuntimeException e) {
                log.warn("옛 사진 삭제 실패 (무시) : {} — {}", old, e.getMessage());
            }
        }
        return item;
    }

    /** 앱이 내려주는 사진 주소 (ImageController). 사진이 없으면 null */
    public String imageUrl(MenuItem item) {
        return item.getImageKey() == null ? null : ImageStorage.URL_PREFIX + item.getImageKey();
    }
}
