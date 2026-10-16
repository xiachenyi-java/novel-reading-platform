/**
 * 把后端的返回"掰"成前端习惯的样子
 * ==========================================================================
 * 【为什么需要这个文件】
 *   排行榜接口（GET /novels/ranking）返回的 NovelRankVO，字段名是
 *   **大写开头**的：
 *
 *       NovelId / Title / CoverUrl / Heat
 *       ↑ Java 那边写成了 `private Integer NovelId`（首字母大写，不合规范），
 *         Jackson 就原样序列化成大写开头的 key 了
 *
 *   前端如果只按驼峰读（item.id / item.title），会**永远读到 undefined**：
 *   书名一片空白、"热度"显示 0、点了没反应 —— 而且**不报错**。
 *   这就是书架页踩过的那个坑（那次读的是根本不存在的字段）。
 *
 * 【怎么办】
 *   兼容两种命名：驼峰优先，大写开头兜底。集中在这一个函数里 ——
 *   书城的人气榜和排行榜页都用它，别在每页各写一遍 `??` 兜底。
 *
 * ★ 给后端的建议（前端不擅自改接口）：
 *   NovelRankVO 的字段名改成小写驼峰（novelId / title / coverUrl / heat）更规范。
 *   但那属于**接口契约变更**，改了前端也得跟着改，所以先留着这层兼容，
 *   等哪次一起动。记在 docs/设计/前端改版-设计方案.txt 里了。
 */

/**
 * 单条榜单数据 → 统一的形状
 * @param {object} raw 后端返回的一条
 * @returns {{id:number|null, title:string, coverUrl:string, heat:number}}
 */
export function rankItem(raw) {
  if (!raw) return { id: null, title: '', coverUrl: '', heat: 0 }

  return {
    id: raw.id ?? raw.NovelId ?? raw.novelId ?? null,
    title: raw.title ?? raw.Title ?? '',
    coverUrl: raw.coverUrl ?? raw.CoverUrl ?? raw.coverUr1 ?? '',
    // ★ Number() 兜一下：兼容后端把热度给成 "1234" 字符串的情况
    heat: Number(raw.heat ?? raw.Heat ?? 0) || 0
  }
}

/**
 * 一整个响应 → 数组
 * @param {any} data res.data.data（可能是数组，也可能是分页对象）
 */
export function rankList(data) {
  const arr = Array.isArray(data) ? data : data?.content || []
  return arr.map(rankItem).filter((it) => it.id)
}
