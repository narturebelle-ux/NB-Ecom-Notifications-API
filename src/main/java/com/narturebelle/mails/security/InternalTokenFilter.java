package com.narturebelle.mails.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Guards /internal/**: service-to-service calls only, with the shared private token. An unset token refuses everything. */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    private final byte[] expected;

    public InternalTokenFilter(@Value("${internal.api-token:}") String token) {
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    /** The decoded path Spring routes on. getRequestURI() is raw, so /%69nternal/... would skip this filter and still
     * reach the endpoint. pathInfo is included because servletPath alone is empty for a servlet not mapped to "/". */
    public static boolean isInternal(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String path = (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
        return path.startsWith("/internal/");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isInternal(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        boolean allowed = expected.length > 0 && given != null
                && MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8));
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"Missing or invalid internal token.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
