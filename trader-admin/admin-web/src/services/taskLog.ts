import { http } from './http'
import type { ResData, PageRes } from '../types'

export type TaskLogDto = {
  id: number
  taskName: string
  taskGroup: string
  traceId?: string
  startTime: string
  endTime: string
  status: string
  content: string
  remark?: string
  errorMsg: string
  executionMs: number
  createdAt: string
}

export type TaskLogQuery = {
  pageNo: number
  pageSize: number
  taskCode?: string
  taskName?: string
  status?: string
  startTime?: string
  endTime?: string
}

export type TaskLogDeleteParam = {
  id: number
}

export const listTaskLogs = async (query: TaskLogQuery): Promise<ResData<PageRes<TaskLogDto>>> => {
  const res = await http.get('/logs/task', { params: query })
  return res.data as ResData<PageRes<TaskLogDto>>
}

export const getTaskLogDetail = async (id: number): Promise<ResData<TaskLogDto>> => {
  const res = await http.get(`/logs/task/${id}`)
  return res.data as ResData<TaskLogDto>
}

export const deleteTaskLog = async (payload: TaskLogDeleteParam): Promise<ResData<void>> => {
  const res = await http.post('/logs/task/delete', payload)
  return res.data as ResData<void>
}
