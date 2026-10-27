package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recusa corpos que declaram {@code Content-Length} acima do limite. Corpos em chunked não declaram tamanho e ficam
 * fora desta verificação simples. Não é um bean: é instalado só na cadeia de segurança.
 */
class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;
    private final ProblemResponseWriter writer;

    RequestBodySizeLimitFilter(long maxBytes, ProblemResponseWriter writer) {
        this.maxBytes = maxBytes;
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            writer.write(request, response, HttpStatus.CONTENT_TOO_LARGE, ErrorCode.REQUEST_NOT_SUPPORTED);
            return;
        }
        chain.doFilter(request, response);
    }
}
