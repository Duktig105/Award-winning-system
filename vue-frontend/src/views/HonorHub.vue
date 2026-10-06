<template>
  <div class="honor-hub-page login-wrapper-style">
    <NavBar />
    <div class="content-container">
      <el-card class="hub-card">
        <template #header>
          <div class="card-header premium-header">
            <div class="header-title-wrapper">
              <div class="decorative-line"></div>
              <h2>荣誉中心</h2>
              <el-tag size="small" effect="light" class="version-tag">NEW</el-tag>
            </div>
            <div class="header-actions">
              <el-button :icon="Refresh" round @click="refreshAll">刷新</el-button>
            </div>
          </div>
        </template>

        <!-- 顶部统计（点击跳转对应内容） -->
        <div class="summary-grid">
          <div
            class="summary-card glass-card clickable"
            v-for="item in summaryCards"
            :key="item.key"
            @click="jumpTo(item.key)"
          >
            <div class="summary-icon" :style="{ background: item.color }">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="summary-content">
              <span class="summary-label">{{ item.label }}</span>
              <span class="summary-value">{{ item.value }}</span>
              <span class="summary-tip">{{ item.tip }}</span>
            </div>
            <el-icon class="summary-arrow"><ArrowRight /></el-icon>
          </div>
        </div>

        <!-- 二级导航 -->
        <div class="hub-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="hub-tab"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
            <span>{{ tab.label }}</span>
          </div>
        </div>

        <!-- 概览页 -->
        <div v-if="activeTab === 'overview'" class="tab-content">
          <el-row :gutter="24">
            <el-col :xs="24" :md="14">
              <div class="panel-card glass-card">
                <h3 class="panel-title">
                  <el-icon><DataLine /></el-icon>
                  最近荣誉积分
                </h3>
                <ul class="recent-list" v-if="recentScores.length > 0">
                  <li v-for="(item, idx) in recentScores.slice(0,5)" :key="idx">
                    <span class="recent-score" :class="{ minus: item.severity === 'minus' }">+{{ item.score }}</span>
                    <span class="recent-desc">{{ item.description }}</span>
                    <span class="recent-time">{{ formatTime(item.createTime) }}</span>
                  </li>
                </ul>
                <el-empty v-else description="暂无积分明细" :image-size="80" />
              </div>
            </el-col>
            <el-col :xs="24" :md="10">
              <div class="panel-card glass-card">
                <h3 class="panel-title">
                  <el-icon><Medal /></el-icon>
                  已获勋章
                  <span class="panel-sub" v-if="representativeBadge">代表勋章：{{ representativeBadge.badgeName }}</span>
                  <el-button link type="primary" class="panel-more" @click="activeTab = 'badges'">勋章墙 →</el-button>
                </h3>
                <BadgeShowcase :badges="badges" />
              </div>
            </el-col>
          </el-row>

          <div class="panel-card glass-card">
            <h3 class="panel-title">
              <el-icon><CollectionTag /></el-icon>
              我的荣誉标签
            </h3>
            <div class="tag-list" v-if="honorTags.length > 0">
              <el-tag
                v-for="tag in honorTags"
                :key="tag.tagId"
                effect="light"
                round
                size="large"
                :type="tagTypeMap[tag.tagType] || 'success'"
                class="honor-tag-chip"
              >
                {{ tag.tagName }}
              </el-tag>
            </div>
            <el-empty v-else description="尚未获得荣誉标签" :image-size="80" />
          </div>
        </div>

        <!-- 技能档案 -->
        <div v-if="activeTab === 'skills'" class="tab-content">
          <SkillProfilePanel />
        </div>

        <!-- 技能树 -->
        <div v-if="activeTab === 'tree'" class="tab-content">
          <SkillTreePanel />
        </div>

        <!-- 勋章墙 -->
        <div v-if="activeTab === 'badges'" class="tab-content">
          <BadgePanel :badges="badges" @refresh="loadBadges" />
        </div>

        <!-- 积分明细 -->
        <div v-if="activeTab === 'score'" class="tab-content">
          <ScoreLogPanel :initial-filter="scoreFilter" />
        </div>
        <div v-if="activeTab === 'tags'" class="tab-content">
          <HonorTagPanel />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Trophy, Medal, DataLine, CollectionTag, Refresh,
  Reading, Aim, MagicStick, ArrowRight
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import SkillProfilePanel from '@/components/honor/SkillProfilePanel.vue'
import SkillTreePanel from '@/components/honor/SkillTreePanel.vue'
import BadgePanel from '@/components/honor/BadgePanel.vue'
import BadgeShowcase from '@/components/honor/BadgeShowcase.vue'
import ScoreLogPanel from '@/components/honor/ScoreLogPanel.vue'
import HonorTagPanel from '@/components/honor/HonorTagPanel.vue'

