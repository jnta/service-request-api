package com.jnta.search.application;

public class SearchAccessDeniedException extends RuntimeException {

    public SearchAccessDeniedException() {
        super("The user is not allowed to search solicitations");
    }
}
