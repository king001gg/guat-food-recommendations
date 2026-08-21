<template>
  <div>
    <AppHeader />

    <div class="page-container">
      <!-- 顶部横幅 -->
      <div class="hero">
        <div class="hero-stars"></div>
        <div class="hero-orbit hero-orbit-1"></div>
        <div class="hero-orbit hero-orbit-2"></div>
        <div class="hero-orbit hero-orbit-3"></div>
        <div class="hero-rocket">🚀</div>
        <h1>桂航美食推荐排行榜</h1>
        <p>桂林航天工业学院 · 最受同学们欢迎的食堂档口与菜品榜单</p>
        <div class="hero-chips">
          <span class="chip">🏆 多维评分</span>
          <span class="chip">🌟 真实评价</span>
          <span class="chip">🛰️ 桂航专属</span>
        </div>
        <div class="hero-search">
          <el-input
            v-model="keyword"
            placeholder="搜索档口或菜品…"
            size="large"
            clearable
            @keyup.enter="onSearch"
            @clear="onSearch"
          >
            <template #append>
              <el-button :icon="'Search'" @click="onSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 切换：档口 / 菜品 -->
      <div class="toolbar">
        <el-radio-group v-model="target" @change="resetAndLoad">
          <el-radio-button value="window">档口排行</el-radio-button>
          <el-radio-button value="dish">菜品排行</el-radio-button>
        </el-radio-group>

        <div class="toolbar-right">
          <el-select v-model="canteenId" placeholder="全部食堂" clearable style="width: 160px" @change="resetAndLoad">
            <el-option v-for="c in canteens" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button type="primary" round @click="$router.push('/submit')">
            <el-icon><Plus /></el-icon>&nbsp;我要投稿
          </el-button>
        </div>
      </div>

      <!-- 榜单类型 -->
      <div class="type-tabs">
        <div
          v-for="t in typeOptions"
          :key="t.value"
          class="type-tab"
          :class="{ active: type === t.value }"
          @click="type = t.value; resetAndLoad()"
        >
          {{ t.label }}
        </div>
      </div>

      <!-- 榜单列表 -->
      <div v-loading="loading" class="rank-list">
        <template v-if="list.length">
          <RankItem
            v-for="(item, idx) in list"
            :key="item.id"
            :rank="(page - 1) * size + idx + 1"
            :item="item"
            :target="target"
          />
        </template>
        <el-empty v-else-if="!loading" description="暂无上榜数据，快来投稿吧" />
      </div>

      <div v-if="total > size" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import RankItem from '@/components/RankItem.vue'
import { rankWindows, rankDishes, getCanteens } from '@/api'

const typeOptions = [
  { label: '综合榜', value: 'overall' },
  { label: '好评榜', value: 'taste' },
  { label: '人气榜', value: 'hot' },
  { label: '热门榜', value: 'recent' },
]

const target = ref('window')
const type = ref('overall')
const canteenId = ref(null)
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const canteens = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const params = { type: type.value, canteenId: canteenId.value || null, keyword: keyword.value || null, page: page.value, size }
    const res = target.value === 'window'
      ? await rankWindows(params)
      : await rankDishes(params)
    list.value = res.data.list || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

const resetAndLoad = () => {
  page.value = 1
  load()
}
const onPageChange = (p) => {
  page.value = p
  load()
}
const onSearch = () => resetAndLoad()

onMounted(async () => {
  const res = await getCanteens()
  canteens.value = res.data || []
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.toolbar-right { display: flex; align-items: center; gap: 10px; }
.type-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.type-tab {
  padding: 8px 22px;
  border-radius: 20px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}
.type-tab.active {
  background: var(--guat-blue);
  color: #fff;
  font-weight: 600;
}
.rank-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.pager { display: flex; justify-content: center; margin-top: 20px; }
</style>
