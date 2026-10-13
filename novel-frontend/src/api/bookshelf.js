import apiClient from './index.js'

// ===== 加入书架 =====
export const addToBookshelf = (novelId) => {
  return apiClient.post('/bookshelf', { novelId })
}

// ===== 书架列表（分页 + 排序）=====
// sort 可选值：RECENT_READ | RECENT_ADD | NOVEL_UPDATE
export const getBookshelfList = (page = 1, size = 10, sort = 'RECENT_READ') => {
  const params = new URLSearchParams({ page, size, sort })
  return apiClient.get(`/bookshelf?${params.toString()}`)
}

// ===== 置顶 / 取消置顶 =====
export const toggleTop = (id, top) => {
  return apiClient.put(`/bookshelf/${id}/top`, { top })
}

// ===== 移出书架（单个，软删）=====
export const removeFromBookshelf = (id) => {
  return apiClient.delete(`/bookshelf/${id}`)
}

// ===== 批量移出 =====
export const batchRemoveFromBookshelf = (ids) => {
  return apiClient.post('/bookshelf/batch-remove', { ids })
}