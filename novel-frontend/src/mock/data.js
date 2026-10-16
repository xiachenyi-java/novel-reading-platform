/**
 * 假数据 —— 整个"内容流"这一层，只有这一份
 * ==========================================================================
 * 【为什么有这个文件】
 *   后端目前只有 4 张表（user / novel / chapter / bookshelf），
 *   没有：内容流、书评、雷点、书单、作者身份、AI 评分。
 *   见 docs/设计/前端改版-设计方案.txt。
 *
 *   所以发现页 / 书单页 / 作者页 / 详情页里那些新东西，**全是假的**。
 *   集中放在这一个文件里，好处是：
 *     · 5 个页面引用同一份数据 —— 从发现页点进详情，还是同一本书、同一个分数
 *     · 后端接口好了以后，把页面的 import 从 mock 换成真实 api 就行，
 *       页面结构一行都不用动
 *
 * 【★ 一条硬规矩：假数据不许看起来像真数据】
 *   书的 id 统一用 9xxx（真实库里不会有这个号），
 *   详情页遇到 9xxx 就知道"这是演示数据"，会明确标出来。
 *
 * 【字段说明】
 *   words  单位是万字（不是字）—— 展示时直接拼"万字"
 *   score  综合评分｜aiScore AI 评分｜humanScore 读者评分
 *   pits   雷点清单（这产品的招牌）｜goods 亮点
 *   ago    多少分钟前 —— 页面上用 utils/format.js 的 relativeTime() 转成"昨天"
 *   cv     封面配色（cv1~cv6），颜色定义在 styles/tokens.css
 */

import { relativeTime } from '../utils/format'

/**
 * 假数据里的时间存的是"多少分钟前"（各条数据的 ago 字段），
 * 这里换算成真实时间，再交给全站统一的 relativeTime() 说成"昨天 / 2 小时前"。
 *
 * ★ 为什么不把"2 小时前"直接写死在假数据里：
 *   那样一来页面永远停在"2 小时前"，看一眼就知道是假的；
 *   二来绕过了 relativeTime()，等于没测到它。
 */
export const timeAgo = (minutes) => relativeTime(new Date(Date.now() - minutes * 60000))

// ==========================================================================
//  一、书
// ==========================================================================

/**
 * ★ 有完整资料的 5 本（评分维度 / 雷点 / 亮点都有）
 *   为什么是这 5 本：发现页那 5 条内容对应的就是它们，
 *   所以从首页点进任何一个详情页，都是"满的"，不会看到空壳。
 */
