<template>
  <div>
    <AppHeader />
    <div class="page-container">
      <div class="back">
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>&nbsp;返回</el-button>
      </div>

      <template v-if="dish">
        <div class="guat-card detail-card">
          <div class="cover">
            <img v-if="dish.image" :src="dish.image" alt="cover" />
            <el-icon v-else class="cover-placeholder"><Food /></el-icon>
          </div>
          <div class="info">
            <div class="title-row">
              <h2>{{ dish.name }}</h2>
              <el-tag type="warning" size="large" effect="light">¥{{ Number(dish.price ?? 0).toFixed(2) }}</el-tag>
            </div>
            <div class="desc">{{ dish.description || '暂无简介' }}</div>
            <div v-if="dish.windowName" class="loc">
              <el-icon><Shop /></el-icon>
              <el-link type="primary" @click="$router.push(`/windows/${dish.windowId}`)">{{ dish.windowName }}</el-link>
            </div>

            <div class="score-box">
              <div class="score-main">
                <span class="big">{{ (dish.scoreAvg ?? 0).toFixed(1) }}</span>
                <span class="of">/ 5.0</span>
                <div class="stars"><StarRating :value="dish.scoreAvg ?? 0" /></div>
              </div>
              <div class="score-dims">
                <div class="dim"><span>口味</span><el-rate :model-value="dish.tasteAvg ?? 0" disabled allow-half :disabled-void-color="'#e4e7ed'" /></div>
                <div class="dim"><span>性价比</span><el-rate :model-value="dish.valueAvg ?? 0" disabled allow-half :disabled-void-color="'#e4e7ed'" /></div>
                <div class="dim"><span>分量</span><el-rate :model-value="dish.portionAvg ?? 0" disabled allow-half :disabled-void-color="'#e4e7ed'" /></div>
              </div>
            </div>

            <div class="stats">
              <span><el-icon><ChatDotRound /></el-icon>{{ dish.ratingCount || 0 }} 评价</span>
              <span><el-icon><View /></el-icon>{{ dish.viewCount || 0 }} 浏览</span>
            </div>

            <div class="actions">
              <el-button type="primary" round @click="openRate"><el-icon><EditPen /></el-icon>&nbsp;写评价</el-button>
              <el-button round :type="dish.isLiked ? 'danger' : 'default'" @click="handleLike">
                <el-icon><CaretTop /></el-icon>&nbsp;点赞 {{ likeCount }}
              </el-button>
              <el-button round :type="dish.isFavorited ? 'warning' : 'default'" @click="handleFavorite">
                <el-icon><Star /></el-icon>&nbsp;{{ dish.isFavorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </div>

        <h3 class="section-title"><el-icon><ChatDotRound /></el-icon>&nbsp;同学评价</h3>
        <div v-if="ratings.length" class="rating-list">
          <div v-for="r in ratings" :key="r.id" class="guat-card rating-card">
            <div class="rating-head">
              <el-avatar :size="36" :src="r.user?.avatar || ''">{{ (r.user?.nickname || 'U').slice(0, 1) }}</el-avatar>
              <div class="r-user">
                <div class="r-name">{{ r.user?.nickname || '匿名同学' }}</div>
                <div class="r-time">{{ formatTime(r.createdAt) }}</div>
              </div>
              <div class="r-score">{{ ((r.taste + r.valueScore + r.portion) / 3).toFixed(1) }}</div>
            </div>
            <div class="r-dims">
              <el-rate :model-value="r.taste" disabled :disabled-void-color="'#e4e7ed'" /> <span class="dim-label">口味</span>
              <el-rate :model-value="r.valueScore" disabled :disabled-void-color="'#e4e7ed'" /> <span class="dim-label">性价比</span>
              <el-rate :model-value="r.portion" disabled :disabled-void-color="'#e4e7ed'" /> <span class="dim-label">分量</span>
            </div>
            <div v-if="r.comment" class="r-comment">{{ r.comment }}</div>
          </div>
        </div>
        <el-empty v-else description="还没有评价，快来抢沙发" />
      </template>
    </div>

    <RatingDialog
      v-model="rateVisible"
      target-type="DISH"
      :target-id="id"
      :target-name="dish?.name || ''"
      @rated="reload"
    />
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import StarRating from '@/components/StarRating.vue'
import RatingDialog from '@/components/RatingDialog.vue'
import { getDish, getRatings, toggleLike, toggleFavorite } from '@/api'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const id = route.params.id

const dish = ref(null)
const ratings = ref([])
const likeCount = ref(0)
const rateVisible = ref(false)

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const loadRatings = async () => {
  const res = await getRatings({ targetType: 'DISH', targetId: id, page: 1, size: 50 })
  ratings.value = res.data.records || []
}

const reload = async () => {
  const res = await getDish(id)
  dish.value = res.data
  likeCount.value = res.data.likeCount || 0
  loadRatings()
}

const requireLogin = () => {
  if (!userStore.isLogin()) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

const openRate = () => {
  if (requireLogin()) rateVisible.value = true
}

const handleLike = async () => {
  if (!requireLogin()) return
  const res = await toggleLike({ targetType: 'DISH', targetId: id })
  dish.value.isLiked = res.data.liked
  likeCount.value = res.data.count
}
const handleFavorite = async () => {
  if (!requireLogin()) return
  const res = await toggleFavorite({ targetType: 'DISH', targetId: id })
  dish.value.isFavorited = res.data
}

onMounted(async () => {
  const res = await getDish(id)
  dish.value = res.data
  likeCount.value = res.data.likeCount || 0
  loadRatings()
})
</script>

<style scoped>
.back { margin-bottom: 12px; }
.detail-card { display: flex; gap: 24px; padding: 24px; }
.cover { flex-shrink: 0; width: 260px; height: 200px; border-radius: 12px; overflow: hidden; background: linear-gradient(135deg, #e8f0fb, #d8e4f5); display: flex; align-items: center; justify-content: center; color: #7ba0d3; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 64px; }
.info { flex: 1; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 12px; }
.title-row h2 { margin: 0; }
.desc { margin: 10px 0 6px; color: #606266; font-size: 14px; line-height: 1.6; }
.loc { color: #909399; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.score-box { display: flex; gap: 30px; align-items: center; margin: 16px 0; padding: 16px; background: #f7f9fc; border-radius: 10px; }
.score-main { text-align: center; }
.score-main .big { font-size: 40px; font-weight: 800; color: var(--guat-orange); }
.score-main .of { color: #909399; }
.score-dims { display: flex; flex-direction: column; gap: 6px; }
.dim { display: flex; align-items: center; gap: 8px; color: #606266; font-size: 13px; }
.dim span { width: 44px; }
.stats { display: flex; gap: 20px; color: #606266; font-size: 13px; margin-bottom: 14px; }
.stats span { display: inline-flex; align-items: center; gap: 4px; }
.actions { display: flex; gap: 10px; }
.section-title { display: flex; align-items: center; gap: 6px; margin: 24px 0 12px; font-size: 18px; }
.rating-list { display: flex; flex-direction: column; gap: 12px; }
.rating-card { padding: 16px; }
.rating-head { display: flex; align-items: center; gap: 12px; }
.r-user { flex: 1; }
.r-name { font-weight: 600; font-size: 14px; }
.r-time { color: #909399; font-size: 12px; }
.r-score { font-size: 22px; font-weight: 800; color: var(--guat-orange); }
.r-dims { display: flex; align-items: center; gap: 6px; margin: 12px 0 4px; }
.dim-label { color: #909399; font-size: 12px; margin-right: 10px; }
.r-comment { color: #303133; font-size: 14px; line-height: 1.6; }
@media (max-width: 640px) {
  .detail-card { flex-direction: column; }
  .cover { width: 100%; height: 180px; }
}
</style>
