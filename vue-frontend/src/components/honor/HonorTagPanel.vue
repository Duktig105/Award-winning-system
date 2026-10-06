<template>
  <div class="honor-tag-panel">
    <!-- 头部 -->
    <div class="panel-head">
      <div class="head-title">
        <el-icon class="head-icon"><CollectionTag /></el-icon>
        <div>
          <h3>荣誉标签</h3>
          <span class="head-tip">系统根据你的获奖记录、技能档案与成长轨迹自动生成</span>
        </div>
      </div>
      <el-button :icon="Refresh" round @click="loadAll">刷新 / 重算</el-button>
    </div>

    <!-- 统计概览 -->
    <div class="stats-row">
      <div class="stat-card glass-card" v-for="s in stats" :key="s.key">
        <div class="stat-icon" :style="{ background: s.color }">
          <el-icon><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-label">{{ s.label }}</span>
          <span class="stat-value">{{ s.value }}</span>
          <span class="stat-tip">{{ s.tip }}</span>
        </div>
      </div>
    </div>

    <!-- 总体进度 + 类型分布 -->
    <div class="progress-card glass-card">
      <div class="progress-head">
        <span class="progress-title">总体解锁进度</span>
        <span class="progress-meta">{{ obtainedCount }} / {{ totalEnabled }} （{{ progressPercent }}%）</span>
      </div>
      <el-progress
        :percentage="progressPercent"
        :stroke-width="12"
        :show-text="false"
        color="#6366f1"
        :duration="800"
        class="progress-bar"
      />
      <div class="type-chips">
        <div
          v-for="t in typeStats"
          :key="t.type"
          class="type-chip"
          :class="['chip-' + t.type]"
        >
          <span class="chip-name">{{ typeLabels[t.type] }}</span>
          <span class="chip-progress">
            <strong>{{ t.obtained }}</strong> / {{ t.total }}
          </span>
          <div class="chip-track">
            <div class="chip-fill" :style="{ width: (t.total ? (t.obtained / t.total) * 100 : 0) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-row">
      <div class="filter-left">
        <el-radio-group v-model="filterType" size="default">
          <el-radio-button value="all">全部类型</el-radio-button>
          <el-radio-button value="award_level">获奖等级</el-radio-button>
          <el-radio-button value="competition_direction">竞赛方向</el-radio-button>
          <el-radio-button value="role">角色</el-radio-button>
          <el-radio-button value="ability">能力</el-radio-button>
          <el-radio-button value="growth">成长</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="filterStatus" size="default" class="status-radio">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="obtained">已获得</el-radio-button>
          <el-radio-button value="locked">待解锁</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-right">
        <el-input
          v-model="keyword"
          placeholder="搜索标签名称或描述"
          clearable
          :prefix-icon="Search"
          style="width: 220px;"
        />
      </div>
    </div>

    <!-- 标签网格 -->
    <div v-loading="loading" class="tag-grid-wrap">
      <div v-if="filteredTags.length > 0" class="tag-grid">
        <div
          v-for="tag in filteredTags"
          :key="tag.tagId"
          class="tag-card"
          :class="['type-' + tag.tagType, { locked: !tag.obtained, obtained: tag.obtained }]"
        >
          <div class="tag-icon-wrap">
            <el-icon class="tag-icon" :size="32"><component :is="iconForTag(tag)" /></el-icon>
            <el-icon v-if="!tag.obtained" class="lock-icon"><Lock /></el-icon>
            <el-icon v-if="tag.obtained" class="check-icon"><CircleCheckFilled /></el-icon>
          </div>
          <div class="tag-name">{{ tag.tagName }}</div>
          <div class="tag-type-badge">
            <el-tag :type="tagTypeMap[tag.tagType] || 'info'" size="small" round effect="plain">
              {{ typeLabels[tag.tagType] || tag.tagType }}
            </el-tag>
          </div>
          <div class="tag-desc">{{ tag.description }}</div>
          <div class="tag-foot">
            <span v-if="tag.obtained" class="foot-obtained">
              <el-icon><Calendar /></el-icon>
              {{ formatTime(tag.obtainedAt) }}
            </span>
            <span v-else class="foot-locked">
              <el-icon><Lock /></el-icon>
              未解锁
            </span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && filteredTags.length === 0" description="没有符合条件的标签" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh, Search, Calendar, Lock, CircleCheckFilled,
  CollectionTag, Trophy, MagicStick, Medal, StarFilled,
  UserFilled, TrendCharts, Aim, Sunny, Compass, Briefcase,
  DataLine, Reading
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const mineTags = ref([])        // /api/honor/student/tags — 已获得
const allTags = ref([])         // /api/honor/tag/list — 全部（含条件）
const filterType = ref('all')
const filterStatus = ref('all')
const keyword = ref('')

