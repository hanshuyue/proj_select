import type { HttpClient } from './http'
export interface DepartmentOption { id:number; name:string }
export interface Registration { id:number; phone:string; realName:string; deptName:string; status:number; auditStatus:string; administrator:boolean|number; createTime:string }
export interface UserDocument { id:number; title:string; description?:string; businessType?:'SELECTION'|'INITIATION'|'OTHER'; projectId?:number; projectName?:string; revisionNo:number; previousDocumentId?:number; originalName:string; fileSize:number; status:string; feedback?:string; submitterId?:number; submitterName:string; deptName?:string; reviewerName?:string; submitTime:string; reviewTime?:string }
export function createCollaborationApi(client:HttpClient){return {
 departments:()=>client.get<DepartmentOption[]>('/public/departments'), register:(body:{realName:string;phone:string;deptId:number;password:string;captcha:string;captchaUuid:string})=>client.post<void>('/auth/register',body),
 registrations:()=>client.get<Registration[]>('/admin/registrations'), setAdministrator:(id:number,administrator:boolean)=>client.put<void>(`/admin/registrations/${id}/administrator`,{administrator}),
 documents:()=>client.get<UserDocument[]>('/documents'), uploadDocument:(form:FormData)=>client.upload<{id:number}>('/documents',form),
 async uploadFinishedPpt(businessType:'SELECTION'|'INITIATION',projectId:number,projectName:string,file:File,changeSummary:string){const form=new FormData();form.append('title',`${projectName}成品PPT`);form.append('description',changeSummary);form.append('businessType',businessType);form.append('projectId',String(projectId));form.append('projectName',projectName);form.append('file',file);return client.upload<{id:number}>('/documents',form)},
 downloadDocument:(id:number)=>client.download(`/documents/${id}/file`),
 feedback:(id:number,body:{status:'APPROVED'|'REVISION_REQUIRED';feedback:string})=>client.put<void>(`/documents/${id}/feedback`,body),
}}
