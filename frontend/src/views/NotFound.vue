<template>
  <div class="nf">
    <el-icon class="rocket"><Promotion /></el-icon>
    <h1>404</h1>
    <p>页面走丢了，先吃点好的吧</p>
    <el-button type="primary" round @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.nf {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #606266;
}
.nf .rocket { font-size: 64px; color: var(--guat-blue); }
.nf h1 { font-size: 56px; margin: 0; color: var(--guat-blue); }
</style>
