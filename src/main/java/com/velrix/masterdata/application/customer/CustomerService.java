package com.velrix.masterdata.application.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.masterdata.application.impact.ImpactSummary;
import com.velrix.masterdata.application.impact.MasterDataImpactService;
import com.velrix.masterdata.application.impact.MasterDataKind;
import com.velrix.masterdata.application.support.MasterdataAuditWriter;
import com.velrix.masterdata.domain.CodeNormalizer;
import com.velrix.masterdata.domain.MasterDataStatus;
import com.velrix.masterdata.domain.customer.MdCustomer;
import com.velrix.masterdata.infrastructure.persistence.customer.MdCustomerMapper;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import com.velrix.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final MdCustomerMapper mdCustomerMapper;
    private final MasterdataAuditWriter auditWriter;
    private final MasterDataImpactService impactService;

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

    public MdCustomer get(Long id) {
        return requireCustomer(id);
    }

    public List<MdCustomer> list(String keyword, String status) {
        LambdaQueryWrapper<MdCustomer> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            q.and(w -> w.like(MdCustomer::getCode, k).or().like(MdCustomer::getName, k));
        }
        if (status != null && !status.isBlank()) {
            q.eq(MdCustomer::getStatus, status.trim());
        }
        q.orderByDesc(MdCustomer::getId);
        return mdCustomerMapper.selectList(q);
    }

    public MdCustomer patchStatus(Long id, String status, String actor, String ip) {
        if (!MasterDataStatus.ACTIVE.equals(status) && !MasterDataStatus.INACTIVE.equals(status)) {
            throw new BizException(ApiCodes.BIZ_ERROR, "状态只能是 ACTIVE 或 INACTIVE");
        }
        MdCustomer customer = requireCustomer(id);
        customer.setStatus(status);
        mdCustomerMapper.updateById(customer);
        auditWriter.write(actor, "MD_CUSTOMER_STATUS", "MD_CUSTOMER", id,
                "客户 " + customer.getCodeNorm() + " 状态 → " + status, ip);
        return customer;
    }

    public ImpactSummary impact(Long id) {
        requireCustomer(id);
        return impactService.summarize(MasterDataKind.CUSTOMER, id);
    }

    /**
     * 主数据仅停用不物理删（D2 定稿）：DELETE 一律 409。
     * 有引用时 message 带分类型数量，无引用时提示改用停用。
     */
    public void delete(Long id) {
        requireCustomer(id);
        ImpactSummary summary = impactService.summarize(MasterDataKind.CUSTOMER, id);
        if (!summary.deletable()) {
            throw new ConflictException(ApiCodes.BIZ_CONFLICT, summary.message() + "，不能删除");
        }
        throw new ConflictException(ApiCodes.BIZ_CONFLICT, "客户不支持物理删除，请使用停用");
    }

    private MdCustomer requireCustomer(Long id) {
        if (id == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户不存在");
        }
        MdCustomer customer = mdCustomerMapper.selectById(id);
        if (customer == null) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户不存在");
        }
        return customer;
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
        if (norm.isEmpty()) {
            throw new BizException(ApiCodes.BIZ_ERROR, "客户编码不能为空");
        }
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
