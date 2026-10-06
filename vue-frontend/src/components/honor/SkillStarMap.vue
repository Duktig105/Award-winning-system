<template>
  <div class="star-map-shell">
    <div class="star-map">
      <!-- 星域背景 -->
      <div class="nebula"></div>
      <span
        v-for="s in bgStars"
        :key="s.id"
        class="bg-star"
        :style="starStyle(s)"
      ></span>

      <!-- 流星 -->
      <div class="comet" :style="{ animationDelay: (5 + Math.random() * 7).toFixed(1) + 's' }"></div>
    
      <!-- 能量连线：确保严格连接两个圆 -->
      <svg class="map-svg" viewBox="0 0 900 900" preserveAspectRatio="xMidYMid meet">
        <path
          v-for="(l, i) in links"
          :key="i"
          :d="l.d"
          class="link"
          :class="[l.tier === 0 ? 'tier0' : l.tier === 1 ? 'tier1' : 'tier2', l.state]"
        />
      </svg>
    
      <!-- 中心核心：方向中枢 (紫色) -->
      <div class="core" :style="corePos">
        <div class="core-progress" :style="{ background: coreConic }">
          <div class="core-inner">
            <el-icon :size="26"><component :is="coreIcon" /></el-icon>
          </div>
        </div>
        <div class="core-halo"></div>
        <span class="core-name">{{ categoryName }}</span>
        <span class="core-sub">{{ coreUnlocked }}/{{ coreTotal }} 已点亮</span>
      </div>
    
      <!-- 星点节点 -->
      <div
        v-for="n in laid"
        :key="n.id"
        class="star-node"
        :class="[
          'depth-' + n.depth,
          { unlocked: !!n.node.unlocked, locked: !n.node.unlocked, hovered: hoverId === n.id }
        ]"
        :style="nodePos(n)"
        @click="$emit('select', { node: n.node, rootIcon: coreIcon })"
        @mouseenter="hoverId = n.id"
        @mouseleave="hoverId = null"
      >
        <!-- 进度环（内部漂浮动画） -->
        <div class="node-orb" :style="[n.node.skillId ? { background: orbConic(n) } : null, floatStyle(n)]">
          <div class="orb-inner">
            <el-icon :size="n.depth === 1 ? 20 : 16">
              <component :is="nodeIcon(n)" />
            </el-icon>
          </div>
          <el-icon v-if="!n.node.unlocked" class="orb-lock"><Lock /></el-icon>
          <el-icon v-else class="orb-check"><CircleCheckFilled /></el-icon>
        </div>
        <span class="node-name" :style="floatStyle(n)">{{ n.node.name }}</span>
    
        <!-- 悬停详情卡 (修复：现只会显示当前悬停的节点) -->
        <transition name="tip-fade">
          <div v-if="hoverId === n.id" class="tip" :class="tipClass(n)">
            <div class="tip-head">
              <span class="tip-name">{{ n.node.name }}</span>
              <span class="tip-kind">{{ kindText(n) }}</span>
            </div>
            <div class="tip-progress">
              <div class="tip-track">
                <div class="tip-fill" :style="{ width: tipPercent(n) + '%' }"></div>
              </div>
              <span class="tip-num">{{ tipNum(n) }}</span>
            </div>
            <div class="tip-hint">{{ tipHint(n) }}</div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  Lock, CircleCheckFilled, MagicStick,
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook, Histogram, Promotion, Reading, Trophy
} from '@element-plus/icons-vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  categoryName: { type: String, default: '编程与开发' },
  categoryIcon: { type: String, default: '' },
  description: { type: String, default: '' }
})
defineEmits(['select'])

const hoverId = ref(null)

const iconMap = {
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook, Histogram,
  MagicStick, Promotion, Reading, Trophy, Lock
}
const coreIcon = computed(() => iconMap[props.categoryIcon] || Monitor)
const nodeIcon = (n) => iconMap[n.node.icon] || (n.node.unlocked ? Promotion : Lock)

/* ============ 布局：严格扇区比例划分，绝对不重叠 ============ */
const C = 450 
const RADII = [0, 180, 310, 400] 

const subtreeSize = (n) => 1 + (n.children || []).reduce((s, c) => s + subtreeSize(c), 0)

