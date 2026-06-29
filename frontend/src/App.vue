<template>
 <router-view/>
</template>
<script setup>
// 根组件只承载 router-view，实际页面由路由懒加载。
</script>
<style>
html,
body,
#app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
