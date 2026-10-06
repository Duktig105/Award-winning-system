<template>
  <div class="saims-global-wrapper">
    <!-- 全站动态背景层 -->
    <div class="saims-background-layer">
      <div class="bg-pattern"></div>
      <div class="color-blob blob-1"></div>
      <div class="color-blob blob-2"></div>
      <div class="color-blob blob-3"></div>
    </div>
    
    <!-- 路由页面出口 -->
    <router-view class="saims-router-view" />
  </div>
</template>

<script>
/**
 * Vue应用根组件
 * 功能：
 * 1. 作为整个Vue应用的入口组件
 * 2. 通过RouterView实现单页应用的路由切换
 * 3. 所有页面组件都会在此处渲染
 * 4. 提供全局动态美化背景
 */
export default {
  name: 'App'
}
</script>

<style>
/* 全局基础重置与字体设定 */
body {
  margin: 0;
  padding: 0;
  background-color: #f8fafc; /* 基本底色 */
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow-x: hidden;
}

/* 网站全局包装器，提供层叠上下文基准点 */
.saims-global-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100%;
}

/* ====== 动态流光背景全局样式 ====== */
.saims-background-layer {
  position: fixed; /* 固定在窗口底部，即使滚动也不动 */
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;     /* 确保放在所有内容之下 */
  pointer-events: none; /* 防止遮挡点击事件 */
  overflow: hidden;
}

/* 带有波点的细微网格 */
.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: radial-gradient(#cbd5e1 1px, transparent 1px);
  background-size: 32px 32px;
  opacity: 0.4;
}

/* 发光的动态球体 */
.color-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px); /* 强模糊形成光晕 */
  opacity: 0.6;
  animation: float 10s infinite ease-in-out alternate;
}

.blob-1 {
  width: 40vw; max-width: 400px;
  height: 40vw; max-height: 400px;
  background: #bae6fd; /* 浅蓝色 */
  top: -10%;
  left: -5%;
  animation-delay: 0s;
}

.blob-2 {
  width: 50vw; max-width: 500px;
  height: 50vw; max-height: 500px;
  background: #ddd6fe; /* 浅紫偏蓝色 */
  bottom: -20%;
  right: -10%;
  animation-delay: -2s;
}

.blob-3 {
  width: 30vw; max-width: 300px;
  height: 30vw; max-height: 300px;
  background: #bfdbfe; /* 较深的浅蓝色 */
  top: 40%;
  left: 60%;
  animation-delay: -4s;
}

@keyframes float {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -50px) scale(1.1); }
  100% { transform: translate(-20px, 20px) scale(0.9); }
}

/* ====== 全局透明毛玻璃卡片覆盖 (针对 Element Plus/原生卡片) ====== */
/* 使得各个页面的基础卡片具有通透性，体现出后面动态背景的美感 */
.el-card, .box-card, .glass-card {
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(16px) !important;
  -webkit-backdrop-filter: blur(16px) !important;
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05) !important;
  border-radius: 12px;
}

/* 让表格等组件尽量透明以适配新背景 */
.el-table {
  background-color: transparent !important;
}

.el-table tr {
  background-color: transparent !important;
}

.el-table th.el-table__cell {
  background-color: rgba(255, 255, 255, 0.5) !important;
}

/* 设置路由视图的默认层级，略高于背景 */
.saims-router-view {
  position: relative;
  z-index: 10;
}

/* ====== 移动端全局适配媒体查询 ====== */
@media screen and (max-width: 768px) {
  /* 减小背景漂浮物尺寸，避免在移动端显得过大 */
  .blob-1 {
    width: 60vw; height: 60vw;
  }
  .blob-2 {
    width: 70vw; height: 70vw;
  }
  .blob-3 {
    width: 50vw; height: 50vw;
  }

  /* 移动端全局卡片间距和边距优化 
     使用 :deep() 穿透但由于是全局非scoped可以直写，
     降低默认的 20px padding 到更适合手机的尺寸 */
  .el-card__body {
    padding: 16px !important;
  }
  .el-card__header {
    padding: 12px 16px !important;
  }
  
  /* 为了防止页面出现横向滚动条，强制加上 max-width */
  body {
    overflow-x: hidden;
    width: 100vw;
  }
}
</style>
