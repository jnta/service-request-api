package com.jnta.search.application;

public enum SearchSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    PREFERRED_DATE("preferredDate");

    private final String property;

    SearchSortField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }

    public static SearchSortField fromProperty(String property) {
        for (var field : values()) {
            if (field.property.equals(property)) {
                return field;
            }
        }
        throw new InvalidSearchQueryException("Unsupported sort field");
    }
}
