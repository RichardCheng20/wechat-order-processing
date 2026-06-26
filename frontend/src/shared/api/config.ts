import { request } from './request'

export interface MiniProgramConfig {
  orderNotifyTemplateId?: string | null
}

export function fetchMiniProgramConfig() {
  return request<MiniProgramConfig>({
    url: '/api/config/mini-program',
    method: 'GET',
  })
}
