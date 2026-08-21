<template>
  <div class="guat-card rank-item" @click="go">
    <div class="rank-badge" :class="badgeClass">
      <span v-if="rank === 1" class="crown">👑</span>
      {{ rank }}
    </div>
    <div class="rank-thumb">
      <img v-if="image" :src="image" :alt="item.name" />
      <el-icon v-else><Food /></el-icon>
    </div>
    <div class="rank-body">
      <div class="rank-title">
        <span>{{ item.name }}</span>
        <el-tag v-if="target === 'dish' && item.price != null" size="small" type="warning" effect="plain">
          ¥{{ Number(item.price).toFixed(2) }}
        </el-tag>
      </div>
      <div class="rank-desc">{{ item.description || '暂无简介' }}</div>
      <div class="rank-meta">
        <span class="score-text">{{ score }}</span>
        <span class="meta-chip"><el-icon><Star /></el-icon> 综合评分</span>
        <span class="meta-chip"><el-icon><ChatDotRound /></el-icon> {{ item.ratingCount || 0 }} 条评价</span>
        <span class="meta-chip"><el-icon><View /></el-icon> {{ item.viewCount || 0 }} 浏览</span>
        <span v-if="target === 'window' && item.canteen" class="meta-chip">
          <el-icon><Location /></el-icon> {{ item.canteen.name }} · {{ item.location || '未知位置' }}
        </span>
        <span v-if="target === 'dish' && item.windowName" class="meta-chip">
          <el-icon><Shop /></el-icon> {{ item.windowName }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  rank: { type: Number, default: 1 },
  item: { type: Object, required: true },
  target: { type: String, default: 'window' }, // window | dish
})

const router = useRouter()

const image = computed(() =>
  props.target === 'window' ? props.item.coverImage : props.item.image
)
const score = computed(() =>
  (props.item.scoreAvg ?? 0).toFixed(1)
)
const badgeClass = computed(() => {
  if (props.rank === 1) return 'top1'
  if (props.rank === 2) return 'top2'
  if (props.rank === 3) return 'top3'
  return ''
})

const go = () => {
  const prefix = props.target === 'window' ? 'windows' : 'dishes'
  router.push(`/${prefix}/${props.item.id}`)
}
</script>
