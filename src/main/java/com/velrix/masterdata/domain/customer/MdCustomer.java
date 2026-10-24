package com.velrix.masterdata.domain.customer;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("md_customer")
public class MdCustomer {

    @TableId
    private Long id;
    private String code;
    private String codeNorm;
    private String name;
    private String status;
    private String contactName;
    private String contactPhone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
