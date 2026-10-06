<template>
  <div class="badge-panel">
    <!-- 勋章墙概览 -->
    <div class="wall-head glass-card">
      <div class="wall-head-left">
        <div class="wall-icon" style="background: linear-gradient(135deg,#ec4899,#f97316)">
          <el-icon><Medal /></el-icon>
        </div>
        <div class="wall-head-body">
          <span class="wall-title">勋章墙</span>
          <span class="wall-sub">
            已获得 <strong>{{ unlockedList.length }}</strong> / {{ list.length }} 枚勋章
            <span v-if="representativeBadge"> · 代表勋章：{{ representativeBadge.badgeName }}</span>
          </span>
          <div class="wall-bar">
            <div class="wall-fill" :style="{ width: wallPercent + '%' }"></div>
          </div>
        </div>
      </div>
      <div class="wall-head-right">
        <div class="wall-filter">
          <div
            v-for="t in filterTabs"
            :key="t.key"
            class="filter-chip"
            :class="{ active: filter === t.key }"
            @click="filter = t.key"
          >
            {{ t.label }}<span class="chip-count">{{ t.count }}</span>
          </div>
        </div>
        <el-button :icon="Refresh" :loading="checking" round @click="recheck">手动重新检查勋章</el-button>
      </div>
    </div>

    <!-- 代表勋章展示 -->
    <div v-if="representativeBadge" class="representative-showcase">
      <div class="showcase-inner" :style="{ background: themeOf(representativeBadge).bg }">
        <div class="showcase-ring">
          <el-icon class="showcase-icon"><component :is="iconOf(representativeBadge)" /></el-icon>
        </div>
        <div class="showcase-text">
          <span class="showcase-label">我的代表勋章</span>
          <span class="showcase-name">{{ representativeBadge.badgeName }}</span>
          <span class="showcase-desc">{{ representativeBadge.description }}</span>
        </div>
        <el-button class="showcase-btn" round @click="clearRepresentative">取消代表</el-button>
      </div>
    </div>

    <!-- 勋章网格 -->
    <div class="badge-grid">
      <div
        v-for="b in filteredList"
        :key="b.badgeId"
        class="badge-card"
        :class="{ unlocked: isUnlocked(b), locked: !isUnlocked(b), representative: !!b.isRepresentative }"
        @click="onCardClick(b)"
      >
        <div class="badge-ribbon" :style="{ background: themeOf(b).bg }" v-if="isUnlocked(b)">
          <el-icon><CircleCheck /></el-icon>
        </div>

        <div class="badge-medal" :style="{ background: isUnlocked(b) ? themeOf(b).bg : undefined }">
          <el-icon class="medal-icon"><component :is="isUnlocked(b) ? iconOf(b) : Lock" /></el-icon>
          <el-icon v-if="isUnlocked(b)" class="medal-glow"><StarFilled /></el-icon>
        </div>

        <div class="badge-name">{{ b.badgeName }}</div>
        <div class="badge-desc">{{ b.description }}</div>

        <div class="badge-cond">
          <el-icon><Aim /></el-icon>
          <span>{{ conditionText(b) }}</span>
        </div>

        <div class="badge-progress">
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: progressPct(b) + '%', background: themeOf(b).bg }"></div>
          </div>
          <span class="progress-num">{{ b.currentProgress || 0 }}/{{ b.unlockThreshold || 1 }}</span>
        </div>

        <div v-if="isUnlocked(b)" class="badge-foot">
          <el-tag v-if="b.isRepresentative" type="warning" effect="dark" round size="small">代表勋章</el-tag>
          <template v-else>
            <span class="unlock-time"><el-icon><Clock /></el-icon>{{ formatTime(b.unlockedAt) }}</span>
            <el-button size="small" type="primary" plain round @click.stop="setRepresentative(b)">设为代表</el-button>
          </template>
        </div>
        <div v-else class="badge-foot">
          <span class="remain-hint">{{ remainText(b) }}</span>
        </div>
      </div>
    </div>

    <el-empty v-if="filteredList.length === 0" description="暂无符合条件的勋章" :image-size="90" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Medal, StarFilled, Lock, Refresh, Aim, Clock, CircleCheck,
  Trophy, User, CollectionTag, Guide, Connection
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({ badges: { type: Array, default: () => [] } })
const emit = defineEmits(['refresh'])
const checking = ref(false)
const filter = ref('all')

