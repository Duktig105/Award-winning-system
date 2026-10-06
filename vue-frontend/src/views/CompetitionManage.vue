<template>
  <div class="competition-manage-page">
    <NavBar />
    
    <div class="content-container">
      <el-card class="manage-card">
        <template #header>
          <div class="card-header">
            <h2>竞赛管理</h2>
          </div>
        </template>
        
        <!-- 高级数据大屏 -->
        <div class="premium-dashboard">
          <div class="elegant-card total" :class="{ active: activeCard === 'all' }" @click="handleCardClick('all')">
            <div class="card-content">
              <span class="card-title">竞赛总数</span>
              <div class="card-num">{{ statistics.totalCount }}</div>
            </div>
          </div>
          <div class="elegant-card level-a" :class="{ active: activeCard === 'A' }" @click="handleCardClick('A')">
            <div class="card-content">
              <span class="card-title">A等次赛事</span>
              <div class="card-num">{{ statistics.levelACount }}</div>
            </div>
          </div>
          <div class="elegant-card level-b" :class="{ active: activeCard === 'B' }" @click="handleCardClick('B')">
            <div class="card-content">
              <span class="card-title">B等次赛事</span>
              <div class="card-num">{{ statistics.levelBCount }}</div>
            </div>
          </div>
          <div class="elegant-card level-c" :class="{ active: activeCard === 'C' }" @click="handleCardClick('C')">
            <div class="card-content">
              <span class="card-title">C等次赛事</span>
              <div class="card-num">{{ statistics.levelCCount }}</div>
            </div>
          </div>
          <div class="elegant-card level-d" :class="{ active: activeCard === 'D' }" @click="handleCardClick('D')">
            <div class="card-content">
              <span class="card-title">D等次赛事</span>
              <div class="card-num">{{ statistics.levelDCount }}</div>
            </div>
          </div>
        </div>
        
        <!-- 操作栏 -->
        <div class="filter-bar">
          <div class="left-actions">
            <el-button type="primary" @click="handleAdd" :icon="Plus">
              新增竞赛
            </el-button>
            <el-button 
              type="danger" 
              :icon="Delete" 
              @click="handleBatchDelete"
              class="desktop-only"
            >
              批量删除
            </el-button>
            <el-upload
              class="desktop-only"
              action=""
              :show-file-list="false"
              :before-upload="handleImport"
              accept=".xlsx,.xls"
            >
              <el-button :icon="Upload">批量导入</el-button>
            </el-upload>
            <el-button class="desktop-only" :icon="Download" @click="handleExport">批量导出</el-button>
          </div>
          <el-input
            v-model="searchKeyword"
            placeholder="请输入竞赛名称查询"
            clearable
            @input="handleSearch"
            style="width: 300px"
            :prefix-icon="Search"
          />
        </div>
        
        <!-- 竞赛列表 -->
        <el-table
          :data="competitionList"
          stripe
          v-loading="loading"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          :row-class-name="tableRowClassName"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="competitionName" label="竞赛名称" min-width="200" />
          <el-table-column prop="awardRank" label="获奖等次" width="150" align="center">
            <template #default="{ row }">
              <el-tag :type="getAwardRankType(row.awardRank)">{{ row.awardRank }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="组队" width="110" align="center">
            <template #default="{ row }"><el-tag :type="row.teamEnabled ? 'success' : 'info'">{{ row.teamEnabled ? `${row.minTeamSize}-${row.maxTeamSize}人` : '未开放' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small" 
                :icon="Edit"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          :pager-count="5"
          style="margin-top: 20px; justify-content: flex-end"
        />
      </el-card>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(680px, 94vw)"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="竞赛名称" prop="competitionName">
          <el-input
            v-model="formData.competitionName"
            placeholder="请输入竞赛名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="获奖等次" prop="awardRank">
          <el-select
            v-model="formData.awardRank"
            placeholder="请选择获奖等次"
            style="width: 100%"
          >
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">竞赛组队规则</el-divider>
        <el-form-item label="开放组队"><el-switch v-model="formData.teamEnabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="竞赛类型"><el-select v-model="formData.competitionType" style="width:100%"><el-option label="团体赛" value="团体赛" /><el-option label="个人赛" value="个人赛" /></el-select></el-form-item>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="最少人数"><el-input-number v-model="formData.minTeamSize" :min="1" :max="50" /></el-form-item></el-col><el-col :span="12"><el-form-item label="最多人数"><el-input-number v-model="formData.maxTeamSize" :min="1" :max="100" /></el-form-item></el-col></el-row>
        <el-form-item label="组队时间"><el-date-picker v-model="teamTimeRange" type="datetimerange" start-placeholder="开放时间" end-placeholder="截止时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Search, Upload, Download } from '@element-plus/icons-vue'
import axios from 'axios'
import NavBar from '../components/NavBar.vue'

const API_BASE_URL = 'http://localhost:9998/api/competition-manage'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增竞赛')
const formRef = ref(null)
const searchKeyword = ref('')
const selectedIds = ref([])
const activeCard = ref('all') // 当前高亮的统计类别，默认为 'all'

const statistics = reactive({
  totalCount: 0,
  levelACount: 0,
  levelBCount: 0,
  levelCCount: 0,
  levelDCount: 0
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const competitionList = ref([])

const formData = reactive({
  competitionId: null,
  competitionName: '',
  awardRank: '',
  competitionType: '团体赛',
  minTeamSize: 2,
  maxTeamSize: 10,
  teamOpenTime: null,
  teamCloseTime: null,
  teamEnabled: 1
})
const teamTimeRange = ref([])

const formRules = {
  competitionName: [
    { required: true, message: '请输入竞赛名称', trigger: 'blur' }
  ],
  awardRank: [
    { required: true, message: '请选择获奖等次', trigger: 'change' }
  ]
}

onMounted(() => {
  loadStatistics()
  loadCompetitionList()
})

const loadStatistics = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/statistics`)
    statistics.totalCount = response.data.totalCount || 0
    statistics.levelACount = response.data.levelACount || 0
    statistics.levelBCount = response.data.levelBCount || 0
    statistics.levelCCount = response.data.levelCCount || 0
    statistics.levelDCount = response.data.levelDCount || 0
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadCompetitionList = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE_URL}/list`, {
      params: {
        competitionName: searchKeyword.value,
        awardRank: activeCard.value === 'all' ? null : activeCard.value,
        page: pagination.page,
        pageSize: pagination.pageSize
      }
    })
    competitionList.value = response.data.list || []
    pagination.total = response.data.total || 0
  } catch (error) {
    ElMessage.error('加载竞赛列表失败')
    console.error('加载竞赛列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCardClick = (type) => {
  if (activeCard.value === type) return // 若已选则无视
  activeCard.value = type
  searchKeyword.value = '' // 清除搜索框状态，体验更好
  pagination.page = 1
  loadCompetitionList()
}

const handleAdd = () => {
  dialogTitle.value = '新增竞赛'
  formData.competitionId = null
  formData.competitionName = ''
  formData.awardRank = ''
  Object.assign(formData, { competitionType: '团体赛', minTeamSize: 2, maxTeamSize: 10, teamOpenTime: null, teamCloseTime: null, teamEnabled: 1 })
  teamTimeRange.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑竞赛'
  formData.competitionId = row.competitionId
  formData.competitionName = row.competitionName
  formData.awardRank = row.awardRank
  Object.assign(formData, { competitionType: row.competitionType || '团体赛', minTeamSize: row.minTeamSize || 1, maxTeamSize: row.maxTeamSize || 10, teamOpenTime: row.teamOpenTime || null, teamCloseTime: row.teamCloseTime || null, teamEnabled: row.teamEnabled ?? 1 })
  teamTimeRange.value = row.teamOpenTime && row.teamCloseTime ? [row.teamOpenTime, row.teamCloseTime] : []
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除竞赛"${row.competitionName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${API_BASE_URL}/delete/${row.competitionId}`)
      ElMessage.success('删除成功')
      loadStatistics()
      loadCompetitionList()
    } catch (error) {
      ElMessage.error(error.response?.data?.error || '删除失败')
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的竞赛')
    return
  }
  
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个竞赛吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${API_BASE_URL}/batch-delete`, {
        data: selectedIds.value
      })
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      loadStatistics()
      loadCompetitionList()
    } catch (error) {
      ElMessage.error(error.response?.data?.error || '批量删除失败')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        formData.teamOpenTime = teamTimeRange.value?.[0] || null
        formData.teamCloseTime = teamTimeRange.value?.[1] || null
        if (formData.competitionId) {
          await axios.put(`${API_BASE_URL}/update`, formData)
          ElMessage.success('更新成功')
        } else {
          await axios.post(`${API_BASE_URL}/add`, formData)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadStatistics()
        loadCompetitionList()
      } catch (error) {
        ElMessage.error(error.response?.data?.error || '操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleSearch = () => {
  pagination.page = 1
  loadCompetitionList()
}

const handlePageChange = (page) => {
  pagination.page = page
  loadCompetitionList()
}

const handleSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadCompetitionList()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.competitionId)
}

const tableRowClassName = ({ row }) => {
  if (!row.awardRank) return ''
  return `rank-${row.awardRank.toLowerCase()}`
}

const handleDialogClose = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleImport = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  loading.value = true
  try {
    const response = await axios.post(`${API_BASE_URL}/import`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    const result = response.data
    if (result.errorCount > 0) {
      ElMessageBox.alert(
        `成功导入 ${result.successCount} 条，失败 ${result.errorCount} 条\n错误信息：\n${result.errors.join('\n')}`,
        '导入结果',
        { confirmButtonText: '确定' }
      )
    } else {
      ElMessage.success(`成功导入 ${result.successCount} 条数据`)
    }
    
    loadStatistics()
    loadCompetitionList()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '导入失败')
  } finally {
    loading.value = false
  }
  
  return false
}

const handleExport = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/export`, {
      params: {
        competitionName: searchKeyword.value
      },
      responseType: 'blob'
    })
    
    const blob = new Blob([response.data], { 
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '竞赛列表.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
    console.error('导出失败:', error)
  }
}

const getAwardRankType = (rank) => {
  const typeMap = {
    'A': 'danger',
    'B': 'warning',
    'C': 'success',
    'D': 'info'
  }
  return typeMap[rank] || 'info'
}
</script>

<style scoped>
.competition-manage-page {
  min-height: 100vh;
}

.content-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.manage-card {
  margin-top: 20px;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  border-radius: 20px !important;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1e293b;
  font-weight: 600;
}

/* 高级数据大屏 */
.premium-dashboard {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.elegant-card {
  position: relative;
  border-radius: 16px;
  padding: 24px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.elegant-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: inherit;
  filter: blur(20px);
  opacity: 0;
  transition: opacity 0.4s ease;
  z-index: 0;
}

.elegant-card:hover, .elegant-card.active {
  transform: scale(1.02);
  border-width: 1px;
}
.elegant-card:hover::before, .elegant-card.active::before { opacity: 0.2; }

.card-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  display: block;
  letter-spacing: 0.5px;
}

.card-num {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  transition: all 0.3s ease;
}

/* 具体颜色主题：高级白透毛玻璃 + 主题亮彩文字 */
.elegant-card.total {
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.elegant-card.total .card-title { color: #4f46e5; }
.elegant-card.total .card-num { background-image: linear-gradient(to right, #4338ca, #818cf8); }
.elegant-card.total:hover, .elegant-card.total.active { 
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.85) 0%, rgba(224, 231, 255, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(79, 70, 229, 0.35);
  border-color: rgba(199, 210, 254, 0.9);
}

.elegant-card.level-a {
  background: linear-gradient(135deg, rgba(255, 241, 242, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.elegant-card.level-a .card-title { color: #e11d48; }
.elegant-card.level-a .card-num { background-image: linear-gradient(to right, #be123c, #fb7185); }
.elegant-card.level-a:hover, .elegant-card.level-a.active { 
  background: linear-gradient(135deg, rgba(255, 241, 242, 0.85) 0%, rgba(255, 228, 230, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(225, 29, 72, 0.35);
  border-color: rgba(254, 205, 211, 0.9);
}

.elegant-card.level-b {
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.elegant-card.level-b .card-title { color: #ea580c; }
.elegant-card.level-b .card-num { background-image: linear-gradient(to right, #c2410c, #fb923c); }
.elegant-card.level-b:hover, .elegant-card.level-b.active { 
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.85) 0%, rgba(255, 237, 213, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(234, 88, 12, 0.35);
  border-color: rgba(254, 215, 170, 0.9);
}

.elegant-card.level-c {
  background: linear-gradient(135deg, rgba(254, 252, 232, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.elegant-card.level-c .card-title { color: #ca8a04; }
.elegant-card.level-c .card-num { background-image: linear-gradient(to right, #a16207, #facc15); }
.elegant-card.level-c:hover, .elegant-card.level-c.active { 
  background: linear-gradient(135deg, rgba(254, 252, 232, 0.85) 0%, rgba(254, 249, 195, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(202, 138, 4, 0.35);
  border-color: rgba(254, 240, 138, 0.9);
}

.elegant-card.level-d {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.elegant-card.level-d .card-title { color: #16a34a; }
.elegant-card.level-d .card-num { background-image: linear-gradient(to right, #15803d, #4ade80); }
.elegant-card.level-d:hover, .elegant-card.level-d.active { 
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.85) 0%, rgba(220, 252, 231, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(22, 163, 74, 0.35);
  border-color: rgba(187, 247, 208, 0.9);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0.2) 100%);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  padding: 16px 24px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 24px -6px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.left-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* ======== 表格与筛选区深度重构：现代玻璃与微缩阴影 ======== */

/* 搜索框与按钮拟态圆角化 */
:deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}
:deep(.el-input__wrapper:hover), :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15) !important;
  border-color: rgba(99, 102, 241, 0.4);
}
:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: none;
}
:deep(.el-button:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  filter: brightness(1.05); /* 仅用少许亮度变化代替位移，按客户要求不要往上跑 */
}

/* 主按钮定制光效（“新增竞赛/编辑” 等） - 调浅更温和的高级紫/蓝 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.85) 0%, rgba(129, 140, 248, 0.9) 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px -2px rgba(99, 102, 241, 0.2);
}
:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  box-shadow: 0 6px 16px -2px rgba(99, 102, 241, 0.35);
}

/* 危险按钮定制光效（“批量删除/删除” 等） - 调浅更通透的玫瑰色/水红 */
:deep(.el-button--danger) {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.85) 0%, rgba(251, 113, 133, 0.9) 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px -2px rgba(244, 63, 94, 0.2);
}
:deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #f43f5e 0%, #fb7185 100%);
  box-shadow: 0 6px 16px -2px rgba(244, 63, 94, 0.35);
}

/* 默认按钮定制光效（“批量导入/导出” 等） */
:deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.8);
  color: #475569;
  backdrop-filter: blur(4px);
}
:deep(.el-button--default:hover) {
  background: #ffffff;
  color: #4f46e5;
  border-color: #c7d2fe;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}

/* 主表格透明与拟态化 */
:deep(.el-table) {
  background-color: transparent !important;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  margin-top: 10px;
  --el-table-border-color: rgba(226, 232, 240, 0.4);
  --el-table-row-hover-bg-color: rgba(241, 245, 249, 0.7);
}

:deep(.el-table tr), :deep(.el-table th.el-table__cell) {
  background-color: transparent !important;
}

/* 斑马隔行弱化处理，保持通透感 */
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(248, 250, 252, 0.4) !important;
}

/* 表头重塑：微透强对比度 */
:deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, rgba(241, 245, 249, 0.8) 0%, rgba(248, 250, 252, 0.5) 100%) !important;
  color: #475569;
  font-weight: 700;
  font-size: 14px;
  height: 56px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.8) !important;
}

/* 行悬浮高级高亮：毛玻璃呼吸光 */
:deep(.el-table tbody tr:hover > td.el-table__cell) {
  background: linear-gradient(90deg, rgba(238, 242, 255, 0.4) 0%, rgba(224, 231, 255, 0.6) 50%, rgba(238, 242, 255, 0.4) 100%) !important;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

/* 单元格字体精修 */
:deep(.el-table td.el-table__cell) {
  padding: 14px 0;
  color: #334155;
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

/* ======== 多选框组件(Checkbox)果冻悬浮特效 ======== */
:deep(.el-checkbox__inner) {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  border: 1px solid rgba(147, 197, 253, 0.8) !important; /* 浅蓝边框 */
  background-color: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(4px);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); /* Q弹缩放 */
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 悬浮微扩与高亮 */
:deep(.el-checkbox:hover .el-checkbox__inner) {
  border-color: #3b82f6 !important; /* 亮蓝 */
  transform: scale(1.1);
  box-shadow: 0 0 10px rgba(59, 130, 246, 0.4);
}

/* 选中高定渐变背景及厚重阴影 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important; /* 天蓝到深蓝渐变 */
  border-color: transparent !important; /* 不用 none 以免边界塌陷，用透明色即可 */
  transform: scale(1.1);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.5) !important; /* 更均匀的全向光晕，不再像切断的阴影 */
}

/* 解决原版 Element Plus 容器会截断大面积 hover shadow 的 Bug */
/* 必须穿透到单元格 .cell 级别，否则表格单元的 `overflow: hidden` 会将上下的发光完美一刀切 */
:deep(.el-table .cell),
:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell),
:deep(.el-checkbox),
:deep(.el-checkbox__input) {
  overflow: visible !important;
}

/* 内部对勾精细居中对齐及发光重构 */
:deep(.el-checkbox__inner::after) {
  box-sizing: content-box;
  content: "";
  border: 2px solid white;
  border-left: 0;
  border-top: 0;
  height: 8px; 
  width: 4px;
  position: static !important; /* 弃用容易错位的绝对居中，采用外层 FLEX 对齐 */
  margin-top: -2px; /* 针对 L 形对勾进行视觉重心偏上微调补偿 */
  margin-left: -1px; /* 偏左视觉补偿 */
  transform: rotate(45deg) scaleY(0);
  transition: transform .15s ease-in .05s;
}

/* 强制重写组件原来的 transform 以确保绝对居中，避免 Element Plus 的默认覆盖 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  transform: rotate(45deg) scaleY(1) !important;
}

/* 单元格字体精修 */
:deep(.el-table td.el-table__cell) {
  padding: 14px 0;
  color: #334155;
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

/* 标签圆润化 */
:deep(.el-tag) {
  border-radius: 8px;
  border: none;
  font-weight: 600;
  letter-spacing: 0.5px;
  padding: 0 12px;
}
:deep(.el-tag--danger) { background-color: rgba(254, 226, 226, 0.8); color: #dc2626; }
:deep(.el-tag--warning) { background-color: rgba(254, 243, 199, 0.8); color: #d97706; }
:deep(.el-tag--success) { background-color: rgba(220, 252, 231, 0.8); color: #16a34a; }
:deep(.el-tag--info) { background-color: rgba(241, 245, 249, 0.8); color: #64748b; }

/* 分页器收尾圆角及悬浮加持 */
:deep(.el-pagination) {
  margin-top: 24px !important;
  padding: 10px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}
:deep(.el-pagination button), :deep(.el-pager li) {
  background: transparent !important;
  border-radius: 8px;
  transition: all 0.2s ease;
}
:deep(.el-pager li:hover) {
  color: #4f46e5;
  background: rgba(238, 242, 255, 0.8) !important;
}
:deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%) !important;
  color: #fff;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
  font-weight: bold;
}

/* ========================================= */
/* 📱 移动端深度适配 (CompetitionManage)       */
/* ========================================= */
@media screen and (max-width: 768px) {
  /* 外层容器边距 */
  .content-container {
    padding: 12px;
  }
  
  /* 卡片主容器消除刚硬边框 */
  :deep(.manage-card) {
    border-radius: 20px;
    border: none;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
  }
  :deep(.manage-card .el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px dashed rgba(226, 232, 240, 0.6);
  }
  .card-header h2 {
    font-size: 20px;
  }

  /* 💎 1. 顶部五联指标卡片：从横向滑动改为 1大 + 4小 的稳重宫格组合 */
  .premium-dashboard {
    display: grid;
    grid-template-columns: 1fr 1fr; /* 分两列 */
    gap: 12px;
    margin-bottom: 20px;
    padding-bottom: 0;
  }

  .elegant-card {
    height: auto;
    border-radius: 16px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }

  /* 竞赛总数（第一个）作为核心统率全宽，呈现开阔的大长方形 */
  .elegant-card.total {
    grid-column: 1 / -1;
    flex-direction: row;        /* 主卡片内部横向排布 */
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.08) 0%, rgba(129, 140, 248, 0.15) 100%);
    border: 1px solid rgba(99, 102, 241, 0.2);
  }

  .elegant-card.total .card-content {
    flex-direction: row;
    width: 100%;
    align-items: center;
    justify-content: space-between;
  }
  
  .elegant-card.total .card-title {
    font-size: 16px;
    font-weight: 700;
  }
  
  .elegant-card.total .card-num {
    font-size: 42px;
  }

  /* 子级别赛事（A/B/C/D）成为 2x2 精致小方块 */
  .elegant-card:not(.total) {
    padding: 14px 16px;
  }

  .card-content {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .card-title {
    font-size: 12px;
    opacity: 0.85;
    font-weight: 600;
  }
  
  .card-num {
    font-size: 28px;
    font-weight: 800;
    line-height: 1;
  }
  
  /* 🎛️ 2. 操作与搜索栏：从左右冲突改为上下分层的高定工具栏 */
  .filter-bar {
    display: flex;
    flex-direction: column-reverse; /* 搜索框重要性更高，在手机端将其置顶 */
    align-items: stretch;           /* 让内部元素左右撑满 */
    gap: 16px;
    margin-bottom: 16px;
    padding: 0;
  }

  /* 搜索框独占一行，变身大圆角胶囊 */
  .filter-bar > .el-input {
    width: 100% !important;
  }
  .filter-bar :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  }

  /* 四大操作按钮改为 2x2 网格并排拼图，或者灵活换行 */
  .left-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    width: 100%;
  }

  /* 手机端隐藏桌面专属按钮（批量删除、批量导入、批量导出） */
  .desktop-only {
    display: none !important;
  }
  
  /* 让剩余操作按钮占据一半，呈现统一按键质感 */
  .left-actions > .el-button,
  .left-actions > .el-upload {
    flex: 1 1 calc(50% - 10px); /* 每个差不多占手机一半宽 */
    margin: 0 !important;      /* 去掉 Element 自带的紧凑横向间距 */
  }
  
  .left-actions > .el-upload .el-button {
    width: 100%;
  }

  /* 📄 3. 核心大杀器：表格转为“精美卡片流”（Card Stream） */
  
  /* 隐藏传统的表头和底部边框 */
  :deep(.el-table__header-wrapper) {
    display: none !important;
  }
  :deep(.el-table__inner-wrapper::before) {
    display: none !important;
  }
  
  /* 为了体现卡片独立性，将外层设为全透明 */
  :deep(.el-table) {
    background: transparent !important;
    border: none !important;
    box-shadow: none !important; 
    margin-top: 0;
  }

  /* 每一行 `tr` 就是一个圆润漂亮的白金数据卡片 */
  :deep(.el-table__body-wrapper) {
    background: transparent !important;
  }
  
  /* 必须摧毁深层 table 渲染模式，否则宽度会缩水成一条棍子 */
  :deep(.el-table__body), 
  :deep(tbody) {
    display: block !important;
    width: 100% !important;
  }
  
  :deep(colgroup) {
    display: none !important;
  }
  
  :deep(.el-table__row) {
    display: flex !important;
    flex-direction: column !important;
    width: 100% !important;
    position: relative;
    
    /* 定义动态气泡与卡片的基准主色调，默认为原先的高级蓝紫 */
    --card-theme-color: #818cf8;
    --card-theme-bg: rgba(238, 242, 255, 0.9);
    --card-theme-shadow: rgba(99, 102, 241, 0.12);
    --card-theme-text: #4f46e5;
    --card-glow-shadow: rgba(99, 102, 241, 0.08);

    /* 采用更清透、更有呼吸感的空气质感渐变 */
    background: linear-gradient(145deg, rgba(255, 255, 255, 0.95) 0%, rgba(246, 248, 255, 0.8) 100%) !important;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 20px !important;
    padding: 24px 20px 20px 24px !important;
    margin-bottom: 20px !important;
    
    /* 引入带有弥散光感的三层阴影体系，由动态变量控制光晕色 */
    box-shadow: 
      0 10px 30px -5px var(--card-glow-shadow), 
      0 4px 12px -2px rgba(0, 0, 0, 0.04),
      inset 0 1px 1px rgba(255, 255, 255, 0.9) !important;
    transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
    
    /* 侧边栏与高光边界联动动态主题色 */
    border: 1px solid rgba(255, 255, 255, 0.6) !important;
    border-left: 5px solid var(--card-theme-color) !important; 
    --el-table-tr-bg-color: transparent !important;
    box-sizing: border-box;
  }

  /* 💎 动态层级注入：精准匹配上方四大指标卡的渐变基调与光感 */
  :deep(.el-table__row.rank-a) {
    --card-theme-color: #f43f5e; /* 玫瑰红 */
    --card-theme-bg: rgba(255, 241, 242, 0.9);
    --card-theme-shadow: rgba(244, 63, 94, 0.15);
    --card-theme-text: #e11d48;
    --card-glow-shadow: rgba(244, 63, 94, 0.08);
  }
  :deep(.el-table__row.rank-b) {
    --card-theme-color: #f97316; /* 橙金 */
    --card-theme-bg: rgba(255, 247, 237, 0.9);
    --card-theme-shadow: rgba(249, 115, 22, 0.15);
    --card-theme-text: #c2410c;
    --card-glow-shadow: rgba(249, 115, 22, 0.08);
  }
  :deep(.el-table__row.rank-c) {
    --card-theme-color: #eab308; /* 琉璃黄 */
    --card-theme-bg: rgba(254, 252, 232, 0.9);
    --card-theme-shadow: rgba(234, 179, 8, 0.15);
    --card-theme-text: #a16207;
    --card-glow-shadow: rgba(234, 179, 8, 0.08);
  }
  :deep(.el-table__row.rank-d) {
    --card-theme-color: #22c55e; /* 翡翠绿 */
    --card-theme-bg: rgba(240, 253, 244, 0.9);
    --card-theme-shadow: rgba(34, 197, 94, 0.15);
    --card-theme-text: #15803d;
    --card-glow-shadow: rgba(34, 197, 94, 0.08);
  }

  :deep(.el-table__row:active) {
    transform: scale(0.975);
    box-shadow: 
      0 4px 10px -2px var(--card-theme-shadow), 
      inset 0 1px 1px rgba(255, 255, 255, 0.8) !important;
  }

  /* 处理每个单元格 td */
  :deep(.el-table td.el-table__cell) {
    display: flex !important;
    align-items: flex-start !important;
    justify-content: flex-start !important;
    width: 100% !important;
    padding: 0 !important;
    border: none !important;
    background: transparent !important;
  }

  /* -- 第 1 列：复选框 (沉稳安静的右上角点缀) */
  :deep(.el-table td.el-table__cell:nth-child(1)) {
    position: absolute;
    top: 24px; /* 下沉一点，与左侧放大的 A 标签中心对齐 */
    right: 20px;
    width: auto !important;
    z-index: 5;
  }
  
  /* -- 第 2 列：序号 (隐藏) */
  :deep(.el-table td.el-table__cell:nth-child(2)) {
    display: none !important;
  }
  
  /* -- 第 4 列：等次徽章 (极简有力的顶级标签展示) */
  :deep(.el-table td.el-table__cell:nth-child(4)) {
    order: 1;
    margin-bottom: 16px; /* 进一步拉开与下方标题的距离，让呼吸感更强 */
  }
  :deep(.el-table td.el-table__cell:nth-child(4) .cell) {
    display: flex;
    justify-content: flex-start !important;
    align-items: center; /* 解决高宽挤压问题 */
  }
  /* 为不同等级动态赋予自带弥散外发光的微渐变小水晶牌 */
  :deep(.el-table td.el-table__cell:nth-child(4) .el-tag) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 14px; /* 字号再稍微调大，更像独立的业务 Icon */
    font-weight: 900;
    height: 28px; /* 强制高度，解决压扁拉伸 */
    padding: 0 14px;
    border-radius: 8px;
    letter-spacing: 0.5px;
    border: none;
    /* 核心修复：彻底接入动态主题色管道，跟随整卡变色 */
    background: var(--card-theme-bg);
    color: var(--card-theme-text);
    box-shadow: 0 3px 10px var(--card-glow-shadow), inset 0 1px 1px rgba(255,255,255,0.8);
  }

  /* -- 第 3 列：竞赛名称 (空灵且不失稳重的标题重心) */
  :deep(.el-table td.el-table__cell:nth-child(3)) {
    order: 2;
    font-size: 16px; /* 字体稍微收敛一点，配合取消加粗 */
    font-weight: 500; /* 取消超粗体，改为具有墨水感的中等字重 */
    color: #1e293b;
    margin-bottom: 24px;
    padding-right: 36px !important; /* 给右上角多留出一点空间防碰撞 */
    line-height: 1.5;
    letter-spacing: 0.3px;
    text-shadow: 0 1px 2px rgba(255,255,255,0.8);
  }
  
  /* -- 第 5 列：操作按钮组 (全方位重造：打造有呼吸感的高亮磨砂按键) */
  :deep(.el-table td.el-table__cell:nth-child(5)) {
    order: 3;
  }
  :deep(.el-table td.el-table__cell:nth-child(5) .cell) {
    display: flex;
    justify-content: space-between;
    width: 100%;
    gap: 12px;
  }
  
  /* 基础按钮底座：剥离所有生硬边框与深色填充 */
  :deep(.el-table td.el-table__cell:nth-child(5) .el-button) {
    margin: 0 !important;
    flex: 1;
    border-radius: 12px;
    height: 40px;
    font-size: 14px;
    font-weight: 700;
    border: none !important;
    position: relative;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  }
  
  /* 💡 编辑按钮：琉璃荧光蓝（极透背景 + 主题文字 + 同色系微光） */
  :deep(.el-table td.el-table__cell:nth-child(5) .el-button--primary) {
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.12) 0%, rgba(129, 140, 248, 0.08) 100%) !important;
    color: #4f46e5 !important;
    box-shadow: inset 0 0 0 1px rgba(99, 102, 241, 0.1) !important;
  }
  :deep(.el-table td.el-table__cell:nth-child(5) .el-button--primary:active) {
    background: rgba(99, 102, 241, 0.2) !important;
  }

  /* 🚫 删除按钮：水樱蔷薇红（极透背景 + 危险文字 + 同色系微光） */
  :deep(.el-table td.el-table__cell:nth-child(5) .el-button--danger) {
    background: linear-gradient(135deg, rgba(244, 63, 94, 0.1) 0%, rgba(251, 113, 133, 0.06) 100%) !important;
    color: #e11d48 !important;
    box-shadow: inset 0 0 0 1px rgba(244, 63, 94, 0.1) !important;
  }
  :deep(.el-table td.el-table__cell:nth-child(5) .el-button--danger:active) {
    background: rgba(244, 63, 94, 0.2) !important;
  }

  /* 消除单元格默认的 cell 左右 padding，使其完全受外部 flex 支配 */
  :deep(.el-table .cell) {
    padding: 0 !important;
    width: 100% !important;
  }

  /* 📑 4. 移动端专属分页器深度美化 (极简高定风) */
  :deep(.el-pagination) {
    display: flex;
    flex-wrap: nowrap !important; /* 不准再换行！强制保持一字长蛇阵 */
    justify-content: center !important;
    align-items: center;
    padding: 16px 8px 24px 8px;
    margin-top: 10px;
    gap: 6px; /* 间距拉小一点点 */
    max-width: 100%;
    box-sizing: border-box;
    overflow-x: auto; /* 给极致小屏(如 iPhone SE) 保留一丝丝最后的滑动尊严(隐藏滚动条) */
    -webkit-overflow-scrolling: touch;
  }
  
  /* 隐藏滚动条让它像原生横滑区一样干净 */
  :deep(.el-pagination)::-webkit-scrollbar {
    display: none;
  }
  
  /* 隐藏在手机端冗余的统计与跳转信息，只保留最核心的页码列 */
  :deep(.el-pagination__total),
  :deep(.el-pagination__sizes),
  :deep(.el-pagination__jump) {
    display: none !important;
  }

  /* 上下页按钮基础形态优化 */
  :deep(.el-pagination button) {
    background: rgba(255, 255, 255, 0.6) !important;
    border-radius: 10px;
    height: 36px !important;
    min-width: 36px !important;
    color: #64748b !important;
    border: 1px solid rgba(226, 232, 240, 0.8) !important;
    transition: all 0.2s ease;
    box-sizing: border-box !important;
  }
  
  /* 常规页码块水晶化 */
  :deep(.el-pagination .el-pager li) {
    background: rgba(255, 255, 255, 0.6) !important;
    border-radius: 10px;
    height: 36px !important;
    min-width: 36px !important;
    line-height: 34px !important; /* 减去边框修正行高，避免文字偏下 */
    font-size: 14px;
    font-weight: 600;
    color: #475569 !important;
    border: 1px solid rgba(226, 232, 240, 0.8) !important;
    transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
    box-sizing: border-box !important;
  }

  /* 活动页码（当前页）终极高光光效：与上方新增按钮光泽呼应的高级蓝紫渐变 */
  :deep(.el-pagination .el-pager li.is-active) {
    background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%) !important;
    color: #ffffff !important;
    /* 核心修复：用透明边框占位，永远不要取消它的边框导致盒模型坍塌尺寸不一致 */
    border: 1px solid transparent !important;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3), inset 0 1px 1px rgba(255, 255, 255, 0.4) !important;
    font-weight: 800;
  }
  :deep(.el-pagination .el-select) {
    margin: 0 !important;
  }
  
  /* 移动端操作按钮组精简与高定化 */
  :deep(.filter-bar .left-actions) {
    display: flex;
    justify-content: space-between;
    width: 100%;
    gap: 12px;
  }
  
  :deep(.filter-bar .left-actions .el-button) {
    flex: 1; /* 均分可用宽度 */
    height: 38px;
    margin: 0 !important;
    border-radius: 10px !important; /* 拒绝药丸圆角，使用克制的微圆矩形 */
    font-size: 14px;
    font-weight: 700;
    border: none !important;
    letter-spacing: 0.5px;
    transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  /* 顶部新增竞赛：莹润透光蓝 */
  :deep(.filter-bar .left-actions .el-button--primary) {
    background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%) !important;
    color: #fff !important;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25), inset 0 1px 1px rgba(255, 255, 255, 0.3) !important;
  }
  :deep(.filter-bar .left-actions .el-button--primary:active) {
    transform: scale(0.96);
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.2) !important;
  }

  /* 顶部批量删除：晶透玫瑰红 */
  :deep(.filter-bar .left-actions .el-button--danger) {
    background: linear-gradient(135deg, #f43f5e 0%, #fb7185 100%) !important;
    color: #fff !important;
    box-shadow: 0 4px 12px rgba(244, 63, 94, 0.25), inset 0 1px 1px rgba(255, 255, 255, 0.3) !important;
  }
  :deep(.filter-bar .left-actions .el-button--danger:active) {
    transform: scale(0.96);
    box-shadow: 0 2px 6px rgba(244, 63, 94, 0.2) !important;
  }

  /* 🪟 5. 弹窗自适应优化 */
  :deep(.el-dialog) {
    width: 90% !important;
    border-radius: 16px !important;
  }
}
</style>
