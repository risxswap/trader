import { http } from './http';
import type { ResData } from '../types';

export interface ChartDataDto {
  label: string;
  value: number;
}

export interface DashboardDto {
  totalAsset: number;
  todayChange: number;
  totalProfit: number;
  holdingCount: number;
  winRate: string;
  maxDrawdown: string;
  riskIndicator: string;
  assetTrend: ChartDataDto[];
  assetDistribution: ChartDataDto[];
}

export interface SystemStatusDto {
  redisStatus: string;
  clickHouseStatus: string;
  mysqlStatus: string;
}

export const getDashboardOverview = () => {
  return http.get('/dashboard/overview').then(res => res.data as ResData<DashboardDto>);
};

export const getSystemStatus = () => {
  return http.get('/dashboard/system-status').then(res => res.data as ResData<SystemStatusDto>);
};
