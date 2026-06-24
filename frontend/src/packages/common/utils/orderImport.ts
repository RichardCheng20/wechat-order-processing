export type ImportImageSource = 'camera' | 'album'

export function chooseImportImage(sourceType: ImportImageSource[]): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType,
      success: (res) => {
        const path = res.tempFiles?.[0]?.tempFilePath
        if (!path) {
          reject(new Error('未选择图片'))
          return
        }
        resolve(path)
      },
      fail: (err) => {
        if (`${err.errMsg || ''}`.includes('cancel')) {
          reject(new Error('已取消'))
          return
        }
        reject(new Error(err.errMsg || '选择图片失败'))
      },
    })
  })
}

export function chooseImportExcel(): Promise<string> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['.xlsx', '.xls'],
      success: (res) => {
        const path = res.tempFiles?.[0]?.path
        if (!path) {
          reject(new Error('未选择文件'))
          return
        }
        resolve(path)
      },
      fail: (err) => {
        if (`${err.errMsg || ''}`.includes('cancel')) {
          reject(new Error('已取消'))
          return
        }
        reject(new Error(err.errMsg || '选择 Excel 失败'))
      },
    })
    // #endif
    // #ifndef MP-WEIXIN
    uni.chooseFile({
      count: 1,
      extension: ['.xlsx', '.xls'],
      success: (res) => {
        const path = res.tempFilePaths?.[0]
        if (!path) {
          reject(new Error('未选择文件'))
          return
        }
        resolve(path)
      },
      fail: (err) => {
        if (`${err.errMsg || ''}`.includes('cancel')) {
          reject(new Error('已取消'))
          return
        }
        reject(new Error(err.errMsg || '选择 Excel 失败'))
      },
    })
    // #endif
  })
}

export function openPhotoImportMenu(onPick: (index: number) => void) {
  uni.showActionSheet({
    itemList: ['拍摄手写单', '从相册选择', '导入 Excel', '粘贴文本'],
    success: (res) => onPick(res.tapIndex),
  })
}
