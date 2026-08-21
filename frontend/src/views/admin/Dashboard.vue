<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="4">
        <div class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="8">
        <div class="chart-card">
          <h4>各食堂档口分布</h4>
          <div ref="canteenChart" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="chart-card">
          <h4>档口综合评分 TOP10</h4>
          <div ref="windowChart" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="chart-card">
          <h4>菜品综合评分 TOP10</h4>
          <div ref="dishChart" class="chart"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getCanteenStats, getTopWindows, getTopDishes } from '@/api'

const cards = ref([
  { label: '注册用户', value: 0, color: '#1e5aa8' },
  { label: '食堂数量', value: 0, color: '#3b7fd1' },
  { label: '在榜档口', value: 0, color: '#f6b93b' },
  { label: '在榜菜品', value: 0, color: '#ff9f43' },
  { label: '评价总数', value: 0, color: '#e74c3c' },
  { label: '待审核', value: 0, color: '#909399' },
])

const canteenChart = ref()
const windowChart = ref()
const dishChart = ref()
let charts = []

const barOption = (data) => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 10, right: 30, top: 10, bottom: 10, containLabel: true },
  xAxis: { type: 'value', max: 5 },
  yAxis: { type: 'category', data: data.map((d) => d.name).reverse(), axisLabel: { fontSize: 11 } },
  series: [
    {
      type: 'bar',
      data: data.map((d) => (d.score ?? 0).toFixed(1)).reverse(),
      itemStyle: { color: '#1e5aa8', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', fontSize: 11 },
    },
  ],
})

const pieOption = (data) => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      data,
      label: { formatter: '{b}: {c}', fontSize: 11 },
    },
  ],
})

onMounted(async () => {
  const ov = await getOverview()
  cards.value[0].value = ov.data.userCount || 0
  cards.value[1].value = ov.data.canteenCount || 0
  cards.value[2].value = ov.data.windowCount || 0
  cards.value[3].value = ov.data.dishCount || 0
  cards.value[4].value = ov.data.ratingCount || 0
  cards.value[5].value = ov.data.pendingCount || 0

  const canteen = await getCanteenStats()
  const win = await getTopWindows(10)
  const dish = await getTopDishes(10)

  const c = echarts.init(canteenChart.value)
  c.setOption(pieOption(canteen.data || []))
  charts.push(c)

  const w = echarts.init(windowChart.value)
  w.setOption(barOption(win.data || []))
  charts.push(w)

  const d = echarts.init(dishChart.value)
  d.setOption(barOption(dish.data || []))
  charts.push(d)

  window.addEventListener('resize', onResize)
})

const onResize = () => charts.forEach((c) => c.resize())
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  text-align: center;
}
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 30px; font-weight: 800; margin-top: 8px; }
.chart-row { margin-top: 4px; }
.chart-card { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
.chart-card h4 { margin: 0 0 8px; font-size: 14px; color: #303133; }
.chart { height: 300px; }
</style>