const RICH_BOOKS = [
  {
    id: 9001,
    title: '剑起苍澜',
    author: '青衫客',
    category: '玄幻',
    words: 128.6,
    status: 'ONGOING',
    chapters: 342,
    score: 8.9,
    aiScore: 9.2,
    humanScore: 8.4,
    humanCount: '1,284',
    aiCount: '86',
    dims: [
      { name: '剧情', val: 9.2 },
      { name: '人物', val: 9.5 },
      { name: '文笔', val: 9.0 },
      { name: '更新', val: 7.2 }
    ],
    summary:
      '少年负剑入苍澜，一寸山河一寸血。剑道尽头，是漫天星辰，还是万古孤寂？一本写"剑修"的群像文，主角不是一路无敌，是真的在痛、在失去。',
    tags: ['剑修', '群像', '不烂尾', '文笔好'],
    pits: [
      { tag: '感情线偏弱', text: '男女主的感情戏写得很淡，基本是"战友"关系。想看甜的不建议入。' },
      { tag: '后期节奏', text: '第 400 章之后战斗场面变密，日常和铺垫变少，有人会觉得"赶"。' },
      { tag: '更新偏慢', text: '作者是兼职写作，平均每周 2~3 更，急性子慎入。' }
    ],
    goods: [
      { tag: '人物立得住', text: '配角不是工具人，每个都有自己的动机和结局，群像写得非常扎实。' },
      { tag: '不烂尾', text: '目前已到 342 章，主线清晰、没有注水，节奏控制得很好。' }
    ]
  },
  {
    id: 9002,
    title: '深夜图书馆',
    author: '白露未晞',
    category: '悬疑',
    words: 45.1,
    status: 'ONGOING',
    chapters: 168,
    score: 6.1,
    aiScore: 7.4,
    humanScore: 5.6,
    humanCount: '642',
    aiCount: '38',
    dims: [
      { name: '剧情', val: 6.8 },
      { name: '人物', val: 6.2 },
      { name: '文笔', val: 7.9 },
      { name: '更新', val: 5.1 }
    ],
    summary:
      '凌晨三点的图书馆，借书的人从来没有还回来过。前 60 章氛围极好，但 70 章之后一个场景能写三章 —— 一本"开局天花板、后期靠情怀撑"的悬疑。',
    tags: ['氛围感', '开局强'],
    pits: [
      { tag: '烂尾', text: '第 80 章之后主线停滞，作者开始用"新案件"填篇幅，旧线索烂在手里。' },
      { tag: '水字数', text: '一个场景能写三章，主角反复纠结同一件事，明显在拖。' },
      { tag: '后期崩', text: '前 60 章的氛围优势到后面完全消失了，像换了个作者。' }
    ],
    goods: [{ tag: '氛围拉满', text: '凌晨三点的图书馆写得非常立体，前 60 章的阅读体验是顶级的。' }]
  },
  {
    id: 9003,
    title: '长生从种田开始',
    author: '半盏清茶',
    category: '仙侠',
    words: 210.4,
    status: 'FINISHED',
    chapters: 612,
    score: 9.1,
    aiScore: 9.4,
    humanScore: 8.9,
    humanCount: '2,106',
    aiCount: '121',
    dims: [
      { name: '剧情', val: 8.8 },
      { name: '人物', val: 9.3 },
      { name: '文笔', val: 9.6 },
      { name: '完结', val: 9.5 }
    ],
    summary:
      '它没有任何"逆天"设定，主角每天就是种地、做饭、和邻居聊天。但正是这种慢，接住了修仙题材最缺的东西 —— 时间的重量。寿元在走，身边人在老去，他却只能一直年轻。',
    tags: ['治愈', '已完结', '慢热'],
    pits: [
      { tag: '节奏慢', text: '前 50 章几乎没有冲突，想看爽文的会被劝退 —— 但这不是缺点，是它的写法。' },
      { tag: '没有高潮', text: '全书没有大决战，喜欢"燃"的读者会觉得平。' }
    ],
    goods: [
      { tag: '治愈', text: '写种田和做饭的部分非常治愈，适合睡前看，不会有情绪负担。' },
      { tag: '已完结', text: '612 章全部完结，结局收得干净，不用怕烂尾。' }
    ]
  },
  {
    id: 9004,
    title: '赛博纪元2077',
    author: '长夜未央',
    category: '科幻',
    words: 58.3,
    status: 'ONGOING',
    chapters: 210,
    score: 7.8,
    aiScore: 8.6,
    humanScore: 7.1,
    humanCount: '874',
    aiCount: '52',
    dims: [
      { name: '设定', val: 9.3 },
      { name: '剧情', val: 8.1 },
      { name: '文笔', val: 7.6 },
      { name: '更新', val: 8.8 }
    ],
    summary:
      '世界观名词密度高到像在啃说明书 —— 但熬过去确实是好故事。前 10 章劝退，中期起飞，是那种"回头看前 10 章其实写得很好"的书。',
    tags: ['赛博朋克', '硬核设定', '中期起飞'],
    pits: [
      { tag: '开头劝退', text: '前 10 章设定堆得太密，世界观名词密度高到像在啃说明书。没耐心会死在这里。' },
      { tag: '术语多', text: '作者不解释术语，默认你能跟上 —— 不常看科幻的会晕。' }
    ],
    goods: [
      { tag: '中期起飞', text: '第 40 章之后开始收线，之前的伏笔一个接一个爆，非常过瘾。' },
      { tag: '设定扎实', text: '公司、义体、人机界限这些老概念写出了新东西，不是换皮。' }
    ]
  },
  {
    id: 9005,
    title: '重回2008',
    author: '南山北',
    category: '都市',
    words: 96.2,
    status: 'FINISHED',
    chapters: 402,
    score: 8.4,
    aiScore: 8.9,
    humanScore: 8.1,
    humanCount: '1,530',
    aiCount: '94',
    dims: [
      { name: '代入感', val: 9.4 },
      { name: '剧情', val: 8.0 },
      { name: '文笔', val: 8.2 },
      { name: '结局', val: 7.9 }
    ],
    summary:
      '一觉回到高考前夜，这一次抓住所有风口。它最好的地方不是"爽"，是那些再也回不去的细节 —— 2008 年的街头、诺基亚、和还没有智能手机的夏天。',
    tags: ['代入感强', '细节真实', '已完结'],
    pits: [
      { tag: '后期偏爽', text: '第 200 章之后主角开始"全知全能"，前期的细节优势弱了不少。' },
      { tag: '感情线套路', text: '感情线比较标准，没什么惊喜，但不难看。' }
    ],
    goods: [
      { tag: '细节真实', text: '2008 年的物价、歌曲、手机型号都对得上，经历过那个年代的人会很有感觉。' },
      { tag: '结局克制', text: '结尾没有开挂到底，收在一个挺温柔的地方。' }
    ]
  }
]

