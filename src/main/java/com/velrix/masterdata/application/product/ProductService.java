package com.velrix.masterdata.application.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.CodeNormalizer;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.product.MdProduct;
import com.velrix.masterdata.infrastructure.persistence.product.MdProductMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final MdProductMapper mdProductMapper;
    private final MasterdataAuditWriter auditWriter;

    public MdProduct create(String code, String name, String unit, Boolean batchManaged, Boolean serialManaged,
                            String actor, String ip) {
        MdProduct product = prepare(null, code, name, unit, batchManaged, serialManaged, MasterDataStatus.ACTIVE);
        try {
            mdProductMapper.insert(product);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品编码已存在");
        }
        auditWriter.write(actor, "MD_PRODUCT_CREATE", "MD_PRODUCT", product.getId(),
                "创建商品 " + product.getCodeNorm(), ip);
        return product;
    }

    public MdProduct update(Long id, String code, String name, String unit, Boolean batchManaged, Boolean serialManaged,
                            String actor, String ip) {
        if (id == null || mdProductMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品不存在");
        }
        MdProduct product = prepare(id, code, name, unit, batchManaged, serialManaged, null);
        try {
            mdProductMapper.updateById(product);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品编码已存在");
        }
        auditWriter.write(actor, "MD_PRODUCT_UPDATE", "MD_PRODUCT", id,
                "更新商品 " + product.getCodeNorm(), ip);
        return mdProductMapper.selectById(id);
    }

    public MdProduct get(Long id) {
        MdProduct product = mdProductMapper.selectById(id);
        if (product == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品不存在");
        }
        return product;
    }

    public List<MdProduct> list(String keyword, String status) {
        LambdaQueryWrapper<MdProduct> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            q.and(w -> w.like(MdProduct::getCode, k).or().like(MdProduct::getName, k));
        }
        if (status != null && !status.isBlank()) {
            q.eq(MdProduct::getStatus, status.trim());
        }
        q.orderByDesc(MdProduct::getId);
        return mdProductMapper.selectList(q);
    }

    public MdProduct requireActive(long productId) {
        MdProduct product = mdProductMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品不存在");
        }
        if (!MasterDataStatus.isActive(product.getStatus())) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品已停用");
        }
        return product;
    }

    private MdProduct prepare(Long id, String code, String name, String unit,
                              Boolean batchManaged, Boolean serialManaged, String statusIfCreate) {
        if (code == null || code.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品编码不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品名称不能为空");
        }
        if (unit == null || unit.isBlank()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "计量单位不能为空");
        }
        String trimmedCode = code.trim();
        String norm = CodeNormalizer.normalizeCode(trimmedCode);
        if (norm.isEmpty()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品编码不能为空");
        }
        LambdaQueryWrapper<MdProduct> dup = new LambdaQueryWrapper<MdProduct>()
                .eq(MdProduct::getCodeNorm, norm);
        if (id != null) {
            dup.ne(MdProduct::getId, id);
        }
        if (mdProductMapper.selectOne(dup) != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "商品编码已存在");
        }

        MdProduct product;
        if (id == null) {
            product = new MdProduct();
        } else {
            product = mdProductMapper.selectById(id);
            if (product == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "商品不存在");
            }
        }
        product.setCode(trimmedCode);
        product.setCodeNorm(norm);
        product.setName(name.trim());
        product.setUnit(unit.trim());
        product.setBatchManaged(Boolean.TRUE.equals(batchManaged));
        product.setSerialManaged(Boolean.TRUE.equals(serialManaged));
        if (statusIfCreate != null) {
            product.setStatus(statusIfCreate);
        }
        return product;
    }
}
