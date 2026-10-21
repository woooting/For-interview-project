package com.velrix.masterdata.application.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.impact.MasterDataImpactService;
import com.velrix.masterdata.application.impact.MasterDataKind;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.CodeNormalizer;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdWarehouseMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final MdWarehouseMapper mdWarehouseMapper;
    private final MdLocationMapper mdLocationMapper;
    private final WarehouseReadinessPolicy readinessPolicy;
    private final MasterdataAuditWriter auditWriter;
    private final MasterDataImpactService impactService;

    public MdWarehouse create(String code, String name, String address, String actor, String ip) {
        MdWarehouse warehouse = prepare(null, code, name, address, MasterDataStatus.ACTIVE);
        try {
            mdWarehouseMapper.insert(warehouse);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库编码已存在");
        }
        auditWriter.write(actor, "MD_WAREHOUSE_CREATE", "MD_WAREHOUSE", warehouse.getId(),
                "创建仓库 " + warehouse.getCodeNorm(), ip);
        return warehouse;
    }

    public MdWarehouse update(Long id, String code, String name, String address, String actor, String ip) {
        MdWarehouse existing = requireWarehouse(id);
        rejectIfFrozen(existing, "仓库已停用，不能编辑");
        MdWarehouse warehouse = prepare(id, code, name, address, null);
        try {
            mdWarehouseMapper.updateById(warehouse);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库编码已存在");
        }
        auditWriter.write(actor, "MD_WAREHOUSE_UPDATE", "MD_WAREHOUSE", id,
                "更新仓库 " + warehouse.getCodeNorm(), ip);
        return mdWarehouseMapper.selectById(id);
    }

    public MdWarehouse patchStatus(Long id, String status, String actor, String ip) {
        if (!MasterDataStatus.ACTIVE.equals(status) && !MasterDataStatus.INACTIVE.equals(status)) {
            throw new BizException(ApiCodes.BIZ_ERROR, "状态只能是 ACTIVE 或 INACTIVE");
        }
        MdWarehouse warehouse = requireWarehouse(id);
        warehouse.setStatus(status);
        mdWarehouseMapper.updateById(warehouse);
        auditWriter.write(actor, "MD_WAREHOUSE_STATUS", "MD_WAREHOUSE", id,
                "仓库 " + warehouse.getCodeNorm() + " 状态 → " + status, ip);
        return warehouse;
    }

    public MdWarehouse get(Long id) {
        return requireWarehouse(id);
    }

    public List<WarehouseListItem> list() {
        List<MdWarehouse> rows = mdWarehouseMapper.selectList(
                new LambdaQueryWrapper<MdWarehouse>().orderByDesc(MdWarehouse::getId));
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> warehouseIds = rows.stream().map(MdWarehouse::getId).toList();
        List<MdLocation> allLocations = mdLocationMapper.selectList(
                new LambdaQueryWrapper<MdLocation>().in(MdLocation::getWarehouseId, warehouseIds));

        Map<Long, List<MdLocation>> locationsByWarehouse = new HashMap<>();
        for (MdLocation loc : allLocations) {
            locationsByWarehouse.computeIfAbsent(loc.getWarehouseId(), k -> new ArrayList<>()).add(loc);
        }

        List<WarehouseListItem> result = new ArrayList<>(rows.size());
        for (MdWarehouse row : rows) {
            List<MdLocation> locations = locationsByWarehouse.getOrDefault(row.getId(), List.of());
            boolean inboundReady = readinessPolicy.isInboundReady(row, locations);
            boolean frozen = readinessPolicy.isWarehouseFrozen(row);
            result.add(new WarehouseListItem(row, inboundReady, frozen));
        }
        return result;
    }

    public ImpactSummary impact(Long id) {
        requireWarehouse(id);
        return impactService.summarize(MasterDataKind.WAREHOUSE, id);
    }

    public MdWarehouse requireWarehouse(Long id) {
        if (id == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库不存在");
        }
        MdWarehouse warehouse = mdWarehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库不存在");
        }
        return warehouse;
    }

    public void rejectIfFrozen(MdWarehouse warehouse, String message) {
        if (readinessPolicy.isWarehouseFrozen(warehouse)) {
            throw new BizException(ApiCodes.BIZ_ERROR, message);
        }
    }

    private MdWarehouse prepare(Long id, String code, String name, String address, String statusIfCreate) {
        // 非空校验在 DTO 层（@NotBlank + @Valid）；这里只管规范化、查重与组装
        String trimmedCode = code.trim();
        String norm = CodeNormalizer.normalizeCode(trimmedCode);
        LambdaQueryWrapper<MdWarehouse> dup = new LambdaQueryWrapper<MdWarehouse>()
                .eq(MdWarehouse::getCodeNorm, norm);
        if (id != null) {
            dup.ne(MdWarehouse::getId, id);
        }
        if (mdWarehouseMapper.selectOne(dup) != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "仓库编码已存在");
        }
        MdWarehouse warehouse;
        if (id == null) {
            warehouse = new MdWarehouse();
        } else {
            warehouse = mdWarehouseMapper.selectById(id);
            if (warehouse == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "仓库不存在");
            }
        }
        warehouse.setCode(trimmedCode);
        warehouse.setCodeNorm(norm);
        warehouse.setName(name.trim());
        warehouse.setAddress(address == null || address.isBlank() ? null : address.trim());
        if (statusIfCreate != null) {
            warehouse.setStatus(statusIfCreate);
        }
        return warehouse;
    }

    public record WarehouseListItem(MdWarehouse warehouse, boolean inboundReady, boolean warehouseFrozen) {
    }
}
