<template>
  <div class="tree-node-wrapper" :class="[`level-${node.level || 1}`, { 'is-leaf': hasChildren === false }]">
    <div
      class="tree-node"
      :class="{ unlocked: !!node.unlocked, locked: !node.unlocked }"
      @click="onSelect"
    >
      <div class="node-icon">
        <el-icon><component :is="resolvedIcon" /></el-icon>
      </div>

      <div class="node-content">
        <div class="node-title-row">
          <span class="node-name">{{ node.name }}</span>
          <el-tag size="small" round effect="plain" :type="levelTagType">{{ levelLabel }}</el-tag>
          <el-tag size="small" round :type="node.unlocked ? 'success' : 'info'" effect="light">
            {{ node.unlocked ? '已解锁' : '未解锁' }}
          </el-tag>
          <el-tag v-if="node.unlocked && node.skillId" size="small" round type="success" effect="plain">
            <el-icon class="inline-icon"><CircleCheck /></el-icon> 已验证
          </el-tag>
        </div>

        <div class="node-desc" v-if="node.description">{{ node.description }}</div>

        <div class="node-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
          </div>
          <span class="progress-text">{{ progressText }}</span>
        </div>

        <div v-if="!node.unlocked" class="node-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ suggestion }}</span>
        </div>
        <div v-else-if="hasChildren" class="node-hint ok">
          <el-icon><Unlock /></el-icon>
          <span>已解锁，可继续挑战下级分支</span>
        </div>
      </div>

      <div class="node-arrow">
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>

    <div v-if="hasChildren" class="tree-children">
      <SkillTreeNode
        v-for="child in node.children"
        :key="child.nodeId"
        :node="child"
        :root-icon="rootIcon"
        @select="(p) => emit('select', p)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Lock, Unlock, InfoFilled, ArrowRight, CircleCheck,
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook, Histogram,
  MagicStick, Promotion, Reading, Trophy
} from '@element-plus/icons-vue'

const props = defineProps({
  node: { type: Object, required: true },
  rootIcon: { type: [Object, String, Function], default: null }
})
const emit = defineEmits(['select'])

const iconMap = {
  Monitor, DataAnalysis, Picture, Cpu, Briefcase, Notebook, Histogram,
  MagicStick, Promotion, Reading, Trophy,
  Lock, Unlock
}

const hasChildren = computed(() => (props.node.children || []).length > 0)

const resolvedIcon = computed(() => {
  const byName = props.node.icon ? iconMap[props.node.icon] : null
  if (byName) return byName
  if (props.rootIcon) return props.rootIcon
  return props.node.unlocked ? Unlock : Lock
})

const levelLabel = computed(() => ({ 1: '一级方向', 2: '二级分支', 3: '三级技能' }[props.node.level] || '技能'))
const levelTagType = computed(() => ({ 1: 'primary', 2: 'warning', 3: 'danger' }[props.node.level] || 'info'))

const current = computed(() => {
  const v = props.node.skillId ? props.node.skillExperience : props.node.verifiedSkillCount
  return Number(v || 0)
})
const threshold = computed(() => Number(props.node.unlockThreshold || 0))

const progressPct = computed(() => {
  if (threshold.value <= 0) return 100
  return Math.min(100, Math.round((current.value / threshold.value) * 100))
})

const progressText = computed(() => {
  if (threshold.value <= 0) return '无需门槛'
  return props.node.skillId
    ? `${current.value}/${threshold.value} 经验值`
    : `${current.value}/${threshold.value} 项已验证技能`
})

const suggestion = computed(() => {
  const need = Math.max(0, threshold.value - current.value)
  if (props.node.skillId) {
    return `再获得 ${need} 点经验值即可解锁，提交该方向竞赛获奖申请可自动累积。`
  }
  return `再解锁 ${need} 项已验证技能即可解锁该方向，可先完成二级技能节点。`
})

const onSelect = () => emit('select', { node: props.node, rootIcon: resolvedIcon.value })
</script>

<style scoped>
.tree-node-wrapper { position: relative; margin-bottom: 10px; }
.level-2 { margin-left: 30px; }
.level-3 { margin-left: 60px; }

.tree-node {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255,255,255,0.62);
  border: 1px solid rgba(255,255,255,0.7);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.05);
  cursor: pointer;
  transition: all 0.25s;
}
.tree-node:hover { border-color: #6366f1; transform: translateX(4px); box-shadow: 0 8px 18px rgba(99, 102, 241, 0.15); }

.tree-node.unlocked {
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.05));
  border-color: rgba(99,102,241,0.3);
}
.tree-node.locked { background: rgba(248,250,252,0.55); border-style: dashed; opacity: 0.94; }

.level-1 > .tree-node { border-left: 4px solid #6366f1; }
.level-2 > .tree-node { border-left: 4px solid #8b5cf6; }
.level-3 > .tree-node { border-left: 4px solid #f59e0b; }

.node-icon {
  width: 42px; height: 42px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, rgba(99,102,241,0.12), rgba(139,92,246,0.08));
  color: #6366f1; font-size: 19px; flex-shrink: 0;
}
.tree-node.locked .node-icon { background: #f1f5f9; color: #94a3b8; }

.node-content { flex: 1; min-width: 0; }
.node-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.node-name { font-size: 15px; font-weight: 700; color: #1e293b; }
.inline-icon { margin-right: 2px; vertical-align: -2px; }
.node-desc { font-size: 13px; color: #64748b; margin-top: 5px; line-height: 1.5; }

.node-progress { display: flex; align-items: center; gap: 10px; margin-top: 9px; }
.progress-bar { flex: 1; height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #6366f1, #8b5cf6); transition: width 0.4s; }
.node-progress .progress-text { font-size: 12px; color: #475569; white-space: nowrap; }

.node-hint {
  margin-top: 9px; font-size: 12px; color: #f59e0b;
  display: flex; align-items: center; gap: 5px;
  padding: 6px 10px; background: rgba(245,158,11,0.1); border-radius: 8px;
}
.node-hint.ok { color: #6366f1; background: rgba(99,102,241,0.08); }

.node-arrow { align-self: center; color: #cbd5e1; font-size: 16px; }
.tree-node:hover .node-arrow { color: #6366f1; }

.tree-children { margin-top: 10px; position: relative; }
.tree-children::before {
  content: ''; position: absolute; left: 14px; top: 0; bottom: 12px; width: 2px;
  background: linear-gradient(to bottom, rgba(99,102,241,0.25), rgba(99,102,241,0.05));
}
</style>