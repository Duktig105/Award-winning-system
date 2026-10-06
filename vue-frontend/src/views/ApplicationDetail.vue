<template>
  <div class="application-detail-page">
    <NavBar />
    
    <div class="content-container">
      <el-card class="detail-card" v-loading="loading">
        <template #header>
          <div class="card-header">
            <div>
              <h2>申请详情</h2>
              <div class="application-number">申请编号：{{ detail.applicationNumber }}</div>
            </div>
            <div class="header-actions">
              <el-tag 
                :type="getStatusType(detail.applicationStatus)" 
                size="large"
                effect="dark"
              >
                {{ detail.statusText }}
              </el-tag>
              <el-button @click="goBack" :icon="Back">返回</el-button>
            </div>
          </div>
        </template>
        
        <div class="detail-content" v-if="!loading && detail.applicationId">
          <!-- 基本信息 -->
          <div class="info-section">
            <h3 class="section-title">基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="竞赛名称">
                {{ detail.competitionName }}
              </el-descriptions-item>
              <el-descriptions-item label="项目名称">
                {{ detail.projectName }}
              </el-descriptions-item>
              <el-descriptions-item label="团队名称" v-if="detail.teamName">
                {{ detail.teamName }}
              </el-descriptions-item>
              <el-descriptions-item label="竞赛级别">
                {{ detail.competitionLevel }}
              </el-descriptions-item>
              <el-descriptions-item label="获奖等级">
                <el-tag type="success">{{ detail.awardLevel }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="获奖名次">
                {{ detail.awardRank }}
              </el-descriptions-item>
              <el-descriptions-item label="获奖时间">
                {{ detail.awardTime }}
              </el-descriptions-item>
              <el-descriptions-item label="获奖数量">
                {{ detail.awardQuantity }}
              </el-descriptions-item>
              <el-descriptions-item label="获奖人数">
                {{ detail.awardPersonCount }}
              </el-descriptions-item>
              <el-descriptions-item label="联系方式">
                {{ detail.contact }}
              </el-descriptions-item>
              <el-descriptions-item label="提交时间">
                {{ formatDateTime(detail.createTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <!-- 指导老师信息 -->
          <div class="info-section" v-if="detail.teachers && detail.teachers.length > 0">
            <h3 class="section-title">指导老师信息</h3>
            <el-table :data="detail.teachers" border stripe>
              <el-table-column prop="teacherName" label="姓名" width="120" />
              <el-table-column prop="teacherDepartment" label="部门" min-width="150" />
              <el-table-column prop="teacherNo" label="工号" width="120" />
            </el-table>
          </div>
          
          <!-- 团队成员信息 -->
          <div class="info-section" v-if="detail.teamMembers && detail.teamMembers.length > 0">
            <h3 class="section-title">团队成员信息</h3>
            <el-table :data="detail.teamMembers" border stripe>
              <el-table-column prop="sortOrder" label="排序" width="80" />
              <el-table-column label="成员姓名" width="120">
                <template #default="{ row }">
                  {{ row.studentName || row.externalName }}
                </template>
              </el-table-column>
              <el-table-column label="学号" width="150">
                <template #default="{ row }">
                  {{ row.studentNumber || row.externalNumber }}
                </template>
              </el-table-column>
              <el-table-column label="学校" min-width="150">
                <template #default="{ row }">
                  {{ row.externalSchool || '本校' }}
                </template>
              </el-table-column>
              <el-table-column label="是否队长" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isLeader === 1 || row.isLeader === true" type="success" size="small">队长</el-tag>
                  <span v-else>队员</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <!-- 证明材料 -->
          <div class="info-section" v-if="detail.files && detail.files.length > 0">
            <h3 class="section-title">证明材料</h3>
            <div class="file-list">
              <div 
                v-for="file in detail.files" 
                :key="file.fileId" 
                class="file-item"
              >
                <div class="file-info">
                  <el-icon class="file-icon"><Document /></el-icon>
                  <div class="file-details">
                    <div class="file-name">{{ file.fileName }}</div>
                    <div class="file-meta">
                      {{ formatFileSize(file.fileSize) }} | 
                      {{ formatDateTime(file.uploadTime) }}
                    </div>
                  </div>
                </div>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="downloadFile(file)"
                  :icon="Download"
                >
                  下载
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <el-empty v-if="!loading && !detail.applicationId" description="申请不存在" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Document, Download } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import request from '../utils/request'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const detail = ref({})

// 状态文本映射
const statusTextMap = {
  'pending': '待审核',
  'approved': '已通过',
  'rejected': '已拒绝',
  'returned': '已打回'
}

onMounted(() => {
  const id = route.params.id
  if (id) {
    loadDetail(id)
  }
})

const loadDetail = async (id) => {
  loading.value = true
  try {
    const res = await request.get(`/application/record/detail/${id}`)
    
    if (res.code === '200') {
      detail.value = {
        ...res.data,
        statusText: statusTextMap[res.data.applicationStatus] || res.data.applicationStatus
      }
    } else {
      ElMessage.error(res.msg || '加载申请详情失败')
    }
  } catch (error) {
    console.error('加载申请详情失败:', error)
    ElMessage.error('加载申请详情失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const typeMap = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger',
    'returned': 'info'
  }
  return typeMap[status] || 'info'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const downloadFile = async (file) => {
  try {
    const response = await axios.get(`http://localhost:9998${file.filePath}`, { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName || '附件'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('附件下载失败')
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.application-detail-page {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.detail-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0 0 5px 0;
  font-size: 20px;
  color: #333;
}

.application-number {
  font-size: 14px;
  color: #666;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.detail-content {
  padding: 20px 0;
}

.info-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background-color: #f9f9f9;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s;
}

.file-item:hover {
  background-color: #f0f0f0;
  border-color: #409eff;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.file-icon {
  font-size: 32px;
  color: #409eff;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.file-meta {
  font-size: 12px;
  color: #999;
}
</style>
