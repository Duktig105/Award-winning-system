<template>
  <div class="skill-profile-panel">
    <!-- 档案统计 -->
    <div class="stat-grid">
      <div class="stat-card glass-card" v-for="s in statCards" :key="s.key">
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

    <div class="profile-grid">
      <!-- 添加自评技能 -->
      <el-card class="profile-card glass-card" shadow="never">
        <div class="profile-card-header">
          <h3 class="panel-title">
            <el-icon><Plus /></el-icon>
            添加自评技能
          </h3>
        </div>
        <el-form :model="form" label-position="top">
          <el-form-item label="选择技能">
            <el-select
              v-model="form.skillId"
              placeholder="搜索并选择技能"
              filterable
              clearable
              style="width: 100%"
              :loading="loadingSkills"
            >
              <el-option
                v-for="skill in skillOptions"
                :key="skill.skillId"
                :label="skill.label"
                :value="skill.skillId"
                :disabled="skill.disabled"
              >
                <span class="option-name">{{ skill.name }}</span>
                <span class="option-cat">{{ skill.categoryName || '未分类' }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="自评熟练程度">
            <el-radio-group v-model="form.selfEvalLevel">
              <el-radio-button value="beginner">入门</el-radio-button>
              <el-radio-button value="intermediate">熟练</el-radio-button>
              <el-radio-button value="advanced">精通</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="经验值(选填)">
            <el-input-number v-model="form.experience" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="备注(选填)">
            <el-input v-model="form.note" placeholder="可填写课程、项目或证书" maxlength="100" show-word-limit />
          </el-form-item>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="自评技能为待验证状态，获奖审核通过后将自动升级为已验证"
            class="self-eval-tip"
          />
          <el-button type="primary" :icon="Plus" @click="submitSkill" :loading="submitting" class="premium-btn">
            添加到我的技能档案
          </el-button>
        </el-form>
      </el-card>

      <!-- 技能档案列表 -->
      <el-card class="profile-card glass-card" shadow="never">
        <div class="profile-card-header">
          <h3 class="panel-title">
            <el-icon><Reading /></el-icon>
            我的技能档案（{{ skills.length }}）
          </h3>
          <div class="list-tools">
            <el-input
              v-model="keyword"
              placeholder="搜索技能"
              :prefix-icon="Search"
              clearable
              size="small"
              class="search-input"
            />
            <el-select v-model="categoryFilter" placeholder="全部方向" clearable size="small" class="cat-select">
              <el-option
                v-for="c in skillCategories"
                :key="c.skillCategoryId"
                :label="c.categoryName"
                :value="c.categoryName"
              />
            </el-select>
          </div>
        </div>

        <div class="source-tabs">
          <div
            v-for="t in sourceTabs"
            :key="t.key"
            class="source-tab"
            :class="{ active: filter === t.key }"
            @click="filter = t.key"
          >
            {{ t.label }}
            <span class="tab-count">{{ t.count }}</span>
          </div>
        </div>

        <ul class="skill-list" v-if="filteredSkills.length > 0">
          <li v-for="item in filteredSkills" :key="item.skillId" class="skill-item" :class="{ verified: !!item.verified }">
            <div class="skill-main">
              <div class="skill-icon" :class="{ 'icon-verified': !!item.verified }">
                <el-icon><component :is="skillIconMap[item.categoryName] || MagicStick" /></el-icon>
              </div>
              <div class="skill-info">
                <div class="skill-title-row">
                  <span class="skill-name">{{ item.skillName }}</span>
                  <el-tag v-if="item.categoryName" size="small" effect="plain" round class="cat-tag">
                    {{ item.categoryName }}
                  </el-tag>
                </div>
                <div class="skill-meta">
                  <el-tag size="small" :type="levelColorMap[computedLevel(item)] || 'info'" round>
                    {{ levelText(computedLevel(item)) }}
                  </el-tag>
                  <el-tag size="small" effect="plain" round>
                    自评 · {{ levelText(item.selfEvalLevel) }}
                  </el-tag>
                  <el-tag size="small" :type="sourceTagType(item.source)" effect="plain" round class="source-tag">
                    <span class="source-tag-inner">
                      <el-icon class="source-icon"><component :is="sourceIcon(item.source)" /></el-icon>
                      {{ sourceText(item.source) }}
                    </span>
                  </el-tag>
                  <span class="skill-exp">经验值 {{ item.experience || 0 }}</span>
                </div>

                <div class="level-progress">
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: levelProgress(item) + '%' }"></div>
                  </div>
                  <span class="progress-text">{{ levelProgressText(item) }}</span>
                </div>

                <div v-if="item.source === 'award' && item.competitionName" class="skill-source">
                  <el-icon><DocumentChecked /></el-icon>
                  来源：{{ item.competitionName }} · {{ item.awardRank }}{{ item.awardLevel ? '/' + item.awardLevel : '' }}
                </div>
                <div v-else-if="item.note" class="skill-source">
                  <el-icon><EditPen /></el-icon>
                  备注：{{ item.note }}
                </div>
              </div>
            </div>
            <div class="skill-actions">
              <el-button
                v-if="item.source === 'self_eval' && !item.verified"
                size="small" :icon="Edit" @click="openEdit(item)"
              />
              <el-button
                v-if="item.source === 'self_eval' && !item.verified"
                size="small" :icon="Delete" type="danger" @click="removeSkill(item)"
              />
            </div>
          </li>
        </ul>
        <el-empty v-else :description="skills.length ? '没有符合筛选条件的技能' : '暂无技能，赶紧添加吧！'" :image-size="80" />
      </el-card>
    </div>

    <!-- 编辑自评技能 -->
    <el-dialog v-model="editVisible" title="修改自评技能" width="420px" class="premium-dialog">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="技能名称">
          <span class="edit-skill-name">{{ editForm.skillName }}</span>
        </el-form-item>
        <el-form-item label="熟练程度">
          <el-radio-group v-model="editForm.selfEvalLevel">
            <el-radio-button value="beginner">入门</el-radio-button>
            <el-radio-button value="intermediate">熟练</el-radio-button>
            <el-radio-button value="advanced">精通</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="经验值">
          <el-input-number v-model="editForm.experience" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.note" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Reading, Edit, Delete, MagicStick, DocumentChecked, EditPen, Search,
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook,
  Collection, CircleCheck, Clock, TrendCharts
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const skillOptions = ref([])
const skillCategories = ref([])
const skills = ref([])
const loadingSkills = ref(false)
const submitting = ref(false)
const filter = ref('all')
const keyword = ref('')
const categoryFilter = ref('')