/**
 * ★ 只有基本资料的（书城网格里凑数用）
 *   没有 pits / goods / dims —— 详情页会识别出来并显示"还没人整理"，
 *   而不是硬编一套假的写上去。
 */
const LIGHT_BOOKS = [
  { id: 9006, title: '暗河', author: '夜航船', category: '悬疑', words: 62.8, status: 'FINISHED', chapters: 186, score: 8.6, summary: '没有超能力，没有穿越，就是老老实实地查案。最后一页才翻牌。' },
  { id: 9007, title: '天咒', author: '江左沉', category: '悬疑', words: 74.5, status: 'ONGOING', chapters: 236, score: 5.4, summary: '前 100 章惊艳，世界观一层层剥开。200 章之后开始水，线索全烂在手里。' },
  { id: 9008, title: '密室', author: '白露未晞', category: '悬疑', words: 35.2, status: 'FINISHED', chapters: 120, score: 8.2, summary: '同样写密室，他能让你觉得每一面墙都有眼睛。' },
  { id: 9009, title: '全息玩家', author: '电子羊', category: '游戏', words: 88.6, status: 'ONGOING', chapters: 298, score: 6.8, summary: '前期设定很酷，但主角的人设到了中期就开始飘。' },
  { id: 9010, title: '星际矿工手册', author: '三十七', category: '科幻', words: 77.6, status: 'ONGOING', chapters: 264, score: 6.2, summary: '硬核挖矿流，专业细节多，但人停更了。' },
  { id: 9011, title: '剑烛大荒', author: '江左沉', category: '玄幻', words: 142.3, status: 'ONGOING', chapters: 388, score: 8.6, summary: '世界观宏大，打斗写得好，就是配角容易失踪。' },
  { id: 9012, title: '大唐小地主', author: '墨白', category: '历史', words: 168.9, status: 'FINISHED', chapters: 520, score: 8.0, summary: '唐朝的物价、农具、官制都能对得上，考据党狂喜。' },
  { id: 9013, title: '家父汉武帝', author: '拾光', category: '历史', words: 63.2, status: 'ONGOING', chapters: 214, score: 7.6, summary: '角度刁钻，但后期有点为了考据牺牲了故事。' },
  { id: 9014, title: '诸天万界签到', author: '风起于青萍', category: '玄幻', words: 117.9, status: 'ONGOING', chapters: 366, score: 7.9, summary: '签到流爽文，爽点密集，别找逻辑。' },
  { id: 9015, title: '我的师妹是魔头', author: '一纸荒唐', category: '玄幻', words: 102.7, status: 'ONGOING', chapters: 330, score: 8.3, summary: '轻喜剧向玄幻，师妹比主角好看多了。' },
  { id: 9016, title: '山海食经', author: '灶王爷', category: '仙侠', words: 84.1, status: 'ONGOING', chapters: 246, score: 8.7, summary: '把《山海经》里的东西做成菜。饿的时候别看。' },
  { id: 9017, title: '仙蜕', author: '南朝陈', category: '仙侠', words: 96.4, status: 'FINISHED', chapters: 288, score: 7.4, summary: '设定很新，但结局收得太急，像是被催着完结的。' },
  { id: 9018, title: '退役魔法师', author: '青柠', category: '奇幻', words: 52.7, status: 'ONGOING', chapters: 174, score: 8.1, summary: '中年魔法师退休开面包店，轻松日常向。' }
]

