<template>
  <div class="statistics-wrapper">
    <NavBar />
    <div class="statistics-page">
      <div class="content-container">
        <el-card class="statistics-card">
          <template #header>
            <!-- 页面标题 -->
            <div class="page-header">
              <div class="header-left">
                <h2 class="page-title">
                  <el-icon class="title-icon"><DataAnalysis /></el-icon>
                  数据统计
                </h2>
                <p class="page-subtitle">获奖申请数据可视化分析</p>
              </div>
              <div class="header-right">
                <div class="update-badge" :title="lastUpdateTime ? '数据于 ' + lastUpdateTime + ' 刷新' : '未加载'">
                  <el-icon><Clock /></el-icon>
                  <span>{{ lastUpdateTime ? lastUpdateTime + ' 更新' : '加载中...' }}</span>
                </div>
                <el-button class="refresh-btn" :disabled="loading" @click="loadData">
                  <el-icon class="refresh-icon" :class="{ 'is-spinning': loading }"><Refresh /></el-icon>
                  {{ loading ? '刷新中...' : '刷新' }}
                </el-button>
              </div>
            </div>
          </template>

          <!-- 加载状态 -->
          <div v-if="loading" class="loading-overlay">
            <el-icon class="loading-icon is-loading"><Loading /></el-icon>
            <span>数据加载中...</span>
          </div>

    <template v-else>
      <!-- ① KPI 数字卡片行 -->
      <div class="kpi-grid">
        <div class="kpi-card kpi-total">
          <div class="kpi-icon-wrap">
            <el-icon><Document /></el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">{{ kpi.totalCount ?? 0 }}</div>
            <div class="kpi-label">申请总数</div>
          </div>
          <div class="kpi-badge">总计</div>
        </div>

        <div class="kpi-card kpi-approved">
          <div class="kpi-icon-wrap">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">{{ kpi.approvedCount ?? 0 }}</div>
            <div class="kpi-label">已通过</div>
          </div>
          <div class="kpi-badge">审核通过</div>
        </div>

        <div class="kpi-card kpi-pending">
          <div class="kpi-icon-wrap">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">{{ kpi.pendingCount ?? 0 }}</div>
            <div class="kpi-label">审核中</div>
          </div>
          <div class="kpi-badge">待审核</div>
        </div>

        <div class="kpi-card kpi-returned">
          <div class="kpi-icon-wrap">
            <el-icon><RefreshLeft /></el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">{{ kpi.returnedCount ?? 0 }}</div>
            <div class="kpi-label">已退回</div>
          </div>
          <div class="kpi-badge">退回修改</div>
        </div>
      </div>

      <!-- ② 第一行图表：获奖等次 + 竞赛级别 -->
      <div class="chart-row chart-row-2col">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">🏅 获奖等次分布</span>
            <span class="chart-subtitle">已通过申请</span>
          </div>
          <div class="chart-placeholder">
            <div ref="chartAwardRank" class="chart-canvas"></div>
            <div v-if="emptyAwardRank" class="chart-empty">
              <span class="empty-icon">📊</span>
              <span class="empty-text">暂无获奖等次数据</span>
              <span class="empty-hint">当有申请通过审核后自动统计</span>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">🌐 竞赛级别分布</span>
            <span class="chart-subtitle">已通过申请</span>
          </div>
          <div class="chart-placeholder">
            <div ref="chartCompetitionLevel" class="chart-canvas"></div>
            <div v-if="emptyCompetitionLevel" class="chart-empty">
              <span class="empty-icon">🌐</span>
              <span class="empty-text">暂无竞赛级别数据</span>
              <span class="empty-hint">当有申请通过审核后自动统计</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ③ 第二行图表：申请状态 + Top10热门竞赛 -->
      <div class="chart-row chart-row-2col">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">📋 申请状态分布</span>
            <span class="chart-subtitle">全部申请</span>
          </div>
          <div class="chart-placeholder">
            <div ref="chartStatus" class="chart-canvas"></div>
            <div v-if="emptyStatus" class="chart-empty">
              <span class="empty-icon">📋</span>
              <span class="empty-text">暂无申请数据</span>
              <span class="empty-hint">当学生提交申请后自动统计</span>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">🔥 Top 10 热门竞赛</span>
            <span class="chart-subtitle">按申请次数排名</span>
          </div>
          <div class="chart-placeholder" :style="{ height: top10ChartHeight + 'px' }">
            <div ref="chartTop10" class="chart-canvas"></div>
            <div v-if="emptyTop10" class="chart-empty">
              <span class="empty-icon">🔥</span>
              <span class="empty-text">暂无竞赛申请数据</span>
              <span class="empty-hint">当学生提交申请后自动统计</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ④ 第三行图表：各年级获奖人数 + 月度申请趋势 -->
      <div class="chart-row chart-row-2col">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">🎓 各年级获奖人数对比</span>
            <span class="chart-subtitle">已通过申请 · 按年级统计</span>
          </div>
          <div class="chart-placeholder" :style="{ height: gradeChartHeight + 'px' }">
            <div ref="chartGrade" class="chart-canvas"></div>
            <div v-if="emptyGrade" class="chart-empty">
              <span class="empty-icon">🎓</span>
              <span class="empty-text">暂无年级获奖数据</span>
              <span class="empty-hint">当有学生申请通过审核后自动统计</span>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">📈 月度申请趋势</span>
            <span class="chart-subtitle">近12个月 · 申请量与通过量</span>
          </div>
          <div class="chart-placeholder chart-placeholder-tall">
            <div ref="chartMonthly" class="chart-canvas"></div>
            <div v-if="emptyMonthly" class="chart-empty">
              <span class="empty-icon">📈</span>
              <span class="empty-text">近12个月暂无申请数据</span>
              <span class="empty-hint">当学生提交申请后自动显示趋势</span>
            </div>
          </div>
        </div>
      </div>
          </template>
        </el-card>
      </div><!-- /content-container -->
    </div><!-- /statistics-page -->
  </div><!-- /statistics-wrapper -->
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import NavBar from '@/components/NavBar.vue'

