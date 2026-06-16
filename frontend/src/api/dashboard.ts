import { request } from './request'

export interface BossDashboard {
  todaySales: number
  todayProfit: number
  totalReceivable: number
  totalPayable: number
}

export function fetchBossDashboard() {
  return request<BossDashboard>({
    url: '/api/boss/dashboard',
    method: 'GET',
  })
}
