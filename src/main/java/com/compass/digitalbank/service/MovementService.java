package com.compass.digitalbank.service;

import com.compass.digitalbank.dto.MovementResponse;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.MovementRepository;
import com.compass.digitalbank.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;

    public MovementService(MovementRepository movementRepository, AccountRepository accountRepository) {
        this.movementRepository = movementRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> getMovementsByAccount(Long accountId, Pageable pageable) {
        SecurityUtils.requireAccountOwnership(accountId);

        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return movementRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(MovementResponse::from);
    }
}