const form = ref({ skillId: null, selfEvalLevel: 'beginner', experience: 0, note: '' })

const skillIconMap = {
  '编程与开发': Monitor,
  '数学分析': DataAnalysis,
  '设计与创意': Picture,
  '商业与表达': Briefcase,
  '科研能力': Notebook,
  '电子硬件': Cpu
}

const levelColorMap = { beginner: 'info', intermediate: 'warning', advanced: 'danger' }
const levelText = (l) => ({ beginner: '入门', intermediate: '熟练', advanced: '精通' }[l] || '入门')
const sourceText = (s) => ({ self_eval: '自评待验证', award: '获奖已验证', team: '团队推荐' }[s] || '自评待验证')
const sourceTagType = (s) => ({ self_eval: 'info', award: 'success', team: 'warning' }[s] || 'info')
const sourceIcon = (s) => ({ self_eval: 'Clock', award: 'CircleCheck', team: 'TrendCharts' }[s] || 'Clock')

/** 计算技能等级：由经验值 + 是否已验证综合得出 */
const computedLevel = (item) => {
  const exp = Number(item.experience || 0)
  if (item.verified && exp >= 80) return 'advanced'
  if (exp >= 80) return 'advanced'
  if (exp >= 30) return 'intermediate'
  return 'beginner'
}

/** 等级进度：入门(0-30) / 熟练(30-80) / 精通(80+) */
const levelProgress = (item) => {
  const exp = Number(item.experience || 0)
  if (exp >= 80) return 100
  if (exp >= 30) return Math.round(((exp - 30) / 50) * 100)
  return Math.round((exp / 30) * 100)
}
const levelProgressText = (item) => {
  const exp = Number(item.experience || 0)
  if (exp >= 80) return '已精通 '
  if (exp >= 30) return `距精通还需 ${80 - exp} 经验值`
  return `距熟练还需 ${30 - exp} 经验值`
}

const filteredSkills = computed(() => {
  let list = skills.value
  if (filter.value === 'verified') list = list.filter(s => !!s.verified)
  else if (filter.value === 'self') list = list.filter(s => !s.verified)
  if (categoryFilter.value) list = list.filter(s => s.categoryName === categoryFilter.value)
  const kw = keyword.value.trim().toLowerCase()
  if (kw) list = list.filter(s => String(s.skillName || '').toLowerCase().includes(kw))
  return list
})

const sourceTabs = computed(() => [
  { key: 'all', label: '全部', count: skills.value.length },
  { key: 'verified', label: '已验证', count: skills.value.filter(s => !!s.verified).length },
  { key: 'self', label: '自评待验证', count: skills.value.filter(s => !s.verified).length }
])

