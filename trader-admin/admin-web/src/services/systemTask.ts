import { http } from './http'
import type { PageRes, ResData } from '../types'

export interface SystemTaskQuery {
  pageNo: number
  pageSize: number
  appName?: string
  taskType?: string
  includeInvestment?: boolean
  taskCode?: string
  taskName?: string
  status?: string
}

export interface SystemTaskDto {
  id: number
  appName: string
  taskType?: string
  sourceType?: string
  taskCode: string
  taskName: string
  cron: string
  enabled?: boolean
  status: string
  result?: string
  paramsJson?: string
  paramSchema?: string
  defaultParamsJson?: string
  remark?: string
  version?: number
  updatedAt?: string
  createdAt?: string
}

export interface SystemTaskUpdateParam {
  id: number
  cron: string
  enabled?: boolean
  status: string
  paramsJson?: string
  remark?: string
}

export interface SystemTaskTriggerParam {
  id: number
}

export interface TaskDefinitionDto {
  taskType: string
  taskCode: string
  taskName: string
  defaultCron?: string
  defaultEnabled: boolean
  paramSchema?: string
  defaultParamsJson?: string
  reportNodeId?: string
  reportNodeType?: string
  reportAt?: string
}

export const listTaskDefinitions = async (data: { taskType?: string; taskCode?: string; taskName?: string }): Promise<ResData<TaskDefinitionDto[]>> => {
  const res = await http.post('/task/definition/list', data)
  return res.data as ResData<TaskDefinitionDto[]>
}

export const createTaskInstance = async (data: {
  taskType: string
  taskCode: string
  taskName: string
  cron: string
  enabled?: boolean
  status: string
  paramsJson?: string
  remark?: string
}): Promise<ResData<void>> => {
  const res = await http.post('/task/instance/create', data)
  return res.data as ResData<void>
}

export const deleteTaskInstance = async (data: { id: number }): Promise<ResData<void>> => {
  const res = await http.post('/task/instance/delete', data)
  return res.data as ResData<void>
}

export const listSystemTasks = async (payload: SystemTaskQuery): Promise<ResData<PageRes<SystemTaskDto>>> => {
  const res = await http.post('/task/list', payload)
  return res.data as ResData<PageRes<SystemTaskDto>>
}

export const updateSystemTask = async (payload: SystemTaskUpdateParam): Promise<ResData<void>> => {
  const res = await http.post('/task/update', payload)
  return res.data as ResData<void>
}

export const triggerSystemTask = async (payload: SystemTaskTriggerParam): Promise<ResData<void>> => {
  const res = await http.post('/task/trigger', payload)
  return res.data as ResData<void>
}

export const getSystemTaskDetail = async (id: number): Promise<ResData<SystemTaskDto>> => {
  const res = await http.get(`/task/${id}`)
  return res.data as ResData<SystemTaskDto>
}
