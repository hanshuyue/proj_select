export interface RegistrationForm {
  realName: string
  phone: string
  deptId?: number
  password: string
  confirm: string
  captcha: string
}

export function registrationErrors(form: RegistrationForm): string[] {
  const errors: string[] = []
  if (!form.realName.trim()) errors.push('请输入真实姓名')
  else if (form.realName.trim().length > 64) errors.push('真实姓名不能超过 64 个字符')
  if (!form.phone.trim()) errors.push('请输入手机号')
  else if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) errors.push('手机号格式不正确，请输入以 13–19 开头的 11 位手机号')
  if (!form.deptId) errors.push('请选择所属市县')
  if (!form.password.trim()) errors.push('请输入密码')
  else if (form.password.length < 6 || form.password.length > 64) errors.push('密码长度应为 6–64 位，支持纯数字')
  if (!form.confirm) errors.push('请再次输入密码')
  else if (form.password !== form.confirm) errors.push('两次密码输入不一致')
  if (!form.captcha.trim()) errors.push('请输入图形验证码')
  return errors
}
