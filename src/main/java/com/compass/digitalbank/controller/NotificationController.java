package com.compass.digitalbank.controller;

import com.compass.digitalbank.config.OpenApiConfig;
import com.compass.digitalbank.dto.NotificationResponse;
import com.compass.digitalbank.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/me/notifications")
@Tag(name = "Notifications", description = "Account notification history")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    @Operation(summary = "List notifications for the authenticated account")
    public Page<NotificationResponse> getMyNotifications(@PageableDefault(size = 20) Pageable pageable) {
        return notificationQueryService.getMyNotifications(pageable);
    }
}
