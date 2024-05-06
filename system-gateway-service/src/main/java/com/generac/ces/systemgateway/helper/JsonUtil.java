package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.control.message.List;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

public final class JsonUtil {

    private JsonUtil() {}

    public static List.ListResponse toListResponse(String json)
            throws InvalidProtocolBufferException {
        List.ListResponse.Builder builder = List.ListResponse.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return builder.build();
    }
}
