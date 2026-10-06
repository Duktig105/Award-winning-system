<template>
  <div class="showcase-wrap">
    <div
      v-if="ordered.length > 0"
      class="showcase-stage"
      @mouseenter="paused = true"
      @mouseleave="paused = false"
    >
      <button v-if="ordered.length > 1" class="stage-nav left" @click="step(-1)">
        <el-icon><ArrowLeft /></el-icon>
      </button>

      <div class="stage-track">
        <div
          v-for="(b, i) in ordered"
          :key="b.badgeId"
          class="stage-item"
          :class="{ center: i === center, representative: !!b.isRepresentative }"
          :style="itemStyle(i)"
          @click="focus(i)"
        >
          <div class="medal" :style="{ background: themeOf(b).bg }">
            <el-icon><component :is="iconOf(b)" /></el-icon>
            <span v-if="b.isRepresentative" class="rep-crown" title="代表勋章">
              <el-icon><StarFilled /></el-icon>
            </span>
          </div>
          <span class="medal-name">{{ b.badgeName }}</span>
          <span class="medal-time">{{ formatTime(b.unlockedAt) }}</span>
        </div>
      </div>

      <button v-if="ordered.length > 1" class="stage-nav right" @click="step(1)">
        <el-icon><ArrowRight /></el-icon>
      </button>
    </div>

    <div v-else class="showcase-empty">
      <el-empty description="暂未获得勋章，去申请获奖点亮勋章墙吧" :image-size="90" />
    </div>

    <!-- 指示点 -->
    <div v-if="ordered.length > 1" class="stage-dots">
      <span
        v-for="(b, i) in ordered"
        :key="b.badgeId"
        class="dot"
        :class="{ active: i === center }"
        @click="focus(i)"
      ></span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  Medal, StarFilled, Trophy, User, CollectionTag, Guide, Connection,
  ArrowLeft, ArrowRight
} from '@element-plus/icons-vue'

const props = defineProps({ badges: { type: Array, default: () => [] } })

const iconMap = {
  'star-on': StarFilled, 'medal': Medal, 'trophy': Trophy, 'user': User,
  'collection': CollectionTag, 'guide': Guide, 'connection': Connection,
  'handshake': Connection, 'aim': Guide
}
const iconOf = (b) => iconMap[b.icon] || Medal
const themeOf = (b) =>
  ({ first_award: 'linear-gradient(135deg,#f59e0b,#d97706)',
    national_honor: 'linear-gradient(135deg,#ef4444,#f59e0b)',
    provincial_expert: 'linear-gradient(135deg,#6366f1,#0ea5e9)',
    team_star: 'linear-gradient(135deg,#3b82f6,#06b6d4)',
    versatile: 'linear-gradient(135deg,#8b5cf6,#ec4899)',
    pioneer: 'linear-gradient(135deg,#f97316,#eab308)',
    golden_partner: 'linear-gradient(135deg,#eab308,#f59e0b)',
    continuous_growth: 'linear-gradient(135deg,#10b981,#14b8a6)' }[b.badgeCode]
    || 'linear-gradient(135deg,#6366f1,#8b5cf6)')

/* 代表勋章排第一，其余按解锁时间倒序 */
const ordered = computed(() => {
  const unlocked = props.badges.filter(b => !!(b.unlocked || b.unlockedAt))
  return [...unlocked].sort((a, b) => {
    if (!!a.isRepresentative !== !!b.isRepresentative) return a.isRepresentative ? -1 : 1
    return new Date(b.unlockedAt || 0) - new Date(a.unlockedAt || 0)
  })
})

const center = ref(0)
const paused = ref(false)
let timer = null

/* 有代表勋章时从它开始展示（排序已置顶），无则从第一枚开始 */
watch(() => ordered.value, (list) => {
  if (center.value >= list.length) center.value = 0
}, { immediate: true })

/* 每隔 3 秒自动切换展示下一枚勋章 */
const startAuto = () => {
  stopAuto()
  timer = setInterval(() => {
    if (paused.value) return
    if (ordered.value.length < 2) return
    center.value = (center.value + 1) % ordered.value.length
  }, 3000)
}
const stopAuto = () => { if (timer) { clearInterval(timer); timer = null } }

