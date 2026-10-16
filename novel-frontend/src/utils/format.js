/**
 * 把机器给的数据，变成人念得出来的话
 * ------------------------------------------------------------
 * 【为什么要这个文件】
 *   后端给的是 "2026-10-13T22:41:00" 和 123456。
 *   这是一台机器在说话 —— 精确、完整、但没人味。
 *   同一个事实，换成人话是「昨晚」和「12.3 万字」。
 *
 *   一句话原则：**报得越精确，越像机器。**
 *   朋友之间不报秒，也不报六位数字。
 *
 * 【现在谁在用】
 *   views/MyShelf.vue —— 书架卡片上的"上次读到什么时候 / 什么时候加的 /
 *                        什么时候更新过 / 多少字"
 *
 * 【以后谁会用】
 *   设计稿里那几个页面（发现流说"昨晚"、详情页说"12.3 万字"），
 *   改前端时直接 import，别再各写一份。
 */

/**
 * 把各种形状的时间输入，统一转成 Date
 * ★ 为什么要兜这么多形状：后端现在是 LocalDateTime → ISO 字符串，
 *   但万一哪天序列化配置变了（比如变成 [2026,10,13,22,41] 这种数组），
 *   前端就会**静默地**全部显示成空白 —— 不报错，只是没字。
 *   这种"假成功"最难查，所以这里一次性认下来。
 * @returns {Date|null} 认不出来就返回 null，调用方给空字符串
 */
function toDate(input) {
  if (!input) return null
  if (input instanceof Date) return Number.isNaN(input.getTime()) ? null : input

  // Jackson 开 write-dates-as-timestamps 时会变成数组
  if (Array.isArray(input)) {
    const [y, m, d, h = 0, min = 0, s = 0] = input
    return new Date(y, m - 1, d, h, min, s)
  }

  if (typeof input !== 'string' && typeof input !== 'number') return null

  // ★ 把 "2026-10-13 22:41:00" 的空格换成 T：
  //   Safari 认 "2026-10-13T22:41:00"，不认带空格的写法（会返回 Invalid Date）
  const date = new Date(String(input).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

/** 取"那一天的 0 点"，用来判断是不是同一天 */
function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

/**
 * 相对时间："2026-10-13T22:41:00" → "昨晚"
 *
 * 【设计约定 —— 这两条是刻意的，不是偷懒】
 *   1. 按**自然日**算，不按 24 小时算。
 *      23:59 读的、次日 00:01 看，是"昨天"和"今天"，不是"1 分钟前"。
 *      人记日子就是这么记的。
 *   2. 超过一年直接给年份，不说"13 个月前" —— 那个数字人记不住。
 *
 * @param {string|number|Date|Array} input 后端时间字段
 * @returns {string} 认不出来返回空字符串（调用方自己决定显示什么）
 */
export function relativeTime(input) {
  const target = toDate(input)
  if (!target) return ''

  const now = new Date()

  // 未来时间（服务器时钟快了几分钟也会出现）不说"1 分钟后"，统一当"刚刚"
  const ms = now.getTime() - target.getTime()
  if (ms < 60 * 1000) return '刚刚'

  const days = Math.round((startOfDay(now) - startOfDay(target)) / 86400000)

  // 今天：说分钟 / 小时
  if (days <= 0) {
    const minutes = Math.floor(ms / 60000)
    if (minutes < 60) return `${minutes} 分钟前`
    return `${Math.floor(minutes / 60)} 小时前`
  }

  if (days === 1) return '昨天'
  if (days === 2) return '前天'
  if (days <= 6) return `${days} 天前`
  if (days <= 13) return '上周'
  if (days <= 29) return `${Math.floor(days / 7)} 周前`
  if (days <= 364) return `${Math.floor(days / 30)} 个月前`
  return `${Math.floor(days / 365)} 年前`
}

/**
 * 字数：123456 → "12.3 万字"
 * 同一个道理 —— 六位数字没人念得出来。
 * 不到一万字就直说，别写成 "0.8 万字"（人对小数不敏感，对整数敏感）
 *
 * @param {number} n 后端 totalWords
 * @returns {string} 没有有效值返回空字符串
 */
export function formatWords(n) {
  const num = Number(n)
  if (!Number.isFinite(num) || num <= 0) return ''
  if (num < 10000) return `${num} 字`
  return `${(num / 10000).toFixed(1)} 万字`
}