const loading = ref(true)
const kpi = ref({})
const lastUpdateTime = ref('')

function formatTime(date = new Date()) {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
}

// 图表 DOM refs（后续挂载 ECharts 用）
const chartAwardRank       = ref(null)
const chartCompetitionLevel= ref(null)
const chartGrade           = ref(null)
const chartStatus          = ref(null)
const chartTop10           = ref(null)
const chartMonthly         = ref(null)

// 原始数据（后续逐步用来渲染图表）
const rawData = ref({})

// ========== 各图表空状态标志 ==========
const emptyAwardRank        = ref(false)
const emptyCompetitionLevel = ref(false)
const emptyGrade            = ref(false)
const emptyStatus           = ref(false)
const emptyTop10            = ref(false)
const emptyMonthly          = ref(false)

// 自适应高度（年级图/Top10图）
const gradeChartHeight = ref(280)
const top10ChartHeight = ref(280)

// ========== 图表实例 ==========
let chartInstanceAwardRank = null
let chartInstanceCompetitionLevel = null
let chartInstanceGrade = null
let chartInstanceStatus = null
let chartInstanceTop10 = null
let chartInstanceMonthly = null

// ========== 图表1：获奖等次环形图 ==========
function initChartAwardRank(data) {
  emptyAwardRank.value = !data || data.length === 0
  if (!chartAwardRank.value) return
  if (chartInstanceAwardRank) chartInstanceAwardRank.dispose()
  chartInstanceAwardRank = echarts.init(chartAwardRank.value)

  const RANK_MAP = { A: 'A等（最高）', B: 'B等', C: 'C等', D: 'D等' }
  
  // 极尽丰富、区分度高的 12 色阶渐变高级色盘
  const COLORS = [
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#a855f7' }]), // 1. 经典紫蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#f43f5e' }, { offset: 1, color: '#fb923c' }]), // 2. 珊瑚橙红 (高对比暖色)
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }]), // 3. 翡翠薄荷 (清透绿)
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#0ea5e9' }, { offset: 1, color: '#38bdf8' }]), // 4. 塞班天蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#f59e0b' }, { offset: 1, color: '#fbbf24' }]), // 5. 琥珀亮黄
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#ec4899' }, { offset: 1, color: '#f472b6' }]), // 6. 蜜桃魅粉
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#8b5cf6' }, { offset: 1, color: '#d946ef' }]), // 7. 赛博紫霓虹
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#14b8a6' }, { offset: 1, color: '#2dd4bf' }]), // 8. 蒂芙尼青蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#ef4444' }, { offset: 1, color: '#f87171' }]), // 9. 活力火红
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#60a5fa' }]), // 10. 海洋深蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#84cc16' }, { offset: 1, color: '#a3e635' }]), // 11. 青柠草绿
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#64748b' }, { offset: 1, color: '#94a3b8' }])  // 12. 质感银灰 (托底色)
  ]

  const seriesData = (data || []).map((item, i) => ({
    name: RANK_MAP[item.rank] || item.rank,
    value: item.count,
    itemStyle: { color: COLORS[i % COLORS.length] }
  }))

  const total = seriesData.reduce((s, d) => s + Number(d.value), 0)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 项 ({d}%)',
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: 'rgba(99,102,241,0.2)',
      textStyle: { color: '#1e293b', fontSize: 13 }
    },
    legend: {
      bottom: '4%',
      left: 'center',
      orient: 'horizontal',
      textStyle: { color: '#64748b', fontSize: 12 }
    },
    graphic: [{
      type: 'text',
      left: 'center',
      top: '38%',
      style: {
        text: total.toString(),
        textAlign: 'center',
        fill: '#1e293b',
        fontSize: 28,
        fontWeight: 'bold'
      }
    }, {
      type: 'text',
      left: 'center',
      top: '50%',
      style: {
        text: '获奖总数',
        textAlign: 'center',
        fill: '#94a3b8',
        fontSize: 12
      }
    }],
    series: [{
      type: 'pie',
      radius: ['50%', '75%'], // 稍微变薄
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { 
        borderRadius: 10,  // 更平滑的圆角
        borderColor: '#fff', 
        borderWidth: 4    // 更宽的缝隙呼吸感
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#1e293b' },
        itemStyle: { 
          shadowBlur: 20, 
          shadowColor: 'rgba(99,102,241,0.4)',
          transform: 'scale(1.05)'
        }
      },
      data: seriesData.length ? seriesData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#f1f5f9' } }]
    }]
  }
  chartInstanceAwardRank.setOption(option)
  window.addEventListener('resize', () => chartInstanceAwardRank?.resize())
}

