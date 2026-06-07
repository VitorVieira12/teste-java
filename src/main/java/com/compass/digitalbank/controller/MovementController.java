package com.compass.digitalbank.controller;

import com.compass.digitalbank.dto.MovementResponse;
import com.compass.digitalbank.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/movements")
@Tag(name = "Movements", description = "Account movement history")
@SecurityRequirement(name = "bearerAuth")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    @Operation(summary = "List movements for your account (paginated)")
    public Page<MovementResponse> getMovements(
            @PathVariable Long accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        return movementService.getMovementsByAccount(accountId, pageable);
    }
}