/* 本地同步一份，便于在 props 更新前即时反馈 */
const localBadges = ref([])
const list = computed(() => (props.badges && props.badges.length ? props.badges : localBadges.value))

const iconMap = {
  'star-on': StarFilled,
  'medal': Medal,
  'trophy': Trophy,
  'user': User,
  'collection': CollectionTag,
  'guide': Guide,
  'connection': Connection,
  'handshake': Connection,
  'aim': Aim
}

/* 每枚勋章的主题色 */
const themeMap = {
  first_award:        { bg: 'linear-gradient(135deg,#f59e0b,#d97706)' },
  national_honor:     { bg: 'linear-gradient(135deg,#ef4444,#f59e0b)' },
  provincial_expert:  { bg: 'linear-gradient(135deg,#6366f1,#0ea5e9)' },
  team_star:          { bg: 'linear-gradient(135deg,#3b82f6,#06b6d4)' },
  versatile:          { bg: 'linear-gradient(135deg,#8b5cf6,#ec4899)' },
  pioneer:            { bg: 'linear-gradient(135deg,#f97316,#eab308)' },
  golden_partner:     { bg: 'linear-gradient(135deg,#eab308,#f59e0b)' },
  continuous_growth:  { bg: 'linear-gradient(135deg,#10b981,#14b8a6)' }
}
const themeOf = (b) => themeMap[b.badgeCode] || { bg: 'linear-gradient(135deg,#6366f1,#8b5cf6)' }

/* 进度单位文案 */
const unitMap = {
  first_award: '次获奖',
  national_honor: '个国家级奖项',
  provincial_expert: '个省级及以上奖项',
  team_star: '次团队获奖',
  versatile: '个不同竞赛类别',
  pioneer: '个奖项',
  golden_partner: '个团队',
  continuous_growth: '个月份'
}

/* 解锁条件可读文案（第一版固定条件类型 + 参数） */
const conditionMap = {
  first_award: '完成首次获奖申请并通过审核（≥1 次获奖）',
  national_honor: '获得 ≥1 个国家级竞赛奖项',
  provincial_expert: '获得 ≥3 个省级及以上竞赛奖项',
  team_star: '以团队成员身份完成团队获奖（≥1 次）',
  versatile: '在 ≥3 个不同竞赛类别中获得奖项',
  pioneer: '累计获得 ≥5 个奖项',
  golden_partner: '在同一团队获奖 ≥3 次',
  continuous_growth: '连续 ≥3 个不同月份有获奖记录'
}
const conditionText = (b) => conditionMap[b.badgeCode] || b.unlockCondition || '达成解锁条件后自动发放'

const iconOf = (b) => iconMap[b.icon] || Medal
const isUnlocked = (b) => !!(b.unlocked || b.unlockedAt)

const unlockedList = computed(() => list.value.filter(isUnlocked))
const representativeBadge = computed(() => list.value.find(b => !!b.isRepresentative))

const filterTabs = computed(() => [
  { key: 'all', label: '全部', count: list.value.length },
  { key: 'unlocked', label: '已获得', count: unlockedList.value.length },
  { key: 'locked', label: '未解锁', count: list.value.length - unlockedList.value.length }
])

const filteredList = computed(() => {
  if (filter.value === 'unlocked') return list.value.filter(isUnlocked)
  if (filter.value === 'locked') return list.value.filter(b => !isUnlocked(b))
  return list.value
})

const wallPercent = computed(() =>
  list.value.length ? Math.round((unlockedList.value.length / list.value.length) * 100) : 0
)

