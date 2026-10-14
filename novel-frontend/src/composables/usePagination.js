import { ref } from 'vue'

/**
 * 统一分页逻辑
 *
 * ★ 用法（fetcher 自己闭包捕获筛选条件，composable 只管 page/size）：
 *
 *   const sort = ref('RECENT_READ')
 *
 *   const {
 *     list, total, loading, page, size, pageSizes,
 *     load, handleCurrentChange, handleSizeChange, reset
 *   } = usePagination(async (p, s) => {
 *     const res = await getBookshelfList(p, s, sort.value)
 *     const data = res.data.data || {}
 *     return {
 *       list: data.content || [],
 *       total: data.totalElements || 0
 *     }
 *   }, {
 *     initialSize: 10,
 *     onError: () => ElMessage.error('加载失败')
 *   })
 *
 *   const switchSort = (val) => {
 *     sort.value = val
 *     reset()   // page = 1 + 加载
 *   }
 *
 *   onMounted(load)
 *
 * @param {Function} fetcher  必填，签名 (page, size) => Promise<{ list, total }>
 * @param {Object}   options
 * @param {number}   options.initialPage  初始页码，默认 1
 * @param {number}   options.initialSize  初始每页条数，默认 10
 * @param {number[]} options.pageSizes    可选每页条数，默认 [10, 20, 30]
 * @param {Function} options.onError      加载失败回调
 * @param {boolean}  options.immediate    是否立即加载一次，默认 false
 */
export function usePagination(fetcher, options = {}) {
  const {
    initialPage = 1,
    initialSize = 10,
    pageSizes = [10, 20, 30],
    onError,
    immediate = false
  } = options

  const page = ref(initialPage)
  const size = ref(initialSize)
  const total = ref(0)
  const list = ref([])
  const loading = ref(false)

  // ===== 核心：加载当前页 =====
  const load = async () => {
    if (typeof fetcher !== 'function') {
      console.warn('[usePagination] fetcher 不是函数，跳过加载')
      return
    }
    loading.value = true
    try {
      const result = await fetcher(page.value, size.value)
      if (result && typeof result === 'object') {
        list.value = result.list || []
        total.value = result.total || 0
      } else {
        list.value = []
        total.value = 0
      }
    } catch (e) {
      console.error('[usePagination] 加载失败:', e)
      list.value = []
      total.value = 0
      if (typeof onError === 'function') {
        onError(e)
      }
    } finally {
      loading.value = false
    }
  }

  // ===== 页码变化 =====
  const handleCurrentChange = () => {
    load()
  }

  // ===== 每页条数变化 =====
  // ★ 必须重置到第 1 页。否则用户从 10 条/页切到 30 条/页时，
  //   如果当前在第 5 页、总数据只有 3 页，就会落在空页。
  const handleSizeChange = () => {
    page.value = 1
    load()
  }

  // ===== 回到第 1 页重新加载 =====
  const reset = () => {
    page.value = 1
    return load()
  }

  if (immediate) {
    load()
  }

  return {
    page,
    size,
    total,
    list,
    loading,
    pageSizes,
    load,
    handleCurrentChange,
    handleSizeChange,
    reset
  }
}