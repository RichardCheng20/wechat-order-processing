import { getApiBaseUrl } from '../utils/config'

export interface OrderImportTextResult {
  text: string
}

export function parseOrderExcel(filePath: string): Promise<OrderImportTextResult> {
  const token = uni.getStorageSync('token') as string
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${getApiBaseUrl()}/api/customer/orders/import/excel`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data as string) as { code: number; message: string; data: OrderImportTextResult }
          if (body.code === 0 && body.data) {
            resolve(body.data)
            return
          }
          reject(new Error(body.message || 'Excel 解析失败'))
        } catch {
          reject(new Error('Excel 解析失败'))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || 'Excel 上传失败')),
    })
  })
}
