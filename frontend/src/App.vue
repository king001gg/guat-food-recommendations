<template>
  <Starfield />
  <div class="app-content">
    <router-view />
  </div>
</template>

<script setup>
import Starfield from '@/components/Starfield.vue'
</script>

<style scoped>
.app-content {
  position: relative;
  z-index: 1;
}
</style>
