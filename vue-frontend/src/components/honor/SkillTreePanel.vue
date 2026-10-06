<template>
  <div class="skill-tree-panel">
    <!-- 总览 -->
    <div class="tree-overview">
      <div class="overview-card glass-card">
        <div class="overview-icon" style="background: linear-gradient(135deg,#6366f1,#8b5cf6)">
          <el-icon><Share /></el-icon>
        </div>
        <div class="overview-body">
          <span class="overview-label">星图探索度</span>
          <span class="overview-value">{{ unlockedNodeCount }} / {{ totalNodeCount }}</span>
          <div class="overview-bar">
            <div class="overview-fill" :style="{ width: totalNodeCount ? Math.round(unlockedNodeCount / totalNodeCount * 100) + '%' : '0%' }"></div>
          </div>
        </div>
      </div>
      <div class="overview-card glass-card">
        <div class="overview-icon" style="background: linear-gradient(135deg,#10b981,#14b8a6)">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="overview-body">
          <span class="overview-label">已验证技能</span>
          <span class="overview-value">{{ treeData.verifiedSkillCount || 0 }}</span>
          <span class="overview-tip">由获奖记录自动确认</span>
        </div>
      </div>
      <div class="overview-card glass-card">
        <div class="overview-icon" style="background: linear-gradient(135deg,#f59e0b,#f97316)">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="overview-body">
          <span class="overview-label">累计技能经验值</span>
          <span class="overview-value">{{ treeData.totalSkillExperience || 0 }}</span>
          <span class="overview-tip">经验值决定技能等级</span>
        </div>
      </div>
    </div>

    <!-- 图例：层级（颜色）+ 状态（图标）分组 -->
    <div class="skill-tree-legend">
      <!-- 第一组：颜色代表的层级 -->
      <div class="legend-group">
        <div class="legend-item">
          <span class="legend-dot bg-blue"></span>
          <span>一级方向</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot bg-gold"></span>
          <span>二级技能</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot bg-pink"></span>
          <span>三级进阶</span>
        </div>
      </div>

      <div class="legend-divider"></div>

      <!-- 第二组：图标代表的状态 -->
      <div class="legend-group">
        <div class="legend-item">
          <el-icon class="icon-unlocked"><CircleCheckFilled /></el-icon>
          <span>已点亮</span>
        </div>
        <div class="legend-item">
          <el-icon class="icon-locked"><Lock /></el-icon>
          <span>未解锁</span>
        </div>
      </div>

      <div class="legend-hint">悬停星点查看进度，点击查看获奖记录与点亮攻略</div>
    </div>

    <el-tabs v-model="activeCategory" type="card" class="tree-tabs">
      <el-tab-pane
        v-for="cat in treeData.categories || []"
        :key="cat.skillCategoryId"
        :name="String(cat.skillCategoryId)"
      >
        <template #label>
          <span class="tab-label">
            {{ cat.categoryName }}
            <span class="tab-badge">{{ catUnlocked(cat) }}/{{ catTotal(cat) }}</span>
          </span>
        </template>

        <div class="category-desc" v-if="cat.description">{{ cat.description }}</div>

        <div v-if="(cat.nodes || []).length > 0">
          <SkillStarMap
            :nodes="cat.nodes"
            :category-name="cat.categoryName"
            :category-icon="catIcon(cat)"
            @select="openNode"
          />
        </div>
        <el-empty v-else description="该方向暂未配置技能节点" :image-size="90" />
      </el-tab-pane>
    </el-tabs>

    <!-- 节点详情 / 证据 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="620px"
      class="premium-dialog tree-detail-dialog"
      append-to-body
      align-center
    >
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <!-- 节点为技能节点 -->
          <template v-if="detail.kind === 'skill'">
            <div class="detail-head">
              <div class="detail-icon" :class="{ unlocked: detail.unlocked }">
                <el-icon><component :is="detail.icon" /></el-icon>
              </div>
              <div class="detail-head-body">
                <div class="detail-name">
                  {{ detail.name }}
                  <el-tag size="small" round :type="detail.unlocked ? 'success' : 'info'" effect="plain">
                    {{ detail.unlocked ? '已解锁' : '未解锁' }}
                  </el-tag>
                </div>
                <div class="detail-sub">
                  <el-tag size="small" effect="plain" round>{{ detail.categoryName || '未分类' }}</el-tag>
                  <el-tag size="small" round :type="levelColorMap[computedLevel]">{{ levelText(computedLevel) }}</el-tag>
                  <span class="detail-exp">经验值 {{ evidence?.experience || 0 }} / {{ Number(detail.threshold || 0) <= 0 ? '无门槛' : '解锁阈值 ' + detail.threshold }}</span>
                </div>
              </div>
            </div>

            <div class="detail-progress">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: detail.percent + '%' }"></div>
              </div>
              <span class="progress-text">{{ progressText }}</span>
            </div>

            <div class="detail-section">
              <h4 class="section-title"><el-icon><DocumentChecked /></el-icon> 对应获奖记录</h4>
              <el-table v-if="(evidence?.awards || []).length > 0" :data="evidence.awards" stripe size="small" class="award-table">
                <el-table-column label="竞赛名称" prop="competitionName" min-width="180" show-overflow-tooltip />
                <el-table-column label="级别" prop="competitionLevel" width="80" />
                <el-table-column label="获奖" width="110">
                  <template #default="{ row }">{{ row.awardRank }}{{ row.awardLevel ? ' · ' + row.awardLevel : '' }}</template>
                </el-table-column>
                <el-table-column label="时间" prop="awardTime" width="110" />
              </el-table>
              <el-empty v-else :image-size="70" description="暂无直接对应的获奖记录" />
            </div>

            <div class="detail-section" v-if="evidence?.verified">
              <div class="source-line">
                <el-icon><CircleCheck /></el-icon>
                技能来源：获奖已验证
                <template v-if="evidence.note"> · 备注：{{ evidence.note }}</template>
              </div>
            </div>

            <div class="detail-section" v-else>
              <div class="source-line pending">
                <el-icon><Clock /></el-icon>
                当前为自评技能（待验证），提交对应方向获奖申请并审核通过后自动升级为已验证
              </div>
            </div>

            <div class="detail-section">
              <h4 class="section-title"><el-icon><Aim /></el-icon> 提升建议</h4>
              <ul class="advice-list">
                <li v-for="(a, i) in detail.advice" :key="i">{{ a }}</li>
              </ul>
            </div>

            <div class="detail-section" v-if="(evidence?.competitionCategories || []).length > 0">
              <h4 class="section-title"><el-icon><CollectionTag /></el-icon> 可积累该技能的竞赛方向</h4>
              <div class="cat-chips">
                <el-tag v-for="c in evidence.competitionCategories" :key="c.categoryId" effect="light" round>
                  {{ c.categoryName }}
                </el-tag>
              </div>
            </div>
          </template>

          <!-- 节点为方向/分支（无绑定技能） -->
          <template v-else>
            <div class="detail-head">
              <div class="detail-icon" :class="{ unlocked: detail.unlocked }">
                <el-icon><component :is="detail.icon" /></el-icon>
              </div>
              <div class="detail-head-body">
                <div class="detail-name">
                  {{ detail.name }}
                  <el-tag size="small" round :type="detail.unlocked ? 'success' : 'info'" effect="plain">
                    {{ detail.unlocked ? '已解锁' : '未解锁' }}
                  </el-tag>
                </div>
                <div class="detail-sub">
                  <el-tag size="small" effect="plain" round>一级方向</el-tag>
                  <span class="detail-exp">依赖已验证技能数 {{ detail.current }} / {{ detail.threshold }}</span>
                </div>
              </div>
            </div>
            <div class="detail-progress">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: detail.percent + '%' }"></div>
              </div>
              <span class="progress-text">{{ progressText }}</span>
            </div>
            <div class="detail-section">
              <h4 class="section-title"><el-icon><Aim /></el-icon> 提升建议</h4>
              <ul class="advice-list">
                <li v-for="(a, i) in detail.advice" :key="i">{{ a }}</li>
              </ul>
            </div>
            <div class="detail-section">
              <h4 class="section-title"><el-icon><Share /></el-icon> 下级分支</h4>
              <div class="child-list">
                <div v-for="c in detail.children" :key="c.nodeId" class="child-item" :class="{ unlocked: c.unlocked }">
                  <el-icon><component :is="c.unlocked ? Unlock : Lock" /></el-icon>
                  <span>{{ c.name }}</span>
                  <el-tag size="small" effect="plain" round>{{ c.unlocked ? '已解锁' : `需 ${c.unlockThreshold} 经验` }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </template>
      </div>
      <template #footer>
        <el-button v-if="canLightUp" type="primary" :loading="lightLoading" @click="lightUpNode">
          <el-icon style="margin-right:4px"><MagicStick /></el-icon>点亮此技能
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Share, CircleCheck, CircleCheckFilled, TrendCharts, DocumentChecked, Clock, Aim, CollectionTag, Lock, Unlock,
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook, Histogram, MagicStick, Promotion, Reading, Trophy
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import SkillStarMap from './SkillStarMap.vue'

const treeData = ref({ categories: [], verifiedSkillCount: 0, totalSkillExperience: 0 })
const activeCategory = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const evidence = ref(null)
const lightLoading = ref(false)

/** 无门槛节点（阈值<=0）且当前未点亮时，允许学生主动点亮 */
const canLightUp = computed(() =>
  !!detail.value && !detail.value.unlocked && Number(detail.value.threshold || 0) <= 0
)

/** 点亮无门槛节点：调用后端接口记录，并刷新技能树 */
const lightUpNode = async () => {
  if (!detail.value) return
  lightLoading.value = true
  try {
    const res = await request.post('/api/honor/student/skill-tree/unlock', { nodeId: detail.value.nodeId })
    if (res.code === '200') {
      ElMessage.success('已点亮')
      detail.value.unlocked = true
      await loadTree()
    } else {
      ElMessage.error(res.msg || '点亮失败')
    }
  } catch (e) {
    ElMessage.error('点亮失败')
  } finally {
    lightLoading.value = false
  }
}

/** 详情弹窗进度文案：已解锁 / 无门槛待点亮 / 还差多少经验 */
const progressText = computed(() => {
  if (!detail.value) return ''
  if (detail.value.unlocked) return '已解锁'
  if (Number(detail.value.threshold || 0) <= 0) return '无门槛 · 点击下方按钮点亮'
  return detail.value.kind === 'skill'
    ? `还需 ${detail.value.toGo} 经验值`
    : `还需 ${detail.value.toGo} 项已验证技能`
})

const iconMap = {
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook, Histogram,
  MagicStick, Promotion, Reading, Trophy
}
const levelColorMap = { beginner: 'info', intermediate: 'warning', advanced: 'danger' }
const levelText = (l) => ({ beginner: '入门', intermediate: '熟练', advanced: '精通' }[l] || '入门')

const computedLevel = computed(() => {
  const exp = Number(evidence.value?.experience || 0)
  if (exp >= 80) return 'advanced'
  if (exp >= 30) return 'intermediate'
  return 'beginner'
})

/** 统计解锁节点数（递归） */
const countNodes = (nodes, onlyUnlocked) => {
  let total = 0
  for (const n of nodes || []) {
    if (!onlyUnlocked || n.unlocked) total += 1
    total += countNodes(n.children, onlyUnlocked)
  }
  return total
}
const totalNodeCount = computed(() => countNodes(treeData.value.categories?.flatMap(c => c.nodes || []), false))
const unlockedNodeCount = computed(() => countNodes(treeData.value.categories?.flatMap(c => c.nodes || []), true))
const catTotal = (cat) => countNodes(cat.nodes || [], false)
const catUnlocked = (cat) => countNodes(cat.nodes || [], true)

const resolveIcon = (node, rootIcon) => iconMap[node.icon] || rootIcon || MagicStick
const catIcon = (cat) => {
  const first = (cat.nodes || [])[0]
  return (first && first.icon) || ''
}

const loadTree = async () => {
  try {
    const res = await request.get('/api/honor/student/skill-tree')
    if (res.code === '200') {
      treeData.value = res.data || { categories: [] }
      if (treeData.value.categories?.length > 0) {
        activeCategory.value = String(treeData.value.categories[0].skillCategoryId)
      }
    }
  } catch (e) {
    ElMessage.error('加载技能树失败')
  }
}

const openNode = async (payload) => {
  const { node, rootIcon } = payload
  const threshold = Number(node.unlockThreshold || 0)
  const current = node.skillId ? Number(node.skillExperience || 0) : Number(node.verifiedSkillCount || 0)
  // 阈值为0的节点：未点亮时进度显示0%、至少还需1点经验/技能（避免"进度100%却未点亮"的矛盾）
  const percent = threshold > 0 ? Math.min(100, Math.round((current / threshold) * 100)) : (node.unlocked ? 100 : 0)
  const toGo = Math.max(node.unlocked ? 0 : 1, threshold - current)
  const icon = resolveIcon(node, rootIcon)

  if (!node.skillId) {
    detail.value = {
      kind: 'branch',
      nodeId: node.nodeId,
      name: node.name,
      icon,
      unlocked: !!node.unlocked,
      threshold,
      current,
      percent,
      toGo,
      children: node.children || [],
      advice: node.unlocked
        ? ['该方向已解锁，可继续向下解锁二级分支与三级技能节点。']
        : threshold <= 0
          ? ['该方向为无门槛技能，点击下方"点亮此技能"即可解锁。']
          : [`再解锁 ${toGo} 项已验证技能即可解锁该方向。`, '建议优先提交对应竞赛方向的获奖申请，通过审核后技能将自动验证。']
    }
    detailVisible.value = true
    return
  }

  detailLoading.value = true
  detailVisible.value = true
  detail.value = {
    kind: 'skill',
    skillId: node.skillId,
    name: node.name,
    icon,
    unlocked: !!node.unlocked,
    threshold,
    current,
    percent,
    toGo,
    advice: node.unlocked
      ? [`该技能已解锁，当前经验值 ${current}。`, '继续参与该方向竞赛可累积经验值，从而提升技能等级。']
      : threshold <= 0
        ? ['该技能为无门槛技能，点击下方"点亮此技能"即可解锁。', '参与对应方向竞赛可继续累积经验、提升技能等级。']
        : [`再获得 ${toGo} 点经验值即可解锁该节点。`, '通过提交该方向竞赛获奖申请，审核通过后自动累积经验值。']
  }
  try {
    const res = await request.get(`/api/honor/student/skill/${node.skillId}/evidence`)
    if (res.code === '200') evidence.value = res.data
    else evidence.value = null
  } catch (e) {
    evidence.value = null
  } finally {
    detailLoading.value = false
  }
}

const detailTitle = computed(() => (detail.value?.kind === 'branch' ? '方向节点详情' : '技能节点详情'))

onMounted(loadTree)
</script>

<style scoped>
.skill-tree-panel { padding: 4px; display: flex; flex-direction: column; gap: 16px; }

.tree-overview { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
.overview-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px !important; border-radius: 16px !important;
  background: rgba(255,255,255,0.65) !important;
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.8) !important;
}
.overview-icon {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px;
}
.overview-body { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.overview-label { font-size: 13px; color: #64748b; }
.overview-value { font-size: 20px; font-weight: 700; color: #1e293b; }
.overview-tip { font-size: 11px; color: #94a3b8; }
.overview-bar { margin-top: 6px; height: 6px; border-radius: 3px; background: rgba(99,102,241,0.12); overflow: hidden; }
.overview-fill { height: 100%; border-radius: 3px; background: linear-gradient(90deg,#6366f1,#8b5cf6); transition: width 0.4s; }

/* ===== 图例：层级（颜色）+ 状态（图标）===== */
.skill-tree-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 16px;
}

.legend-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 层级颜色点：自带微发光，呼应星图 */
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 0 6px currentColor;
}
.bg-blue { background-color: #38bdf8; color: rgba(56,189,248,0.6); }
.bg-gold { background-color: #fbbf24; color: rgba(251,191,36,0.6); }
.bg-pink { background-color: #ec4899; color: rgba(236,72,153,0.6); }

/* 分隔线 */
.legend-divider {
  width: 1px;
  height: 14px;
  background-color: #cbd5e1;
}

/* 状态图标（与星图节点的绿勾一致） */
.icon-unlocked {
  color: #34d399;
  font-size: 14px;
}
.icon-locked {
  color: #94a3b8;
  font-size: 14px;
}

.legend-hint {
  color: #94a3b8;
  margin-left: auto;
  font-size: 12px;
}

.tree-tabs { background: transparent !important; }
.tree-tabs :deep(.el-tabs__header) { border-bottom: none; }
.tree-tabs :deep(.el-tabs__item) {
  border-radius: 12px !important; padding: 0 18px !important; height: 40px; line-height: 40px;
  font-weight: 600; color: #64748b; background: rgba(255,255,255,0.5); margin-right: 8px;
  border: 1px solid rgba(255,255,255,0.6) !important;
}
.tree-tabs :deep(.el-tabs__item.is-active) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6) !important; color: #fff !important; border: none !important;
  box-shadow: 0 6px 14px rgba(99, 102, 241, 0.3);
}
.tree-tabs :deep(.el-tabs__item.is-active .tab-badge) { background: rgba(255,255,255,0.25); color: #fff; }
.tab-label { display: inline-flex; align-items: center; gap: 8px; }
.tab-badge {
  font-size: 11px; padding: 1px 8px; border-radius: 10px;
  background: rgba(99,102,241,0.12); color: #6366f1;
}
.category-desc {
  color: #64748b; font-size: 14px; margin-bottom: 16px; line-height: 1.6;
  padding: 12px 16px; background: rgba(248,250,252,0.6); border-radius: 12px; border-left: 3px solid #6366f1;
}

/* 弹窗 */
.detail-wrap { min-height: 120px; }
.detail-head { display: flex; gap: 14px; align-items: center; }
.detail-icon {
  width: 48px; height: 48px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: #f1f5f9; color: #94a3b8; font-size: 22px;
}
.detail-icon.unlocked { background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(139,92,246,0.1)); color: #6366f1; }
.detail-head-body { flex: 1; min-width: 0; }
.detail-name { font-size: 16px; font-weight: 700; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.detail-sub { display: flex; align-items: center; gap: 8px; margin-top: 6px; flex-wrap: wrap; }
.detail-exp { font-size: 12px; color: #64748b; }
.detail-progress { display: flex; align-items: center; gap: 10px; margin: 16px 0; }
.progress-track { flex: 1; height: 8px; border-radius: 4px; background: rgba(99,102,241,0.12); overflow: hidden; }
.progress-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg,#6366f1,#8b5cf6); transition: width 0.4s; }
.progress-text { font-size: 12px; color: #64748b; white-space: nowrap; }
.detail-section { margin-top: 16px; }
.section-title { margin: 0 0 10px; font-size: 14px; font-weight: 700; color: #1e293b; display: flex; align-items: center; gap: 6px; }
.section-title .el-icon { color: #6366f1; }
.award-table { border-radius: 10px; overflow: hidden; }
.source-line {
  display: flex; align-items: center; gap: 6px; font-size: 13px; color: #10b981;
  background: rgba(16,185,129,0.08); padding: 10px 12px; border-radius: 10px;
}
.source-line.pending { color: #f59e0b; background: rgba(245,158,11,0.1); }
.advice-list { margin: 0; padding-left: 18px; color: #475569; font-size: 13px; line-height: 1.9; }
.cat-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.child-list { display: flex; flex-direction: column; gap: 8px; }
.child-item {
  display: flex; align-items: center; gap: 8px; font-size: 13px; color: #475569;
  padding: 8px 12px; border-radius: 10px; background: rgba(248,250,252,0.8); border: 1px dashed #e2e8f0;
}
.child-item.unlocked { color: #1e293b; border-style: solid; border-color: rgba(99,102,241,0.3); background: rgba(99,102,241,0.05); }
.child-item span { flex: 1; }
</style>