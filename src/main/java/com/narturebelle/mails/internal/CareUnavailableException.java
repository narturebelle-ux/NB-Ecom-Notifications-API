package com.narturebelle.mails.internal;

/** The Care API couldn't be reached or answered with an error. */
public class CareUnavailableException extends RuntimeException {
    public CareUnavailableException(String message) {
        super(message);
    }
}
