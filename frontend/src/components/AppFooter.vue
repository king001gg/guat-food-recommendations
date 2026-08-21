<template>
  <footer class="app-footer">
    <div class="footer-stars"></div>
    <div class="footer-inner">
      <div class="footer-rocket">🚀</div>
      <p class="brand">桂航美食推荐排行榜</p>
      <p class="sub">桂林航天工业学院校园美食 · 数据来源于同学们的真实评价</p>
      <p class="sub note">仅代表个人口味观点 · 探索星辰，也探索食堂</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #08172e, #133a6f);
  color: #b9c7dd;
  padding: 36px 16px 28px;
  margin-top: 40px;
}
.footer-stars {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(1.4px 1.4px at 18% 30%, rgba(255, 255, 255, 0.8), transparent 60%),
    radial-gradient(1px 1px at 70% 20%, rgba(255, 255, 255, 0.6), transparent 60%),
    radial-gradient(1.4px 1.4px at 85% 60%, rgba(255, 255, 255, 0.7), transparent 60%),
    radial-gradient(1px 1px at 40% 70%, rgba(255, 255, 255, 0.5), transparent 60%);
  background-size: 260px 260px;
  animation: twinkle 6s ease-in-out infinite;
  pointer-events: none;
}
.footer-inner {
  position: relative;
  max-width: 1100px;
  margin: 0 auto;
  text-align: center;
}
.footer-rocket { font-size: 26px; margin-bottom: 8px; }
.app-footer p { margin: 3px 0; font-size: 13px; }
.app-footer .brand { font-size: 15px; font-weight: 700; color: #fff; }
.app-footer .sub { opacity: 0.75; }
.app-footer .note { opacity: 0.5; font-size: 12px; }
</style>
