<template>
  <div class="page-container">
    <NavBar />
    <div class="content-container">
      <el-card class="elegant-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2 class="card-title">风险规则配置</h2>
              <p class="card-subtitle">配置证书查重与OCR预检的风险规则、阈值与风险等级</p>
            </div>
            <div class="header-actions">
              <el-button :icon="Refresh" @click="loadRules" :loading="loading">刷新</el-button>
              <el-button type="primary" :icon="Check" @click="saveRules" :loading="saving">保存配置</el-button>
            </div>
          </div>
        </template>

        <el-alert
          v-if="!ocrConfigured"
          title="OCR服务未配置"
          description="未检测到可用的OCR服务配置，OCR预检将自动转为人工审核。请在后端配置 SAIMS_OCR_API_KEY / SAIMS_OCR_SECRET_KEY 环境变量后重启服务。"
          type="warning"
          show-icon
          :closable="false"
          class="mb-16"
        />

        <el-table :data="rules" v-loading="loading" class="premium-table" stripe>
          <el-table-column label="规则" min-width="200">
            <template #default="{ row }">
              <div class="rule-name">{{ row.ruleName }}</div>
              <div class="rule-desc">{{ row.description }}</div>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="90" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="阈值" width="180" align="center">
            <template #default="{ row }">
              <el-input-number
                v-if="row.ruleType === 'threshold'"
                v-model="row.thresholdValue"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="2"
                :controls="false"
                style="width: 110px"
              />
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="阈值说明" min-width="150">
            <template #default="{ row }">
              <span class="text-muted">{{ row.thresholdUnit || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="触发风险等级" width="150" align="center">
            <template #default="{ row }">
              <el-select v-model="row.riskLevel" style="width: 110px">
                <el-option label="高风险" value="high" />
                <el-option label="中风险" value="medium" />
                <el-option label="低风险" value="low" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="需要人工复核" width="130" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.needManualReview" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
        </el-table>

        <!-- OCR比对字段配置 -->
        <div class="section-title">OCR比对字段配置</div>
        <el-card shadow="never" class="inner-card">
          <div class="compare-fields">
            <el-checkbox v-for="f in ocrFieldOptions" :key="f.value" v-model="f.checked">{{ f.label }}</el-checkbox>
          </div>
          <div class="text-muted compare-tip">勾选的字段将参与OCR识别结果与申报字段的自动比对，未勾选字段不做比对（视为无法判断由人工审核）。</div>
        </el-card>

        <el-alert
          title="规则说明"
          type="info"
          :closable="false"
          class="mt-16"
          description="申请综合风险等级 = 所有被触发规则中的最高风险等级；任一被触发规则要求人工复核时，该申请将标记为需人工复核。同一团队成员上传相同证书不会触发重复风险。"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import NavBar from '@/components/NavBar.vue'

const loading = ref(false)
const saving = ref(false)
const rules = ref([])
const ocrConfigured = ref(false)

const ocrFieldOptions = ref([
  { label: '姓名', value: 'name', checked: true },
  { label: '竞赛名称', value: 'competition', checked: true },
  { label: '获奖等级', value: 'awardLevel', checked: true },
  { label: '获奖时间', value: 'awardTime', checked: true }
])

const loadRules = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin/risk/rules')
    if (res.code === '200') {
      rules.value = res.data.rules || []
      ocrConfigured.value = !!res.data.ocrConfigured
      // 回填OCR比对字段
      const ocrRule = rules.value.find(r => r.ruleKey === 'OCR_COMPARE_FIELDS')
      if (ocrRule && ocrRule.configJson) {
        let fields = []
        try { fields = JSON.parse(ocrRule.configJson) } catch (e) { /* ignore */ }
        ocrFieldOptions.value.forEach(f => { f.checked = fields.includes(f.value) })
        const compareRule = rules.value.find(r => r.ruleKey === 'OCR_COMPARE_FIELDS')
        if (compareRule) compareRule.enabled = 1
      }
    } else {
      ElMessage.error(res.msg || '加载风险规则失败')
    }
  } catch (e) {
    ElMessage.error('加载风险规则失败')
  } finally {
    loading.value = false
  }
}

const saveRules = async () => {
  saving.value = true
  try {
    // 同步OCR比对字段配置
    const checkedFields = ocrFieldOptions.value.filter(f => f.checked).map(f => f.value)
    const payload = rules.value.map(r => ({
      ruleKey: r.ruleKey,
      enabled: r.enabled,
      thresholdValue: r.thresholdValue,
      riskLevel: r.riskLevel,
      needManualReview: r.needManualReview,
      configJson: r.ruleKey === 'OCR_COMPARE_FIELDS' ? JSON.stringify(checkedFields) : r.configJson
    }))
    const res = await request.put('/api/admin/risk/rules', { rules: payload })
    if (res.code === '200') {
      ElMessage.success('风险规则配置已保存')
      await loadRules()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadRules)
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background: #f5f7fb;
}

.content-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.card-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.card-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.premium-table {
  width: 100%;
}

.rule-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.rule-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  line-height: 1.5;
}

.text-muted {
  color: #94a3b8;
  font-size: 13px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 24px 0 12px;
}

.inner-card {
  background: #fafbfe;
  border-radius: 10px;
}

.compare-fields {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.compare-tip {
  font-size: 12px;
}

.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }

:deep(.el-card) {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 12px 32px -16px rgba(15, 23, 42, 0.12);
}
</style>
