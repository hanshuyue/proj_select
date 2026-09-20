import type { ApiResponse, HttpClient } from './http'
import { authSession } from './session'
export interface FinanceItem { id?:number; mode?:string; name:string; amountIncTax?:number; taxRate?:number; amountExTax?:number; description?:string }
export interface InitiationAttachment { id:number; attachmentType:string; originalFilename:string; fileSize:number; createTime?:string }
export interface InitiationProject {
  id?:number; projectName:string; opportunityNo:string; customerName?:string; customerType?:string; industryType?:string
  departmentName?:string; projectManager?:string; reportYear:number; reportMonth:number; agreementYears?:number
  contractAmountIncTax?:number; totalRevenueIncTax?:number; totalCostIncTax?:number; overallProfitRate?:number
  fundRiskLevel?:string; threeLineLevel?:string; status:string; sections:Record<string, any>
  incomeItems:FinanceItem[]; costItems:FinanceItem[]; attachments?:InitiationAttachment[]; updateTime?:string
}
export interface InitiationTemplate { id:number; templateName:string; originalFilename:string; fileSize:number; placeholderKeys?:string; defaultFlag:boolean|number; createTime:string }
async function uploadFile(url:string,name:string,file:File) {
  const form=new FormData();form.append('name',name);form.append('file',file)
  const headers:Record<string,string>={Accept:'application/json'},token=authSession.token();if(token)headers.Authorization=`Bearer ${token}`
  const response=await fetch(`/api${url}`,{method:'POST',headers,body:form}),payload=await response.json() as ApiResponse<{id:number}>
  if(!response.ok||payload.code!==200)throw new Error(payload.message||'上传失败');return payload.data
}
export function createInitiationApi(http:HttpClient){return {
  projects:{
    list:(q:Record<string,string|number|undefined>)=>http.get<{rows:InitiationProject[];total:number;pageNum:number;pageSize:number}>('/initiation/projects',q),
    detail:(id:number)=>http.get<InitiationProject>(`/initiation/projects/${id}`),
    create:(data:InitiationProject)=>http.post<{id:number}>('/initiation/projects',data),
    update:(id:number,data:InitiationProject)=>http.put<void>(`/initiation/projects/${id}`,data),
    remove:(id:number)=>http.delete<void>(`/initiation/projects/${id}`),
    calculate:(data:InitiationProject)=>http.post<{totalRevenueIncTax:number;totalCostIncTax:number;profit:number;overallProfitRate:number}>('/initiation/projects/calculate',data),
    validate:(id:number)=>http.get<string[]>(`/initiation/projects/${id}/validate`),
    exportPpt:(id:number,templateId?:number)=>http.download(`/initiation/projects/${id}/ppt`,{templateId}),
    attachments:(id:number)=>http.get<InitiationAttachment[]>(`/initiation/projects/${id}/attachments`),
    attachmentDownload:(id:number)=>http.download(`/initiation/attachments/${id}/file`),
    attachmentRemove:(id:number)=>http.delete<void>(`/initiation/attachments/${id}`),
    async attachmentUpload(id:number,type:string,file:File){
      const form=new FormData();form.append('type',type);form.append('file',file);const headers:Record<string,string>={Accept:'application/json'},token=authSession.token();if(token)headers.Authorization=`Bearer ${token}`
      const res=await fetch(`/api/initiation/projects/${id}/attachments`,{method:'POST',headers,body:form}),payload=await res.json() as ApiResponse<{id:number}>;if(!res.ok||payload.code!==200)throw new Error(payload.message||'附件上传失败');return payload.data
    },
  },
  templates:{
    list:()=>http.get<InitiationTemplate[]>('/initiation/templates'),upload:(name:string,file:File)=>uploadFile('/initiation/templates',name,file),
    setDefault:(id:number)=>http.put<void>(`/initiation/templates/${id}/default`),download:(id:number)=>http.download(`/initiation/templates/${id}/file`),
    remove:(id:number)=>http.delete<void>(`/initiation/templates/${id}`),
  },
}}
