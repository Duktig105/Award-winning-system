<template>
  <div class="score-log-panel">
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

    <!-- 来源分析：级别分布 + 月度趋势 -->
    <div class="analysis-row">
      <div class="analysis-card glass-card">
        <div class="analysis-head">
          <el-icon><PieChart /></el-icon>
          <h4>积分来源（按竞赛级别）</h4>
        </div>
        <div v-if="sourceByLevel.length > 0" class="source-bars">
          <div v-for="b in sourceByLevel" :key="b.key" class="source-bar-row">
            <div class="source-bar-name">{{ b.key }}</div>
            <div class="source-bar-track">
              <div class="source-bar-fill" :style="{ width: b.percent + '%', background: b.color }"></div>
              <span class="source-bar-text" :class="{ inside: b.percent > 35 }">+{{ b.value }}</span>
            </div>
            <div class="source-bar-meta">{{ b.count }} 次 · {{ b.percent }}%</div>
          </div>
        </div>
        <el-empty v-else description="暂无获奖积分来源" :image-size="80" />
      </div>

      <div class="analysis-card glass-card">
        <div class="analysis-head">
          <el-icon><DataLine /></el-icon>
          <h4>月度积分趋势（近 6 个月）</h4>
        </div>
        <div v-if="trendTotal > 0" class="trend-chart">
          <div v-for="m in monthlyTrend" :key="m.key" class="trend-col">
            <div class="trend-bar-wrap" :title="`${m.label}: +${m.value}`">
              <div class="trend-bar" :style="{ height: (trendTotal ? Math.max(4, (m.value / trendMax) * 100) : 0) + '%' }"></div>
              <span class="trend-tip">+{{ m.value }}</span>
            </div>
            <div class="trend-label">{{ m.label }}</div>
          </div>
        </div>
        <el-empty v-else description="近 6 个月暂无积分" :image-size="80" />
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-row">
      <el-radio-group v-model="filterType" size="default">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="award">获奖积分</el-radio-button>
        <el-radio-button value="recalc">重算积分</el-radio-button>
      </el-radio-group>
      <div class="filter-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          unlink-panels
          size="default"
          style="width: 260px;"
        />
        <el-input
          v-model="keyword"
          placeholder="搜索描述或竞赛名"
          clearable
          :prefix-icon="Search"
          style="width: 200px;"
        />
        <el-button :icon="Refresh" round @click="loadLogs">刷新</el-button>
      </div>
    </div>

    <!-- 明细表 -->
    <div class="table-wrap">
      <el-table :data="pagedLogs" stripe v-loading="loading" class="premium-table">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="积分" width="100" align="center">
          <template #default="{ row }">
            <span class="score-cell" :class="{ minus: row.score < 0 }">{{ row.score > 0 ? '+' : '' }}{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.scoreType] || 'info'" round size="small">{{ typeText(row.scoreType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="竞赛级别" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.competitionLevel" size="small" round effect="plain">{{ row.competitionLevel }}</el-tag>
            <span v-else class="empty-tip">-</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="220">
          <template #default="{ row }">
            <div class="desc-cell">
              <span class="desc-main">{{ row.description }}</span>
              <span v-if="row.competitionName" class="desc-extra">来自竞赛：{{ row.competitionName }} · {{ row.awardRank }}{{ row.awardLevel ? ' · ' + row.awardLevel : '' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="规则快照" width="240">
          <template #default="{ row }">
            <span class="rule-snapshot" v-if="row.ruleSnapshot">{{ row.ruleSnapshot }}</span>
            <span v-else class="empty-tip">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-foot">
        <span class="foot-tip">
          共 <strong>{{ filteredLogs.length }}</strong> 条，本页显示 <strong>{{ pagedLogs.length }}</strong> 条
          · 合计 <strong class="score-cell" :class="{ minus: filteredSum < 0 }">{{ filteredSum > 0 ? '+' : '' }}{{ filteredSum }}</strong> 分
        </span>
        <el-pagination
          background
          layout="sizes, prev, pager, next, jumper"
          :total="filteredLogs.length"
          :page-size="pageSize"
          :current-page="page"
          :page-sizes="[10, 20, 50]"
          @current-change="page = $event"
          @size-change="(p) => { pageSize = p; page = 1 }"
        />
      </div>
    </div>

    <el-empty v-if="!loading && logs.length === 0" description="暂无积分明细" :image-size="100" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Refresh, Search, DataLine, PieChart, Wallet, Trophy, RefreshRight, Coin
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({ initialFilter: { type: String, default: 'all' } })

const logs = ref([])
const loading = ref(false)
const filterType = ref(props.initialFilter || 'all')
const dateRange = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)

const typeMap = { award: 'success', recalc: 'warning', manual: 'info', deduct: 'danger' }
const typeText = (t) => ({ award: '获奖积分', recalc: '重算积分', manual: '手动', deduct: '扣减' }[t] || t)

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/* 顶部统计 */
const stats = computed(() => {
  const total = logs.value.reduce((s, l) => s + Number(l.score || 0), 0)
  const awardScore = logs.value.filter(l => l.scoreType === 'award').reduce((s, l) => s + Number(l.score || 0), 0)
  const recalcScore = logs.value.filter(l => l.scoreType === 'recalc').reduce((s, l) => s + Number(l.score || 0), 0)
  const now = new Date()
  const ymKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const monthScore = logs.value
    .filter(l => String(l.createTime || '').slice(0, 7) === ymKey)
    .reduce((s, l) => s + Number(l.score || 0), 0)
  return [
    { key: 'total',   label: '累计积分', value: total,                  tip: '全部明细汇总',   icon: Wallet,     color: 'linear-gradient(135deg,#6366f1,#8b5cf6)' },
    { key: 'award',   label: '获奖积分', value: awardScore,              tip: '来自获奖的累积',   icon: Trophy,     color: 'linear-gradient(135deg,#10b981,#14b8a6)' },
    { key: 'recalc',  label: '重算积分', value: recalcScore,             tip: '批量重算补发',   icon: RefreshRight,color: 'linear-gradient(135deg,#f59e0b,#f97316)' },
    { key: 'month',   label: '本月新增', value: monthScore,              tip: `${ymKey} 期间`, icon: Coin,       color: 'linear-gradient(135deg,#ec4899,#f97316)' }
  ]
})

/* 竞赛级别分布（仅获奖） */
const levelColors = {
  '国家级': 'linear-gradient(90deg,#ef4444,#f97316)',
  '省级':   'linear-gradient(90deg,#6366f1,#8b5cf6)',
  '校级':   'linear-gradient(90deg,#10b981,#14b8a6)',
  '院级':   'linear-gradient(90deg,#0ea5e9,#22d3ee)',
  '其他':   'linear-gradient(90deg,#94a3b8,#cbd5e1)'
}
const sourceByLevel = computed(() => {
  const groups = new Map()
  for (const l of logs.value) {
    if (l.scoreType !== 'award') continue
    const lvl = l.competitionLevel || '其他'
    const g = groups.get(lvl) || { key: lvl, value: 0, count: 0 }
    g.value += Number(l.score || 0)
    g.count += 1
    groups.set(lvl, g)
  }
  const arr = Array.from(groups.values()).sort((a, b) => b.value - a.value)
  const total = arr.reduce((s, x) => s + x.value, 0)
  return arr.map(x => ({ ...x, percent: total ? Math.round((x.value / total) * 100) : 0, color: levelColors[x.key] || levelColors['其他'] }))
})

/* 月度趋势（近 6 个月，含本月） */
const monthlyTrend = computed(() => {
  const buckets = []
  const now = new Date()
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    buckets.push({ key, label: `${d.getMonth() + 1}月`, value: 0 })
  }
  for (const l of logs.value) {
    const key = String(l.createTime || '').slice(0, 7)
    const b = buckets.find(x => x.key === key)
    if (b) b.value += Number(l.score || 0)
  }
  return buckets
})
const trendTotal = computed(() => monthlyTrend.value.reduce((s, x) => s + x.value, 0))
const trendMax = computed(() => Math.max(1, ...monthlyTrend.value.map(x => x.value)))