const step = (dir) => {
  const n = ordered.value.length
  if (n < 2) return
  center.value = (center.value + dir + n) % n
}
const focus = (i) => { center.value = i }

/* 线性排布，不做首尾回绕：离中心越远的勋章逐渐缩小、模糊、淡出 */
const itemStyle = (i) => {
  const o = i - center.value
  const d = Math.abs(o)
  const capped = Math.min(d, 3)
  return {
    transform: `translateX(${o * 108}px) translateY(${capped * 7}px) scale(${1 - capped * 0.2})`,
    filter: d === 0 ? 'none' : `blur(${Math.min(2.6, d * 0.9)}px)`,
    opacity: d === 0 ? 1 : Math.max(0, 1 - d * 0.34),
    zIndex: 10 - capped
  }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t).slice(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(startAuto)
onBeforeUnmount(stopAuto)
</script>

<style scoped>
.showcase-wrap { display: flex; flex-direction: column; align-items: center; gap: 10px; margin-top: 14px; }

.showcase-stage {
  position: relative; width: 100%; height: 210px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; border-radius: 16px;
  background: linear-gradient(135deg, rgba(99,102,241,0.07), rgba(139,92,246,0.04));
  border: 1px solid rgba(99,102,241,0.12);
}
/* 两侧渐隐遮罩 */
.showcase-stage::before, .showcase-stage::after {
  content: ''; position: absolute; top: 0; bottom: 0; width: 70px; z-index: 20; pointer-events: none;
}
.showcase-stage::before { left: 0; background: linear-gradient(to right, rgba(255,255,255,0.95), transparent); }
.showcase-stage::after { right: 0; background: linear-gradient(to left, rgba(255,255,255,0.95), transparent); }

.stage-track { position: relative; width: 100%; height: 100%; }
.stage-item {
  position: absolute; left: 50%; top: 50%;
  margin-left: -62px; margin-top: -62px;
  width: 124px; display: flex; flex-direction: column; align-items: center; gap: 4px;
  cursor: pointer; transition: transform 0.45s ease, filter 0.45s ease, opacity 0.45s ease;
}
.stage-item:hover .medal { box-shadow: 0 12px 26px rgba(99,102,241,0.4); }

.medal {
  width: 84px; height: 84px; border-radius: 50%; position: relative;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 38px;
  box-shadow: inset 0 0 0 6px rgba(255,255,255,0.32), 0 10px 22px rgba(99,102,241,0.3);
  transition: box-shadow 0.3s;
}
.medal-name { font-size: 14px; font-weight: 700; color: #1e293b; white-space: nowrap; max-width: 130px; overflow: hidden; text-overflow: ellipsis; }
.medal-time { font-size: 11px; color: #94a3b8; }

.rep-crown {
  position: absolute; top: -4px; right: -2px; width: 26px; height: 26px; border-radius: 50%;
  background: #fff; color: #f59e0b; display: flex; align-items: center; justify-content: center;
  font-size: 14px; box-shadow: 0 2px 8px rgba(245,158,11,0.5);
}

.stage-nav {
  position: absolute; top: 50%; transform: translateY(-50%); z-index: 30;
  width: 34px; height: 34px; border-radius: 50%; border: 1px solid rgba(99,102,241,0.25);
  background: rgba(255,255,255,0.9); color: #6366f1; cursor: pointer;
  display: flex; align-items: center; justify-content: center; font-size: 16px;
  transition: all 0.2s;
}
.stage-nav:hover { background: #6366f1; color: #fff; }
.stage-nav.left { left: 10px; }
.stage-nav.right { right: 10px; }

.stage-dots { display: flex; gap: 7px; }
.dot {
  width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; cursor: pointer; transition: all 0.25s;
}
.dot.active { background: linear-gradient(135deg,#6366f1,#8b5cf6); width: 20px; border-radius: 4px; }

@media screen and (max-width: 768px) {
  .showcase-stage { height: 180px; }
  .medal { width: 66px; height: 66px; font-size: 30px; }
  .stage-item { margin-left: -52px; margin-top: -54px; width: 104px; }
}
</style>
