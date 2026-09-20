export function createDefaultBpmnXml(processKey: string, processName: string) {
  const key = safeProcessKey(processKey || 'purchase')
  const name = escapeXml(processName || processKey || '审批流程')
  return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://scaffold.workflow">
  <process id="${key}" name="${name}" isExecutable="true">
    <startEvent id="startEvent" name="提交申请"/>
    <sequenceFlow id="flow_start_dept" sourceRef="startEvent" targetRef="deptApprove"/>
    <userTask id="deptApprove" name="部门负责人审批" flowable:candidateGroups="super_admin,sys_admin"/>
    <sequenceFlow id="flow_dept_finance" sourceRef="deptApprove" targetRef="financeApprove"/>
    <userTask id="financeApprove" name="财务复核" flowable:candidateGroups="super_admin,biz_admin"/>
    <sequenceFlow id="flow_finance_end" sourceRef="financeApprove" targetRef="endEvent"/>
    <endEvent id="endEvent" name="归档"/>
  </process>
</definitions>`
}

function safeProcessKey(value: string) {
  const normalized = value.trim().replace(/[^A-Za-z0-9_]/g, '_')
  if (/^[A-Za-z][A-Za-z0-9_]*$/.test(normalized)) return normalized
  return `process_${normalized || 'default'}`
}

function escapeXml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}
