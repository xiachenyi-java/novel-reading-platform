/**
 * 书名 / 用户名 → 固定的封面配色类名（cv1 ~ cv6）
 *
 * ★ 为什么不随机：
 *   随机会导致刷新一下颜色就变，而且同一本书在发现页和详情页是两个颜色。
 *   这里用字符串算哈希，所以同一个名字永远对应同一个颜色。
 *
 * ★ 为什么单独放一个文件（2026-10-27）：
 *   原来这个函数写在 mock/data.js 里。现在首页接了真接口，
 *   真实数据也要用它上色 —— 再让页面 import mock 就说不清了。
 *   mock/data.js 那边改成 re-export，老页面的 import 一行都不用动。
 */
export function coverColor(title = '') {
  let h = 0
  for (let i = 0; i < title.length; i++) {
    h = (h * 31 + title.charCodeAt(i)) % 9973
  }
  return `cv${(h % 6) + 1}`
}
