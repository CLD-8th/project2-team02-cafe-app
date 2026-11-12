package com.example.cafe.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** menus 표 접근 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findAllByAvailableTrueOrderByIdAsc();

    List<MenuItem> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