const tagTypeMap = {
  award_level: 'danger',
  competition_direction: 'warning',
  role: 'success',
  ability: 'primary',
  growth: 'info'
}
const typeLabels = {
  award_level: '获奖等级',
  competition_direction: '竞赛方向',
  role: '角色',
  ability: '能力',
  growth: '成长'
}
const typeIconMap = {
  award_level: Trophy,
  competition_direction: Compass,
  role: UserFilled,
  ability: MagicStick,
  growth: TrendCharts
}

const iconForTag = (tag) => typeIconMap[tag.tagType] || CollectionTag

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/* 合并已获得 + 全部配置 */
const merged = computed(() => {
  const enabled = allTags.value.filter(t => (t.status || 'enabled') === 'enabled')
  const mineMap = new Map(mineTags.value.map(t => [t.tagId, t]))
  const list = enabled.map(t => {
    const m = mineMap.get(t.tagId)
    return {
      ...t,
      obtained: !!m,
      obtainedAt: m ? m.awardedAt : null
    }
  })
  // 已获得的里面，如果管理员已停用但学生已获过，仍显示
  for (const m of mineTags.value) {
    if (!enabled.find(t => t.tagId === m.tagId)) {
      list.push({ ...m, obtained: true, obtainedAt: m.awardedAt })
    }
  }
  return list.sort((a, b) => {
    if (a.obtained !== b.obtained) return a.obtained ? -1 : 1
    return (a.sortOrder || 0) - (b.sortOrder || 0)
  })
})

const totalEnabled = computed(() => merged.value.length)
const obtainedCount = computed(() => merged.value.filter(t => t.obtained).length)
const lockedCount = computed(() => merged.value.filter(t => !t.obtained).length)
const progressPercent = computed(() =>
  totalEnabled.value ? Math.round((obtainedCount.value / totalEnabled.value) * 100) : 0
)

const stats = computed(() => [
  { key: 'obtain', label: '已获标签', value: obtainedCount.value, tip: '系统自动授予', icon: CollectionTag, color: 'linear-gradient(135deg,#6366f1,#8b5cf6)' },
  { key: 'pool',   label: '标签池',   value: totalEnabled.value,  tip: '当前启用的全部标签', icon: Medal, color: 'linear-gradient(135deg,#10b981,#14b8a6)' },
  { key: 'lock',   label: '待解锁',   value: lockedCount.value,   tip: '达成条件即可获得', icon: TrendCharts, color: 'linear-gradient(135deg,#f59e0b,#f97316)' },
  { key: 'rate',   label: '完成率',   value: progressPercent.value + '%', tip: '解锁进度', icon: DataLine, color: 'linear-gradient(135deg,#ec4899,#f97316)' }
])

const typeStats = computed(() => {
  const buckets = ['award_level', 'competition_direction', 'role', 'ability', 'growth']
  return buckets.map(type => {
    const list = merged.value.filter(t => t.tagType === type)
    return {
      type,
      total: list.length,
      obtained: list.filter(t => t.obtained).length
    }
  }).filter(b => b.total > 0)
})

/* 过滤 */
const filteredTags = computed(() => {
  let list = merged.value
  if (filterType.value !== 'all') list = list.filter(t => t.tagType === filterType.value)
  if (filterStatus.value === 'obtained') list = list.filter(t => t.obtained)
  else if (filterStatus.value === 'locked') list = list.filter(t => !t.obtained)
  if (keyword.value.trim()) {
    const k = keyword.value.trim().toLowerCase()
    list = list.filter(t =>
      String(t.tagName || '').toLowerCase().includes(k) ||
      String(t.description || '').toLowerCase().includes(k)
    )
  }
  return list
})

