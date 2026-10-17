import { createApp, h } from 'vue'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/assets/css/main.css'
import ArticleCard from '@/components/ArticleCard.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import EmptyState from '@/components/EmptyState.vue'
import type { Article } from '@/types'

const article: Article = {
  id: 99, title: '人工智能2026：从大模型到通用智能的探索', content: '',
  summary: '2026年，AI大模型技术持续突破，多模态、长上下文和Agent能力显著提升。',
  coverImage: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="88"%3E%3C/svg%3E',
  authorId: 4, authorName: 'AI前沿实验室', categoryId: 1, categoryName: 'AI', tags: [],
  likeCount: 456, commentCount: 58, collectCount: 1, viewCount: 100, shareCount: 0,
  isLiked: false, isCollected: false, status: 2,
  createdAt: '2026-03-15', updatedAt: '2026-03-15',
}
const app = createApp({ render: () => h('main', { style: 'padding:12px;max-width:1100px;margin:auto' }, [
  h('h1', '加载占位与实际卡片对照'),
  h('h2', '加载中'),
  h('section', { id: 'loading' }, [h(LoadingSkeleton, { type: 'article', count: 1 })]),
  h('h2', '实际内容'),
  h('section', { id: 'loaded' }, [h(ArticleCard, { article })]),
  h('h2', '短摘要、无封面'),
  h('section', { id: 'short' }, [h(ArticleCard, { article: { ...article, title: '短标题', summary: '摘要', coverImage: '' } })]),
  h('h2', '外部头像：使用普通图片请求，不强制 CORS'),
  h('section', { id: 'avatars', style: 'display:flex;gap:16px;padding:12px' }, [
    h(UserAvatar, { alt: '美食创作者', size: 'lg', src: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=food%20blogger%20avatar%2C%20chef%20hat%2C%20warm%20colors%2C%20cute%20illustration&image_size=square_hd' }),
    h(UserAvatar, { alt: '设计创作者', size: 'lg', src: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=graphic%20designer%20avatar%2C%20creative%20tools%2C%20colorful%20illustration&image_size=square_hd' }),
  ]),
  h('h2', '无可推荐用户时的紧凑空态'),
  h('section', { id: 'creator-empty', class: 'card-base' }, [h(EmptyState, { title: '暂无推荐创作者', description: '暂时没有可推荐的创作者，稍后再来看看吧', compact: true })]),
]) })
app.use(createPinia()).use(createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { render: () => null } }] })).use(ElementPlus)
app.component('UserAvatar', UserAvatar)
app.mount('#fixture')
