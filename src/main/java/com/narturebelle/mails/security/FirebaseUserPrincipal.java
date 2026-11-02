package com.narturebelle.mails.security;

import java.util.Set;

/** Built from a verified Firebase token plus the Care API's answer about that user. userId is null when
 * the token is valid but the user hasn't called the Care API's POST /api/v1/auth/firebase yet. */
public record FirebaseUserPrincipal(Long userId, String firebaseUid, String email, Set<String> roles) {
}