const progressPct = (b) => {
  const cur = Number(b.currentProgress || 0)
  const total = Number(b.unlockThreshold || 1)
  return Math.min(100, Math.round((cur / total) * 100))
}

const remainText = (b) => {
  const cur = Number(b.currentProgress || 0)
  const total = Number(b.unlockThreshold || 1)
  const need = Math.max(0, total - cur)
  return `还差 ${need} ${unitMap[b.badgeCode] || '次'}`
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t).slice(0, 10)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const loadBadges = async () => {
  try {
    const res = await request.get('/api/honor/student/badges')
    if (res.code === '200') localBadges.value = res.data || []
  } catch (e) { /* 保持已有数据 */ }
}

const setRepresentative = async (b) => {
  const res = await request.post('/api/honor/student/badge/representative', { badgeId: b.badgeId })
  if (res.code === '200') {
    ElMessage.success(`已将「${b.badgeName}」设为代表勋章`)
    await loadBadges()
    emit('refresh')
  }
}

const clearRepresentative = async () => {
  if (!representativeBadge.value) return
  const res = await request.post('/api/honor/student/badge/representative', { badgeId: 0 })
  if (res.code === '200' || res.code === 200) {
    ElMessage.success('已取消代表勋章')
    await loadBadges()
    emit('refresh')
  }
}

const onCardClick = (b) => {
  if (isUnlocked(b)) {
    ElMessage.info(`「${b.badgeName}」已解锁${b.isRepresentative ? '，当前为代表勋章' : '，可点击下方按钮设为代表勋章'}`)
  } else {
    ElMessage.info(`「${b.badgeName}」${remainText(b)} · ${conditionText(b)}`)
  }
}

