import { http } from './http'
import type { ResData, PageRes } from '../types'

export type ExchangeDto = {
  code: string
  name: string
  timezone?: string
  createdAt?: string
  updatedAt?: string
}

export type TradingDto = {
  id: number
  symbol: string
  type: string
  volume: number
  price: number
  createdAt?: string
  updatedAt?: string
}

export type InvestmentPositionDto = {
  id: number
  investmentId: number
  investmentLogId?: number
  asset: string
  assetType: string
  quantity: number
  buyPrice: number
  costPrice: number
  side: string
  createdAt?: string
  updatedAt?: string
}

export type InvestmentDto = {
  id: number
  name: string
  groupName?: string
  targetType?: string
  investType?: string
  brokerId?: number
  targets?: string[]
  budget?: number
  strategyInfo?: StrategyInfoDto
  cron?: string
  executorId?: string
  status?: string
  profitAmount?: number
  profitRate?: number
  createdAt?: string
  updatedAt?: string
}

export type InvestmentLogDto = {
  id: number
  investmentId: number
  recordDate: string
  type?: string
  cash?: number
  asset?: number
  profit?: number
  remark?: string
  notified?: number
  createdAt?: string
  updatedAt?: string
}

export type CalendarDto = {
  open: number
  date: string
  preDate?: string
  exchange?: string
  createdAt?: string
  updatedAt?: string
}

