import { http } from './http'
import type { ResData } from '../types'

export type FundDto = {
  code: string
  name: string
  status: string
  market: string
  exchange: string
  management?: string
  custodian?: string
  fundType?: string
  managementFee?: number
  custodianFee?: number
  listDate?: string
  foundDate?: string
  updatedAt?: string
  createdAt?: string
}

export type FundMarketDto = {
  time: string
  code?: string
  updatedAt?: string
  open?: number
  high?: number
  low?: number
  close?: number
  amount?: number
  pctChg?: number
}

export type FundAdjDto = {
  time: string
  code?: string
  updatedAt?: string
  adjFactor: number
}

export type FundNavDto = {
  time: string
  code: string
  unitNav: number
  accumNav?: number
  accumDiv?: number
  netAsset?: number
  totalNetAsset?: number
  adjNav?: number
  updatedAt?: string
}

export type PageRes<T> = {
  items: T[]
  total: number
  pageNo: number
  pageSize: number
}

export const listFunds = async (payload: { pageNo: number; pageSize: number; keyword?: string; sortBy?: string; sortOrder?: 'asc' | 'desc'; market?: string; management?: string; custodian?: string; fundType?: string; managementFeeMin?: number; managementFeeMax?: number; custodianFeeMin?: number; custodianFeeMax?: number }): Promise<ResData<PageRes<FundDto>>> => {
  const res = await http.post('/fund/list', payload)
  return res.data as ResData<PageRes<FundDto>>
}

export const getFundDetail = async (code: string): Promise<ResData<any>> => {
  const res = await http.get(`/fund/detail/${code}`)
  return res.data as ResData<any>
}

export const updateFund = async (code: string, payload: { name?: string; status?: string; market?: string; exchange?: string }): Promise<ResData<void>> => {
  const res = await http.put(`/fund/update/${code}`, payload)
  return res.data as ResData<void>
}

export const deleteFund = async (code: string): Promise<ResData<void>> => {
  const res = await http.delete(`/fund/delete/${code}`)
  return res.data as ResData<void>
}

export const getFundMarket = async (payload: { code: string; startDate: string; endDate: string }): Promise<ResData<FundMarketDto[]>> => {
  const res = await http.post('/fund/market', payload)
  return res.data as ResData<FundMarketDto[]>
}

export const getFundAdj = async (payload: { code: string; startDate: string; endDate: string }): Promise<ResData<FundAdjDto[]>> => {
  const res = await http.post('/fund/adj', payload)
  return res.data as ResData<FundAdjDto[]>
}

export const getDefaultFundCode = async (): Promise<ResData<string>> => {
  const res = await http.get('/fund/default-code')
  return res.data as ResData<string>
}

export const listFundMarkets = async (payload: { pageNo: number; pageSize: number; code?: string; startDate?: string; endDate?: string }): Promise<ResData<PageRes<FundMarketDto>>> => {
  const res = await http.post('/fund/market/list', payload)
  return res.data as ResData<PageRes<FundMarketDto>>
}

export const listFundAdjs = async (payload: { pageNo: number; pageSize: number; code?: string; startDate?: string; endDate?: string }): Promise<ResData<PageRes<FundAdjDto>>> => {
  const res = await http.post('/fund/adj/list', payload)
  return res.data as ResData<PageRes<FundAdjDto>>
}

export const listFundNavs = async (payload: { pageNo: number; pageSize: number; code?: string; startTime?: string; endTime?: string }): Promise<ResData<PageRes<FundNavDto>>> => {
  const res = await http.post('/fund-nav/list', payload)
  return res.data as ResData<PageRes<FundNavDto>>
}
