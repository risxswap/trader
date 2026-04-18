import { http } from './http'
import type { ResData } from '../types'

export type CorrelationDto = {
  id: string
  asset1: string
  asset1Type: string
  asset2: string
  asset2Type: string
  coefficient: number
  pValue?: number
  period: string
  createdAt: string
  updatedAt: string
}

export type PageRes<T> = {
  items: T[]
  total: number
  pageNo: number
  pageSize: number
}

export const listCorrelations = async (params: {
  pageNo: number
  pageSize: number
  asset1?: string
  asset2?: string
  period?: string
  minCoefficient?: number
  maxCoefficient?: number
}): Promise<ResData<PageRes<CorrelationDto>>> => {
  const res = await http.get('/correlation', { params })
  return res.data as ResData<PageRes<CorrelationDto>>
}

export const getCorrelationDetail = async (id: string): Promise<ResData<CorrelationDto>> => {
  const res = await http.get(`/correlation/${id}`)
  return res.data as ResData<CorrelationDto>
}
