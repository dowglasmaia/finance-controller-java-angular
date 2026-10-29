package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetRequestLimiter;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Peças da recuperação de senha: executor assíncrono do e-mail, limitador de pedidos, limpeza diária dos tokens vencidos
 * e a exigência de HTTPS no link fora do desenvolvimento.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(PasswordResetProperties.class)
class PasswordResetConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetConfiguration.class);

    /** Fila pequena: se encher, o pedido é descartado (a resposta continua neutra) em vez de atrasar quem pediu. */
    @Bean(name = "mailTaskExecutor")
    ThreadPoolTaskExecutor mailTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mail-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        RejectedExecutionHandler discard = (task, pool) -> log.warn("mail queue full, task dropped");
        executor.setRejectedExecutionHandler(discard);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Bean
    PasswordResetRequestLimiter passwordResetRequestLimiter(PasswordResetProperties properties, Clock clock) {
        var limiter = new InMemoryLoginAttemptLimiter(
            new IdentityProperties.Limiter(properties.maxRequests(), properties.window(), 10_000), clock);
        return (email, origin) -> {
            if (limiter.isBlocked(email, origin)) {
                return false;
            }
            limiter.recordFailure(email, origin);
            return true;
        };
    }

    /** Fora de local e test o link do e-mail precisa ser HTTPS; senão a aplicação não sobe. */
    @Bean
    Object passwordResetBaseUrlGuard(PasswordResetProperties properties, Environment environment) {
        boolean development = environment.acceptsProfiles(Profiles.of("local", "test"));
        if (!development && !properties.appBaseUrl().toLowerCase().startsWith("https://")) {
            throw new IllegalStateException("app.password-reset.app-base-url (env FC_APP_BASE_URL) must be https outside local/test");
        }
        return new Object();
    }

    @Bean
    PasswordResetTokenCleanup passwordResetTokenCleanup(PasswordResetTokenRepository tokens, Clock clock) {
        return new PasswordResetTokenCleanup(tokens, clock);
    }

    /** Remove, uma vez por dia, os tokens vencidos há mais de um dia. */
    static final class PasswordResetTokenCleanup {
        private final PasswordResetTokenRepository tokens;
        private final Clock clock;

        PasswordResetTokenCleanup(PasswordResetTokenRepository tokens, Clock clock) {
            this.tokens = tokens;
            this.clock = clock;
        }

        @Scheduled(cron = "0 30 3 * * *")
        void run() {
            int removed = tokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
            log.info("expired password reset tokens removed count={}", removed);
        }
    }
}