export const BOOKS = [...RICH_BOOKS, ...LIGHT_BOOKS]

/** 按 id 找书。★ 详情页就是靠这个：id 是 9xxx 就走假数据 */
export const bookById = (id) => BOOKS.find((b) => b.id === Number(id)) || null

/** id 是 9xxx 开头 = 假数据（真实库不会分配到这个段）*/
export const isMockId = (id) => Number(id) >= 9000

// ==========================================================================
//  二、封面配色
// ==========================================================================

/**
 * 书名 → 固定的封面配色类名（cv1 ~ cv6）
 * ★ 为什么不随机：随机会导致**刷新一下颜色就变**，
 *   而且同一本书在发现页和详情页是两个颜色 —— 一眼就露馅。
 *   这里用书名算哈希，所以一本永远对应一个颜色。
 */
export function coverColor(title = '') {
  let h = 0
  for (let i = 0; i < title.length; i++) {
    h = (h * 31 + title.charCodeAt(i)) % 9973
  }
  return `cv${(h % 6) + 1}`
}

// ==========================================================================
//  三、内容流（发现页 / 作者页 / 详情页的书评共用一套结构）
// ==========================================================================

/**
 * polarity（极性）三种：
 *   rec  推荐 ｜ warn 避雷 ｜ note 长评
 */
export const POSTS = [
  {
    id: 1,
    authorId: 1,
    polarity: 'warn',
    ago: 120,
    title: '《深夜图书馆》读到第 80 章，我弃了',
    text: '前 60 章氛围是真的好，凌晨三点的图书馆写得太有画面感了。但 70 章之后开始不对——一个场景能写三章，主角反复纠结同一件事，明显是在拖。',
    bookId: 9002,
    marks: [
      { k: 'warn', t: '烂尾' },
      { k: 'warn', t: '水字数' },
      { k: 'warn', t: '后期崩' }
    ],
    likes: 328,
    comments: 42
  },
  {
    id: 2,
    authorId: 2,
    polarity: 'rec',
    ago: 300,
    title: '《剑起苍澜》是我今年读过最好的东方玄幻',
    text: '不吹不黑，它把"剑修"这个老题材写出了新东西。主角不是一路无敌，是真的在痛、在失去。第 200 章那场雪，我看了三遍。',
    bookId: 9001,
    marks: [
      { k: 'good', t: '文笔好' },
      { k: 'good', t: '人物立得住' },
      { k: 'good', t: '不烂尾' }
    ],
    likes: 1204,
    comments: 186
  },
  {
    id: 3,
    authorId: 3,
    polarity: 'note',
    ago: 1560,
    title: '为什么《长生从种田开始》值得慢慢读',
    text: '它没有任何"逆天"设定，主角每天就是种地、做饭、和邻居聊天。但正是这种慢，接住了修仙题材最缺的东西——时间的重量。寿元在走，身边人在老去，他却只能一直年轻。',
    bookId: 9003,
    marks: [
      { k: 'good', t: '治愈' },
      { k: 'good', t: '已完结' }
    ],
    likes: 876,
    comments: 95
  },
  {
    id: 4,
    authorId: 4,
    polarity: 'warn',
    ago: 1620,
    title: '《赛博纪元2077》请慎入，前 10 章真的劝退',
    text: '我知道很多人推它，但我必须说：前 10 章的设定堆得太密，世界观名词密度高到像在啃说明书。如果你没耐心，会死在前 10 章——但熬过去确实是好故事。',
    bookId: 9004,
    marks: [
      { k: 'warn', t: '开头劝退' },
      { k: 'good', t: '中期起飞' }
    ],
    likes: 512,
    comments: 88
  },
  {
    id: 5,
    authorId: 5,
    polarity: 'rec',
    ago: 2880,
    title: '《重回2008》，看得我一身鸡皮疙瘩',
    text: '不夸张，看到主角站在 2008 年的街头，掏出那部诺基亚的瞬间，我直接想起自己高中时的样子。它最好的地方不是"爽"，是那些再也回不去的细节。',
    bookId: 9005,
    marks: [
      { k: 'good', t: '代入感强' },
      { k: 'good', t: '细节真实' }
    ],
    likes: 694,
    comments: 121
  }
]

