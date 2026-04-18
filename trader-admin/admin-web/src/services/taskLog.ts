import { http } from './http'
import type { ResData, PageRes } from '../types'

export type TaskLogDto = {
  id: number
  taskName: string
  taskGroup: string
  startTime: string
  endTime: string
  status: string
  content: string
  errorMsg: string
  executionMs: number
  createdAt: string
}

export type TaskLogQuery = {
  pageNo: number
  pageSize: number
  taskName?: string
  status?: string
  startTime?: string
  endTime?: string
}

export const listTaskLogs = async (query: TaskLogQuery): Promise<ResData<PageRes<TaskLogDto>>> => {
  const res = await http.get('/logs/task', { params: query })
  return res.data as ResData<PageRes<TaskLogDto>>
}