const activeTab = ref('overview')
const summary = ref({})
const recentScores = ref([])
const badges = ref([])
const honorTags = ref([])
const scoreFilter = ref('all')

const tabs = [
  { key: 'overview', label: '概览', icon: DataLine },
  { key: 'skills',   label: '技能档案', icon: Reading },
  { key: 'tree',     label: '技能树', icon: Aim },
  { key: 'badges',   label: '勋章墙', icon: Medal },
  { key: 'tags',     label: '荣誉标签', icon: CollectionTag },
  { key: 'score',    label: '积分明细', icon: DataLine }
]

const summaryCards = computed(() => [
  { key: 'totalScore', label: '累计积分', value: summary.value.totalScore || 0, tip: '历史所有积分', icon: DataLine, color: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
  { key: 'totalAwards', label: '获奖次数', value: summary.value.totalAwards || 0, tip: '审核通过的获奖', icon: Trophy, color: 'linear-gradient(135deg, #f59e0b, #ef4444)' },
  { key: 'totalBadges', label: '已获勋章', value: summary.value.totalBadges || 0, tip: '勋章总数', icon: Medal, color: 'linear-gradient(135deg, #ec4899, #f97316)' },
  { key: 'totalSkills', label: '技能档案', value: `${summary.value.verifiedSkills || 0}/${summary.value.totalSkills || 0}`, tip: '已验证/总技能', icon: MagicStick, color: 'linear-gradient(135deg, #10b981, #22d3ee)' },
  { key: 'totalTags', label: '荣誉标签', value: summary.value.totalTags || 0, tip: '自动获得的标签', icon: CollectionTag, color: 'linear-gradient(135deg, #0ea5e9, #6366f1)' }
])

const representativeBadge = computed(() => badges.value.find(b => !!b.isRepresentative))

/* 统计卡点击跳转：累计积分→全部积分历史；获奖次数→获奖积分明细；
   已获勋章→勋章墙；技能档案→技能档案；荣誉标签→荣誉标签 */
const jumpTargets = {
  totalScore:  { tab: 'score',  filter: 'all' },
  totalAwards: { tab: 'score',  filter: 'award' },
  totalBadges: { tab: 'badges', filter: null },
  totalSkills: { tab: 'skills', filter: null },
  totalTags:   { tab: 'tags',   filter: null }
}
const jumpTo = (key) => {
  const t = jumpTargets[key]
  if (!t) return
  scoreFilter.value = t.filter || 'all'
  activeTab.value = t.tab
}

const tagTypeMap = {
  award_level: 'danger',
  competition_direction: 'warning',
  role: 'success',
  ability: 'primary',
  growth: 'info'
}

const loadSummary = async () => {
  try {
    const res = await request.get('/api/honor/student/score/summary')
    if (res.code === '200') summary.value = res.data || {}
  } catch (e) { console.error(e) }
}

const loadScores = async () => {
  try {
    const res = await request.get('/api/honor/student/score/logs', { params: { limit: 20 } })
    if (res.code === '200') recentScores.value = res.data || []
  } catch (e) { console.error(e) }
}

const loadBadges = async () => {
  try {
    const res = await request.get('/api/honor/student/badges')
    if (res.code === '200') badges.value = res.data || []
  } catch (e) { console.error(e) }
}

const loadHonorTags = async () => {
  try {
    const res = await request.get('/api/honor/student/tags')
    if (res.code === '200') honorTags.value = res.data || []
  } catch (e) { console.error(e) }
}

const refreshAll = () => {
  loadSummary()
  loadScores()
  loadBadges()
  loadHonorTags()
  ElMessage.success('已刷新')
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.honor-hub-page { min-height: 100vh; padding-bottom: 40px; }
.content-container { max-width: 1280px; margin: 0 auto; padding: 20px; position: relative; z-index: 10; }

.hub-card {
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  border-radius: 20px !important;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05) !important;
}

.card-header { display: flex; justify-content: space-between; align-items: center; }
.premium-header { border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 20px; margin-bottom: 24px; }
.header-title-wrapper { display: flex; align-items: center; gap: 12px; }
.decorative-line { width: 4px; height: 24px; background: linear-gradient(to bottom, #6366f1, #a855f7); border-radius: 4px; }
.card-header h2 { margin: 0; font-size: 22px; font-weight: 700; color: #1e293b; letter-spacing: 0.5px; }
.version-tag { border-radius: 6px; font-weight: 600; background: rgba(139, 92, 246, 0.1); color: #8b5cf6; border: none; }

.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }

.summary-card {
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  border-radius: 18px;
  background: rgba(255,255,255,0.65) !important;
  border: 1px solid rgba(255,255,255,0.7) !important;
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
}
.summary-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgba(99, 102, 241, 0.15); }
.summary-card.clickable { cursor: pointer; position: relative; }
.summary-card.clickable:hover { border-color: rgba(99,102,241,0.45) !important; }
.summary-arrow {
  position: absolute; top: 18px; right: 16px; color: #cbd5e1; font-size: 15px;
  transition: all 0.25s;
}
.summary-card.clickable:hover .summary-arrow { color: #6366f1; transform: translateX(3px); }
.summary-icon {
  width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 26px; box-shadow: 0 8px 18px -4px rgba(99, 102, 241, 0.4);
}
.summary-content { display: flex; flex-direction: column; }
.summary-label { font-size: 13px; color: #64748b; font-weight: 500; }
.summary-value { font-size: 28px; font-weight: 700; color: #1e293b; letter-spacing: 0.5px; line-height: 1.2; }
.summary-tip { font-size: 12px; color: #94a3b8; }

.hub-tabs {
  display: flex; gap: 12px; margin: 16px 0 24px; padding: 8px; border-radius: 16px;
  background: rgba(255,255,255,0.55); backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.6);
  overflow-x: auto;
}
.hub-tab {
  flex: 1; min-width: 110px;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 12px 18px; border-radius: 12px; cursor: pointer; color: #64748b; font-weight: 600; font-size: 15px;
  transition: all 0.25s;
}
.hub-tab:hover { color: #4f46e5; background: rgba(99, 102, 241, 0.06); }
.hub-tab.active { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; box-shadow: 0 8px 18px rgba(99, 102, 241, 0.3); }

.tab-content { animation: fadein 0.3s ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(8px);} to {opacity: 1; transform: translateY(0);} }

.panel-card {
  background: rgba(255,255,255,0.65) !important;
  border: 1px solid rgba(255,255,255,0.7) !important;
  border-radius: 16px; padding: 22px 24px; margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.panel-title {
  margin: 0 0 16px; font-size: 16px; font-weight: 700; color: #1e293b;
  display: flex; align-items: center; gap: 8px;
}
.panel-title .el-icon { color: #6366f1; }
.panel-sub { font-size: 12px; font-weight: 500; color: #f59e0b; margin-left: 4px; }
.panel-more { margin-left: auto; font-size: 13px; }

.recent-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.recent-list li { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 10px; background: rgba(248, 250, 252, 0.6); border: 1px solid #e2e8f0; }
.recent-score { font-weight: 700; color: #10b981; min-width: 60px; }
.recent-score.minus { color: #ef4444; }
.recent-desc { flex: 1; color: #334155; font-size: 14px; }
.recent-time { font-size: 12px; color: #94a3b8; }

.tag-list { display: flex; flex-wrap: wrap; gap: 10px; }
.honor-tag-chip { padding: 6px 14px !important; font-weight: 600; }

@media screen and (max-width: 768px) {
  .summary-grid { grid-template-columns: 1fr 1fr; }
  .hub-tabs { padding: 6px; gap: 6px; }
  .hub-tab { padding: 8px 10px; font-size: 13px; min-width: 0; }
  .summary-card { padding: 14px; gap: 10px; }
  .summary-icon { width: 42px; height: 42px; font-size: 20px; }
  .summary-value { font-size: 22px; }
}
</style>