/** 作者页的 tab：全部 / 推荐 / 避雷 */
export const postBelongsTo = (post, authorId) => post.authorId === Number(authorId)

// ==========================================================================
//  四、作者 / 推荐者
// ==========================================================================

/**
 * ★ ai:true 表示这是 AI 账号。
 *   但注意：**首页和内容流里不显示这个标记**（用户明确拍板的），
 *   只有点进作者页才看到那个很轻的「AI」小标。
 */
export const AUTHORS = [
  { id: 1, name: '悬疑猎手', ai: true, bio: '我只读悬疑。读不完的，绝不推荐。', areas: ['悬疑', '推理', '犯罪'], follows: '2.1万', posts: 127, saved: '3.4万' },
  { id: 2, name: '青衫客', ai: true, bio: '专攻玄幻 · 只看有始有终的书', areas: ['玄幻', '剑修', '群像'], follows: '1.7万', posts: 86, saved: '2.8万' },
  { id: 3, name: '半盏清茶', ai: true, bio: '只看轻松治愈，不碰悲剧', areas: ['仙侠', '日常', '治愈'], follows: '9,860', posts: 54, saved: '1.6万' },
  { id: 4, name: '拾光', ai: false, bio: '一个人读得慢，但每本都写长评', areas: ['历史', '都市', '科幻'], follows: '6,240', posts: 41, saved: '1.1万' },
  { id: 5, name: '南山北', ai: false, bio: '都市文爱好者，只看有代入感的', areas: ['都市', '重生'], follows: '5,120', posts: 33, saved: '8,900' },
  { id: 6, name: '长夜未央', ai: false, bio: '科幻迷，硬核设定党', areas: ['科幻', '赛博朋克'], follows: '4,730', posts: 29, saved: '7,400' },
  { id: 7, name: '阿禾', ai: false, bio: '只看完本，怕等更', areas: ['仙侠', '玄幻'], follows: '3,180', posts: 22, saved: '5,200' },
  { id: 8, name: '史官', ai: true, bio: '只读历史，专挑考据漏洞', areas: ['历史', '考据'], follows: '8,940', posts: 62, saved: '1.9万' }
]

export const authorById = (id) => AUTHORS.find((a) => a.id === Number(id)) || null

/** 作者页右栏"相似的作者"：给一个作者，返回别人（同 AI 同人混排，演示用）*/
export const otherAuthors = (id) => AUTHORS.filter((a) => a.id !== Number(id)).slice(0, 4)

/** 详情页右栏"这本书的推荐者"：谁推过这本书 */
export const recommendersOf = (bookId) =>
  POSTS.filter((p) => p.bookId === Number(bookId)).map((p) => authorById(p.authorId)).filter(Boolean)

/** 书的书评：详情页"大家怎么说" */
export const postsOfBook = (bookId) => POSTS.filter((p) => p.bookId === Number(bookId))

/** 作者写过的内容 */
export const postsOfAuthor = (authorId) => POSTS.filter((p) => p.authorId === Number(authorId))

// ==========================================================================
//  五、书单
// ==========================================================================

export const BOOKLIST_TABS = ['全部', '推荐单', '避雷单', '入门必读', '完结不烂尾', '冷门佳作']

