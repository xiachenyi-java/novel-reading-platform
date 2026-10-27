import apiClient from './index.js'

// ============================================================
// 内容流（书评）接口
// 后端：ContentController  →  GET /contents
// ------------------------------------------------------------
// 返回的是 Result<Page<ContentVO>>：
//   { code, msg, data: { content: [...], totalElements, totalPages, ... } }
// ★ 业务异常也是 HTTP 200 + body.code = 500，
//   所以调用方必须看 res.data.code，不能只看 catch。
// ============================================================

/**
 * 分页查询书评
 * @param {Object} params
 *   page      第几页，从 1 开始（后端会转成 PageRequest.of(page-1)）
 *   size      每页条数，后端上限 100
 *   polarity  REC 推荐 | WARN 避雷 | NOTE 长评；不传 = 全部
 *   novelId   只查某本书的书评
 *   userId    只查某个书评人写的（后端参数名叫 userId，查的是 authorId）
 *   keyword   关键词
 */
export const getContents = (params = {}) => {
  const { page = 1, size = 10, polarity = '', novelId = '', userId = '', keyword = '' } = params
  const query = new URLSearchParams({ page, size })

  // 空值不传：后端用 required = false，传空串反而可能被当成关键词
  if (polarity) query.append('polarity', polarity)
  if (novelId) query.append('novelId', novelId)
  if (userId) query.append('userId', userId)
  if (keyword) query.append('keyword', keyword)

  return apiClient.get(`/contents?${query.toString()}`)
}

/**
 * 软删一条书评（管理员）
 * 后端会校验 @RequireAdmin，普通账号调会返回 403
 */
export const deleteContent = (id) => apiClient.delete(`/contents/${id}`)
