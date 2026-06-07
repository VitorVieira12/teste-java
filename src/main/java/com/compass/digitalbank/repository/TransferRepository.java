package com.compass.digitalbank.repository;

import com.compass.digitalbank.domain.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