/* 数据加载 */
const loadAll = async () => {
  loading.value = true
  try {
    const [mine, all] = await Promise.all([
      request.get('/api/honor/student/tags'),
      request.get('/api/honor/tag/list')
    ])
    if (mine.code === '200') mineTags.value = mine.data || []
    if (all.code === '200') allTags.value = all.data || []
    ElMessage.success('已加载并自动重算标签')
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.honor-tag-panel { display: flex; flex-direction: column; gap: 18px; }

/* 头部 */
.panel-head {
  display: flex; justify-content: space-between; align-items: center;
  background: rgba(255,255,255,0.55); padding: 14px 18px;
  border-radius: 14px; border: 1px solid rgba(255,255,255,0.7);
}
.head-title { display: flex; align-items: center; gap: 12px; }
.head-icon { font-size: 22px; color: #6366f1; background: rgba(99,102,241,0.1); padding: 8px; border-radius: 10px; }
.head-title h3 { margin: 0; font-size: 17px; color: #1e293b; }
.head-tip { font-size: 12px; color: #94a3b8; }

/* 统计 */
.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.stat-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px !important; border-radius: 16px !important;
  background: rgba(255,255,255,0.68) !important;
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.8) !important;
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px;
}
.stat-body { display: flex; flex-direction: column; }
.stat-label { font-size: 13px; color: #64748b; }
.stat-value { font-size: 20px; font-weight: 700; color: #1e293b; }
.stat-tip { font-size: 11px; color: #94a3b8; }

/* 进度 */
.progress-card {
  padding: 16px 18px !important; border-radius: 16px !important;
  background: rgba(255,255,255,0.68) !important;
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.8) !important;
}
.progress-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.progress-title { font-weight: 700; color: #1e293b; font-size: 14px; }
.progress-meta { font-size: 13px; color: #64748b; }
.progress-bar { margin: 6px 0 16px; }

.type-chips { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.type-chip {
  background: rgba(248,250,252,0.9); padding: 10px 14px; border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.chip-name { font-size: 13px; color: #475569; font-weight: 600; }
.chip-progress { display: block; font-size: 12px; color: #94a3b8; margin: 4px 0; }
.chip-progress strong { color: #1e293b; font-size: 14px; }
.chip-track { height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden; }
.chip-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }
.chip-award_level .chip-fill { background: linear-gradient(90deg,#ef4444,#f97316); }
.chip-competition_direction .chip-fill { background: linear-gradient(90deg,#6366f1,#8b5cf6); }
.chip-role .chip-fill { background: linear-gradient(90deg,#10b981,#14b8a6); }
.chip-ability .chip-fill { background: linear-gradient(90deg,#3b82f6,#06b6d4); }
.chip-growth .chip-fill { background: linear-gradient(90deg,#0ea5e9,#6366f1); }

/* 筛选 */
.filter-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.filter-left { display: flex; gap: 10px; flex-wrap: wrap; }
.status-radio { margin-left: 4px; }
.filter-right { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

/* 标签网格 */
.tag-grid-wrap { min-height: 120px; }
.tag-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}
.tag-card {
  position: relative; padding: 18px; border-radius: 16px;
  background: rgba(255,255,255,0.75);
  border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 4px 16px rgba(99,102,241,0.05);
  transition: transform 0.3s, box-shadow 0.3s;
  display: flex; flex-direction: column; gap: 10px;
}
/* 悬停略微放大 + 阴影（同勋章墙卡片效果） */
.tag-card:hover { transform: translateY(-4px) scale(1.03); box-shadow: 0 16px 28px rgba(99,102,241,0.18); }
.tag-card.locked { opacity: 0.55; filter: grayscale(0.6); }
.tag-card.locked:hover { opacity: 0.85; filter: grayscale(0.3); }
.tag-card.obtained { background: linear-gradient(135deg, rgba(99,102,241,0.04), rgba(139,92,246,0.04)); border-color: rgba(99,102,241,0.25); }

.tag-icon-wrap {
  position: relative; width: 56px; height: 56px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
}
.type-award_level .tag-icon-wrap { background: linear-gradient(135deg,#ef4444,#f97316); }
.type-competition_direction .tag-icon-wrap { background: linear-gradient(135deg,#6366f1,#8b5cf6); }
.type-role .tag-icon-wrap { background: linear-gradient(135deg,#10b981,#14b8a6); }
.type-ability .tag-icon-wrap { background: linear-gradient(135deg,#3b82f6,#06b6d4); }
.type-growth .tag-icon-wrap { background: linear-gradient(135deg,#0ea5e9,#6366f1); }
.tag-icon { color: #fff; }
.lock-icon, .check-icon {
  position: absolute; right: -4px; bottom: -4px;
  background: #fff; border-radius: 50%; padding: 3px;
  font-size: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}
.lock-icon { color: #94a3b8; }
.check-icon { color: #10b981; }
.tag-name { font-size: 15px; font-weight: 700; color: #1e293b; }
.tag-type-badge .el-tag { font-weight: 600; }
.tag-desc { font-size: 13px; color: #64748b; line-height: 1.5; min-height: 40px; }
.tag-foot {
  margin-top: auto; display: flex; align-items: center; gap: 6px;
  font-size: 12px; padding-top: 8px; border-top: 1px dashed #e2e8f0;
}
.foot-obtained { color: #10b981; display: flex; align-items: center; gap: 4px; font-weight: 600; }
.foot-locked { color: #94a3b8; display: flex; align-items: center; gap: 4px; }
</style>