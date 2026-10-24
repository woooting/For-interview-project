package com.velrix.masterdata.application.supplier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.application.impact.SupplierImpactContributor;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.CodeNormalizer;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.supplier.MdSupplier;
import com.velrix.masterdata.infrastructure.persistence.supplier.MdSupplierMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final MdSupplierMapper mdSupplierMapper;
    private final SupplierImpactContributor supplierImpactContributor;
    private final MasterdataAuditWriter auditWriter;

    public MdSupplier create(String code, String name, String contactName, String contactPhone, String address,
                             String actor, String ip) {
        MdSupplier supplier = prepare(null, code, name, contactName, contactPhone, address, MasterDataStatus.ACTIVE);
        try {
            mdSupplierMapper.insert(supplier);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "供应商编码已存在");
        }
        auditWriter.write(actor, "MD_SUPPLIER_CREATE", "MD_SUPPLIER", supplier.getId(),
                "创建供应商 " + supplier.getCodeNorm(), ip);
        return supplier;
    }

    public MdSupplier update(Long id, String code, String name, String contactName, String contactPhone, String address,
                             String actor, String ip) {
        if (id == null || mdSupplierMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "供应商不存在");
        }
        MdSupplier supplier = prepare(id, code, name, contactName, contactPhone, address, null);
        try {
            mdSupplierMapper.updateById(supplier);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "供应商编码已存在");
        }
        auditWriter.write(actor, "MD_SUPPLIER_UPDATE", "MD_SUPPLIER", id,
                "更新供应商 " + supplier.getCodeNorm(), ip);
        return mdSupplierMapper.selectById(id);
    }

    private MdSupplier prepare(Long id, String code, String name, String contactName, String contactPhone,
                               String address, String statusIfCreate) {
        // 非空校验在 DTO 层（@NotBlank + @Valid）；这里只管规范化、查重与组装
        // 步骤：
        // 1) trim + 大写规范化编码
        // 2) 按 code_norm 查重（更新时排除自身）
        // 3) 新建或按 id 加载实体
        // 4) 回填字段返回
        String trimmedCode = code.trim();
        String norm = CodeNormalizer.normalizeCode(trimmedCode);
        LambdaQueryWrapper<MdSupplier> dup = new LambdaQueryWrapper<MdSupplier>()
                .eq(MdSupplier::getCodeNorm, norm);
        if (id != null) {
            dup.ne(MdSupplier::getId, id);
        }
        if (mdSupplierMapper.selectOne(dup) != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "供应商编码已存在");
        }

        MdSupplier supplier;
        if (id == null) {
            supplier = new MdSupplier();
        } else {
            supplier = mdSupplierMapper.selectById(id);
            if (supplier == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "供应商不存在");
            }
        }
        supplier.setCode(trimmedCode);
        supplier.setCodeNorm(norm);
        supplier.setName(name.trim());
        // 可选字段：传了（非空白）trim 后入库；没传存 null（更新时 null 不参与 UPDATE，自动保留旧值）
        supplier.setContactName(contactName == null || contactName.isBlank() ? null : contactName.trim());
        supplier.setContactPhone(contactPhone == null || contactPhone.isBlank() ? null : contactPhone.trim());
        supplier.setAddress(address == null || address.isBlank() ? null : address.trim());
        if (statusIfCreate != null) {
            supplier.setStatus(statusIfCreate);
        }
        return supplier;
    }
}