/* 过滤 + 分页 */
const filteredLogs = computed(() => {
  let list = logs.value
  if (filterType.value !== 'all') list = list.filter(l => l.scoreType === filterType.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [s, e] = dateRange.value
    list = list.filter(l => {
      const d = String(l.createTime || '').slice(0, 10)
      return d >= s && d <= e
    })
  }
  if (keyword.value.trim()) {
    const k = keyword.value.trim().toLowerCase()
    list = list.filter(l =>
      String(l.description || '').toLowerCase().includes(k) ||
      String(l.competitionName || '').toLowerCase().includes(k)
    )
  }
  return list
})
const filteredSum = computed(() => filteredLogs.value.reduce((s, l) => s + Number(l.score || 0), 0))
const pagedLogs = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredLogs.value.slice(start, start + pageSize.value)
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/honor/student/score/logs', { params: { limit: 500 } })
    if (res.code === '200') {
      logs.value = res.data || []
      page.value = 1
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.score-log-panel { display: flex; flex-direction: column; gap: 18px; }

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

/* 分析 */
.analysis-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 14px; }
.analysis-card {
  padding: 16px 18px !important; border-radius: 16px !important;
  background: rgba(255,255,255,0.68) !important;
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.8) !important;
}
.analysis-head { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.analysis-head .el-icon { color: #6366f1; font-size: 18px; }
.analysis-head h4 { margin: 0; font-size: 14px; font-weight: 700; color: #1e293b; }

/* 来源条 */
.source-bars { display: flex; flex-direction: column; gap: 10px; }
.source-bar-row { display: grid; grid-template-columns: 64px 1fr 100px; align-items: center; gap: 8px; }
.source-bar-name { font-size: 13px; color: #475569; font-weight: 600; }
.source-bar-track {
  position: relative; height: 22px; background: #f1f5f9; border-radius: 6px; overflow: hidden;
}
.source-bar-fill {
  height: 100%; border-radius: 6px; min-width: 4px;
  transition: width 0.5s;
}
.source-bar-text {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: 12px; font-weight: 700; color: #475569; z-index: 1;
}
.source-bar-text.inside { color: #fff; }
.source-bar-meta { font-size: 12px; color: #94a3b8; text-align: right; }

/* 月度趋势 */
.trend-chart {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; height: 180px;
  align-items: end; padding-top: 10px;
}
.trend-col { display: flex; flex-direction: column; align-items: center; gap: 6px; height: 100%; }
.trend-bar-wrap {
  position: relative; flex: 1; width: 100%;
  display: flex; align-items: flex-end; justify-content: center;
}
.trend-bar {
  width: 70%; border-radius: 8px 8px 4px 4px;
  background: linear-gradient(180deg, #6366f1, #8b5cf6);
  box-shadow: 0 -2px 8px rgba(99,102,241,0.2);
  min-height: 4px; transition: height 0.5s;
}
.trend-tip {
  position: absolute; top: -22px; left: 50%; transform: translateX(-50%);
  font-size: 11px; color: #475569; font-weight: 700; white-space: nowrap;
  background: rgba(255,255,255,0.9); padding: 2px 6px; border-radius: 6px; border: 1px solid #e2e8f0;
}
.trend-label { font-size: 12px; color: #64748b; }

/* 过滤 */
.filter-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.filter-right { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

/* 表格 */
.table-wrap {
  background: rgba(255,255,255,0.55); border-radius: 14px;
  padding: 12px 14px; border: 1px solid rgba(255,255,255,0.7);
}
.premium-table { background: transparent !important; border-radius: 12px; overflow: hidden; }
.premium-table :deep(.el-table__row) { background: rgba(255,255,255,0.55) !important; }
.premium-table :deep(.el-table__row:hover) { background: rgba(99, 102, 241, 0.06) !important; }

.score-cell { font-weight: 700; color: #10b981; font-size: 15px; }
.score-cell.minus { color: #ef4444; }
.rule-snapshot { color: #64748b; font-size: 12px; }
.empty-tip { color: #cbd5e1; }

.desc-cell { display: flex; flex-direction: column; gap: 3px; }
.desc-main { font-size: 13px; color: #1e293b; }
.desc-extra { font-size: 12px; color: #94a3b8; }

.table-foot {
  margin-top: 14px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;
}
.foot-tip { font-size: 13px; color: #64748b; }
.foot-tip strong { color: #1e293b; }
</style>