const statCards = computed(() => {
  const totalExp = skills.value.reduce((sum, s) => sum + Number(s.experience || 0), 0)
  const verified = skills.value.filter(s => !!s.verified).length
  const directions = new Set(skills.value.map(s => s.categoryName).filter(Boolean)).size
  return [
    { key: 'total', label: '技能总数', value: skills.value.length, tip: '档案内技能数量', icon: Collection, color: 'linear-gradient(135deg,#6366f1,#8b5cf6)' },
    { key: 'verified', label: '已验证技能', value: verified, tip: '由获奖记录确认', icon: CircleCheck, color: 'linear-gradient(135deg,#10b981,#14b8a6)' },
    { key: 'exp', label: '累计经验值', value: totalExp, tip: '经验越高等级越高', icon: TrendCharts, color: 'linear-gradient(135deg,#f59e0b,#f97316)' },
    { key: 'cat', label: '覆盖方向', value: directions, tip: '涉及技能方向数', icon: Notebook, color: 'linear-gradient(135deg,#3b82f6,#0ea5e9)' }
  ]
})

const loadSkillOptions = async () => {
  loadingSkills.value = true
  try {
    const res = await request.get('/api/honor/skill/list')
    if (res.code === '200') {
      const owned = new Set(skills.value.map(s => s.skillId))
      skillOptions.value = (res.data || []).map(s => ({
        skillId: s.skillId,
        label: `${s.name} · ${s.categoryName || '未分类'}`,
        name: s.name,
        categoryName: s.categoryName,
        allowSelfEval: s.allowSelfEval,
        awardOnly: s.awardOnly,
        disabled: !s.allowSelfEval || s.awardOnly || owned.has(s.skillId)
      }))
    }
  } catch (_) {} finally {
    loadingSkills.value = false
  }
}

const loadSkillCategories = async () => {
  try {
    const res = await request.get('/api/honor/skill-category/list')
    if (res.code === '200') skillCategories.value = res.data || []
  } catch (_) {}
}

const loadMySkills = async () => {
  try {
    const res = await request.get('/api/honor/student/skill/list')
    if (res.code === '200') skills.value = res.data || []
  } catch (_) {} finally {
    loadSkillOptions()
  }
}

const submitSkill = async () => {
  if (!form.value.skillId) return ElMessage.warning('请选择技能')
  submitting.value = true
  try {
    const res = await request.post('/api/honor/student/skill/self', form.value)
    if (res.code === '200') {
      ElMessage.success(res.msg || '已添加到技能档案')
      form.value = { skillId: null, selfEvalLevel: 'beginner', experience: 0, note: '' }
      await loadMySkills()
    }
  } catch (_) {} finally {
    submitting.value = false
  }
}

const editVisible = ref(false)
const editForm = ref({ skillId: null, skillName: '', selfEvalLevel: 'beginner', experience: 0, note: '' })
const openEdit = (item) => {
  editForm.value = {
    skillId: item.skillId,
    skillName: item.skillName,
    selfEvalLevel: item.selfEvalLevel || 'beginner',
    experience: Number(item.experience || 0),
    note: item.note || ''
  }
  editVisible.value = true
}
const saveEdit = async () => {
  const res = await request.put('/api/honor/student/skill/self', editForm.value)
  if (res.code === '200') {
    ElMessage.success('已更新')
    editVisible.value = false
    await loadMySkills()
  }
}

const removeSkill = async (item) => {
  try {
    await ElMessageBox.confirm(`确定移除自评技能【${item.skillName}】吗？`, '提示', {
      confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning', customClass: 'premium-message-box'
    })
    const res = await request.delete(`/api/honor/student/skill/${item.skillId}`)
    if (res.code === '200') {
      ElMessage.success('已移除')
      await loadMySkills()
    }
  } catch (_) {}
}

onMounted(() => {
  loadSkillCategories()
  loadMySkills()
})
</script>

<style scoped>
.skill-profile-panel { display: flex; flex-direction: column; gap: 20px; }

