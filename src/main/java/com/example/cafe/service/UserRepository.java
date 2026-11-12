package com.example.cafe.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** users 표 접근 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
