<template>
  <div class="page-container">
    <NavBar />
    <div class="content-container">
      <el-card class="elegant-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2 class="card-title">学院学生导入</h2>
              <p class="card-subtitle">下载模板、上传学院学生名单，系统自动校验并新增/更新学生数据</p>
            </div>
          </div>
        </template>

        <!-- 导入区 -->
        <div class="import-section">
          <div class="import-actions">
            <el-button type="primary" :icon="Download" @click="downloadTemplate">下载Excel模板</el-button>
            <el-upload action="" :show-file-list="false" :before-upload="handleImport" accept=".xlsx,.xls">
              <el-button type="success" :icon="Upload" :loading="importing">上传学生名单</el-button>
            </el-upload>
          </div>
          <el-alert
            title="导入说明"
            type="info"
            :closable="false"
            class="mt-16"
            description="支持 .xlsx 文件；表头需包含：学号、姓名、年级、专业、班级（学院可选，为空时使用系统默认学院）。系统将校验字段格式、检查文件内学号重复；已存在的学生自动更新，数据一致则跳过；失败的行可在导入结果中下载。"
          />
        </div>

        <!-- 导入结果 -->
        <template v-if="lastResult">
          <div class="section-title">最近一次导入结果</div>
          <div class="result-cards">
            <div class="result-card total">
              <div class="result-num">{{ lastResult.totalCount }}</div>
              <div class="result-label">总行数</div>
            </div>
            <div class="result-card insert">
              <div class="result-num">{{ lastResult.insertCount }}</div>
              <div class="result-label">新增成功</div>
            </div>
            <div class="result-card update">
              <div class="result-num">{{ lastResult.updateCount }}</div>
              <div class="result-label">更新成功</div>
            </div>
            <div class="result-card skip">
              <div class="result-num">{{ lastResult.skipCount }}</div>
              <div class="result-label">跳过</div>
            </div>
            <div class="result-card fail">
              <div class="result-num">{{ lastResult.failCount }}</div>
              <div class="result-label">失败</div>
            </div>
          </div>
          <div class="result-actions" v-if="lastResult.failCount > 0">
            <el-button type="danger" plain :icon="Download" @click="downloadErrors(lastResult.importId)">下载错误数据</el-button>
          </div>
          <el-table :data="lastResult.failDetails" class="premium-table" stripe max-height="320" v-if="lastResult.failDetails.length">
            <el-table-column prop="rowNum" label="行号" width="80" align="center" />
            <el-table-column prop="studentNumber" label="学号" width="150" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column prop="message" label="失败原因" min-width="200" />
          </el-table>
        </template>

        <!-- 导入历史 -->
        <div class="section-title">导入历史</div>
        <div class="filter-bar">
          <el-button :icon="Refresh" @click="loadRecords" :loading="loadingHistory">刷新</el-button>
        </div>
        <el-table :data="records" v-loading="loadingHistory" class="premium-table" stripe>
          <el-table-column prop="importId" label="导入ID" width="90" align="center" />
          <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
          <el-table-column label="结果" width="280" align="center">
            <template #default="{ row }">
              <span class="count-tag insert">新增 {{ row.insertCount }}</span>
              <span class="count-tag update">更新 {{ row.updateCount }}</span>
              <span class="count-tag skip">跳过 {{ row.skipCount }}</span>
              <span class="count-tag fail">失败 {{ row.failCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="120" align="center" />
          <el-table-column prop="importTime" label="导入时间" width="180" align="center" />
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewDetail(row.importId)">明细</el-button>
              <el-button v-if="row.failCount > 0" link type="danger" size="small" @click="downloadErrors(row.importId)">下载错误</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          class="mt-16"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </el-card>
    </div>

    <!-- 导入明细弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="导入明细" width="860px" class="premium-dialog">
      <div class="detail-filter">
        <el-radio-group v-model="detailAction" @change="loadDetail">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="insert">新增</el-radio-button>
          <el-radio-button label="update">更新</el-radio-button>
          <el-radio-button label="skip">跳过</el-radio-button>
          <el-radio-button label="fail">失败</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="detailList" v-loading="detailLoading" stripe max-height="420">
        <el-table-column prop="rowNum" label="行号" width="70" align="center" />
        <el-table-column prop="studentNumber" label="学号" width="140" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="grade" label="年级" width="80" align="center" />
        <el-table-column prop="major" label="专业" min-width="130" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" min-width="150" show-overflow-tooltip />
        <el-table-column label="动作" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small">{{ actionText(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="130" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="detailPagination.page"
        v-model:page-size="detailPagination.pageSize"
        :page-sizes="[20, 50, 100]"
        :total="detailPagination.total"
        layout="total, prev, pager, next"
        class="mt-16"
        @current-change="loadDetail"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import NavBar from '@/components/NavBar.vue'

const importing = ref(false)
const loadingHistory = ref(false)
const lastResult = ref(null)
const records = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailAction = ref('')
const detailList = ref([])
const detailImportId = ref(null)
const detailPagination = reactive({ page: 1, pageSize: 50, total: 0 })

const downloadTemplate = async () => {
  try {
    const res = await request.get('/api/student/import/template', { responseType: 'blob' })
    triggerDownload(res, '学生导入模板.xlsx')
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

const triggerDownload = (blobData, filename) => {
  const blob = new Blob([blobData], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

const handleImport = async (file) => {
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/api/student/import', formData)
    if (res.code === '200') {
      lastResult.value = res.data
      ElMessage.success(`导入完成：新增 ${res.data.insertCount}，更新 ${res.data.updateCount}，跳过 ${res.data.skipCount}，失败 ${res.data.failCount}`)
      loadRecords()
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importing.value = false
  }
  return false // 阻止el-upload自动上传
}

const downloadErrors = async (importId) => {
  try {
    const res = await request.get(`/api/student/import/records/${importId}/errors`, { responseType: 'blob' })
    triggerDownload(res, `导入失败数据_${importId}.xlsx`)
  } catch (e) {
    ElMessage.error('错误数据下载失败')
  }
}

const loadRecords = async () => {
  loadingHistory.value = true
  try {
    const res = await request.get('/api/student/import/records', {
      params: { page: pagination.page, pageSize: pagination.pageSize }
    })
    if (res.code === '200') {
      records.value = res.data.list || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '加载导入历史失败')
    }
  } catch (e) {
    ElMessage.error('加载导入历史失败')
  } finally {
    loadingHistory.value = false
  }
}

const viewDetail = (importId) => {
  detailImportId.value = importId
  detailAction.value = ''
  detailPagination.page = 1
  detailDialogVisible.value = true
  loadDetail()
}

const loadDetail = async () => {
  if (!detailImportId.value) return
  detailLoading.value = true
  try {
    const res = await request.get(`/api/student/import/records/${detailImportId.value}`, {
      params: { action: detailAction.value, page: detailPagination.page, pageSize: detailPagination.pageSize }
    })
    if (res.code === '200') {
      detailList.value = res.data.list || []
      detailPagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载明细失败')
  } finally {
    detailLoading.value = false
  }
}

const actionText = (action) => ({
  insert: '新增', update: '更新', skip: '跳过', fail: '失败'
}[action] || action)

const actionTagType = (action) => ({
  insert: 'success', update: 'primary', skip: 'info', fail: 'danger'
}[action] || 'info')

onMounted(loadRecords)
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

.import-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.result-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 14px;
}

.result-card {
  border-radius: 12px;
  padding: 18px 12px;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.result-card .result-num {
  font-size: 28px;
  font-weight: 800;
}

.result-card .result-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.result-card.total .result-num { color: #475569; }
.result-card.insert .result-num { color: #16a34a; }
.result-card.update .result-num { color: #2563eb; }
.result-card.skip .result-num { color: #94a3b8; }
.result-card.fail .result-num { color: #dc2626; }

.result-actions {
  margin-top: 14px;
  display: flex;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 28px 0 12px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.count-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  margin: 0 3px;
  background: #f1f5f9;
  color: #475569;
}

.count-tag.insert { background: #dcfce7; color: #15803d; }
.count-tag.update { background: #dbeafe; color: #1d4ed8; }
.count-tag.skip { background: #f1f5f9; color: #64748b; }
.count-tag.fail { background: #fee2e2; color: #b91c1c; }

.detail-filter {
  margin-bottom: 14px;
}

.premium-table { width: 100%; }
.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }

:deep(.el-card) {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 12px 32px -16px rgba(15, 23, 42, 0.12);
}
</style>