// ========== 图表2：竞赛级别分布玫瑰图 ==========
function initChartCompetitionLevel(data) {
  emptyCompetitionLevel.value = !data || data.length === 0
  if (!chartCompetitionLevel.value) return
  if (chartInstanceCompetitionLevel) chartInstanceCompetitionLevel.dispose()
  chartInstanceCompetitionLevel = echarts.init(chartCompetitionLevel.value)

  // 同样引用的极尽丰富、区分度高的 12 色阶渐变高级色盘
  const COLORS = [
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }]), // 1. 起手翡翠薄荷 (与第一个图错开首色)
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#a855f7' }]), // 2. 经典紫蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#f59e0b' }, { offset: 1, color: '#fbbf24' }]), // 3. 琥珀亮黄
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#f43f5e' }, { offset: 1, color: '#fb923c' }]), // 4. 珊瑚橙红
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#0ea5e9' }, { offset: 1, color: '#38bdf8' }]), // 5. 塞班天蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#ec4899' }, { offset: 1, color: '#f472b6' }]), // 6. 蜜桃魅粉
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#14b8a6' }, { offset: 1, color: '#2dd4bf' }]), // 7. 蒂芙尼青蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#8b5cf6' }, { offset: 1, color: '#d946ef' }]), // 8. 赛博紫霓虹
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#ef4444' }, { offset: 1, color: '#f87171' }]), // 9. 活力火红
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#60a5fa' }]), // 10. 海洋深蓝
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#84cc16' }, { offset: 1, color: '#a3e635' }]), // 11. 青柠草绿
    new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#64748b' }, { offset: 1, color: '#94a3b8' }])  // 12. 质感银灰
  ]

  const seriesData = (data || []).map((item, i) => ({
    name: item.level,
    value: item.count,
    itemStyle: { color: COLORS[i % COLORS.length] }
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 项 ({d}%)',
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: 'rgba(99,102,241,0.2)',
      textStyle: { color: '#1e293b', fontSize: 13 }
    },
    legend: {
      bottom: '4%',
      left: 'center',
      orient: 'horizontal',
      textStyle: { color: '#64748b', fontSize: 11 },
      itemWidth: 10,
      itemHeight: 10
    },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'], // 调整半径制造空灵感
      center: ['50%', '46%'],
      itemStyle: { 
        borderRadius: 10, // 柔和宽大的圆角
        borderColor: '#fff', 
        borderWidth: 4 
      },
      label: {
        show: true,
        position: 'outside',
        formatter: '{b}\n{c} 项',
        color: '#475569',
        fontSize: 13,
        fontWeight: 'bold',
        lineHeight: 20
      },
      labelLine: { 
        show: true, 
        length: 15, 
        length2: 12, 
        smooth: 0.2, // 曲线化线条
        lineStyle: { width: 1.5, type: 'dashed' } // 使用虚线牵引提升科技感
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: '900',
          color: '#1e293b'
        },
        itemStyle: { 
          shadowBlur: 20, 
          shadowColor: 'rgba(99,102,241,0.4)',
          transform: 'scale(1.05)'
        }
      },
      data: seriesData.length ? seriesData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#f1f5f9' } }]
    }]
  }
  chartInstanceCompetitionLevel.setOption(option)
  window.addEventListener('resize', () => chartInstanceCompetitionLevel?.resize())
}

