package com.velrix.masterdata.infrastructure.stub;

import com.velrix.masterdata.application.port.LocationOccupancyQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Component
public class StubLocationOccupancyQuery implements LocationOccupancyQuery {

    @Override
    public Map<Long, BigDecimal> remainingQtyByProduct(long locationId) {
        return Collections.emptyMap();
    }
}
