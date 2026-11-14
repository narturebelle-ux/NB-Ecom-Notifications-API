package com.narturebelle.mails.internal;

import com.narturebelle.mails.security.InternalTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Private calls to the Care API, which owns users. Nothing it returns is stored here. */
@Component
public class CareClient {

    public record Identity(Long userId, Set<String> roles) {}

    /** firstName is null when the Care API has none, or is a version that doesn't send it. */
    public record Contact(String email, String firstName) {}

    private record CachedIdentity(Identity identity, Instant expiresAt) {}

    private static final Duration IDENTITY_CACHE = Duration.ofSeconds(60);
    private static final int IDENTITY_CACHE_SIZE = 1000;

    private final RestClient restClient;
    private final Map<String, CachedIdentity> identities = new ConcurrentHashMap<>();

    public CareClient(@Value("${care.base-url:http://localhost:8080}") String baseUrl, @Value("${internal.api-token:}") String token) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(3000);
        timeouts.setReadTimeout(10000);
        this.restClient = RestClient.builder()
                .requestFactory(timeouts)
                .baseUrl(baseUrl)
                .defaultHeader(InternalTokenFilter.HEADER, token)
                .build();
    }

    /** Null when Care has no such user yet. Only real users are cached, so a first sign-in is seen at once. */
    public Identity identity(String firebaseUid) {
        CachedIdentity cached = identities.get(firebaseUid);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.identity();
        }
        Identity identity = get("/internal/v1/users/by-firebase-uid/{uid}", Identity.class, firebaseUid);
        if (identity != null) {
            if (identities.size() >= IDENTITY_CACHE_SIZE) {
                identities.clear();
            }
            identities.put(firebaseUid, new CachedIdentity(identity, Instant.now().plus(IDENTITY_CACHE)));
        }
        return identity;
    }

    /** Null when the user is gone or has no address. Looked up at send time, so a changed address is used at once. */
    public Contact contactOf(Long userId) {
        return get("/internal/v1/users/{id}/contact", Contact.class, userId);
    }

    private <T> T get(String uri, Class<T> type, Object variable) {
        try {
            return restClient.get().uri(uri, variable).retrieve().body(type);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw new CareUnavailableException("Care API call failed: " + e.getClass().getSimpleName());
        }
    }
}