// ========== 图表3：各年级获奖人数柱状图 ==========
function initChartGrade(data) {
  emptyGrade.value = !data || data.length === 0
  // 自适应高度：每条数据 68px，最小 200px，最大 480px
  gradeChartHeight.value = Math.min(480, Math.max(200, (data?.length || 1) * 68 + 60))
  if (!chartGrade.value) return
  if (chartInstanceGrade) chartInstanceGrade.dispose()
  chartInstanceGrade = echarts.init(chartGrade.value)

  const sorted = [...(data || [])].sort((a, b) => String(a.grade).localeCompare(String(b.grade)))
  // 增加容错判断：如果数据库本身的 grade 数据已经包含了“级”字，则不再重复追加
  const grades = sorted.map(d => {
    const g = String(d.grade)
    return g.endsWith('级') ? g : g + '级'
  })
  const counts = sorted.map(d => Number(d.count))

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: 'rgba(99,102,241,0.2)',
      textStyle: { color: '#1e293b', fontSize: 13 },
      formatter: (params) => `${params[0].name}：<b>${params[0].value}</b> 人`
    },
    grid: { left: 60, right: 30, top: 40, bottom: 40 }, // 增大 top，防止柱子上方文字标签被截断
    xAxis: {
      type: 'category',
      data: grades.length ? grades : ['暂无数据'],
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 13, fontWeight: 500 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#94a3b8', fontSize: 12 },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: counts.length ? counts : [0],
      barMaxWidth: 36, // 将柱子加厚增强体积感
      label: { show: true, position: 'top', color: '#6366f1', fontWeight: 700, fontSize: 14, distance: 10 },
      itemStyle: {
        borderRadius: [8, 8, 0, 0], // 底部为直角接壤地平线，仅顶部大圆角
        color: (p) => { 
          const BAR_COLORS = [
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#f43f5e' }, { offset: 1, color: '#fb923c' }]), // 热烈红橙
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#a855f7' }]), // 经典紫蓝
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }]), // 翡翠薄荷
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#0ea5e9' }, { offset: 1, color: '#38bdf8' }]), // 阳光天蓝
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#f59e0b' }, { offset: 1, color: '#fbbf24' }]), // 琥珀亮黄
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#ec4899' }, { offset: 1, color: '#f472b6' }]), // 蜜桃魅粉
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#14b8a6' }, { offset: 1, color: '#2dd4bf' }]), // 蒂芙尼青
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#8b5cf6' }, { offset: 1, color: '#d946ef' }]), // 赛博紫粉
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#ef4444' }, { offset: 1, color: '#f87171' }]), // 活力火红
            new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#60a5fa' }])  // 海洋深蓝
          ]
          return BAR_COLORS[p.dataIndex % BAR_COLORS.length]
        }
      },
      emphasis: { 
        itemStyle: { 
          shadowBlur: 16, 
          shadowColor: 'rgba(99,102,241,0.4)',
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#a855f7' },
            { offset: 1, color: '#60a5fa' }
          ])
        } 
      }
    }]
  }
  chartInstanceGrade.setOption(option)
  window.addEventListener('resize', () => chartInstanceGrade?.resize())
}

