package com.generac.ces.systemgateway.model;

import lombok.Getter;

/**
 * Defines a number of HTTP headers that are used to communicate information belonging outside the
 * scope of a REST resource. Whether a particular endpoint will use one or more of these headers
 * depends on its specific use case.
 */
public enum CallerMetadataRequestHeaders {
    @Deprecated
    ODIN_CALLER_ID(Constants.ODIN_CALLER_ID_HEADER_NAME),
    APPLICATION_NAME(Constants.APPLICATION_NAME_HEADER_NAME),
    USER_ID(Constants.USER_ID_HEADER_NAME),
    COGNITO_CLIENT_ID(Constants.COGNITO_CLIENT_ID_HEADER_NAME);

    @Getter private final String headerName;

    CallerMetadataRequestHeaders(String headerName) {
        this.headerName = headerName;
    }

    /**
     * Contains compile-time string constants of the header keys. For use in annotations or anywhere
     * else that requires compile-time constants.
     */
    public static class Constants {
        /**
         * Means more than one thing depending on how the endpoint is called. Will contain either a
         * company ID (string with UUID format) or an application name (string). Duplicated copy of
         * <a
         * href="https://github.com/neurio/odin-microservice/blob/745cfe2d38652d1bac26025d80287bba4e100801/src/main/java/com/generac/ces/system/control/api/configuration/OdinConstants.java#L9">OdinConstants.CALLER_ID_HEADER_KEY</a>
         *
         * @deprecated Confusing and unstructured, this value's meaning is not clear enough to use
         *     from new code. See {@link Constants#APPLICATION_NAME_HEADER_NAME} for an alternative
         *     for an endpoint that accepts the calling application's name. Create a new header for
         *     Company ID if it ever becomes necessary for a future use case.
         */
        @Deprecated public static final String ODIN_CALLER_ID_HEADER_NAME = "x-caller-company-id";
        /** The ID of the caller which the request to System Gateway is being made on behalf of. */
        public static final String CALLER_ID_HEADER_NAME = "caller-id";
        /** The ID of the user which the request to System Gateway is being made on behalf of. */
        public static final String USER_ID_HEADER_NAME = "user-id";
        /** The name of the application which is making a request to System Gateway. */
        public static final String APPLICATION_NAME_HEADER_NAME = "application-name";

        /** The Cognito client ID of the calling application. E.g. "2mhkgquiavvtjsdflkghs3h96d". */
        public static final String COGNITO_CLIENT_ID_HEADER_NAME = "subscriber-clientId";

        private Constants() {}
    }
}
