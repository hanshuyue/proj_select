package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DocumentFeedbackRequest {
    @Size(max=2000) private String feedback;
    @Pattern(regexp="APPROVED|REVISION_REQUIRED", message="审核结果无效") private String status;
    public String getFeedback(){return feedback;} public void setFeedback(String v){feedback=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