// ========== 图表4：申请状态分布环形图 ==========
function initChartStatus(data) {
  emptyStatus.value = !data || data.length === 0
  if (!chartStatus.value) return
  if (chartInstanceStatus) chartInstanceStatus.dispose()
  chartInstanceStatus = echarts.init(chartStatus.value)

  const STATUS_MAP = {
    pending:   { label: '待审核', color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#f59e0b' }, { offset: 1, color: '#fbbf24' }]) }, // 琥珀黄
    approved:  { label: '已通过', color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }]) }, // 翡翠绿
    rejected:  { label: '已拒绝', color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#ef4444' }, { offset: 1, color: '#f87171' }]) }, // 危险红
    returned:  { label: '已退回', color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#8b5cf6' }]) }, // 柔和紫
    withdrawn: { label: '已撤回', color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#64748b' }, { offset: 1, color: '#94a3b8' }]) }  // 极客银灰
  }

  const seriesData = (data || []).map(item => ({
    name: STATUS_MAP[item.status]?.label || item.status,
    value: item.count,
    itemStyle: { color: STATUS_MAP[item.status]?.color || '#c4b5fd' }
  }))

  const total = seriesData.reduce((s, d) => s + Number(d.value), 0)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 项 ({d}%)',
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: 'rgba(99,102,241,0.2)',
      textStyle: { color: '#1e293b', fontSize: 13 }
    },
    legend: {
      bottom: '4%', left: 'center', orient: 'horizontal',
      textStyle: { color: '#64748b', fontSize: 12 }
    },
    graphic: [
      { type:'text', left:'center', top:'38%', style:{ text: total.toString(), textAlign:'center', fill:'#1e293b', fontSize:28, fontWeight:'bold' } },
      { type:'text', left:'center', top:'50%', style:{ text:'总申请数', textAlign:'center', fill:'#94a3b8', fontSize:12 } }
    ],
    series: [{
      type: 'pie',
      radius: ['50%', '75%'], // 加大半径强化呼吸感
      center: ['50%', '44%'],
      avoidLabelOverlap: false,
      itemStyle: { 
        borderRadius: 10,   // 大圆角
        borderColor: '#fff', 
        borderWidth: 4     // 更宽的缝隙呼吸感
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 15, fontWeight: 'bold', color: '#1e293b' },
        itemStyle: { 
          shadowBlur: 20, 
          shadowColor: 'rgba(0,0,0,0.15)', // 通用弥散阴影 
          transform: 'scale(1.05)'
        }
      },
      data: seriesData.length ? seriesData : [{ name:'暂无数据', value:1, itemStyle:{ color:'#f1f5f9' } }]
    }]
  }
  chartInstanceStatus.setOption(option)
  window.addEventListener('resize', () => chartInstanceStatus?.resize())
}

