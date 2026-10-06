<template>
  <div class="student-manage-page">
    <NavBar />
    
    <div class="content-container">
      <el-card class="manage-card">
        <template #header>
          <div class="card-header">
            <h2>学生管理</h2>
          </div>
        </template>
        
        <!-- 高级拟态统计面板 -->
        <div class="advanced-statistics-bar">
          <div 
            class="advanced-stat-card theme-total"
            :class="{ 'is-active': filterGrade === '' }"
            @click="handleGradeCardClick('')"
          >
            <div class="stat-label">学生总数</div>
            <div class="stat-value">{{ statistics.totalCount }}</div>
          </div>
          <template v-if="statistics.gradeStats && statistics.gradeStats.length > 0">
            <div 
              v-for="(item, index) in statistics.gradeStats" 
              :key="item.grade"
              class="advanced-stat-card"
              :class="['theme-grade-' + ((index % 4) + 1), { 'is-active': filterGrade === item.grade }]"
              @click="handleGradeCardClick(item.grade)"
            >
              <div class="stat-label">{{ item.grade }}学生</div>
              <div class="stat-value">{{ item.count }}</div>
            </div>
          </template>
          <!-- 缺省状态占位符，保持布局骨架 -->
          <template v-else>
            <div class="advanced-stat-card theme-grade-1">
              <div class="stat-label">22级学生</div>
              <div class="stat-value">0</div>
            </div>
            <div class="advanced-stat-card theme-grade-2">
              <div class="stat-label">23级学生</div>
              <div class="stat-value">0</div>
            </div>
            <div class="advanced-stat-card theme-grade-3">
              <div class="stat-label">24级学生</div>
              <div class="stat-value">0</div>
            </div>
            <div class="advanced-stat-card theme-grade-4">
              <div class="stat-label">25级学生</div>
              <div class="stat-value">0</div>
            </div>
          </template>
        </div>
        
        <!-- 操作栏 -->
        <div class="filter-bar">
          <div class="left-actions">
            <el-button type="primary" @click="handleAdd" :icon="Plus">
              新增学生
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
            <el-button type="warning" @click="handleResetPassword" :icon="Refresh">
              重置学生密码
            </el-button>
          </div>
          <div class="right-filters">
            <el-input
              v-model="searchKeyword"
              placeholder="请输入学号或姓名查询"
              clearable
              @input="handleSearch"
              style="width: 240px; margin-right: 20px"
              :prefix-icon="Search"
            />
            <el-button @click="openClassSelectDialog" style="margin-right: 10px">
              {{ filterClass ? filterClass : '选择班级' }}
            </el-button>
            <el-button 
              v-if="filterClass" 
              link 
              type="danger" 
              @click="clearClassFilter" 
              style="margin-right: 20px"
            >
              清除
            </el-button>
            <el-select
              v-model="filterGrade"
              placeholder="选择年级"
              clearable
              @change="handleSearch"
              style="width: 120px; margin-right: 20px"
            >
              <el-option v-for="g in dynamicGradeOptions" :key="g" :label="g" :value="g" />
            </el-select>
            <span style="color: #606266; font-size: 14px; white-space: nowrap; display: inline-block; min-width: 100px;">
              共 {{ pagination.total }} 条信息
            </span>
          </div>
        </div>
        
        <!-- 学生列表 -->
        <el-table
          :data="studentList"
          stripe
          v-loading="loading"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          :row-class-name="studentRowClassName"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="studentNumber" label="学号" width="120" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="grade" label="年级" width="100" align="center" />
          <el-table-column prop="major" label="专业" min-width="180" show-overflow-tooltip />
          <el-table-column prop="className" label="班级" min-width="260" show-overflow-tooltip />
          <el-table-column prop="college" label="学院" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="180" align="center">
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
        <div class="pagination-container">
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
        </div>
      </el-card>
    </div>

    <!-- 选择班级弹窗 -->
    <el-dialog
      v-model="classDialogVisible"
      title="选择班级"
      width="600px"
    >
      <div class="class-list" style="max-height: 400px; overflow-y: auto;">
        <el-button 
          v-for="c in classList" 
          :key="c" 
          @click="selectClass(c)"
          style="margin: 0 10px 10px 0;"
          :type="filterClass === c ? 'primary' : 'default'"
        >
          {{ c }}
        </el-button>
        <div v-if="classList.length === 0" style="color: #909399; text-align: center; margin-top: 20px;">
          暂无班级数据
        </div>
      </div>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
      class="premium-dialog"
      :destroy-on-close="true"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="学号" prop="studentNumber">
          <el-input
            v-model="formData.studentNumber"
            placeholder="请输入学号"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="studentName">
          <el-input
            v-model="formData.studentName"
            placeholder="请输入姓名"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-date-picker
            v-model="formData.grade"
            type="year"
            placeholder="请选择入学年份"
            format="YY级"
            value-format="YY级"
            style="width: 100%"
            popper-class="premium-picker-popper"
          />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input
            v-model="formData.major"
            placeholder="请输入专业"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item label="班级" prop="className">
          <el-input
            v-model="formData.className"
            placeholder="请输入班级"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item label="学院" prop="college">
          <el-input
            v-model="formData.college"
            placeholder="请输入学院"
            maxlength="50"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetPasswordDialogVisible"
      title="重置学生密码"
      width="600px"
      @close="handleResetPasswordDialogClose"
      class="premium-dialog"
    >
      <el-form
        ref="resetPasswordFormRef"
        :model="resetPasswordData"
        label-width="100px"
      >
        <el-form-item label="学号">
          <el-input
            v-model="resetPasswordData.studentNumber"
            placeholder="请输入学号，按回车搜索"
            @keyup.enter="handleStudentNumberSearch"
            @input="handleStudentNumberInput"
            clearable
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input
            v-model="resetPasswordData.studentName"
            placeholder="姓名"
            disabled
          />
        </el-form-item>
        <el-form-item label="年级">
          <el-input
            v-model="resetPasswordData.grade"
            placeholder="年级"
            disabled
          />
        </el-form-item>
        <el-form-item label="专业">
          <el-input
            v-model="resetPasswordData.major"
            placeholder="专业"
            disabled
          />
        </el-form-item>
        <el-form-item label="班级">
          <el-input
            v-model="resetPasswordData.className"
            placeholder="班级"
            disabled
          />
        </el-form-item>
        <el-form-item label="学院">
          <el-input
            v-model="resetPasswordData.college"
            placeholder="学院"
            disabled
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">关闭</el-button>
        <el-button 
          type="primary" 
          @click="confirmResetPassword"
          :disabled="!resetPasswordData.studentId"
        >
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Search, Upload, Download, Refresh } from '@element-plus/icons-vue'
import axios from 'axios'
import NavBar from '../components/NavBar.vue'

