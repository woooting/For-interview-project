package com.velrix.masterdata.web.dto;

import java.util.List;

public record CreateLocationsRequest(List<LocationLine> locations) {

    public record LocationLine(String code, String name, String description) {
    }
}