// ========== 图表5： Top10 热门竞赛横向条形图 ==========
function initChartTop10(data) {
  emptyTop10.value = !data || data.length === 0
  // 自适应高度：每条 40px，最小 200px，最大 520px
  top10ChartHeight.value = Math.min(520, Math.max(200, (data?.length || 1) * 40 + 60))
  if (!chartTop10.value) return
  if (chartInstanceTop10) chartInstanceTop10.dispose()
  chartInstanceTop10 = echarts.init(chartTop10.value)

  const sorted = [...(data || [])].sort((a, b) => Number(a.count) - Number(b.count))
  const names = sorted.map(d => d.competitionName)
  const counts = sorted.map(d => Number(d.count))

  const truncate = (s, n = 12) => s && s.length > n ? s.slice(0, n) + '…' : s

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: 'rgba(99,102,241,0.2)',
      textStyle: { color: '#1e293b', fontSize: 12 },
      formatter: (p) => `${p[0].name}<br/>申请次数：<b>${p[0].value}</b>`
    },
    grid: { left: 8, right: 55, top: 8, bottom: 12, containLabel: true },
    xAxis: {
      type: 'value', minInterval: 1,
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: names.length ? names.map(n => truncate(n)) : ['暂无数据'],
      axisLabel: { color: '#475569', fontSize: 11 },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: counts.length ? counts : [0],
      barMaxWidth: 32, // 加厚横向条带体积
      label: { show: true, position: 'right', color: '#6366f1', fontWeight: 700, fontSize: 13, distance: 10 },
      itemStyle: {
        borderRadius: [0, 8, 8, 0], // 右侧流线圆角
        color: (p) => { // 根据排行单独上色
          const BAR_COLORS = [
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#f43f5e' }, { offset: 1, color: '#fb923c' }]), // 热烈红橙
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#a855f7' }]), // 经典紫蓝
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }]), // 翡翠薄荷
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#0ea5e9' }, { offset: 1, color: '#38bdf8' }]), // 阳光天蓝
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#f59e0b' }, { offset: 1, color: '#fbbf24' }]), // 琥珀亮黄
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#ec4899' }, { offset: 1, color: '#f472b6' }]), // 蜜桃魅粉
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#14b8a6' }, { offset: 1, color: '#2dd4bf' }]), // 蒂芙尼青
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#8b5cf6' }, { offset: 1, color: '#d946ef' }]), // 赛博紫粉
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#ef4444' }, { offset: 1, color: '#f87171' }]), // 活力火红
            new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#60a5fa' }])  // 海洋深蓝
          ]
          // 反向取色（让榜首，即上面的柱子最热烈），因为 ECharts bar 是从下往上画的
          return BAR_COLORS[(counts.length - 1 - p.dataIndex) % BAR_COLORS.length]
        }
      },
      emphasis: { 
        itemStyle: { 
          shadowBlur: 16, 
          shadowColor: 'rgba(59, 130, 246, 0.4)'
        } 
      }
    }]
  }
  chartInstanceTop10.setOption(option)
  window.addEventListener('resize', () => chartInstanceTop10?.resize())
}

// ========== 图表6：月度申请趋势折线图 ==========
function initChartMonthly(data) {
  emptyMonthly.value = !data || data.length === 0
  if (!chartMonthly.value) return
  if (chartInstanceMonthly) chartInstanceMonthly.dispose()
  chartInstanceMonthly = echarts.init(chartMonthly.value)

  const months = (data || []).map(d => d.month)
  const totals = (data || []).map(d => Number(d.totalCount))
  const approveds = (data || []).map(d => Number(d.approvedCount))

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderColor: 'rgba(99,102,241,0.2)',
      textStyle: { color: '#1e293b', fontSize: 13 }
    },
    legend: {
      top: 4, right: 12,
      textStyle: { color: '#64748b', fontSize: 12 },
       data: ['总申请数', '通过数']
    },
    grid: { left: 50, right: 20, top: 36, bottom: 30 },
    xAxis: {
      type: 'category',
      data: months.length ? months : [],
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 11, rotate: months.length > 8 ? 30 : 0 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#94a3b8', fontSize: 12 },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
    },
    series: [
      {
        name: '总申请数',
        type: 'line',
        data: totals,
        smooth: true,
        symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2.5, color: '#8b5cf6' },
        itemStyle: { color: '#8b5cf6' },
        areaStyle: {
          color: { type:'linear', x:0, y:0, x2:0, y2:1,
            colorStops: [
              { offset: 0, color: 'rgba(139,92,246,0.22)' },
              { offset: 1, color: 'rgba(139,92,246,0.01)' }
            ]
          }
        }
      },
      {
        name: '通过数',
        type: 'line',
        data: approveds,
        smooth: true,
        symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2.5, color: '#10b981' },
        itemStyle: { color: '#10b981' },
        areaStyle: {
          color: { type:'linear', x:0, y:0, x2:0, y2:1,
            colorStops: [
              { offset: 0, color: 'rgba(16,185,129,0.18)' },
              { offset: 1, color: 'rgba(16,185,129,0.01)' }
            ]
          }
        }
      }
    ]
  }
  chartInstanceMonthly.setOption(option)
  window.addEventListener('resize', () => chartInstanceMonthly?.resize())
}