const API_BASE_URL = 'http://localhost:9998/api/student'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const formRef = ref(null)
const searchKeyword = ref('')
const filterGrade = ref('')
const filterClass = ref('')
const classDialogVisible = ref(false)
const classList = ref([])

const loadClasses = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/classes`)
    classList.value = response.data.data || []
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

const openClassSelectDialog = () => {
  loadClasses()
  classDialogVisible.value = true
}

const selectClass = (className) => {
  filterClass.value = className
  classDialogVisible.value = false
  handleSearch()
}

const clearClassFilter = () => {
  filterClass.value = ''
  handleSearch()
}


const selectedIds = ref([])

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.studentId)
}

const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的学生')
    return
  }
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 名学生吗？此操作不可恢复！`,
    '批量删除警告',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await axios.delete(`${API_BASE_URL}/batch-delete`, { data: selectedIds.value })
      ElMessage.success('批量删除成功')
      handleSearch()
    } catch (error) {
      ElMessage.error('批量删除失败')
    }
  })
}

const handleImport = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await axios.post(`${API_BASE_URL}/import`, formData)
    ElMessage.success(`导入成功: ${response.data.message || '操作完成'}`)
    handleSearch()
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.response?.data?.error || '请检查模板格式'))
  }
  return false
}

// 动态计算年级列表（基于后端返回的实际有数据的年级统计数据）
const dynamicGradeOptions = computed(() => {
  if (statistics.gradeStats && statistics.gradeStats.length > 0) {
    return statistics.gradeStats.map(item => item.grade)
  }
  // 兜底空数组，利用 allow-create 特性用户可以直接自行输入保存，保存后下一次请求统计就会包含自动成为了新选项
  return [] 
})

