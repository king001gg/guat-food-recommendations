<template>
  <div>
    <AppHeader />
    <div class="page-container">
      <!-- 用户信息 -->
      <div class="guat-card user-card">
        <el-avatar :size="64" :src="userStore.user?.avatar || ''">{{ (userStore.user?.nickname || 'U').slice(0, 1) }}</el-avatar>
        <div class="u-info">
          <div class="u-name">
            {{ userStore.user?.nickname }}
            <el-tag v-if="userStore.isAdmin()" size="small" type="danger">管理员</el-tag>
          </div>
          <div class="u-username">@{{ userStore.user?.username }}</div>
        </div>
      </div>

      <el-tabs v-model="tab" class="profile-tabs">
        <el-tab-pane label="我的评价" name="ratings">
          <div v-loading="ratingLoading" class="list-col">
            <div v-for="r in ratings" :key="r.id" class="guat-card item-card">
              <div class="item-head">
                <el-link type="primary" class="target-name" @click="go(r.targetType, r.targetId)">{{ r.targetName || '未知目标' }}</el-link>
                <span class="time">{{ formatTime(r.createdAt) }}</span>
              </div>
              <div class="dims">
                <span>口味 <b>{{ r.taste }}</b></span>
                <span>性价比 <b>{{ r.valueScore }}</b></span>
                <span>分量 <b>{{ r.portion }}</b></span>
              </div>
              <div v-if="r.comment" class="comment">{{ r.comment }}</div>
              <div class="item-actions">
                <el-button size="small" type="danger" link @click="removeRating(r)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!ratingLoading && !ratings.length" description="还没有评价" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的点赞" name="likes">
          <div v-loading="likeLoading" class="list-col">
            <div v-for="l in likes" :key="l.id" class="guat-card item-card" @click="go(l.targetType, l.targetId)">
              <div class="mini-thumb">
                <img v-if="l.targetImage" :src="l.targetImage" alt="" />
                <el-icon v-else><Food /></el-icon>
              </div>
              <div class="mini-info">
                <div class="mini-name">{{ l.targetName || '未知目标' }}</div>
                <div class="time">{{ l.targetType === 'WINDOW' ? '档口' : '菜品' }} · {{ formatTime(l.createdAt) }}</div>
              </div>
            </div>
            <el-empty v-if="!likeLoading && !likes.length" description="还没有点赞" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的收藏" name="favorites">
          <div v-loading="favLoading" class="list-col">
            <div v-for="f in favorites" :key="f.id" class="guat-card item-card" @click="go(f.targetType, f.targetId)">
              <div class="mini-thumb">
                <img v-if="f.targetImage" :src="f.targetImage" alt="" />
                <el-icon v-else><Food /></el-icon>
              </div>
              <div class="mini-info">
                <div class="mini-name">{{ f.targetName || '未知目标' }}</div>
                <div class="time">{{ f.targetType === 'WINDOW' ? '档口' : '菜品' }} · {{ formatTime(f.createdAt) }}</div>
              </div>
            </div>
            <el-empty v-if="!favLoading && !favorites.length" description="还没有收藏" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import { myRatings, myLikes, myFavorites, deleteRating } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const tab = ref('ratings')
const ratings = ref([])
const likes = ref([])
const favorites = ref([])
const ratingLoading = ref(false)
const likeLoading = ref(false)
const favLoading = ref(false)

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

const go = (targetType, targetId) => {
  const prefix = targetType === 'WINDOW' ? 'windows' : 'dishes'
  router.push(`/${prefix}/${targetId}`)
}

const loadRatings = async () => {
  ratingLoading.value = true
  try {
    const res = await myRatings({ page: 1, size: 100 })
    ratings.value = res.data.records || []
  } finally {
    ratingLoading.value = false
  }
}
const loadLikes = async () => {
  likeLoading.value = true
  try {
    const res = await myLikes({ page: 1, size: 100 })
    likes.value = res.data.records || []
  } finally {
    likeLoading.value = false
  }
}
const loadFavorites = async () => {
  favLoading.value = true
  try {
    const res = await myFavorites({ page: 1, size: 100 })
    favorites.value = res.data.records || []
  } finally {
    favLoading.value = false
  }
}

const removeRating = async (r) => {
  await ElMessageBox.confirm('确定删除这条评价吗？', '提示', { type: 'warning' })
  await deleteRating(r.id)
  ElMessage.success('已删除')
  loadRatings()
}

onMounted(() => {
  loadRatings()
  loadLikes()
  loadFavorites()
})
</script>

<style scoped>
.user-card { display: flex; align-items: center; gap: 16px; padding: 24px; margin-bottom: 16px; }
.u-info .u-name { font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.u-info .u-username { color: #909399; font-size: 13px; margin-top: 4px; }
.profile-tabs { background: #fff; border-radius: 12px; padding: 8px 16px 20px; }
.list-col { display: flex; flex-direction: column; gap: 12px; }
.item-card { padding: 16px; }
.item-head { display: flex; align-items: center; justify-content: space-between; }
.target-name { font-size: 16px; font-weight: 600; }
.time { color: #909399; font-size: 12px; }
.dims { display: flex; gap: 20px; margin: 10px 0; color: #606266; font-size: 13px; }
.dims b { color: var(--guat-orange); }
.comment { color: #303133; font-size: 14px; line-height: 1.6; margin-bottom: 6px; }
.item-actions { text-align: right; }
.item-card { display: flex; align-items: center; gap: 14px; cursor: pointer; }
.mini-thumb { width: 60px; height: 60px; border-radius: 8px; overflow: hidden; background: linear-gradient(135deg, #e8f0fb, #d8e4f5); display: flex; align-items: center; justify-content: center; color: #7ba0d3; }
.mini-thumb img { width: 100%; height: 100%; object-fit: cover; }
.mini-info .mini-name { font-size: 15px; font-weight: 600; }
</style>
