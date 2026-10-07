import apiClient from './index.js'

// ===== 小说列表 =====
export const getNovelList = (page = 1, size = 10, category = '', status = '', keyword = '') => {
  const params = new URLSearchParams({ page, size })
  if (category && category !== '全部') {
    params.append('category', category)
  }
  if (status) {
    params.append('status', status)
  }
  if (keyword) {
    params.append('keyword', keyword)
  }
  return apiClient.get(`/novels?${params.toString()}`)
}

// ===== 小说详情 =====
export const getNovelDetail = (id) => {
  return apiClient.get(`/novels/${id}`)
}

// ===== 创建小说（管理员） =====
export const createNovel = (data) => {
  return apiClient.post('/novels', data)
}

// ===== 删除小说（管理员） =====
export const deleteNovel = (id) => {
  return apiClient.delete(`/novels/${id}`)
}

// ===== 上传封面 =====
export const uploadCover = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ===== 添加章节 =====
export const addChapter = (novelId, data) => {
  return apiClient.post(`/novels/${novelId}/chapters`, data)
}

// ===== 修改章节 =====
export const updateChapter = (novelId, chapterId, data) => {
  return apiClient.put(`/novels/${novelId}/chapters/${chapterId}`, data)
}

// ===== 删除章节 =====
export const deleteChapter = (novelId, chapterId) => {
  return apiClient.delete(`/novels/${novelId}/chapters/${chapterId}`)
}

// ===== 阅读章节 =====
export const readChapter = (novelId, chapterId) => {
  return apiClient.get(`/novels/${novelId}/chapters/${chapterId}`)
}

// ===== 获取全部分类 =====
export const getCategories = () => apiClient.get('/novels/categories')

// ===== 排行榜（仅总榜） =====
export const getRanking = (top = 10) => {
  return apiClient.get(`/novels/ranking?top=${top}`)
}