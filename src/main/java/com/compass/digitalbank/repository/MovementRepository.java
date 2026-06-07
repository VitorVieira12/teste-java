package com.compass.digitalbank.repository;

import com.compass.digitalbank.domain.entity.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    Page<Movement> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
}