const statistics = reactive({
  totalCount: 0,
  gradeStats: []
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const studentList = ref([])

const formData = reactive({
  studentId: null,
  studentNumber: '',
  studentName: '',
  grade: '',
  major: '',
  className: '',
  college: ''
})

const formRules = {
  studentNumber: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  studentName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  grade: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  major: [
    { required: true, message: '请输入专业', trigger: 'blur' }
  ]
}

// 重置密码相关
const resetPasswordDialogVisible = ref(false)
const resetPasswordFormRef = ref(null)
const resetPasswordData = reactive({
  studentId: null,
  studentNumber: '',
  studentName: '',
  grade: '',
  major: '',
  className: '',
  college: ''
})

onMounted(() => {
  loadStatistics()
  loadStudentList()
})

const loadStatistics = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/statistics`)
    statistics.totalCount = response.data.totalCount || 0
    statistics.gradeStats = response.data.gradeStats || []
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadStudentList = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE_URL}/list`, {
      params: {
        studentNumber: searchKeyword.value,
        grade: filterGrade.value,
        className: filterClass.value,
        page: pagination.page,
        pageSize: pagination.pageSize
      }
    })
    studentList.value = response.data.list || []
    pagination.total = response.data.total || 0
  } catch (error) {
    ElMessage.error('加载学生列表失败')
    console.error('加载学生列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增学生'
  formData.studentId = null
  formData.studentNumber = ''
  formData.studentName = ''
  formData.grade = ''
  formData.major = ''
  formData.className = ''
  formData.college = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑学生'
  formData.studentId = row.studentId
  formData.studentNumber = row.studentNumber
  formData.studentName = row.studentName
  formData.grade = row.grade
  formData.major = row.major
  formData.className = row.className
  formData.college = row.college
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除学生"${row.studentName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`${API_BASE_URL}/delete/${row.studentId}`)
      ElMessage.success('删除成功')
      loadStatistics()
      loadStudentList()
    } catch (error) {
      ElMessage.error(error.response?.data?.error || '删除失败')
    }
  }).catch(() => {})
}



const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (formData.studentId) {
          await axios.put(`${API_BASE_URL}/update`, formData)
          ElMessage.success('更新成功')
        } else {
          await axios.post(`${API_BASE_URL}/add`, formData)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadStatistics()
        loadStudentList()
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
  loadStudentList()
}

// 点击卡片筛选年级
const handleGradeCardClick = (grade) => {
  filterGrade.value = grade
  handleSearch()
}

const handlePageChange = (page) => {
  pagination.page = page
  loadStudentList()
}

const handleSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadStudentList()
}



const handleDialogClose = () => {
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  // 手动重置表单数据
  formData.studentId = null
  formData.studentNumber = ''
  formData.studentName = ''
  formData.grade = ''
  formData.major = ''
  formData.className = ''
  formData.college = ''
}

// 动态计算行的渐变色主题类名
const studentRowClassName = ({ row, rowIndex }) => {
  // 如果没有年级，就按下标循环给一个伪随机色
  if (!row.grade) {
    return `theme-grade-${(rowIndex % 4) + 1}`
  }
  // 根据"23级"中的数字进行稳定的哈希
  const num = parseInt(row.grade.replace(/[^0-9]/g, '')) || rowIndex
  return `theme-grade-${(num % 4) + 1}`
}



const handleExport = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/export`, {
      params: {
        studentNumber: searchKeyword.value,
        grade: filterGrade.value,
        className: filterClass.value
      },
      responseType: 'blob'
    })
    
    const blob = new Blob([response.data], { 
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生列表.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
    console.error('导出失败:', error)
  }
}

// 重置密码相关方法
const handleResetPassword = () => {
  resetPasswordDialogVisible.value = true
}

const handleStudentNumberInput = () => {
  // 当学号输入框内容改变时，清空学生信息
  if (resetPasswordData.studentId) {
    resetPasswordData.studentId = null
    resetPasswordData.studentName = ''
    resetPasswordData.grade = ''
    resetPasswordData.major = ''
    resetPasswordData.className = ''
    resetPasswordData.college = ''
  }
}

const handleStudentNumberSearch = async () => {
  const studentNumber = resetPasswordData.studentNumber.trim()
  
  if (!studentNumber) {
    ElMessage.warning('请输入学号')
    return
  }
  
  try {
    const response = await axios.get(`${API_BASE_URL}/list`, {
      params: {
        studentNumber: studentNumber,
        page: 1,
        pageSize: 1
      }
    })
    
    if (response.data.list && response.data.list.length > 0) {
      const student = response.data.list[0]
      resetPasswordData.studentId = student.studentId
      resetPasswordData.studentName = student.studentName
      resetPasswordData.grade = student.grade
      resetPasswordData.major = student.major
      resetPasswordData.className = student.className
      resetPasswordData.college = student.college
    } else {
      // 未找到学生
      resetPasswordData.studentId = null
      resetPasswordData.studentName = ''
      resetPasswordData.grade = ''
      resetPasswordData.major = ''
      resetPasswordData.className = ''
      resetPasswordData.college = ''
      ElMessage.warning('未找到该学号对应的学生')
    }
  } catch (error) {
    console.error('搜索学生失败:', error)
    ElMessage.error('搜索学生失败')
  }
}

const confirmResetPassword = async () => {
  if (!resetPasswordData.studentId) {
    ElMessage.warning('请先搜索学生')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要将学生 ${resetPasswordData.studentName}(${resetPasswordData.studentNumber}) 的密码重置为 SY${resetPasswordData.studentNumber} 吗？`,
      '重置密码确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await axios.post(`${API_BASE_URL}/reset-password`, {
      studentId: resetPasswordData.studentId,
      studentNumber: resetPasswordData.studentNumber
    })
    
    if (response.data) {
      ElMessage.success('密码重置成功')
      resetPasswordDialogVisible.value = false
    } else {
      ElMessage.error('密码重置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error(error.response?.data?.error || '重置密码失败')
    }
  }
}

