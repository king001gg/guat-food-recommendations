<template>
  <canvas ref="canvas" class="starfield"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvas = ref(null)
let ctx, raf, width, height
let stars = []
let bursts = []
let burstTimer = 0

const STAR_COUNT = 150

function resize() {
  width = window.innerWidth
  height = window.innerHeight
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.value.width = width * dpr
  canvas.value.height = height * dpr
  canvas.value.style.width = width + 'px'
  canvas.value.style.height = height + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function initStars() {
  stars = []
  for (let i = 0; i < STAR_COUNT; i++) {
    stars.push({
      x: Math.random() * width,
      y: Math.random() * height,
      r: Math.random() * 1.2 + 0.3,
      baseAlpha: Math.random() * 0.5 + 0.15,
      twinkleSpeed: Math.random() * 0.02 + 0.005,
      twinklePhase: Math.random() * Math.PI * 2,
      driftX: (Math.random() - 0.5) * 0.06,
      driftY: (Math.random() - 0.5) * 0.06,
    })
  }
}

function spawnBurst() {
  const cx = Math.random() * width
  const cy = Math.random() * height
  const count = 14 + Math.floor(Math.random() * 12)
  for (let i = 0; i < count; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = Math.random() * 0.9 + 0.3
    bursts.push({
      x: cx,
      y: cy,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: 1,
      decay: Math.random() * 0.012 + 0.006,
      r: Math.random() * 1.4 + 0.4,
    })
  }
}

function tick() {
  ctx.clearRect(0, 0, width, height)

  // 背景星星：闪烁 + 缓慢漂移
  for (const s of stars) {
    s.twinklePhase += s.twinkleSpeed
    const alpha = s.baseAlpha * (0.6 + 0.4 * Math.sin(s.twinklePhase))
    s.x += s.driftX
    s.y += s.driftY
    if (s.x < -2) s.x = width + 2
    if (s.x > width + 2) s.x = -2
    if (s.y < -2) s.y = height + 2
    if (s.y > height + 2) s.y = -2
    ctx.beginPath()
    ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(180, 210, 255, ${alpha})`
    ctx.fill()
  }

  // 发散粒子：从随机点向外扩散并淡出
  burstTimer++
  if (burstTimer > 160) {
    spawnBurst()
    burstTimer = 0
  }
  for (let i = bursts.length - 1; i >= 0; i--) {
    const p = bursts[i]
    p.x += p.vx
    p.y += p.vy
    p.life -= p.decay
    if (p.life <= 0) {
      bursts.splice(i, 1)
      continue
    }
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * p.life, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(210, 228, 255, ${p.life * 0.8})`
    ctx.fill()
  }

  raf = requestAnimationFrame(tick)
}

function onResize() {
  resize()
  initStars()
}

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  resize()
  initStars()
  tick()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.starfield {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
