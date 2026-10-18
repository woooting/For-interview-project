package com.velrix.masterdata.application.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.application.product.ProductService;
import com.velrix.masterdata.application.support.BatchValidation;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.CodeNormalizer;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.warehouse.MdLocation;
import com.velrix.masterdata.domain.warehouse.MdLocationCapacity;
import com.velrix.masterdata.domain.warehouse.MdWarehouse;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationCapacityMapper;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final MdLocationMapper mdLocationMapper;
    private final MdLocationCapacityMapper mdLocationCapacityMapper;
    private final WarehouseService warehouseService;
    private final WarehouseReadinessPolicy readinessPolicy;
    private final ProductService productService;
    private final LocationStorageSummaryAssembler summaryAssembler;
    private final MasterdataAuditWriter auditWriter;

    public List<MdLocation> createBatch(Long warehouseId, List<CreateLocationCommand> commands, String actor, String ip) {
        MdWarehouse warehouse = warehouseService.requireWarehouse(warehouseId);
        warehouseService.rejectIfFrozen(warehouse, "仓库已停用，不能新增库位");

        BatchValidation validation = new BatchValidation();
        if (commands == null || commands.isEmpty()) {
            validation.add("至少添加一个库位");
            validation.throwIfAny();
        }

        Set<String> normsInBatch = new HashSet<>();
        List<MdLocation> prepared = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            CreateLocationCommand cmd = commands.get(i);
            String prefix = "第 " + (i + 1) + " 个库位：";
            if (cmd.code() == null || cmd.code().isBlank()) {
                validation.add(prefix + "编码不能为空");
                continue;
            }
            if (cmd.name() == null || cmd.name().isBlank()) {
                validation.add(prefix + "名称不能为空");
                continue;
            }
            String norm = CodeNormalizer.normalizeCode(cmd.code());
            if (norm.isEmpty()) {
                validation.add(prefix + "编码不能为空");
                continue;
            }
            if (!normsInBatch.add(norm)) {
                validation.add(prefix + "编码「" + norm + "」与本批其它库位重复");
            }
            MdLocation existing = mdLocationMapper.selectOne(new LambdaQueryWrapper<MdLocation>()
                    .eq(MdLocation::getWarehouseId, warehouseId)
                    .eq(MdLocation::getCodeNorm, norm));
            if (existing != null) {
                validation.add(prefix + "编码「" + norm + "」在本仓库已存在");
            }
            MdLocation loc = new MdLocation();
            loc.setWarehouseId(warehouseId);
            loc.setCode(cmd.code().trim());
            loc.setCodeNorm(norm);
            loc.setName(cmd.name().trim());
            loc.setDescription(cmd.description() == null || cmd.description().isBlank()
                    ? null : cmd.description().trim());
            loc.setStatus(MasterDataStatus.ACTIVE);
            prepared.add(loc);
        }
        validation.throwIfAny();

        for (MdLocation loc : prepared) {
            try {
                mdLocationMapper.insert(loc);
            } catch (DuplicateKeyException e) {
                throw new BizException(ApiCodes.BIZ_ERROR, "库位编码冲突：" + loc.getCodeNorm());
            }
        }
        auditWriter.write(actor, "MD_LOCATION_BATCH_CREATE", "MD_WAREHOUSE", warehouseId,
                "新增库位 " + prepared.size() + " 个", ip);
        return prepared;
    }

    public MdLocation update(Long locationId, String code, String name, String description, String actor, String ip) {
        MdLocation location = requireLocation(locationId);
        MdWarehouse warehouse = warehouseService.requireWarehouse(location.getWarehouseId());
        warehouseService.rejectIfFrozen(warehouse, "仓库已停用，不能编辑库位");

        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位编码和名称不能为空");
        }
        String norm = CodeNormalizer.normalizeCode(code);
        MdLocation dup = mdLocationMapper.selectOne(new LambdaQueryWrapper<MdLocation>()
                .eq(MdLocation::getWarehouseId, location.getWarehouseId())
                .eq(MdLocation::getCodeNorm, norm)
                .ne(MdLocation::getId, locationId));
        if (dup != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位编码在本仓库已存在");
        }
        location.setCode(code.trim());
        location.setCodeNorm(norm);
        location.setName(name.trim());
        location.setDescription(description == null || description.isBlank() ? null : description.trim());
        try {
            mdLocationMapper.updateById(location);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位编码已存在");
        }
        auditWriter.write(actor, "MD_LOCATION_UPDATE", "MD_LOCATION", locationId,
                "更新库位 " + location.getCodeNorm(), ip);
        return location;
    }

    public MdLocation patchStatus(Long locationId, String status, String actor, String ip) {
        if (!MasterDataStatus.ACTIVE.equals(status) && !MasterDataStatus.INACTIVE.equals(status)) {
            throw new BizException(ApiCodes.BIZ_ERROR, "状态只能是 ACTIVE 或 INACTIVE");
        }
        MdLocation location = requireLocation(locationId);
        MdWarehouse warehouse = warehouseService.requireWarehouse(location.getWarehouseId());
        warehouseService.rejectIfFrozen(warehouse, "仓库已停用，不能变更库位状态");
        location.setStatus(status);
        mdLocationMapper.updateById(location);
        auditWriter.write(actor, "MD_LOCATION_STATUS", "MD_LOCATION", locationId,
                "库位 " + location.getCodeNorm() + " 状态 → " + status, ip);
        return location;
    }

    public void delete(Long locationId, String actor, String ip) {
        MdLocation location = requireLocation(locationId);
        MdWarehouse warehouse = warehouseService.requireWarehouse(location.getWarehouseId());
        warehouseService.rejectIfFrozen(warehouse, "仓库已停用，不能删除库位");
        long capCount = mdLocationCapacityMapper.selectCount(new LambdaQueryWrapper<MdLocationCapacity>()
                .eq(MdLocationCapacity::getLocationId, locationId));
        if (capCount > 0) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位仍有容量配置，不能删除");
        }
        mdLocationMapper.deleteById(locationId);
        auditWriter.write(actor, "MD_LOCATION_DELETE", "MD_LOCATION", locationId,
                "删除库位 " + location.getCodeNorm(), ip);
    }

    public LocationDetailView getDetail(Long locationId) {
        MdLocation location = requireLocation(locationId);
        MdWarehouse warehouse = warehouseService.requireWarehouse(location.getWarehouseId());
        List<MdLocationCapacity> capacities = mdLocationCapacityMapper.selectList(
                new LambdaQueryWrapper<MdLocationCapacity>().eq(MdLocationCapacity::getLocationId, locationId));
        String storageLabel = summaryAssembler.summarize(warehouse, location, capacities);
        return new LocationDetailView(location, warehouse, capacities, storageLabel,
                readinessPolicy.isWarehouseFrozen(warehouse));
    }

    public List<MdLocationCapacity> replaceCapacities(Long locationId, List<CapacityLineCommand> lines,
                                                      String actor, String ip) {
        MdLocation location = requireLocation(locationId);
        MdWarehouse warehouse = warehouseService.requireWarehouse(location.getWarehouseId());
        warehouseService.rejectIfFrozen(warehouse, "仓库已停用，不能修改容量");
        if (!MasterDataStatus.isActive(location.getStatus())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位已停用，不能修改容量");
        }

        BatchValidation validation = new BatchValidation();
        if (lines == null) {
            lines = List.of();
        }
        Set<Long> productIdsInBatch = new HashSet<>();
        List<MdLocationCapacity> prepared = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            CapacityLineCommand line = lines.get(i);
            String prefix = "第 " + (i + 1) + " 条容量：";
            if (line.productId() == null) {
                validation.add(prefix + "商品不能为空");
                continue;
            }
            if (!productIdsInBatch.add(line.productId())) {
                validation.add(prefix + "商品重复");
                continue;
            }
            if (line.maxQty() == null || line.maxQty().compareTo(BigDecimal.ZERO) <= 0) {
                validation.add(prefix + "maxQty 必须大于 0");
                continue;
            }
            try {
                productService.requireActive(line.productId());
            } catch (BizException e) {
                validation.add(prefix + e.getMessage());
                continue;
            }
            MdLocationCapacity cap = new MdLocationCapacity();
            cap.setLocationId(locationId);
            cap.setProductId(line.productId());
            cap.setMaxQty(line.maxQty());
            prepared.add(cap);
        }
        validation.throwIfAny();

        mdLocationCapacityMapper.delete(new LambdaQueryWrapper<MdLocationCapacity>()
                .eq(MdLocationCapacity::getLocationId, locationId));
        for (MdLocationCapacity cap : prepared) {
            mdLocationCapacityMapper.insert(cap);
        }
        auditWriter.write(actor, "MD_LOCATION_CAPACITY_REPLACE", "MD_LOCATION", locationId,
                "覆盖容量配置 " + prepared.size() + " 条", ip);
        return prepared;
    }

    public List<LocationSummaryView> listByWarehouse(Long warehouseId) {
        MdWarehouse warehouse = warehouseService.requireWarehouse(warehouseId);
        List<MdLocation> locations = mdLocationMapper.selectList(
                new LambdaQueryWrapper<MdLocation>()
                        .eq(MdLocation::getWarehouseId, warehouseId)
                        .orderByAsc(MdLocation::getCodeNorm));
        List<LocationSummaryView> views = new ArrayList<>();
        for (MdLocation loc : locations) {
            List<MdLocationCapacity> caps = mdLocationCapacityMapper.selectList(
                    new LambdaQueryWrapper<MdLocationCapacity>().eq(MdLocationCapacity::getLocationId, loc.getId()));
            views.add(new LocationSummaryView(
                    loc,
                    summaryAssembler.summarize(warehouse, loc, caps),
                    readinessPolicy.isWarehouseFrozen(warehouse)));
        }
        return views;
    }

    public MdLocation requireLocation(Long locationId) {
        if (locationId == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位不存在");
        }
        MdLocation location = mdLocationMapper.selectById(locationId);
        if (location == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "库位不存在");
        }
        return location;
    }

    public record CreateLocationCommand(String code, String name, String description) {
    }

    public record CapacityLineCommand(Long productId, BigDecimal maxQty) {
    }

    public record LocationDetailView(
            MdLocation location,
            MdWarehouse warehouse,
            List<MdLocationCapacity> capacities,
            String storageLabel,
            boolean warehouseFrozen) {
    }

    public record LocationSummaryView(MdLocation location, String storageLabel, boolean warehouseFrozen) {
    }
}
