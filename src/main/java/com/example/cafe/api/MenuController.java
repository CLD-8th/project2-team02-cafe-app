package com.example.cafe.api;

import com.example.cafe.api.dto.MenuRequest;
import com.example.cafe.api.dto.MenuResponse;
import com.example.cafe.common.ForbiddenException;
import com.example.cafe.service.MenuItem;
import com.example.cafe.service.MenuService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * 메뉴.
 *   GET    /api/menus             판매 중인 메뉴 (누구나)
 *   GET    /api/menus?all=true    판매 중지 포함 (직원)
 *   POST   /api/menus             추가 (직원)
 *   PATCH  /api/menus/{id}        이름 · 가격 · 판매 여부 수정 (직원)
 *   DELETE /api/menus/{id}        판매 중지 (직원) — 행은 남김
 *   PUT    /api/menus/{id}/image  사진 올리기 · 교체 (직원) — multipart, 항목 이름 file
 * 쓰기의 직원 확인은 SecurityConfig 에서 함.
 */
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    private MenuResponse toResponse(MenuItem m) {
        return MenuResponse.from(m, service.imageUrl(m));
    }

    @GetMapping
    public List<MenuResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "false") boolean all) {
        if (all && (jwt == null || !CurrentUsers.from(jwt).isStaff())) {
            throw new ForbiddenException("판매 중지 메뉴는 직원만 볼 수 있음");
        }
        return service.list(all).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<MenuResponse> create(@RequestBody MenuRequest req) {
        MenuItem item = service.create(req.name(), req.price());
        return ResponseEntity.created(URI.create("/api/menus/" + item.getId())).body(toResponse(item));
    }

    @PatchMapping("/{id}")
    public MenuResponse update(@PathVariable long id, @RequestBody MenuRequest req) {
        return toResponse(service.update(id, req.name(), req.price(), req.available()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.discontinue(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MenuResponse changeImage(@PathVariable long id, @RequestPart("file") MultipartFile file) throws IOException {
        return toResponse(service.changeImage(id, file));
    }
}
