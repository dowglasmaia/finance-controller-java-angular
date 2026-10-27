package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.domain.User;

import java.time.Instant;

public record AuthResult(User user, String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
