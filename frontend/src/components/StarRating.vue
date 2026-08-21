<template>
  <span class="star-rating">
    <el-rate :model-value="value" disabled allow-half :disabled-void-color="'#e4e7ed'" />
    <span class="num">{{ (value ?? 0).toFixed(1) }}</span>
  </span>
</template>

<script setup>
defineProps({
  value: { type: Number, default: 0 },
})
</script>

<style scoped>
.star-rating { display: inline-flex; align-items: center; gap: 4px; }
.num { color: var(--guat-orange); font-weight: 700; font-size: 14px; }
</style>
