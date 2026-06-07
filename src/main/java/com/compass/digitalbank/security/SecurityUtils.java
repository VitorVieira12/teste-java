package com.compass.digitalbank.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedAccount account) {
            return account.getAccountId();
        }
        throw new IllegalStateException("No authenticated account in security context");
    }

    public static void requireAccountOwnership(Long accountId) {
        if (!currentAccountId().equals(accountId)) {
            throw new com.compass.digitalbank.exception.ForbiddenException(
                    "Access denied to account id: " + accountId);
        }
    }
}