const recheck = async () => {
  checking.value = true
  try {
    const res = await request.post('/api/honor/student/badge/recheck')
    if (res.code === '200') {
      await loadBadges()
      emit('refresh')
      ElMessage.success('已完成勋章重新检查')
    }
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.badge-panel { display: flex; flex-direction: column; gap: 18px; }

/* 概览条 */
.wall-head {
  display: flex; align-items: center; justify-content: space-between; gap: 18px; flex-wrap: wrap;
  padding: 18px 22px !important; border-radius: 16px !important;
  background: rgba(255,255,255,0.68) !important;
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.8) !important;
}
.wall-head-left { display: flex; align-items: center; gap: 14px; min-width: 260px; flex: 1; }
.wall-icon {
  width: 46px; height: 46px; border-radius: 13px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px;
}
.wall-head-body { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.wall-title { font-size: 16px; font-weight: 700; color: #1e293b; }
.wall-sub { font-size: 12px; color: #64748b; margin-top: 2px; }
.wall-sub strong { color: #6366f1; font-size: 14px; }
.wall-bar { margin-top: 8px; height: 6px; border-radius: 3px; background: rgba(99,102,241,0.12); overflow: hidden; }
.wall-fill { height: 100%; border-radius: 3px; background: linear-gradient(90deg,#6366f1,#8b5cf6); transition: width 0.4s; }

.wall-head-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.wall-filter { display: flex; gap: 8px; }
.filter-chip {
  padding: 5px 13px; border-radius: 18px; font-size: 13px; cursor: pointer; user-select: none;
  color: #64748b; background: rgba(255,255,255,0.7); border: 1px solid rgba(99,102,241,0.15);
  transition: all 0.2s;
}
.filter-chip:hover { border-color: #6366f1; color: #6366f1; }
.filter-chip.active { background: linear-gradient(135deg,#6366f1,#8b5cf6); color: #fff; border-color: transparent; }
.chip-count { margin-left: 6px; font-size: 12px; opacity: 0.85; }

/* 代表勋章展示 */
.representative-showcase { border-radius: 18px; overflow: hidden; }
.showcase-inner {
  display: flex; align-items: center; gap: 18px; padding: 20px 24px;
  border-radius: 18px; color: #fff; position: relative; overflow: hidden;
  box-shadow: 0 14px 30px rgba(99,102,241,0.25);
}
.showcase-inner::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(circle at 88% 20%, rgba(255,255,255,0.35), transparent 55%);
}
.showcase-ring {
  width: 62px; height: 62px; border-radius: 50%; flex-shrink: 0;
  background: rgba(255,255,255,0.22); border: 2px solid rgba(255,255,255,0.6);
  display: flex; align-items: center; justify-content: center; font-size: 28px;
}
.showcase-text { display: flex; flex-direction: column; flex: 1; min-width: 0; position: relative; z-index: 1; }
.showcase-label { font-size: 12px; opacity: 0.9; letter-spacing: 1px; }
.showcase-name { font-size: 20px; font-weight: 800; margin: 2px 0 4px; }
.showcase-desc { font-size: 13px; opacity: 0.92; }
.showcase-btn { background: rgba(255,255,255,0.22) !important; border: 1px solid rgba(255,255,255,0.6) !important; color: #fff !important; position: relative; z-index: 1; }

/* 勋章网格 */
.badge-grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(232px, 1fr)); }

.badge-card {
  position: relative; padding: 22px 20px 18px; border-radius: 18px; text-align: center;
  background: rgba(255,255,255,0.7); border: 1px solid rgba(255,255,255,0.75);
  transition: all 0.3s; cursor: pointer; overflow: hidden;
  display: flex; flex-direction: column; align-items: center;
}
.badge-card:hover { transform: translateY(-4px); box-shadow: 0 16px 28px rgba(99,102,241,0.18); }
.badge-card.unlocked { background: linear-gradient(135deg, rgba(99,102,241,0.09), rgba(139,92,246,0.05)); border-color: rgba(99,102,241,0.3); }
.badge-card.locked { background: rgba(248,250,252,0.6); border-style: dashed; }
.badge-card.representative { border-color: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,0.18); }

.badge-ribbon {
  position: absolute; top: 0; right: 0; width: 34px; height: 34px;
  border-bottom-left-radius: 14px; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 16px;
}

.badge-medal {
  width: 74px; height: 74px; border-radius: 50%; position: relative;
  display: flex; align-items: center; justify-content: center;
  background: #e2e8f0; color: #94a3b8; font-size: 30px;
  box-shadow: inset 0 0 0 5px rgba(255,255,255,0.6);
}
.badge-card.unlocked .badge-medal { color: #fff; box-shadow: inset 0 0 0 5px rgba(255,255,255,0.35), 0 10px 20px rgba(99,102,241,0.3); }
.medal-glow {
  position: absolute; top: -7px; right: -7px; font-size: 17px; color: #fbbf24;
  background: #fff; border-radius: 50%; padding: 2px; box-shadow: 0 2px 8px rgba(245,158,11,0.45);
}

.badge-name { font-size: 16px; font-weight: 700; color: #1e293b; margin-top: 12px; }
.badge-card.locked .badge-name { color: #94a3b8; }
.badge-desc { font-size: 13px; color: #64748b; margin-top: 6px; line-height: 1.55; min-height: 40px; }

.badge-cond {
  display: flex; align-items: flex-start; gap: 5px; text-align: left;
  font-size: 12px; color: #6366f1; background: rgba(99,102,241,0.08);
  padding: 7px 10px; border-radius: 9px; margin-top: 10px; width: 100%; line-height: 1.5;
}
.badge-cond .el-icon { margin-top: 2px; flex-shrink: 0; }

.badge-progress { margin-top: 12px; width: 100%; display: flex; align-items: center; gap: 8px; }
.progress-track { flex: 1; height: 7px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 4px; transition: width 0.4s; }
.progress-num { font-size: 12px; color: #475569; white-space: nowrap; font-weight: 600; }

.badge-foot { margin-top: 12px; width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 28px; }
.unlock-time { font-size: 12px; color: #94a3b8; display: inline-flex; align-items: center; gap: 3px; }
.remain-hint { font-size: 12px; color: #f59e0b; font-weight: 600; }
</style>