package com.velrix.masterdata.application.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.application.impact.CustomerImpactContributor;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.CodeNormalizer;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.customer.MdCustomer;
import com.velrix.masterdata.infrastructure.persistence.customer.MdCustomerMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final MdCustomerMapper mdCustomerMapper;
    private final CustomerImpactContributor customerImpactContributor;
    private final MasterdataAuditWriter auditWriter;

    public MdCustomer create(String code, String name, String contactName, String contactPhone, String address,
                             String actor, String ip) {
        MdCustomer customer = prepare(null, code, name, contactName, contactPhone, address, MasterDataStatus.ACTIVE);
        try {
            mdCustomerMapper.insert(customer);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户编码已存在");
        }
        auditWriter.write(actor, "MD_CUSTOMER_CREATE", "MD_CUSTOMER", customer.getId(),
                "创建客户 " + customer.getCodeNorm(), ip);
        return customer;
    }

    public MdCustomer update(Long id, String code, String name, String contactName, String contactPhone, String address,
                             String actor, String ip) {
        if (id == null || mdCustomerMapper.selectById(id) == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户不存在");
        }
        MdCustomer customer = prepare(id, code, name, contactName, contactPhone, address, null);
        try {
            mdCustomerMapper.updateById(customer);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户编码已存在");
        }
        auditWriter.write(actor, "MD_CUSTOMER_UPDATE", "MD_CUSTOMER", id,
                "更新客户 " + customer.getCodeNorm(), ip);
        return mdCustomerMapper.selectById(id);
    }

    private MdCustomer prepare(Long id, String code, String name, String contactName, String contactPhone,
                               String address, String statusIfCreate) {
        // 非空校验在 DTO 层（@NotBlank + @Valid）；这里只管规范化、查重与组装
        // 步骤：
        // 1) trim + 大写规范化编码
        // 2) 按 code_norm 查重（更新时排除自身）
        // 3) 新建或按 id 加载实体
        // 4) 回填字段返回
        String trimmedCode = code.trim();
        String norm = CodeNormalizer.normalizeCode(trimmedCode);
        LambdaQueryWrapper<MdCustomer> dup = new LambdaQueryWrapper<MdCustomer>()
                .eq(MdCustomer::getCodeNorm, norm);
        if (id != null) {
            dup.ne(MdCustomer::getId, id);
        }
        if (mdCustomerMapper.selectOne(dup) != null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户编码已存在");
        }

        MdCustomer customer;
        if (id == null) {
            customer = new MdCustomer();
        } else {
            customer = mdCustomerMapper.selectById(id);
            if (customer == null) {
                throw new BizException(ApiCodes.BIZ_ERROR, "客户不存在");
            }
        }
        customer.setCode(trimmedCode);
        customer.setCodeNorm(norm);
        customer.setName(name.trim());
        // 可选字段：传了（非空白）trim 后入库；没传存 null（更新时 null 不参与 UPDATE，自动保留旧值）
        customer.setContactName(contactName == null || contactName.isBlank() ? null : contactName.trim());
        customer.setContactPhone(contactPhone == null || contactPhone.isBlank() ? null : contactPhone.trim());
        customer.setAddress(address == null || address.isBlank() ? null : address.trim());
        if (statusIfCreate != null) {
            customer.setStatus(statusIfCreate);
        }
        return customer;
    }
}
