package com.narturebelle.mails.security;

import com.narturebelle.mails.internal.CareClient;
import com.narturebelle.mails.internal.CareUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Verifies the Authorization: Bearer <Firebase ID token> header and asks the Care API who that user is.
 * Absent or invalid tokens leave the request anonymous, and the SecurityFilterChain decides from there.
 * A Care outage is a 503, never a 401: the token may be fine, and a 401 tells the UI the person is signed out.
 */
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);

    private final FirebaseTokenVerifier tokenVerifier;
    private final CareClient careClient;

    public FirebaseAuthenticationFilter(FirebaseTokenVerifier tokenVerifier, CareClient careClient) {
        this.tokenVerifier = tokenVerifier;
        this.careClient = careClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return InternalTokenFilter.isInternal(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    authenticate(token);
                } catch (CareUnavailableException e) {
                    log.error("Could not load the signed-in user from the Care API: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"Please try again in a moment.\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        VerifiedFirebaseToken verified;
        try {
            verified = tokenVerifier.verify(token);
        } catch (Exception e) {
            log.debug("Firebase token verification failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            return;
        }
        CareClient.Identity identity = careClient.identity(verified.uid());
        Long userId = identity == null ? null : identity.userId();
        Set<String> roles = identity == null || identity.roles() == null || identity.roles().isEmpty()
                ? Set.of("MOTHER") : identity.roles();

        FirebaseUserPrincipal principal = new FirebaseUserPrincipal(userId, verified.uid(), verified.email(), roles);
        List<GrantedAuthority> authorities = roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
