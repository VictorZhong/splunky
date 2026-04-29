package com.wpb.spky.persistence;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SystemUserIds {

    public static final UUID SYSTEM_USER_ID = UUID.nameUUIDFromBytes(
            "splunky-system-user".getBytes(StandardCharsets.UTF_8));
    public static final String SYSTEM_USERNAME = "system";

    private SystemUserIds() {}
}
