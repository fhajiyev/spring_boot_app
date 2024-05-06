package com.generac.ces.system.control.api.configuration;

import lombok.Getter;

public enum OdinMetadataRequestHeaders {
    USER_ID(Constants.USER_ID_HEADER_NAME),
    CALLER_ID(Constants.CALLER_ID_HEADER_NAME);

    @Getter private final String headerName;

    OdinMetadataRequestHeaders(String headerName) {
        this.headerName = headerName;
    }

    /**
     * Contains compile-time string constants of the header keys. For use in annotations or anywhere
     * else that requires compile-time constants.
     */
    public static class Constants {
        @Deprecated public static final String ODIN_CALLER_ID_HEADER_NAME = "x-caller-company-id";
        /** The ID of the user which the request to Odin is being made on behalf of. */
        public static final String USER_ID_HEADER_NAME = "user-id";
        /** The id of the caller which is making a request. */
        public static final String CALLER_ID_HEADER_NAME = "caller-id";

        private Constants() {}
    }
}