async function loadData() {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:9998/api/statistics/overview')
    rawData.value = res.data
    kpi.value = res.data.kpi || {}
    lastUpdateTime.value = formatTime()
    // ⚠️ 必须先关闭 loading，让 v-else 内容渲染到 DOM，再初始化图表
    loading.value = false
    await nextTick()
    initChartAwardRank(res.data.awardRankStats)
    initChartCompetitionLevel(res.data.competitionLevelStats)
    initChartGrade(res.data.gradeAwardStats)
    initChartStatus(res.data.statusStats)
    initChartTop10(res.data.top10Competitions)
    initChartMonthly(res.data.monthlyTrend)
  } catch (e) {
    console.error('加载统计数据失败', e)
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* ===== 外层包裹（撑满全屏） ===== */
.statistics-wrapper {
  width: 100%;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ===== 页面容器（含 NavBar 偏移） ===== */
.statistics-page {
  flex: 1;
  box-sizing: border-box;
  color: #1e293b;
}

/* ===== 内容居中，限制最大宽度 ===== */
.content-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* ===== 外层毛玻璃大卡片 ===== */
.statistics-card {
  margin-top: 20px;
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  border-radius: 20px !important;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05) !important;
}


/* ===== 页面标题 ===== */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 4px 0;
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-icon {
  font-size: 24px;
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.update-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  height: 32px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08), rgba(99, 102, 241, 0.05));
  border: 1px solid rgba(139, 92, 246, 0.22);
  border-radius: 16px;
  color: #7c3aed;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 10px rgba(139, 92, 246, 0.04);
}
.update-badge .el-icon {
  font-size: 14px;
  opacity: 0.85;
  margin-top: -1px;
}


.refresh-btn {
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.25);
  color: #6366f1;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.refresh-btn:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.18);
  border-color: #6366f1;
  transform: scale(1.05);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.2);
}
.refresh-btn:active:not(:disabled) {
  transform: scale(0.96);
}

/* 旋转动画 */
@keyframes spin360 {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
.refresh-icon {
  transition: transform 0.2s ease;
}
.refresh-icon.is-spinning {
  animation: spin360 0.7s linear infinite;
}


/* ===== 加载状态 ===== */
.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 12px;
  color: #6366f1;
  font-size: 15px;
}
.loading-icon {
  font-size: 36px;
}

/* ===== KPI 卡片网格 ===== */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 20px 20px 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.1);
}

.kpi-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  flex-shrink: 0;
  font-size: 22px;
}

.kpi-value {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 4px;
}
.kpi-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.kpi-badge {
  position: absolute;
  top: 12px;
  right: 14px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 600;
  opacity: 0.7;
}

