package com.velrix.masterdata.application.impact;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聚合所有 Contributor，回答"这条主数据被谁引用"（需求 §6.1）。
 * GET impact 与 DELETE 的 409 拒绝共用这里的聚合结果。
 */
@Service
public class MasterDataImpactService {

    private final Map<MasterDataKind, List<MasterDataImpactContributor>> contributorsByKind;

    public MasterDataImpactService(List<MasterDataImpactContributor> contributors) {
        Map<MasterDataKind, List<MasterDataImpactContributor>> grouped = new EnumMap<>(MasterDataKind.class);
        for (MasterDataImpactContributor contributor : contributors) {
            grouped.computeIfAbsent(contributor.kind(), k -> new ArrayList<>()).add(contributor);
        }
        this.contributorsByKind = grouped;
    }

    public ImpactSummary summarize(MasterDataKind kind, long entityId) {
        List<ImpactItem> items = contributorsByKind.getOrDefault(kind, List.of()).stream()
                .flatMap(contributor -> contributor.contribute(entityId).stream())
                .collect(Collectors.toList());
        boolean deletable = items.isEmpty();
        return new ImpactSummary(deletable, buildMessage(kind, items), items);
    }

    private String buildMessage(MasterDataKind kind, List<ImpactItem> items) {
        if (items.isEmpty()) {
            return kindLabel(kind) + "暂无业务引用";
        }
        String joined = items.stream()
                .map(item -> item.label() + " " + item.count() + " 条")
                .collect(Collectors.joining("、"));
        return kindLabel(kind) + "被引用：" + joined;
    }

    private String kindLabel(MasterDataKind kind) {
        return switch (kind) {
            case PRODUCT -> "商品";
            case WAREHOUSE -> "仓库";
            case CUSTOMER -> "客户";
            case SUPPLIER -> "供应商";
            case LOCATION -> "库位";
        };
    }
}