const handleResetPasswordDialogClose = () => {
  resetPasswordData.studentId = null
  resetPasswordData.studentNumber = ''
  resetPasswordData.studentName = ''
  resetPasswordData.grade = ''
  resetPasswordData.major = ''
  resetPasswordData.className = ''
  resetPasswordData.college = ''
}
</script>

<style scoped>
.student-manage-page {
  min-height: 100vh;
}

.content-container {
  max-width: 1320px;
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
  color: #303133;
}

/* ====== 高级拟态统计面板 ====== */
.advanced-statistics-bar {
  display: flex;
  flex-wrap: wrap; 
  gap: 20px;
  margin-bottom: 24px;
}

.advanced-stat-card {
  flex: 0 1 calc(20% - 16px); 
  min-width: 140px; 
  max-width: none;
  border-radius: 20px; /* 加大圆角 */
  padding: 24px 10px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05); /* 更实一点的初始阴影 */
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

/* 移除旧的空背景框，使用发光背板 */
.advanced-stat-card::after {
  display: none;
}
.advanced-stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: inherit;
  filter: blur(20px);
  opacity: 0;
  transition: opacity 0.4s ease;
  z-index: 0;
}

.advanced-stat-card:hover, .advanced-stat-card.is-active {
  transform: scale(1.02);
  border-width: 1px; /* 覆盖旧特效 */
}
.advanced-stat-card:hover::before, .advanced-stat-card.is-active::before { 
  opacity: 0.2; 
}

