import { request } from './request'

export type PersonnelJobRole = 'STALL_OWNER' | 'STALL_MANAGER' | 'DELIVERY'

export interface PersonnelItem {
  id: number
  userId?: number
  workerCode?: string
  name: string
  phone?: string
  jobRole?: PersonnelJobRole | string
  jobRoleLabel?: string
  bound?: boolean
  status?: number
}

export interface PersonnelFormData {
  name: string
  phone: string
  jobRole: PersonnelJobRole
}

export const PERSONNEL_ROLE_OPTIONS: {
  value: PersonnelJobRole
  label: string
  desc: string
}[] = [
  { value: 'STALL_OWNER', label: '档口老板', desc: '拥有老板端业务权限，可查看全部经营数据' },
  { value: 'STALL_MANAGER', label: '档口经理', desc: '拥有老板端业务权限，不可查看数据平台' },
  { value: 'DELIVERY', label: '配送员', desc: '负责拣单，标记已拣单' },
]

export function fetchPersonnelList() {
  return request<PersonnelItem[]>({
    url: '/api/boss/personnel',
    method: 'GET',
  })
}

export function createPersonnel(data: PersonnelFormData) {
  return request<PersonnelItem>({
    url: '/api/boss/personnel',
    method: 'POST',
    data,
  })
}

export function updatePersonnel(id: number, data: PersonnelFormData) {
  return request<PersonnelItem>({
    url: `/api/boss/personnel/${id}`,
    method: 'PUT',
    data,
  })
}

export function disablePersonnel(id: number) {
  return request<void>({
    url: `/api/boss/personnel/${id}`,
    method: 'DELETE',
  })
}

export function normalizePersonnelJobRole(role?: string): PersonnelJobRole {
  if (role === 'STALL_OWNER') return 'STALL_OWNER'
  if (role === 'STALL_MANAGER' || role === 'STALL_ADMIN') return 'STALL_MANAGER'
  return 'DELIVERY'
}