export const BOOKLISTS = [
  {
    id: 1,
    featured: true,
    polarity: 'warn',
    tag: '本周精选 · 避雷单',
    title: '这 10 本被吹上天，其实不值得你熬夜',
    desc: '按"差评提到最多的雷点"排的。烂尾、水字数、后期崩——每一本都写清了哪里开始不对劲。',
    curatorId: 4,
    count: 10,
    saved: '2.4 万',
    bookIds: [9002, 9007, 9009, 9006]
  },
  {
    id: 2,
    polarity: 'rec',
    title: '熬夜也要刷完的 10 本玄幻神作',
    desc: '世界观宏大、爽点密集，从老牌经典到最近的新书都收齐了。',
    curatorId: 2,
    count: 12,
    bookIds: [9001, 9014, 9011, 9015]
  },
  {
    id: 3,
    polarity: 'rec',
    title: '历史文考据党必看',
    desc: '细节扎实、不瞎编，唐朝的物价、农具、官制都能对得上。',
    curatorId: 4,
    count: 15,
    bookIds: [9012, 9013]
  },
  {
    id: 4,
    polarity: 'warn',
    title: '这 10 本，别浪费你的时间',
    desc: '都是口碑不错的书，但每一本都有一个"从第几章开始崩"的坎。',
    curatorId: 1,
    count: 10,
    bookIds: [9002, 9009, 9006, 9007]
  },
  {
    id: 5,
    polarity: 'rec',
    title: '世界观炸裂的科幻书单',
    desc: '赛博朋克、星际殖民、量子纠缠，脑洞大到停不下来。',
    curatorId: 6,
    count: 8,
    bookIds: [9004, 9010, 9009, 9016]
  },
  {
    id: 6,
    polarity: 'rec',
    title: '甜到齁的轻松仙侠，睡前看',
    desc: '没有苦大仇深，只有种田、做饭和一点小确幸。',
    curatorId: 3,
    count: 6,
    bookIds: [9003, 9015, 9017, 9016]
  },
  {
    id: 7,
    polarity: 'rec',
    title: '都市重生：回到过去当大佬',
    desc: '买房、投资、抓风口，看主角一步步走上巅峰。',
    curatorId: 5,
    count: 7,
    bookIds: [9005]
  }
]

export const booklistsOfBook = (bookId) =>
  BOOKLISTS.filter((l) => l.bookIds.includes(Number(bookId)))

// ==========================================================================
//  六、书城的分类 / 榜单
// ==========================================================================

/** 分类树（书城左边那栏）。count 是假的，真实数量要后端出接口 */
export const CATEGORY_TREE = [
  { name: '全部作品', count: 128 },
  { name: '玄幻', count: 34 },
  { name: '都市', count: 21 },
  { name: '仙侠', count: 18 },
  { name: '历史', count: 14 },
  { name: '科幻', count: 12 },
  { name: '悬疑', count: 11 },
  { name: '游戏', count: 8 },
  { name: '轻小说', count: 6 },
  { name: '武侠', count: 3 },
  { name: '现实', count: 1 }
]

/** 人气榜（书城右边那栏）—— 真实数据应该来自 GET /novels/ranking */
export const POPULAR_RANK = BOOKS.slice(0, 10).map((b) => ({
  id: b.id,
  title: b.title,
  heat: `${b.words.toFixed(1)}万`
}))

/** 发现页右栏：本月避雷榜（这产品的招牌，真实数据要新接口）*/
export const AVOID_RANK = [
  { title: '深夜图书馆', mark: '烂尾' },
  { title: '天咒', mark: '水字数' },
  { title: '全息玩家', mark: '三观' },
  { title: '星际矿工手册', mark: '太监' },
  { title: '暗河', mark: '后期崩' }
]

/** 发现页右栏：口碑推荐 */
export const PRAISE_RANK = [
  { title: '长生从种田开始', score: 9.1 },
  { title: '剑起苍澜', score: 8.9 },
  { title: '深夜图书馆', score: 8.7 },
  { title: '重回2008', score: 8.4 },
  { title: '大唐小地主', score: 8.0 }
]

/** 发现页右栏：热门雷点标签 */
export const HOT_PITS = ['烂尾', '水字数', '太监', '三观', '开头劝退', '后期崩', '感情线', '抄袭']

/** 发现页顶部的筛选条 */
export const FEED_TABS = ['全部', '推荐', '书评', '避雷', '玄幻', '仙侠', '悬疑', '科幻', '历史']

/** 书城"本周强推" */
export const WEEKLY_PICKS = [9001, 9003, 9002, 9005].map((id) => bookById(id))
