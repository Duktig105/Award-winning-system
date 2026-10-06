<template>
  <div class="page-container">
    <NavBar />
    <div class="content-container">
      <el-card class="elegant-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2 class="card-title">证书查重记录</h2>
              <p class="card-subtitle">每次证书上传的SHA-256完全重复与pHash相似检测结果，含关联申请和相似度</p>
            </div>
            <div class="header-actions">
              <el-input v-model="queryStudentNumber" placeholder="按学号搜索" clearable style="width: 200px" @keyup.enter="handleSearch" />
              <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
              <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
            </div>
          </div>
        </template>

        <el-table :data="records" v-loading="loading" class="premium-table" stripe>
          <el-table-column prop="checkId" label="ID" width="70" align="center" />
          <el-table-column label="申请人" width="150">
            <template #default="{ row }">
              <div>{{ row.studentName }}</div>
              <div class="text-muted">{{ row.studentNumber }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="applicationNumber" label="申请编号" width="150" />
          <el-table-column prop="competitionName" label="竞赛" min-width="160" show-overflow-tooltip />
          <el-table-column label="证书文件" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <el-image
                v-if="isImage(row.filePath)"
                :src="fileUrl(row.filePath)"
                :preview-src-list="[fileUrl(row.filePath)]"
                preview-teleported
                fit="cover"
                style="width: 44px; height: 44px; border-radius: 6px"
              />
              <span v-else>{{ row.fileName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="完全重复" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.exactDuplicate ? 'danger' : 'info'" size="small">
                {{ row.exactDuplicate ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="相似数" width="90" align="center">
            <template #default="{ row }">
              <span :class="{ 'sim-hot': row.similarCount > 0 }">{{ row.similarCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最高相似度" width="110" align="center">
            <template #default="{ row }">
              <span v-if="row.maxSimilarity">{{ (row.maxSimilarity * 100).toFixed(1) }}%</span>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskText(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="人工标记" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.manualMark" :type="markTagType(row.manualMark)" size="small">{{ markText(row.manualMark) }}</el-tag>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.checkStatus === 'done' ? 'success' : 'warning'" size="small">
                {{ row.checkStatus === 'done' ? '完成' : '异常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="checkTime" label="查重时间" width="170" align="center" />
          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewMatches(row)">相似明细</el-button>
              <el-button link type="warning" size="small" @click="recheck(row)">重新预检</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          class="mt-16"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </el-card>
    </div>

    <!-- 相似匹配明细 -->
    <el-dialog v-model="matchDialogVisible" title="相似证书明细" width="960px" class="premium-dialog">
      <el-empty v-if="!currentMatches.length" description="没有命中的相似证书" />
      <el-table v-else :data="currentMatches" stripe>
        <el-table-column label="匹配类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.matchType === 'exact' ? 'danger' : 'warning'" size="small">
              {{ row.matchType === 'exact' ? '完全相同' : 'pHash相似' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="相似度" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'sim-hot': row.similarity >= 0.9 }">{{ (row.similarity * 100).toFixed(1) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="关联申请" min-width="140">
          <template #default="{ row }">
            <div>{{ row.matchedApplicationNumber }}</div>
            <div class="text-muted">{{ row.matchedStudentName }}（{{ row.matchedStudentNumber }}）</div>
          </template>
        </el-table-column>
        <el-table-column prop="matchedCompetitionName" label="竞赛" min-width="140" show-overflow-tooltip />
        <el-table-column label="团队关系" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.teamRelated ? 'success' : 'danger'" size="small">
              {{ row.teamRelated ? '同团队' : '无团队关系' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="匹配文件" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="isImage(row.matchedFilePath)"
              :src="fileUrl(row.matchedFilePath)"
              :preview-src-list="[fileUrl(row.matchedFilePath)]"
              preview-teleported
              fit="cover"
              style="width: 44px; height: 44px; border-radius: 6px"
            />
            <span v-else class="text-muted">文件</span>
          </template>
        </el-table-column>
        <el-table-column label="已处理" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.handled ? 'success' : 'info'" size="small">{{ row.handled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import NavBar from '@/components/NavBar.vue'

const loading = ref(false)
const records = ref([])
const queryStudentNumber = ref('')
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const matchDialogVisible = ref(false)
const currentMatches = ref([])

const isImage = (path) => /\.(jpe?g|png|bmp|webp)$/i.test(path || '')

const fileUrl = (path) => (path ? `http://localhost:9998${path}` : '')

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/review/duplicate/list', {
      params: { studentNumber: queryStudentNumber.value, page: pagination.page, pageSize: pagination.pageSize }
    })
    if (res.code === '200') {
      records.value = res.data.list || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '加载查重记录失败')
    }
  } catch (e) {
    ElMessage.error('加载查重记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadRecords()
}

const handleRefresh = () => {
  queryStudentNumber.value = ''
  pagination.page = 1
  loadRecords()
}

const viewMatches = (row) => {
  currentMatches.value = row.matches || []
  matchDialogVisible.value = true
}

const recheck = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定对申请 ${row.applicationNumber} 重新执行证书预检（查重 + OCR + 风险计算）吗？`,
      '重新预检',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await request.post(`/api/review/duplicate/recheck/${row.applicationId}`)
    if (res.code === '200') {
      ElMessage.success('重新预检完成')
      loadRecords()
    } else {
      ElMessage.error(res.msg || '预检失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('预检失败')
  }
}

const riskText = (level) => ({ high: '高风险', medium: '中风险', low: '低风险', none: '无风险' }[level] || '未评估')
const riskTagType = (level) => ({ high: 'danger', medium: 'warning', low: 'info', none: 'success' }[level] || 'info')

const markText = (mark) => ({
  normal_reuse: '正常复用', abnormal_duplicate: '异常重复', undetermined: '无法判断'
}[mark] || mark)
const markTagType = (mark) => ({
  normal_reuse: 'success', abnormal_duplicate: 'danger', undetermined: 'warning'
}[mark] || 'info')

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
  align-items: center;
}

.text-muted {
  color: #94a3b8;
  font-size: 12px;
}

.sim-hot {
  color: #dc2626;
  font-weight: 700;
}

.premium-table { width: 100%; }
.mt-16 { margin-top: 16px; }

:deep(.el-card) {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 12px 32px -16px rgba(15, 23, 42, 0.12);
}
</style>
