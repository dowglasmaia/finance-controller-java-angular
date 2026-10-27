package com.dowglasmaia.financecontroller.identity.application.usecase;

import java.time.Instant;

public record TokenRefreshResult(String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
