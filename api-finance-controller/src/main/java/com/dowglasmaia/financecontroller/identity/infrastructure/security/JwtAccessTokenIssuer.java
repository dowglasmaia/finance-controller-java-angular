package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.application.port.out.AccessTokenIssuer;
import com.dowglasmaia.financecontroller.identity.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** JWT HS256 de vida curta: {@code sub} = id do utilizador, claim {@code email}, {@code iss}, {@code iat}, {@code exp}, {@code jti}. */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final IdentityProperties.Jwt jwt;
    private final Clock clock;

    JwtAccessTokenIssuer(JwtEncoder encoder, IdentityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.jwt = properties.jwt();
        this.clock = clock;
    }

    @Override
    public String issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwt.issuer())
            .subject(user.id().value().toString())
            .claim("email", user.email().value())
            .issuedAt(now)
            .expiresAt(now.plus(jwt.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