export type BrokerDto = {
  id: number
  name: string
  code: string
  initialCapital?: number
  currentCapital?: number
  intro?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export type NodeGroupDto = {
  id: number
  name: string
  code: string
  sort?: number
  defaultPending?: boolean
  nodeCount?: number
}

export type NodeGroupParam = {
  id?: number
  name: string
  code: string
  sort?: number
}

export type StrategyInfoDto = {
  className: string
  name: string
  configSchame?: string
  config?: string
}

export type StrategiesDto = {
  version: string
  lastUpdated: string
  items: StrategyInfoDto[]
}

export const listStrategies = async (): Promise<ResData<StrategiesDto>> => {
  const res = await http.get('/basic/strategies')
  return res.data as ResData<StrategiesDto>
}

export const listExchanges = async (payload: { pageNo: number; pageSize: number; keyword?: string }): Promise<ResData<PageRes<ExchangeDto>>> => {
  const res = await http.get('/exchange/list', { params: payload })
  return res.data as ResData<PageRes<ExchangeDto>>
}

export const listTradings = async (payload: { pageNo: number; pageSize: number; symbol?: string }): Promise<ResData<PageRes<TradingDto>>> => {
  const res = await http.get('/investment-trading/list', { params: payload })
  return res.data as ResData<PageRes<TradingDto>>
}

export const listPositions = async (payload: { pageNo: number; pageSize: number; asset?: string; investmentId?: number }): Promise<ResData<PageRes<InvestmentPositionDto>>> => {
  const res = await http.get('/investment-position/list', { params: payload })
  return res.data as ResData<PageRes<InvestmentPositionDto>>
}

export const addPosition = async (payload: Partial<InvestmentPositionDto>): Promise<ResData<void>> => {
  const res = await http.post('/investment-position/add', payload)
  return res.data as ResData<void>
}

export const updatePosition = async (payload: Partial<InvestmentPositionDto>): Promise<ResData<void>> => {
  const res = await http.post('/investment-position/update', payload)
  return res.data as ResData<void>
}

export const deletePosition = async (id: number): Promise<ResData<void>> => {
  const res = await http.post('/investment-position/delete', null, { params: { id } })
  return res.data as ResData<void>
}

export const listInvestments = async (payload: { 
  pageNo: number; 
  pageSize: number;
  name?: string;
  strategy?: string;
  budget?: number;
  status?: string;
}): Promise<ResData<PageRes<InvestmentDto>>> => {
  const res = await http.get('/investments', { params: payload })
  return res.data as ResData<PageRes<InvestmentDto>>
}

export const addInvestment = async (payload: Partial<InvestmentDto>): Promise<ResData<void>> => {
  const res = await http.post('/investments', payload)
  return res.data as ResData<void>
}

export const updateInvestment = async (payload: Partial<InvestmentDto>): Promise<ResData<void>> => {
  const res = await http.put('/investments', payload)
  return res.data as ResData<void>
}

export const deleteInvestment = async (id: number): Promise<ResData<void>> => {
  const res = await http.delete(`/investments/${id}`)
  return res.data as ResData<void>
}

export const getInvestment = async (id: number): Promise<ResData<InvestmentDto>> => {
  const res = await http.get(`/investments/${id}`)
  return res.data as ResData<InvestmentDto>
}

export const getSymbols = async (type: string, keyword?: string): Promise<ResData<{ value: string; label: string }[]>> => {
  const res = await http.get('/basic/symbols', { params: { type, keyword } })
  return res.data as ResData<{ value: string; label: string }[]>
}

export const listCalendars = async (payload: { pageNo: number; pageSize: number; exchange?: string; startDate?: string; endDate?: string }): Promise<ResData<PageRes<CalendarDto>>> => {
  const res = await http.get('/calendar/list', { params: payload })
  return res.data as ResData<PageRes<CalendarDto>>
}

export const listBrokers = async (payload: { pageNo: number; pageSize: number; keyword?: string }): Promise<ResData<PageRes<BrokerDto>>> => {
  const res = await http.get('/broker', { params: payload })
  return res.data as ResData<PageRes<BrokerDto>>
}

export const getBroker = async (id: number): Promise<ResData<BrokerDto>> => {
  const res = await http.get(`/broker/${id}`)
  return res.data as ResData<BrokerDto>
}

export const addBroker = async (payload: Partial<BrokerDto>): Promise<ResData<void>> => {
  const res = await http.post('/broker', payload)
  return res.data as ResData<void>
}

export const updateBroker = async (payload: Partial<BrokerDto>): Promise<ResData<void>> => {
  const res = await http.put('/broker', payload)
  return res.data as ResData<void>
}

export const deleteBroker = async (id: number): Promise<ResData<void>> => {
  const res = await http.delete(`/broker/${id}`)
  return res.data as ResData<void>
}

export const listInvestmentLogs = async (payload: {
  pageNo: number
  pageSize: number
  investmentId?: number
  type?: string
}): Promise<ResData<PageRes<InvestmentLogDto>>> => {
  const res = await http.get('/investment-logs', { params: payload })
  return res.data as ResData<PageRes<InvestmentLogDto>>
}

export const addInvestmentLog = async (payload: Partial<InvestmentLogDto>): Promise<ResData<void>> => {
  const res = await http.post('/investment-logs', payload)
  return res.data as ResData<void>
}

export const updateInvestmentLog = async (payload: Partial<InvestmentLogDto>): Promise<ResData<void>> => {
  const res = await http.put('/investment-logs', payload)
  return res.data as ResData<void>
}

export const deleteInvestmentLog = async (id: number): Promise<ResData<void>> => {
  const res = await http.delete(`/investment-logs/${id}`)
  return res.data as ResData<void>
}

export const listNodeGroups = async (): Promise<ResData<NodeGroupDto[]>> => {
  const res = await http.get('/node/group/list')
  return res.data as ResData<NodeGroupDto[]>
}

export const addNodeGroup = async (payload: NodeGroupParam): Promise<ResData<void>> => {
  const res = await http.post('/node/group', payload)
  return res.data as ResData<void>
}

export const updateNodeGroup = async (payload: NodeGroupParam): Promise<ResData<void>> => {
  const res = await http.put('/node/group', payload)
  return res.data as ResData<void>
}

export const deleteNodeGroup = async (id: number): Promise<ResData<void>> => {
  const res = await http.delete(`/node/group/${id}`)
  return res.data as ResData<void>
}
