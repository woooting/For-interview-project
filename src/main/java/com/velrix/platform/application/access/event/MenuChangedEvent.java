package com.velrix.platform.application.access.event;

import java.util.List;
import java.util.Optional;

public record MenuChangedEvent(Long menuId, Optional<List<Long>> userIds) {
}