const layout = computed(() => {
  const items = [] 

  const place = (nodes, depth, a0, a1, parentIdx) => {
    if (!nodes || nodes.length === 0) return
    const weights = nodes.map(subtreeSize)
    const total = weights.reduce((a, b) => a + b, 0)
    
    const gap = depth === 1 ? 0 : (a1 - a0) * 0.12
    const startA = a0 + gap
    const available = (a1 - a0) - gap * 2

    let acc = startA
    nodes.forEach((node, i) => {
      const sweep = (available * weights[i]) / total
      const mid = acc + sweep / 2 
      const idx = items.length
      
      // 【关键修复】：赋予每一个节点一个唯一的 id，这样鼠标悬停才会只命中一个
      items.push({ id: idx, node, depth, mid, r: RADII[Math.min(depth, RADII.length - 1)], parentIdx })
      
      place(node.children || [], depth + 1, acc, acc + sweep, idx)
      acc += sweep
    })
  }
  place(props.nodes, 1, -Math.PI / 2, Math.PI * 1.5, -1)

  items.forEach(it => {
    it.x = C + it.r * Math.cos(it.mid)
    it.y = C + it.r * Math.sin(it.mid)
  })

  const ORB_R = { core: 38, 1: 28, 2: 24, 3: 20 } 
  const core = { x: C, y: C }
  
  const lines = items.map(it => {
    const isRoot = it.parentIdx < 0
    const from = isRoot ? core : items[it.parentIdx]
    const fromR = isRoot ? ORB_R.core : ORB_R[items[it.parentIdx].depth]
    const toR = ORB_R[it.depth]
    
    const dx = it.x - from.x
    const dy = it.y - from.y
    const len = Math.hypot(dx, dy) || 1
    const ux = dx / len
    const uy = dy / len
    const tier = isRoot ? 0 : items[it.parentIdx].depth
    
    return {
      d: `M ${from.x + ux * fromR} ${from.y + uy * fromR} L ${it.x - ux * toR} ${it.y - uy * toR}`,
      tier,
      state: ((isRoot || items[it.parentIdx].node.unlocked) && it.node.unlocked)
        ? 'active'
        : (it.node.unlocked ? 'partial' : 'dim')
    }
  })
  return { items, lines }
})
const laid = computed(() => layout.value.items)
const links = computed(() => layout.value.lines)

/* ============ 中心数据及杂项辅助 ============ */
const countNodes = (nodes, onlyUnlocked) =>
  (nodes || []).reduce((s, n) => s + (onlyUnlocked && !n.unlocked ? 0 : 1) + countNodes(n.children, onlyUnlocked), 0)
const coreTotal = computed(() => countNodes(props.nodes, false))
const coreUnlocked = computed(() => countNodes(props.nodes, true))
const corePercent = computed(() => (coreTotal.value ? Math.round((coreUnlocked.value / coreTotal.value) * 100) : 0))
// 中心核心圆环改为紫色
const coreConic = computed(() => `conic-gradient(#a855f7 ${corePercent.value * 3.6}deg, rgba(148,163,184,0.18) 0deg)`)

const corePos = { left: '50%', top: '50%' }
const nodePos = (n) => ({ left: (n.x / 900) * 100 + '%', top: (n.y / 900) * 100 + '%' })

const floatStyle = (n) => ({
  animationDelay: (n.id % 7) * 0.7 + 's',
  animationDuration: 5 + (n.id % 5) * 0.9 + 's'
})

const kindText = (n) => ({ 1: '一级方向', 2: '二级技能', 3: '三级进阶' }[n.node.level] || '技能节点')
const nodeCurrent = (n) => Number(n.node.skillId ? n.node.skillExperience || 0 : n.node.verifiedSkillCount || 0)
const nodeThreshold = (n) => Number(n.node.unlockThreshold || 0)
// 阈值为0的节点（无门槛）：未点亮时进度环为空、提示可主动点亮；点亮后显示"无门槛"满环
const tipPercent = (n) => {
  const t = nodeThreshold(n)
  if (t <= 0) return n.node.unlocked ? 100 : 0
  return Math.min(100, Math.round((nodeCurrent(n) / t) * 100))
}
const tipNum = (n) => {
  const t = nodeThreshold(n)
  if (t <= 0) return n.node.unlocked ? '无门槛' : '待点亮'
  return n.node.skillId ? `${nodeCurrent(n)} / ${t} 经验` : `${nodeCurrent(n)} / ${t} 项`
}
const tipHint = (n) => {
  if (n.node.unlocked) return '已点亮 · 继续积累可提升'
  if (nodeThreshold(n) <= 0) return '无门槛 · 点击星点亮灯'
  return n.node.skillId ? `再获得 ${Math.max(1, nodeThreshold(n) - nodeCurrent(n))} 点经验值点亮此星` : `再解锁 ${Math.max(1, nodeThreshold(n) - nodeCurrent(n))} 项技能点亮此域`
}

