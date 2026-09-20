package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "请输入真实姓名") @Size(max = 64) private String realName;
    @NotBlank(message = "请输入手机号") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") private String phone;
    @NotNull(message = "请选择所属部门") private Long deptId;
    @NotBlank(message = "请输入密码") @Size(min = 8, max = 64, message = "密码长度应为 8-64 位") private String password;
    @NotBlank(message = "请输入图形验证码") private String captcha;
    @NotBlank(message = "验证码标识不能为空") private String captchaUuid;
    public String getRealName(){return realName;} public void setRealName(String v){realName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getCaptcha(){return captcha;} public void setCaptcha(String v){captcha=v;}
    public String getCaptchaUuid(){return captchaUuid;} public void setCaptchaUuid(String v){captchaUuid=v;}
}