/* KPI 颜色主题 */
.kpi-total    { background: linear-gradient(135deg, rgba(99,102,241,0.1), rgba(139,92,246,0.08)); border-color: rgba(99,102,241,0.2); }
.kpi-total    .kpi-icon-wrap { background: rgba(99,102,241,0.15); color: #6366f1; }
.kpi-total    .kpi-value { color: #6366f1; }
.kpi-total    .kpi-badge { background: rgba(99,102,241,0.12); color: #6366f1; }

.kpi-approved { background: linear-gradient(135deg, rgba(34,197,94,0.1), rgba(16,185,129,0.06)); border-color: rgba(34,197,94,0.2); }
.kpi-approved .kpi-icon-wrap { background: rgba(34,197,94,0.15); color: #22c55e; }
.kpi-approved .kpi-value { color: #16a34a; }
.kpi-approved .kpi-badge { background: rgba(34,197,94,0.12); color: #16a34a; }

.kpi-pending  { background: linear-gradient(135deg, rgba(245,158,11,0.1), rgba(251,191,36,0.06)); border-color: rgba(245,158,11,0.2); }
.kpi-pending  .kpi-icon-wrap { background: rgba(245,158,11,0.15); color: #f59e0b; }
.kpi-pending  .kpi-value { color: #d97706; }
.kpi-pending  .kpi-badge { background: rgba(245,158,11,0.12); color: #d97706; }

.kpi-returned { background: linear-gradient(135deg, rgba(239,68,68,0.08), rgba(248,113,113,0.05)); border-color: rgba(239,68,68,0.18); }
.kpi-returned .kpi-icon-wrap { background: rgba(239,68,68,0.12); color: #ef4444; }
.kpi-returned .kpi-value { color: #dc2626; }
.kpi-returned .kpi-badge { background: rgba(239,68,68,0.1); color: #dc2626; }

/* ===== 图表行布局 ===== */
.chart-row {
  margin-bottom: 20px;
}

.chart-row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-row-1col {
  display: grid;
  grid-template-columns: 1fr;
}

/* ===== 图表卡片 ===== */
.chart-card {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: box-shadow 0.2s ease;
}
.chart-card:hover {
  box-shadow: 0 8px 28px rgba(99, 102, 241, 0.1);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0 20px;
  border-bottom: 1px solid rgba(99,102,241,0.08);
  padding-bottom: 12px;
  margin-bottom: 4px;
}

.chart-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.chart-subtitle {
  font-size: 12px;
  color: #94a3b8;
  background: rgba(99,102,241,0.08);
  padding: 3px 10px;
  border-radius: 20px;
}

.chart-placeholder {
  height: 280px;
  position: relative;  /* 让 .chart-empty 可以绝对定位 */
  padding: 8px 16px 16px 16px;
}

.chart-placeholder-tall {
  height: 320px;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

/* ===== 空状态覆盖层 ===== */
.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(248, 250, 252, 0.85);
  border-radius: 12px;
  pointer-events: none; /* 不遮挡图表交互 */
}

.empty-icon {
  font-size: 40px;
  opacity: 0.35;
  line-height: 1;
}

.empty-text {
  font-size: 15px;
  font-weight: 600;
  color: #94a3b8;
}

  .empty-hint {
    font-size: 12px;
    color: #cbd5e1;
  }
  
  /* ========================================= */
  /* 📱 移动端深度适配 (AdminStatistics)       */
  /* ========================================= */
  @media screen and (max-width: 768px) {
    /* 1. 顶部标题区紧凑组合，避免占用宝贵首屏 */
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }
    .header-left {
      margin-bottom: 0;
    }
    .header-right {
      width: 100%;
      justify-content: flex-start; /* 靠左排列标签和按钮 */
      gap: 12px;
    }
    
    /* 2. KPI 核心指标：高定卡片风（纵向紧缩） */
    .kpi-grid {
      grid-template-columns: repeat(2, 1fr); 
      gap: 12px;
    }
    
    .kpi-card {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      justify-content: flex-start;
      padding: 14px 12px !important;
      border-radius: 20px; 
      border: none;        
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
      min-height: auto !important; /* 解除可能继承的 PC 端高度 */
      gap: 0;
    }
    
    /* 隐藏底纹图标 */
    .kpi-card::before {
      content: none;
    }
    
    /* 图标组件：缩小体积并向左上靠拢 */
    .kpi-icon-wrap {
      width: 32px;
      height: 32px;
      font-size: 16px;
      border-radius: 8px;
      margin-bottom: 6px; 
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    /* 数值区块：保持辨识度的同时削减行高抢占的高度 */
    .kpi-value {
      font-size: 28px;
      font-weight: 800;
      line-height: 1;
      letter-spacing: -0.5px;
      margin-bottom: 6px; 
    }
    
    /* 深层整合文字标签和徽章在一行横排 */
    .kpi-info {
      width: 100%;
      display: flex;
      flex-direction: row;     /* 强制横排 */
      justify-content: space-between;
      align-items: flex-end;   /* 底部对齐 */
      margin-top: 0;           /* 去除推压 */
    }
    
    .kpi-label {
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      margin-bottom: 0; 
      line-height: 1.2;
    }
    
    /* 胶囊徽章变得更精巧 */
    .kpi-badge {
      position: static;
      display: inline-block;
      font-size: 10px;
      padding: 2px 8px;
      border-radius: 20px;
      font-weight: 700;
      opacity: 0.9;
    }
    
    /* 各图表容器降维为瀑布流单列 */
    .chart-row-2col {
      grid-template-columns: 1fr;
      gap: 16px;
    }
    
    /* 图表头部在小屏下折叠，防止主标题和副标题互相挤压 */
    .chart-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
      padding: 16px 16px 0 16px;
    }
    
    .chart-title {
      font-size: 15px;
      line-height: 1.4;
      white-space: normal; /* 允许折行 */
      word-break: break-all;
    }
    
    .chart-subtitle {
      font-size: 11px;
      align-self: flex-start; /* 将副标题固定至左侧不跟随外层干扰 */
      margin-bottom: 4px;
    }
    
    /* 适当压缩移动端图表高度防止过分拉伸，保持良好屏显比例 */
    .chart-placeholder, .chart-placeholder-tall {
      height: 240px !important;
    }
  }
</style>