.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.stat-card {
  background: rgba(255,255,255,0.65) !important;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.8) !important;
  border-radius: 16px !important;
  padding: 16px 18px !important;
  display: flex; align-items: center; gap: 14px;
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 20px;
}
.stat-body { display: flex; flex-direction: column; min-width: 0; }
.stat-label { font-size: 13px; color: #64748b; }
.stat-value { font-size: 22px; font-weight: 700; color: #1e293b; line-height: 1.3; }
.stat-tip { font-size: 11px; color: #94a3b8; }

.profile-grid { display: grid; grid-template-columns: 380px 1fr; gap: 20px; }

.profile-card {
  background: rgba(255,255,255,0.65) !important;
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.8) !important;
  border-radius: 18px !important;
  padding: 22px 24px !important;
}

.profile-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.panel-title { margin: 0; font-size: 16px; font-weight: 700; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.panel-title .el-icon { color: #6366f1; }

.list-tools { display: flex; gap: 8px; }
.search-input { width: 160px; }
.cat-select { width: 130px; }

.option-name { float: left; }
.option-cat { float: right; color: #94a3b8; font-size: 12px; }

.self-eval-tip { margin: 4px 0 16px; border-radius: 10px; }

.premium-btn {
  width: 100%; height: 44px; border-radius: 12px !important;
  background: linear-gradient(135deg, #6366f1, #8b5cf6) !important; border: none !important;
  font-weight: 600 !important; box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3) !important;
}

.source-tabs { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.source-tab {
  padding: 6px 14px; border-radius: 20px; font-size: 13px; cursor: pointer;
  color: #64748b; background: rgba(255,255,255,0.6); border: 1px solid rgba(99,102,241,0.15);
  transition: all 0.2s; user-select: none;
}
.source-tab:hover { border-color: #6366f1; color: #6366f1; }
.source-tab.active { background: linear-gradient(135deg,#6366f1,#8b5cf6); color: #fff; border-color: transparent; }
.tab-count { margin-left: 6px; font-size: 12px; opacity: 0.85; }

.skill-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; max-height: 620px; overflow-y: auto; }
.skill-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  background: rgba(255,255,255,0.65);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 14px;
  transition: all 0.25s;
}
.skill-item:hover { border-color: #6366f1; box-shadow: 0 6px 16px rgba(99,102,241,0.1); transform: translateY(-1px); }
.skill-item.verified { background: linear-gradient(135deg, rgba(16,185,129,0.06), rgba(20,184,166,0.04)); border-color: rgba(16,185,129,0.3); }

.skill-main { flex: 1; display: flex; align-items: flex-start; gap: 14px; min-width: 0; }
.skill-icon {
  width: 42px; height: 42px; border-radius: 12px;
  background: linear-gradient(135deg, rgba(99,102,241,0.1), rgba(139,92,246,0.08));
  display: flex; align-items: center; justify-content: center; color: #6366f1; font-size: 20px;
  flex-shrink: 0;
}
.skill-icon.icon-verified { background: linear-gradient(135deg, rgba(16,185,129,0.14), rgba(20,184,166,0.1)); color: #10b981; }
.skill-info { flex: 1; min-width: 0; }
.skill-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.skill-name { font-size: 15px; font-weight: 700; color: #1e293b; }
.cat-tag { background: rgba(99,102,241,0.08); border-color: rgba(99,102,241,0.25); color: #6366f1; }
.skill-meta { display: flex; gap: 8px; align-items: center; margin-top: 6px; flex-wrap: wrap; }
/* 防止标签内文字折行（图标被顶到上方） */
.skill-meta :deep(.el-tag) {
  white-space: nowrap !important;
  flex-shrink: 0;
  height: auto;
  max-width: none;
}
.skill-meta :deep(.el-tag .el-tag__content) {
  display: inline-flex !important;
  align-items: center;
  white-space: nowrap;
}
.source-tag { flex-shrink: 0; }
.source-tag-inner {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  gap: 3px;
}
.source-tag-inner .el-icon { flex-shrink: 0; }
.skill-exp { font-size: 12px; color: #94a3b8; }

.level-progress { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.progress-track { flex: 1; height: 6px; border-radius: 4px; background: rgba(99,102,241,0.12); overflow: hidden; }
.progress-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg,#6366f1,#8b5cf6); transition: width 0.4s ease; }
.skill-item.verified .progress-fill { background: linear-gradient(90deg,#10b981,#14b8a6); }
.progress-text { font-size: 11px; color: #94a3b8; white-space: nowrap; }

.skill-source { margin-top: 8px; font-size: 12px; color: #64748b; display: flex; align-items: center; gap: 4px; }

.skill-actions { display: flex; gap: 4px; align-self: center; }
.edit-skill-name { font-size: 16px; font-weight: 600; color: #1e293b; }

@media screen and (max-width: 900px) {
  .profile-grid { grid-template-columns: 1fr; }
  .list-tools { width: 100%; }
  .search-input, .cat-select { flex: 1; width: auto; }
}
</style>