const tipClass = (n) => ({
  above: n.y >= 450,
  below: n.y < 450,
  left: n.x < 300,
  right: n.x > 600,
  center: n.x >= 300 && n.x <= 600
})

const orbConic = (n) =>
  n.node.unlocked
    ? 'conic-gradient(rgba(var(--c), 1), rgba(var(--c), 0.5), rgba(var(--c), 1))'
    : `conic-gradient(rgba(var(--c), 1) ${tipPercent(n) * 3.6}deg, rgba(148,163,184,0.22) 0deg)`

const bgStars = Array.from({ length: 46 }, (_, i) => ({
  id: i, x: Math.random() * 100, y: Math.random() * 100, size: 1 + Math.random() * 2.2, delay: Math.random() * 4, dur: 2.5 + Math.random() * 3
}))
const starStyle = (s) => ({
  left: s.x + '%', top: s.y + '%', width: s.size + 'px', height: s.size + 'px', animationDelay: s.delay + 's', animationDuration: s.dur + 's'
})
</script>

<style scoped>
.star-map-shell {
  width: 100%;
  display: flex;
  justify-content: center;
}

/* ===== 星域容器 ===== */
.star-map {
  position: relative;
  width: 100%;
  max-width: 1080px;
  aspect-ratio: 1 / 1;
  border-radius: 22px;
  overflow: hidden;
  background:
    radial-gradient(ellipse 70% 55% at 50% 42%, #2e1a6e 0%, transparent 65%),
    radial-gradient(ellipse 40% 30% at 78% 22%, rgba(56,189,248,0.16) 0%, transparent 70%),
    radial-gradient(ellipse 45% 35% at 20% 80%, rgba(217,70,239,0.12) 0%, transparent 70%),
    linear-gradient(160deg, #171235 0%, #0f172a 55%, #0a0f1f 100%);
  box-shadow: inset 0 0 80px rgba(99,102,241,0.12), 0 18px 40px -18px rgba(49,46,129,0.55);
  border: 1px solid rgba(129,140,248,0.22);
}
.nebula {
  position: absolute; inset: 0;
  background: radial-gradient(circle at 50% 42%, rgba(168,85,247,0.18), transparent 45%);
  animation: nebulaPulse 8s ease-in-out infinite alternate, nebulaHue 18s linear infinite;
}
@keyframes nebulaPulse { from { opacity: 0.5; transform: scale(1); } to { opacity: 0.9; transform: scale(1.06); } }
@keyframes nebulaHue { 0%, 100% { filter: hue-rotate(0deg); } 50% { filter: hue-rotate(35deg); } }

.bg-star { position: absolute; border-radius: 50%; background: #e0e7ff; animation: twinkle linear infinite; z-index: 1; }
@keyframes twinkle { 0%, 100% { opacity: 0.15; } 50% { opacity: 0.9; } }

.comet {
  position: absolute; top: 12%; left: -8%; width: 110px; height: 2px; border-radius: 2px;
  background: linear-gradient(90deg, transparent, rgba(224,231,255,0.9));
  filter: drop-shadow(0 0 6px rgba(165,180,252,0.9)); transform: rotate(24deg); opacity: 0;
  animation: cometFly 10s ease-in infinite; z-index: 3; pointer-events: none;
}
@keyframes cometFly { 0%, 78% { opacity: 0; translate: 0 0; } 82% { opacity: 1; } 96% { opacity: 0; } 100% { opacity: 0; translate: 420px 190px; } }

/* ===== 连线层级定制 ===== */
.map-svg { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 2; pointer-events: none; }
.link { fill: none; transition: stroke 0.3s; }

/* 核心 → 一级方向域（紫芯连出紫线） */
.link.tier0 { stroke-width: 3.5; }
.link.tier0.active { stroke: #a855f7; filter: drop-shadow(0 0 6px rgba(168,85,247,0.8)); }
.link.tier0.partial { stroke: rgba(168,85,247,0.6); }
.link.tier0.dim { stroke: rgba(168,85,247,0.25); stroke-dasharray: 4 5; }

/* 一级 → 二级技能域（蓝点连出蓝线） */
.link.tier1 { stroke-width: 1.5; }
.link.tier1.active { stroke: #38bdf8; filter: drop-shadow(0 0 4px rgba(56,189,248,0.8)); }
.link.tier1.partial { stroke: rgba(56,189,248,0.5); }
.link.tier1.dim { stroke: rgba(56,189,248,0.25); stroke-dasharray: 3 5; }

/* 二级 → 三级技能星点（金点连出金线） */
.link.tier2 { stroke-width: 1; }
.link.tier2.active { stroke: #fbbf24; filter: drop-shadow(0 0 3px rgba(251,191,36,0.7)); }
.link.tier2.partial { stroke: rgba(251,191,36,0.45); }
.link.tier2.dim { stroke: rgba(251,191,36,0.25); stroke-dasharray: 2 4; }

/* ===== 中心核心 (改为紫色风格) ===== */
.core { position: absolute; transform: translate(-50%, -50%); display: flex; flex-direction: column; align-items: center; gap: 6px; z-index: 5; }
.core-progress { width: 84px; height: 84px; border-radius: 50%; display: flex; align-items: center; justify-content: center; padding: 5px; animation: coreSpin 14s linear infinite; }
@keyframes coreSpin { to { transform: rotate(360deg); } }
.core-inner { width: 100%; height: 100%; border-radius: 50%; background: linear-gradient(145deg, #4c1d95, #1e1b4b); border: 1px solid rgba(168,85,247,0.5); display: flex; align-items: center; justify-content: center; color: #d8b4fe; box-shadow: 0 0 24px rgba(168,85,247,0.5); animation: coreSpinRev 14s linear infinite; }
@keyframes coreSpinRev { to { transform: rotate(-360deg); } }
.core-halo { position: absolute; top: 50%; left: 50%; width: 130px; height: 130px; transform: translate(-50%, -50%); border-radius: 50%; border: 1px dashed rgba(168,85,247,0.35); animation: haloSpin 22s linear infinite; pointer-events: none; }
@keyframes haloSpin { to { transform: translate(-50%, -50%) rotate(360deg); } }
.core-name { font-size: 15px; font-weight: 800; color: #e0e7ff; letter-spacing: 2px; text-shadow: 0 0 12px rgba(168,85,247,0.8); }
.core-sub { font-size: 11px; color: rgba(216,180,254,0.8); letter-spacing: 1px; }

/* ===== 星点节点与三级色彩映射 ===== */
.star-node { position: absolute; transform: translate(-50%, -50%); display: flex; flex-direction: column; align-items: center; gap: 5px; z-index: 6; cursor: pointer; }

/* 配置颜色映射！一级=蓝，二级=金，三级=紫/粉 */
.depth-1 { --c: 56, 189, 248; }   /* 一级：蓝色 */
.depth-2 { --c: 251, 191, 36; }   /* 二级：金色 */
.depth-3 { --c: 236, 72, 153; }   /* 三级：粉紫色(区分于核心紫) */

.node-orb, .node-name { animation: bob 4s ease-in-out infinite; }
@keyframes bob { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }

.node-orb { position: relative; width: 52px; height: 52px; border-radius: 50%; padding: 4px; transition: transform 0.25s ease; }
.orb-inner { width: 100%; height: 100%; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: linear-gradient(145deg, #1e1b4b, #171235); border: 1px solid rgba(148,163,184,0.35); color: #94a3b8; }
.orb-lock, .orb-check { position: absolute; right: -3px; bottom: -3px; background: #0f172a; border-radius: 50%; padding: 2px; font-size: 11px; border: 1px solid rgba(148,163,184,0.4); }
.orb-lock { color: #64748b; }
.orb-check { color: #34d399; border-color: rgba(52,211,153,0.5); }

.depth-1 .node-orb { width: 62px; height: 62px; }
.depth-3 .node-orb { width: 46px; height: 46px; }

/* 未解锁底色保留色彩倾向 */
.depth-1 .orb-inner { background: linear-gradient(145deg, #0c4a6e, #172554); border-color: rgba(56,189,248,0.45); color: #7dd3fc; }
.depth-2 .orb-inner { background: linear-gradient(145deg, #713f12, #1e1b4b); border-color: rgba(251,191,36,0.45); color: #fcd34d; }
.depth-3 .orb-inner { background: linear-gradient(145deg, #831843, #1e1b4b); border-color: rgba(236,72,153,0.45); color: #f9a8d4; }

/* 已解锁辉光与亮色 */
.star-node.unlocked .node-orb { animation: bob 4s ease-in-out infinite, glowPulse 3.6s ease-in-out infinite; }
@keyframes glowPulse { 0%, 100% { filter: drop-shadow(0 0 7px rgba(var(--c), 0.55)); } 50% { filter: drop-shadow(0 0 15px rgba(var(--c), 0.95)); } }

.depth-1.unlocked .orb-inner { background: linear-gradient(145deg, #0284c7, #172554); border-color: #38bdf8; color: #fff; }
.depth-2.unlocked .orb-inner { background: linear-gradient(145deg, #b45309, #1e1b4b); border-color: #fbbf24; color: #fff; }
.depth-3.unlocked .orb-inner { background: linear-gradient(145deg, #be185d, #1e1b4b); border-color: #ec4899; color: #fff; }

.node-name { font-size: 12px; font-weight: 600; max-width: 96px; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-shadow: 0 1px 6px rgba(15,23,42,0.9); }
.depth-1 .node-name { color: #bae6fd; }
.depth-2 .node-name { color: #fde68a; }
.depth-3 .node-name { color: #fbcfe8; }

.star-node:hover .node-orb, .star-node.hovered .node-orb { transform: scale(1.15) translateY(-4px); }
.star-node:hover .node-name { color: #fff; }

/* ===== 悬停详情卡 ===== */
.tip { position: absolute; width: 216px; padding: 12px 14px; border-radius: 12px; background: rgba(10, 15, 31, 0.94); border: 1px solid rgba(var(--c), 0.45); box-shadow: 0 12px 32px rgba(0,0,0,0.5), 0 0 20px rgba(var(--c), 0.25); backdrop-filter: blur(8px); z-index: 30; pointer-events: none; }
.tip.above { bottom: calc(100% + 14px); }
.tip.below { top: calc(100% + 14px); }
.tip.center { left: 50%; transform: translateX(-50%); }
.tip.left { left: -10px; }
.tip.right { right: -10px; }
.tip::after { content: ''; position: absolute; border: 6px solid transparent; }
.tip.above::after { top: 100%; border-top-color: rgba(var(--c), 0.45); }
.tip.below::after { bottom: 100%; border-bottom-color: rgba(var(--c), 0.45); }
.tip.center::after { left: 50%; transform: translateX(-50%); }
.tip.left::after { left: 26px; }
.tip.right::after { right: 26px; }

.tip-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.tip-name { font-size: 14px; font-weight: 700; color: #e0e7ff; }
.tip-kind { font-size: 10px; padding: 1px 8px; border-radius: 8px; background: rgba(var(--c), 0.2); color: rgba(var(--c), 0.9); white-space: nowrap; }
.tip-progress { display: flex; align-items: center; gap: 8px; margin-top: 9px; }
.tip-track { flex: 1; height: 5px; border-radius: 3px; background: rgba(148,163,184,0.2); overflow: hidden; }
.tip-fill { height: 100%; border-radius: 3px; background: rgba(var(--c), 0.9); }
.tip-num { font-size: 11px; color: rgba(var(--c), 0.9); white-space: nowrap; }
.tip-hint { margin-top: 8px; font-size: 11px; color: #c4b5fd; line-height: 1.5; }
.tip-fade-enter-active, .tip-fade-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.tip-fade-enter-from, .tip-fade-leave-to { opacity: 0; transform: translateY(4px); }

@media screen and (max-width: 768px) {
  .star-map { max-width: 100%; }
  .node-orb { width: 40px; height: 40px; }
  .depth-1 .node-orb { width: 50px; height: 50px; }
  .depth-3 .node-orb { width: 36px; height: 36px; }
  .core-progress { width: 62px; height: 62px; }
  .node-name { font-size: 10px; max-width: 68px; }
  .tip { width: 172px; }
}
</style>