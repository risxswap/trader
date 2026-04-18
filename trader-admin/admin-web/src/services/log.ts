import { http } from './http'
import type { ResData, PageRes } from '../types'

export type MsgPushLogDto = {
  id: number
  type: string
  content: string
  status: string
  channel: string
  title: string
  recipient: string
  createdAt: string
  updatedAt: string
}

export type MsgPushLogQuery = {
  pageNo: number
  pageSize: number
  type?: string
  channel?: string
  status?: string
}

export const listMsgPushLogs = async (query: MsgPushLogQuery): Promise<ResData<PageRes<MsgPushLogDto>>> => {
  const res = await http.get('/msg-push-log', { params: query })
  return res.data as ResData<PageRes<MsgPushLogDto>>
}

export const getMsgPushLogDetail = async (id: number): Promise<ResData<MsgPushLogDto>> => {
  const res = await http.get(`/msg-push-log/${id}`)
  return res.data as ResData<MsgPushLogDto>
}
