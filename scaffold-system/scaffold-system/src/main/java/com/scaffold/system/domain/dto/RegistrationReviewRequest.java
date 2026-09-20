package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationReviewRequest {
    @NotBlank @Pattern(regexp="APPROVED|REJECTED", message="审核结果无效") private String status;
    @Size(max=500) private String remark;
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
}
