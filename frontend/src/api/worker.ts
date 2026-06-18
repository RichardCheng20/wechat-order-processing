import { request } from './request'

export interface WorkerItem {
  id: number
  userId?: number
  name: string
  phone?: string
  status?: number
}

export interface WorkerTaskItem {
  id: number
  productId: number
  productName: string
  orderQty: number
  actualQty?: number
  unit: string
  shortageFlag?: number
  pickRemark?: string
}

export interface WorkerTask {
  id: number
  orderNo: string
  customerName: string
  deliveryAddressShort?: string
  status: string
  statusLabel: string
  deliveryDate?: string
  remark?: string
  createdAt?: string
  itemCount?: number
  items?: WorkerTaskItem[]
}

export function fetchBossWorkers() {
  return request<WorkerItem[]>({
    url: '/api/boss/workers',
    method: 'GET',
  })
}

export function assignOrderToWorker(orderId: number, workerId: number) {
  return request<WorkerTask>({
    url: `/api/boss/orders/${orderId}/assign`,
    method: 'POST',
    data: { workerId },
  })
}

export function fetchWorkerTasks() {
  return request<WorkerTask[]>({
    url: '/api/worker/tasks',
    method: 'GET',
  })
}

export function fetchWorkerTaskDetail(id: number) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${id}`,
    method: 'GET',
  })
}

export function startWorkerTask(id: number) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${id}/start`,
    method: 'POST',
  })
}

export function updateWorkerTaskItem(
  orderId: number,
  itemId: number,
  data: { actualQty?: number; shortageFlag?: number; pickRemark?: string },
) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${orderId}/items/${itemId}`,
    method: 'PATCH',
    data,
  })
}

export function fillWorkerTaskQty(id: number) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${id}/fill-qty`,
    method: 'POST',
  })
}

export function completeWorkerTaskPick(id: number) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${id}/complete-pick`,
    method: 'POST',
  })
}

export function markWorkerTaskPicked(id: number) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${id}/picked`,
    method: 'POST',
  })
}

export function markWorkerTaskDelivered(id: number) {
  return request<WorkerTask>({
    url: `/api/worker/tasks/${id}/delivered`,
    method: 'POST',
  })
}
