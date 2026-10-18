package com.velrix.masterdata.domain;

public final class MasterDataStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private MasterDataStatus() {
    }

    public static boolean isActive(String status) {
        return ACTIVE.equals(status);
    }

    public static void requireKnown(String status) {
        if (!ACTIVE.equals(status) && !INACTIVE.equals(status)) {
            throw new IllegalArgumentException("status");
        }
    }
}