.advanced-stat-card .stat-label {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

.advanced-stat-card .stat-value {
  font-size: 38px;
  font-weight: 900;
  line-height: 1;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

/* ====== 多彩主题配色渐变 ====== */

/* 冰晶蓝 - 总数 */
.theme-total {
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.theme-total .stat-label { color: #4f46e5; }
.theme-total .stat-value { background-image: linear-gradient(to right, #4338ca, #818cf8); }
.theme-total:hover, .theme-total.is-active { 
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.85) 0%, rgba(224, 231, 255, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(79, 70, 229, 0.35);
  border-color: rgba(199, 210, 254, 0.9);
}

/* 樱花粉 - 第1年级 (22级) */
.theme-grade-1 {
  background: linear-gradient(135deg, rgba(255, 241, 242, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.theme-grade-1 .stat-label { color: #e11d48; }
.theme-grade-1 .stat-value { background-image: linear-gradient(to right, #be123c, #fb7185); }
.theme-grade-1:hover, .theme-grade-1.is-active { 
  background: linear-gradient(135deg, rgba(255, 241, 242, 0.85) 0%, rgba(255, 228, 230, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(225, 29, 72, 0.35);
  border-color: rgba(254, 205, 211, 0.9);
}

/* 元气橙 - 第2年级 (23级) */
.theme-grade-2 {
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.theme-grade-2 .stat-label { color: #ea580c; }
.theme-grade-2 .stat-value { background-image: linear-gradient(to right, #c2410c, #fb923c); }
.theme-grade-2:hover, .theme-grade-2.is-active { 
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.85) 0%, rgba(255, 237, 213, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(234, 88, 12, 0.35);
  border-color: rgba(254, 215, 170, 0.9);
}

/* 阳光黄 - 第3年级 (24级) */
.theme-grade-3 {
  background: linear-gradient(135deg, rgba(254, 252, 232, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.theme-grade-3 .stat-label { color: #ca8a04; }
.theme-grade-3 .stat-value { background-image: linear-gradient(to right, #a16207, #facc15); }
.theme-grade-3:hover, .theme-grade-3.is-active { 
  background: linear-gradient(135deg, rgba(254, 252, 232, 0.85) 0%, rgba(254, 249, 195, 0.6) 100%);
  box-shadow: 0 10px 40px -10px rgba(202, 138, 4, 0.35);
  border-color: rgba(254, 240, 138, 0.9);
}

/* 薄荷绿 - 第4年级 (25级) */
.theme-grade-4 {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.65) 0%, rgba(255, 255, 255, 0.3) 100%);
  border-color: rgba(255, 255, 255, 0.8);
}
.theme-grade-4 .stat-label { color: #16a34a; }
.theme-grade-4 .stat-value { background-image: linear-gradient(to right, #15803d, #4ade80); }
.theme-grade-4:hover, .theme-grade-4.is-active { 
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

.right-filters {
  display: flex;
  align-items: center;
}

/* ======== 表格与筛选区深度重构：现代玻璃与微缩阴影 ======== */

/* 搜索框与表单输入框拟态圆角化（长方形高级圆角，大气尺寸） */
:deep(.el-input__wrapper) {
  border-radius: 12px;
  background-color: rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
  padding: 8px 16px; /* 增加上下内边距，撑高表单 */
}
:deep(.el-input__inner) {
  font-size: 15px; /* 略微加大字体 */
  color: #334155;
  height: 32px; 
  line-height: 32px;
}

:deep(.el-input__wrapper:hover), :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15) !important;
  border-color: rgba(99, 102, 241, 0.4);
}

/* 弹窗高级宽敞布局 */
:deep(.premium-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}
:deep(.premium-dialog .el-dialog__header) {
  padding: 24px 30px;
  margin: 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(248, 250, 252, 0.5);
}
:deep(.premium-dialog .el-dialog__title) {
  font-weight: 700;
  font-size: 18px;
  color: #1e293b;
}
:deep(.premium-dialog .el-dialog__body) {
  padding: 35px 40px; /* 拉大主体区的上下左右边距 */
}
:deep(.premium-dialog .el-form-item) {
  margin-bottom: 24px; /* 增加表单项的纵向呼吸感间距 */
}
:deep(.premium-dialog .el-form-item__label) {
  font-weight: 600;
  color: #475569;
  padding-right: 20px;
}
:deep(.premium-dialog .el-dialog__footer) {
  padding: 20px 30px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(248, 250, 252, 0.5);
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

/* 警告按钮定制光效（重置密码） */
:deep(.el-button--warning) {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.85) 0%, rgba(251, 191, 36, 0.9) 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px -2px rgba(245, 158, 11, 0.2);
}
:deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, #f59e0b 0%, #fcd34d 100%);
  box-shadow: 0 6px 16px -2px rgba(245, 158, 11, 0.35);
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
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border-color: transparent !important;
  transform: scale(1.1);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.5) !important;
}

/* 解决原版 Element Plus 容器会截断大面积 hover shadow 的 Bug */
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
  position: static !important;
  margin-top: -2px;
  margin-left: -1px;
  transform: rotate(45deg) scaleY(0);
  transition: transform .15s ease-in .05s;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  transform: rotate(45deg) scaleY(1) !important;
}

/* 单元格字体精修 */
:deep(.el-table td.el-table__cell) {
  padding: 14px 0;
  color: #334155;
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

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

/* =========================================================
   📱 终极响应式适配 - Student.vue 移动端高定降维重塑
   ========================================================= */
@media screen and (max-width: 768px) {
  /* 🪟 1. 全局容器极简释放 - 取消左右无意义留白与双层卡片底色 */
  .content-container {
    padding: 0 !important;
  }
  .manage-card {
    margin-top: 0;
    border: none !important;
    border-radius: 0 !important;
    background: transparent !important;
    box-shadow: none !important;
  }
  :deep(.manage-card > .el-card__header) {
    padding: 12px 16px;
    border-bottom: none !important; /* 取消自带的分割线 */
  }
  :deep(.manage-card > .el-card__body) {
    padding: 0 12px 12px 12px !important; /* 顶部压缩，左右保留安全边距 */
  }

  /* 📊 2. 顶级数据看板 (首行独占 + 次行 2x2 网格) */
  .advanced-statistics-bar {
    display: grid !important; /* 致命修正：强行转网格以承载下面的 column 分配 */
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 12px !important;
    margin-bottom: 20px !important;
  }
  .advanced-stat-card.theme-total {
    grid-column: 1 / -1; /* 第一张“总数”卡横跨两列独占一行 */
    min-height: 90px;
  }
  .advanced-stat-card {
    min-height: 85px;
    padding: 16px 12px;
    flex: auto !important; /* 解除原有弹性计算 */
    max-width: none !important;
    min-width: 0 !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important; /* 卡片本身补充阴影 */
  }
  .stat-label {
    font-size: 13px;
  }
  .stat-value {
    font-size: 26px;
  }

  /* 🎛️ 3. 灵动操作栏精简堆叠 */
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    margin-bottom: 16px;
  }
  
  /* 隐藏大而无当的统计文字和桌面专属按钮 */
  .right-filters > span,
  .desktop-only {
    display: none !important;
  }

  /* 操作按钮组两两并排，圆角微距矩形高级光感 */
  .left-actions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: auto auto; /* 允许换行，重置密码按钮独占一行 */
    gap: 12px;
    width: 100%;
  }

  .left-actions .el-button {
    height: 38px;
    border-radius: 10px !important;
    font-size: 14px;
    font-weight: 700;
    margin: 0 !important;
    width: 100%;
    border: none !important;
    box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.3) !important;
  }
  


  /* 搜索及过滤栏紧凑化 */
  .right-filters {
    display: flex;
    flex-wrap: wrap; /* 允许跨行折叠 */
    justify-content: space-between;
    width: 100%;
    gap: 10px;
  }
  .right-filters > .el-input {
    width: 100% !important; /* 搜索框霸占一整行 */
    margin-right: 0 !important;
  }
  .right-filters > .el-button,
  .right-filters > .el-select {
    flex: 1; /* 班级、年级下拉框平分一行 */
    margin-right: 0 !important;
    margin-bottom: 0 !important;
  }

  /* 💳 4. 重磅：将原生表格降维重构为“工业级严谨网格”版 */
  :deep(.el-table),
  :deep(.el-table__inner-wrapper),
  :deep(.el-table__body-wrapper),
  :deep(.el-table__body),
  :deep(table),
  :deep(.el-table tbody),
  :deep(.el-scrollbar__view),
  :deep(.el-scrollbar__wrap) {
    display: block !important;
    width: 100% !important;
    box-sizing: border-box !important;
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
  }
  
  :deep(.el-table td.el-table__cell) {
    display: flex !important;
    align-items: center !important; /* 核心：强制所有行内元素垂直居中对齐 */
    box-sizing: border-box !important;
    border: none !important;
    background: transparent !important;
    padding: 0 !important;
    min-width: 0 !important;
    max-width: none !important;
  }
  
  :deep(.el-table__header-wrapper),
  :deep(.el-table colgroup) {
    display: none !important;
  }

  /* 行变卡片：采用严格的对齐网格逻辑 */
  :deep(.el-table__row) {
    display: flex !important;
    flex-wrap: wrap !important;
    width: 100% !important;
    box-sizing: border-box !important;
    background: #ffffff !important;
    border-radius: 12px;
    margin-bottom: 12px; 
    padding: 16px 14px 14px 14px !important;
    border: 1px solid #eef2f6;
    position: relative;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  /* 复选框 - 依旧悬浮独立 */
  :deep(.el-table td.el-table__cell:nth-child(1)) {
    position: absolute;
    top: 14px; right: 14px;
    width: auto !important;
    z-index: 10;
  }

  /* ---------------------------------
     第一排：学号(3) + 姓名(4) - 字体统一化
     --------------------------------- */
  :deep(.el-table td.el-table__cell:nth-child(3)) {
    width: 52% !important; 
    order: 1;
    margin-bottom: 12px;
  }
  :deep(.el-table td.el-table__cell:nth-child(3) .cell) {
    font-size: 14px; 
    color: #64748b;
    /* 移除 monospace，回归系统统一字体 */
    font-family: inherit; 
    font-weight: 400 !important;
  }
  
  :deep(.el-table td.el-table__cell:nth-child(4)) {
    width: 48% !important; 
    order: 2;
    margin-bottom: 12px;
    padding-left: 4px !important;
  }
  :deep(.el-table td.el-table__cell:nth-child(4) .cell) {
    font-size: 18px; 
    font-weight: 900 !important; /* 姓名保持最粗，形成视觉重心 */
    color: #0f172a; /* 极深蓝黑 */
    line-height: 1;
  }

  /* ---------------------------------
     第二排：专业(6) + 年级(5) - 背景纯化
     --------------------------------- */
  :deep(.el-table td.el-table__cell:nth-child(6)) {
    width: 52% !important;
    order: 3;
    margin-bottom: 10px;
  }
  
  :deep(.el-table td.el-table__cell:nth-child(5)) {
    width: 48% !important;
    order: 4;
    margin-bottom: 10px;
    padding-left: 4px !important;
  }
  :deep(.el-table td.el-table__cell:nth-child(5) .cell) {
    font-size: 12px;
    font-weight: 400 !important;
    padding: 0 !important; /* 移除内边距 */
    background: transparent !important; /* 移除背景框 */
    color: #64748b;
    letter-spacing: 0.5px;
  }

  /* ---------------------------------
     第三排：班级(7) + 学院(8) - 取消加粗
     --------------------------------- */
  :deep(.el-table td.el-table__cell:nth-child(7)) {
    width: 52% !important;
    order: 5;
    margin-bottom: 4px;
  }
  
  :deep(.el-table td.el-table__cell:nth-child(8)) {
    width: 48% !important;
    order: 6;
    margin-bottom: 4px;
    padding-left: 4px !important;
  }

  /* 单元格通用：强化非加粗设定 & 移除条目虚线 */
  :deep(.el-table td.el-table__cell .cell) {
    display: flex !important;
    align-items: center !important;
    justify-content: flex-start !important;
    text-align: left !important;
    font-size: 13.5px;
    color: #475569;
    font-weight: 400 !important;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    border-bottom: none !important; /* 移除底下白框/分割线 */
    padding-bottom: 0 !important;
  }
  
  :deep(.el-table td.el-table__cell:nth-child(6) .cell::before),
  :deep(.el-table td.el-table__cell:nth-child(7) .cell::before),
  :deep(.el-table td.el-table__cell:nth-child(8) .cell::before) { 
    font-size: 13px; width: 22px; display: inline-block; opacity: 0.8;
  }


  /* ---------------------------------
     第 4 排：操作栏(9)
     --------------------------------- */
  :deep(.el-table td.el-table__cell:nth-child(9)) {
    width: 100% !important;
    order: 7;
    margin-top: 6px;
    padding-top: 14px !important;
    border-top: 1px dashed rgba(226, 232, 240, 0.6) !important;
    display: block !important;
  }
  
  :deep(.el-table td.el-table__cell:nth-child(9) .cell) {
    display: flex;
    gap: 12px;
  }
  
  :deep(.el-table td.el-table__cell:nth-child(9) .el-button) {
    flex: 1; 
    height: 40px;
    border-radius: 10px;
    font-weight: 500; /* 适度降权，不加粗但保持清晰 */
    border: none !important;
  }
  
  /* 隐藏多余项 */
  :deep(.el-table td.el-table__cell:nth-child(2)) { display: none !important; }
  
  :deep(.el-table .cell) {
    padding: 0 !important;
    width: 100% !important;
    overflow: visible;
  }
  :deep(.el-table td.el-table__cell:nth-child(9) .cell) {
    display: flex;
    gap: 12px;
    padding-left: 0 !important;
    padding-right: 0 !important;
  }
  
  /* 按钮质感重塑 - 引入 Apple 风格的毛玻璃填涂 */
  :deep(.el-table td.el-table__cell:nth-child(9) .el-button) {
    flex: 1; 
    height: 42px; /* 增加点击舒适度 */
    margin: 0 !important;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 700;
    border: none !important;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  /* 编辑按钮 - 琉璃蓝 */
  :deep(.el-table td.el-table__cell:nth-child(9) .el-button--primary) {
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(99, 102, 241, 0.15) 100%) !important;
    color: #6366f1 !important;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.08);
  }
  
  /* 删除按钮 - 琉璃红 */
  :deep(.el-table td.el-table__cell:nth-child(9) .el-button--danger) {
    background: linear-gradient(135deg, rgba(244, 63, 94, 0.08) 0%, rgba(244, 63, 94, 0.12) 100%) !important;
    color: #f43f5e !important;
    box-shadow: 0 4px 12px rgba(244, 63, 94, 0.08);
  }

  /* 点击时的轻微缩放回弹动效 */
  :deep(.el-table td.el-table__cell:nth-child(9) .el-button:active) {
    transform: scale(0.96);
    filter: brightness(0.9);
  }
  
  /* 消除表格默认内容的占位符外溢 */
  :deep(.el-table .cell) {
    padding: 0 !important;
    width: 100% !important;
    overflow: visible;
  }

  /* 📑 5. 分页器神还原：绝不换行的紫晶单排 */
  .pagination-container {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin: 0 -10px; /* 补偿外层的 padding，让滑动域最大化 */
    padding: 0 10px;
    scrollbar-width: none;
  }
  .pagination-container::-webkit-scrollbar {
    display: none;
  }
  :deep(.el-pagination) {
    display: flex;
    flex-wrap: nowrap !important; 
    justify-content: center !important;
    align-items: center;
    padding: 16px 8px 24px 8px;
    gap: 6px; 
    max-width: 100%;
    box-sizing: border-box;
    background: transparent;
  }
  
  :deep(.el-pagination__total),
  :deep(.el-pagination__sizes),
  :deep(.el-pagination__jump) {
    display: none !important; /* 斩首所有冗余下拉 */
  }

  :deep(.el-pagination button),
  :deep(.el-pagination .el-pager li) {
    background: rgba(255, 255, 255, 0.6) !important;
    border-radius: 10px;
    height: 36px !important;
    min-width: 36px !important;
    line-height: 34px !important;
    font-size: 14px;
    font-weight: 600;
    color: #475569 !important;
    border: 1px solid rgba(226, 232, 240, 0.8) !important;
    box-sizing: border-box !important;
  }

  :deep(.el-pagination .el-pager li.is-active) {
    background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%) !important;
    color: #ffffff !important;
    border: 1px solid transparent !important; /* 防坍塌神技 */
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3), inset 0 1px 1px rgba(255, 255, 255, 0.4) !important;
    font-weight: 800;
  }

  /* =============================================
     💬 弹窗移动端适配：宽度、表单、按钮全方位优化
     ============================================= */

  /* 1. 所有弹窗宽度改为 95% 屏幕宽，防止溢出 */
  :deep(.el-dialog) {
    width: 95vw !important;
    max-width: 95vw !important;
    margin: 5vh auto !important;
    border-radius: 16px !important;
  }
  :deep(.el-dialog__header) {
    padding: 16px 20px 12px !important;
    border-bottom: 1px solid #f1f5f9;
  }
  :deep(.el-dialog__title) {
    font-size: 17px !important;
    font-weight: 700 !important;
  }
  :deep(.el-dialog__body) {
    padding: 16px 16px 8px !important;
  }

  /* 2. 表单字段：label 上移为顶对齐，缩小 label 宽度 */
  :deep(.el-form-item) {
    margin-bottom: 14px !important;
    flex-direction: column !important;
  }
  :deep(.el-form-item__label) {
    width: auto !important;
    text-align: left !important;
    line-height: 1.4 !important;
    padding-bottom: 4px !important;
    font-size: 13px !important;
    color: #64748b !important;
  }
  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  /* 3. 底部操作按钮：撑满全宽，高度适合拇指点击 */
  :deep(.el-dialog__footer) {
    padding: 12px 16px 16px !important;
    border-top: 1px solid #f1f5f9;
  }
  :deep(.el-dialog__footer .el-button) {
    flex: 1 !important;
    height: 44px !important;
    border-radius: 10px !important;
    font-size: 15px !important;
    font-weight: 600 !important;
    margin: 0 !important;
  }
  :deep(.el-dialog__footer .el-button + .el-button) {
    margin-left: 10px !important;
  }

  /* 4. 班级选择弹窗内的按钮列表优化 */
  .class-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding: 4px 0;
    max-height: 55vh !important;
  }
  .class-list .el-button {
    flex: 1 1 calc(50% - 5px) !important;
    min-width: 0 !important;
    margin: 0 !important;
    height: 40px !important;
    border-radius: 8px !important;
    font-size: 13px !important;
    white-space: normal !important;
    word-break: break-all !important;
  }

}
</style>

<style>
/* ====== 全局或弹出层日历美化 (强视觉冲击版) ====== */
.premium-picker-popper {
  border-radius: 16px !important;
  border: 1px solid rgba(99, 102, 241, 0.2) !important;
  box-shadow: 0 20px 50px -12px rgba(79, 70, 229, 0.25), 0 0 0 1px rgba(255, 255, 255, 0.5) inset !important;
  padding: 12px !important;
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%) !important;
  backdrop-filter: blur(20px);
}

/* 顶部年份区间标题区域 */
.premium-picker-popper .el-date-picker__header {
  margin: 8px 12px 20px;
  position: relative;
}
.premium-picker-popper .el-date-picker__header::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 10%;
  width: 80%;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
}

.premium-picker-popper .el-date-picker__header-label {
  font-weight: 800;
  color: #1e293b;
  font-size: 16px;
  letter-spacing: 0.5px;
}

/* 左右切页图标按钮 */
.premium-picker-popper .el-picker-panel__icon-btn {
  color: #6366f1;
  font-weight: bold;
  font-size: 16px;
  transition: all 0.3s;
}
.premium-picker-popper .el-picker-panel__icon-btn:hover {
  color: #4338ca;
  background-color: #e0e7ff;
  border-radius: 50%;
  transform: scale(1.1);
}

/* 年份单元格包裹层 */
.premium-picker-popper .el-year-table td {
  padding: 8px 4px;
}

/* 年份文本主体 */
.premium-picker-popper .el-year-table td .cell {
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  height: 44px;
  line-height: 44px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  color: #475569;
  position: relative;
  overflow: hidden;
  border: 1px solid transparent;
}

/* 悬停特效：浮现带边框的微光背景 */
.premium-picker-popper .el-year-table td .cell:hover {
  background: linear-gradient(135deg, #e0e7ff 0%, #ede9fe 100%);
  color: #4f46e5;
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 6px 15px -3px rgba(99, 102, 241, 0.15);
}

/* 选中当前年份：强烈的渐变冲击按钮 */
.premium-picker-popper .el-year-table td.current:not(.disabled) .cell {
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 8px 20px -4px rgba(99, 102, 241, 0.5), inset 0 2px 0 rgba(255, 255, 255, 0.3) !important;
  font-weight: 800;
  border: none;
  transform: scale(1.05);
}

/* 禁用年份的微弱存在感 */
.premium-picker-popper .el-year-table td.disabled .cell {
  background-color: transparent;
  color: #cbd5e1;
  font-weight: 400;
}
</style>
