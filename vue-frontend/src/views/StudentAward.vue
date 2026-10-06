<template>
  <div class="student-award-page login-wrapper-style">
    <NavBar />
    
    <div class="content-container">
      <el-card class="form-card">
        <template #header>
          <div class="card-header premium-header">
            <div class="header-title-wrapper">
              <div class="decorative-line"></div>
              <h2>竞赛获奖申请</h2>
              <el-tag size="small" effect="light" class="version-tag">V2.0</el-tag>
            </div>
            <div class="header-actions">
              <el-radio-group v-model="applicationType" @change="handleTypeChange" class="custom-radio-group">
                <el-radio-button value="individual">
                  <el-icon><User /></el-icon> 个人赛
                </el-radio-button>
                <el-radio-button value="team">
                  <el-icon><UserFilled /></el-icon> 团体赛
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </template>
        
        <el-form 
          ref="formRef" 
          :model="formData" 
          :rules="rules" 
          label-width="140px"
          class="application-form"
        >
          <!-- 基本信息 -->
          <div class="form-section">
            <h3 class="section-title">
              <span class="title-with-icon">
                <el-icon class="title-icon"><Document /></el-icon>
                基本信息
              </span>
            </h3>
            
            <el-form-item prop="competitionName" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap" style="display: flex; align-items: center; gap: 8px;">
                  <span class="custom-label">赛事名称</span>
                </div>
              </template>
              
              <div class="competition-select-trigger mobile-stack">
                <el-select 
                  v-model="formData.competitionName" 
                  placeholder="在此直接选择常见赛事，或点击右侧搜索" 
                  class="flex-1-select"
                  clearable
                  filterable
                  allow-create
                  default-first-option
                  @change="handleCompetitionSelectChange"
                >
                  <el-option
                    v-for="name in commonCompetitionNames"
                    :key="name"
                    :label="name"
                    :value="name"
                  />
                </el-select>
                <el-button 
                  class="premium-search-btn"
                  @click="openCompetitionDialog"
                >
                  <el-icon style="margin-right: 4px;"><Search /></el-icon> <span class="search-btn-text">搜索赛事</span>
                </el-button>
              </div>
            </el-form-item>
            
            <el-form-item label="项目名称" prop="projectName">
              <el-input 
                v-model="formData.projectName" 
                placeholder="请输入项目名称（没有则填“无”）"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item label="团队名称" prop="teamName" v-if="applicationType === 'team'">
              <el-input 
                v-model="formData.teamName" 
                placeholder="请输入团队名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="已锁定队伍" v-if="applicationType === 'team'">
              <el-select v-model="formData.teamId" placeholder="可选择组队板块中已锁定的队伍" clearable style="width:100%" @change="applyLockedTeam">
                <el-option v-for="team in lockedTeams" :key="team.teamId" :label="`${team.teamName} · ${team.competitionName}`" :value="team.teamId" />
              </el-select>
              <div class="locked-team-tip">选择后将自动带入竞赛、队伍名称与正式成员；不选择时仍可手工填写。</div>
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="竞赛级别" prop="competitionLevel">
                  <el-select v-model="formData.competitionLevel" placeholder="请选择" style="width: 100%">
                    <el-option label="国家级" value="国家级" />
                    <el-option label="省级" value="省级" />
                    <el-option label="校级" value="校级" />
                    <el-option label="院级" value="院级" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="获奖等次" prop="awardRank">
                  <el-input 
                    v-model="formData.awardRank" 
                    placeholder="选择竞赛后自动填充" 
                    readonly
                    disabled
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="获奖等级" prop="awardLevel">
                  <el-input v-model="formData.awardLevel" placeholder="如：金奖/一等奖/M奖" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="获奖时间" prop="awardTime">
                  <el-date-picker
                    v-model="formData.awardTime"
                    type="date"
                    placeholder="选择日期"
                    style="width: 100%"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    popper-class="premium-date-picker"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="联系方式" prop="contact">
              <el-input 
                v-model="formData.contact" 
                placeholder="请输入负责人手机号"
              />
            </el-form-item>
          </div>
          
          <!-- 指导老师信息 -->
          <div class="form-section">
            <h3 class="section-title">
              <span class="title-with-icon">
                <el-icon class="title-icon"><Avatar /></el-icon>
                指导老师信息
              </span>
              <el-button 
                type="primary" 
                size="large"
                class="premium-btn-small" 
                @click="openMentorDialog"
                :icon="Plus"
              >
                添加指导老师
              </el-button>
            </h3>
            
            <div v-if="!formData.teachers || formData.teachers.length === 0" class="empty-tip">
              <el-empty description="暂无指导老师信息" :image-size="60" />
            </div>
            
            <draggable
              v-model="formData.teachers"
              item-key="teacherNo"
              handle=".drag-handle"
              animation="200"
              class="mentor-list"
              ghost-class="drag-ghost"
              drag-class="drag-active"
            >
              <template #item="{ element: teacher, index }">
                <div class="mentor-card glass-card">
                  <el-icon class="drag-handle" title="拖动排序"><Rank /></el-icon>
                  <span class="mentor-title">指导老师{{ index + 1 }}</span>
                  <div class="info-item">
                    <span class="info-label">姓名：</span>
                    <span class="info-value">{{ teacher.teacherName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">学院：</span>
                    <span class="info-value">{{ teacher.teacherDepartment }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">工号：</span>
                    <span class="info-value">{{ teacher.teacherNo }}</span>
                  </div>
                  <el-button 
                    type="danger" 
                    size="small"
                    :icon="Delete" 
                    @click="removeMentor(index)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </draggable>
          </div>
          
          <!-- 团队成员信息 (仅团体赛) -->
          <div class="form-section" v-if="applicationType === 'team' && !formData.teamId">
            <h3 class="section-title">
              <span class="title-with-icon">
                <el-icon class="title-icon"><User /></el-icon>
                团队成员信息
              </span>
              <el-button 
                type="primary" 
                size="large"
                class="premium-btn-small" 
                @click="openMemberDialog"
                :icon="Plus"
              >
                添加成员
              </el-button>
            </h3>
            
            <div class="member-instruction-row premium-instruction">
              <div class="instruction-content">
                <el-icon class="instruction-icon"><InfoFilled /></el-icon>
                <span class="instruction-text">请添加所有团队成员的信息</span>
              </div>
              <el-radio-group v-model="formData.confirmMemberOrder" size="small" class="custom-radio-group-small">
                <el-radio-button :value="false">无排序</el-radio-button>
                <el-radio-button :value="true">按序号排序</el-radio-button>
              </el-radio-group>
            </div>
            
            <div v-if="!formData.teamMembers || formData.teamMembers.length === 0" class="empty-tip">
              <el-empty description="暂无团队成员信息" :image-size="60" />
            </div>
            
            <draggable
              v-model="formData.teamMembers"
              item-key="studentNumber"
              handle=".drag-handle"
              animation="200"
              class="member-list"
              ghost-class="drag-ghost"
              drag-class="drag-active"
            >
              <template #item="{ element: member, index }">
                <div class="member-card glass-card">
                  <el-icon class="drag-handle" title="拖动排序"><Rank /></el-icon>
                  <span class="member-title">
                    <template v-if="formData.confirmMemberOrder">
                      排序{{ index + 1 }}
                    </template>
                    <template v-else>
                      成员{{ index + 1 }}
                    </template>
                  </span>
                  <div class="info-item">
                    <el-tag :type="member.isExternal ? 'warning' : 'success'" size="small">
                      {{ member.isExternal ? '校外' : '校内' }}
                    </el-tag>
                  </div>
                  <div class="info-item">
                    <span class="info-label">姓名：</span>
                    <span class="info-value">{{ member.isExternal ? member.externalName : member.studentName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">{{ member.isExternal ? '学校' : '学院' }}：</span>
                    <span class="info-value">{{ member.isExternal ? member.externalSchool : member.studentDepartment }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">学号：</span>
                    <span class="info-value">{{ member.isExternal ? member.externalNumber : member.studentNumber }}</span>
                  </div>
                  <div class="info-item" v-if="formData.confirmMemberOrder ? index === 0 : member.isLeader">
                    <el-tag type="danger" size="small">队长</el-tag>
                  </div>
                  <el-button 
                    type="danger" 
                    size="small"
                    :icon="Delete" 
                    @click="removeTeamMember(index)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </draggable>
          </div>
          
          <!-- 文件上传 -->
          <div class="form-section">
            <h3 class="section-title">
              <span class="title-with-icon">
                <el-icon class="title-icon"><FolderChecked /></el-icon>
                证明材料上传
              </span>
            </h3>
            <el-form-item label="上传文件" class="premium-form-item upload-form-item">
              <el-upload
                ref="uploadRef"
                class="premium-upload-dragger"
                :auto-upload="false"
                :on-change="handleFileChange"
                :file-list="fileList"
                :show-file-list="false"
                multiple
                drag
                :limit="5"
                accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">
                  请将文件拖拽至此处，或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="upload-tip premium-upload-tip">
                    <el-icon><InfoFilled /></el-icon>
                    支持 PDF、图片、Word 格式，单个文件不超过 10MB，最多上传 5 个文件
                  </div>
                </template>
              </el-upload>

              <!-- 自定义文件列表 -->
              <transition-group tag="ul" name="file-list" class="custom-file-list" v-if="fileList.length > 0">
                <li
                  v-for="(file, index) in fileList"
                  :key="file.uid || file.name"
                  class="custom-file-item"
                >
                  <!-- 文件类型图标 -->
                  <span class="file-type-badge" :class="getFileTypeClass(file.name)">
                    {{ getFileTypeLabel(file.name) }}
                  </span>
                  <!-- 文件名（可点击预览） -->
                  <span class="file-name" @click="handlePreview(file)" :title="file.name">
                    {{ file.name }}
                  </span>
                  <!-- 文件大小 -->
                  <span class="file-size" v-if="file.size">
                    {{ formatFileSize(file.size) }}
                  </span>
                  <!-- 预览按钮 -->
                  <el-button
                    class="file-preview-btn"
                    size="small"
                    circle
                    :icon="View"
                    @click="handlePreview(file)"
                    title="预览"
                  />
                  <!-- 删除按钮（红色） -->
                  <el-button
                    class="file-delete-btn"
                    size="small"
                    circle
                    :icon="Close"
                    @click="removeFile(index)"
                    title="删除"
                  />
                </li>
              </transition-group>

              <!-- 图片预览弹窗 -->
              <el-dialog
                v-model="previewDialogVisible"
                class="preview-image-dialog"
                :show-close="true"
                width="auto"
                align-center
                append-to-body
                @close="closePreviewDialog"
              >
                <img :src="previewImageUrl" style="max-width: 80vw; max-height: 80vh; border-radius: 8px; display: block;" />
              </el-dialog>
            </el-form-item>
          </div>
          
          <!-- 提交按钮 -->
          <el-form-item class="submit-action-row">
            <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting" class="premium-submit-btn">
              提交申请
            </el-button>
            <el-button size="large" @click="handleReset" class="premium-reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
    
    <!-- 添加指导老师弹窗 -->
    <el-dialog 
      v-model="mentorDialogVisible" 
      class="custom-mini-dialog"
      :show-close="false"
      width="480px"
      align-center
      @close="closeMentorDialog"
    >
      <template #header="{ close }">
        <div class="mini-dialog-header">
          <div class="header-title" style="color: #1e293b;">
            <el-icon style="color: #6366f1;"><UserFilled /></el-icon>
            <span>添加指导老师</span>
          </div>
          <el-icon class="mini-header-close" @click="close"><Close /></el-icon>
        </div>
      </template>

      <div class="dialog-body-content" style="padding: 24px; padding-bottom: 8px;">
        <el-form :model="mentorForm" :rules="mentorRules" ref="mentorFormRef" label-position="top" class="premium-dialog-form">
          <el-form-item prop="teacherName" class="premium-form-item">
            <template #label>
              <div class="custom-label-wrap">
                <span class="custom-label">姓名</span>
              </div>
            </template>
            <el-input v-model="mentorForm.teacherName" placeholder="请输入老师姓名" />
          </el-form-item>
          <el-form-item prop="teacherDepartment" class="premium-form-item">
             <template #label>
              <div class="custom-label-wrap">
                <span class="custom-label">学院</span>
              </div>
            </template>
            <el-input v-model="mentorForm.teacherDepartment" placeholder="请输入所属学院" />
          </el-form-item>
          <el-form-item prop="teacherNo" class="premium-form-item">
            <template #label>
              <div class="custom-label-wrap">
                <span class="custom-label">工号</span>
              </div>
            </template>
            <el-input v-model="mentorForm.teacherNo" placeholder="请输入工号" />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <div class="dialog-footer premium-dialog-footer">
          <el-button @click="closeMentorDialog" class="premium-cancel-btn">取消</el-button>
          <el-button type="primary" class="premium-confirm-btn" @click="saveMentor">保存添加</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 添加团队成员弹窗 -->
    <el-dialog 
      v-model="memberDialogVisible" 
      class="custom-mini-dialog"
      :show-close="false"
      width="480px"
      align-center
      @close="closeMemberDialog"
    >
      <template #header="{ close }">
        <div class="mini-dialog-header">
          <div class="header-title" style="color: #1e293b;">
            <el-icon style="color: #6366f1;"><Avatar /></el-icon>
            <span>添加团队成员</span>
          </div>
          <el-icon class="mini-header-close" @click="close"><Close /></el-icon>
        </div>
      </template>

      <div class="dialog-body-content" style="padding: 24px; padding-bottom: 8px;">
        <el-form :model="memberForm" :rules="memberRules" ref="memberFormRef" label-position="top" class="premium-dialog-form">
          <el-form-item class="premium-form-item">
            <template #label>
              <div class="custom-label-wrap">
                <span class="custom-label">成员类型</span>
              </div>
            </template>
            <el-radio-group v-model="memberForm.isExternal" @change="handleMemberTypeChange">
              <el-radio :value="false">校内</el-radio>
              <el-radio :value="true">校外</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <!-- 校内成员 -->
          <template v-if="!memberForm.isExternal">
            <el-form-item prop="studentName" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap">
                  <span class="custom-label">姓名</span>
                </div>
              </template>
              <el-input v-model="memberForm.studentName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item prop="studentDepartment" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap">
                  <span class="custom-label">学院</span>
                </div>
              </template>
              <el-input v-model="memberForm.studentDepartment" placeholder="请输入所在学院" />
            </el-form-item>
            <el-form-item prop="studentNumber" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap">
                  <span class="custom-label">学号</span>
                </div>
              </template>
              <el-input v-model="memberForm.studentNumber" placeholder="请输入学号" />
            </el-form-item>
          </template>
          
          <!-- 校外成员 -->
          <template v-else>
            <el-form-item prop="externalName" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap">
                  <span class="custom-label">姓名</span>
                </div>
              </template>
              <el-input v-model="memberForm.externalName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item prop="externalSchool" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap">
                  <span class="custom-label">学校</span>
                </div>
              </template>
              <el-input v-model="memberForm.externalSchool" placeholder="请输入所在学校" />
            </el-form-item>
            <el-form-item prop="externalNumber" class="premium-form-item">
              <template #label>
                <div class="custom-label-wrap">
                  <span class="custom-label">联系方式 (选填)</span>
                </div>
              </template>
              <el-input v-model="memberForm.externalNumber" placeholder="请输入联系方式" />
            </el-form-item>
          </template>
          
          <el-form-item class="premium-form-item">
            <template #label>
              <div class="custom-label-wrap">
                <span class="custom-label">角色标识</span>
              </div>
            </template>
            <el-checkbox v-model="memberForm.isLeader">设为队长 (第一作者)</el-checkbox>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer premium-dialog-footer">
          <el-button @click="closeMemberDialog" class="premium-cancel-btn">取消</el-button>
          <el-button type="primary" class="premium-confirm-btn" @click="saveMember">保存添加</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 赛事列表定制弹窗 -->
    <el-dialog 
      v-model="competitionDialogVisible" 
      class="custom-competition-dialog"
      :show-close="false"
      width="680px"
      top="5vh"
    >
      <template #header="{ close }">
        <div class="dialog-custom-header">
          <div class="header-title">
            <el-icon><Search /></el-icon>
            <span>搜索全局赛事库</span>
          </div>
          <el-icon class="header-close" @click="close"><Close /></el-icon>
        </div>
      </template>

      <div class="dialog-body-content">
        <!-- 弹窗内部搜索框 -->
        <div class="dialog-search-area">
          <el-input
            v-model="competitionSearchKeyword"
            placeholder="输入关键字搜索全部赛事，如：数学、编程、设计等"
            class="header-search-input"
            clearable
            size="large"
          >
            <template #append>
              <el-button type="primary" class="header-search-btn" @click="confirmCompetitionSelection">确认</el-button>
            </template>
          </el-input>
        </div>

        <!-- 弹窗列表 -->
        <div class="competition-list-container">
          <div v-for="comp in filteredCompetitions" 
            :key="comp.competitionId || comp.competitionName"
            class="competition-card"
            :class="{ 'is-selected': tempSelectedCompetition?.competitionName === comp.competitionName }"
            @click="selectTempCompetition(comp)"
          >
            <span class="comp-card-name">{{ comp.competitionName }}</span>
            <span v-if="comp.awardRank" class="comp-card-tag" :class="'tag-' + comp.awardRank">{{ comp.awardRank }}类</span>
          </div>
          
          <div v-if="filteredCompetitions.length === 0 && competitionSearchKeyword" 
               class="competition-card empty-card is-selected" 
               @click="selectCustomCompetition">
            <span class="comp-card-name">没有找到该赛事，点击这里直接使用输入的名称："{{ competitionSearchKeyword }}"</span>
          </div>
          <div v-if="filteredCompetitions.length === 0 && !competitionSearchKeyword" class="empty-placeholder">
            <el-empty description="暂无过滤数据，请输入关键字检索" :image-size="80" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="competitionDialogVisible = false">取消</el-button>
          <el-button type="primary" class="confirm-select-btn" @click="confirmCompetitionSelection">确认选择</el-button>
        </div>
      </template>
    </el-dialog>


  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import { Trophy, Plus, Delete, Document, Avatar, User, InfoFilled, UserFilled, UploadFilled, Search, ArrowDown, Close, FolderChecked, Upload, Rank, View } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const submitting = ref(false)

const applicationType = ref('individual')
const competitions = ref([])
const individualFileList = ref([])  // 个人赛文件列表
const teamFileList = ref([])        // 团体赛文件列表
const fileList = computed(() => {
  return applicationType.value === 'individual' ? individualFileList.value : teamFileList.value
})
const searchLoading = ref(false)
const lockedTeams = ref([])

// 文件预览相关
const previewDialogVisible = ref(false)
const previewImageUrl = ref('')
let _previewBlobUrl = '' // 保存已创建的 Blob URL，关闭时释放

const handlePreview = (uploadFile) => {
  const name = uploadFile.name || ''
  const ext = name.split('.').pop()?.toLowerCase() || ''

  // 图片类型 — 弹窗预览
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) {
    if (uploadFile.url) {
      previewImageUrl.value = uploadFile.url
      _previewBlobUrl = '' // 不是 Blob URL，无需释放
    } else if (uploadFile.raw) {
      const blobUrl = URL.createObjectURL(uploadFile.raw)
      previewImageUrl.value = blobUrl
      _previewBlobUrl = blobUrl // 记录待释放的 Blob URL
    }
    previewDialogVisible.value = true
    return
  }

  // PDF — 新标签页中打开
  if (ext === 'pdf') {
    if (uploadFile.url) {
      window.open(uploadFile.url, '_blank')
    } else if (uploadFile.raw) {
      const url = URL.createObjectURL(uploadFile.raw)
      window.open(url, '_blank')
    }
    return
  }

  // Word / 其他格式 — 触发下载
  if (uploadFile.raw) {
    const a = document.createElement('a')
    a.href = URL.createObjectURL(uploadFile.raw)
    a.download = name
    a.click()
  } else if (uploadFile.url) {
    window.open(uploadFile.url, '_blank')
  } else {
    ElMessage.info('暂不支持在线预览该文件格式')
  }
}

// 弹窗关闭时释放 Blob URL，避免内存泄漏
const closePreviewDialog = () => {
  if (_previewBlobUrl) {
    URL.revokeObjectURL(_previewBlobUrl)
    _previewBlobUrl = ''
  }
  previewImageUrl.value = ''
}

// 删除已选的文件
const removeFile = (index) => {
  if (applicationType.value === 'individual') {
    individualFileList.value.splice(index, 1)
  } else {
    teamFileList.value.splice(index, 1)
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 根据文件扩展名返回样式类
const getFileTypeClass = (name) => {
  const ext = (name || '').split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) return 'badge-image'
  if (ext === 'pdf') return 'badge-pdf'
  if (['doc', 'docx'].includes(ext)) return 'badge-word'
  return 'badge-other'
}

// 根据文件扩展名返回简短标签
const getFileTypeLabel = (name) => {
  const ext = (name || '').split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) return '图片'
  if (ext === 'pdf') return 'PDF'
  if (['doc', 'docx'].includes(ext)) return 'Word'
  return ext?.toUpperCase() || '文件'
}

// 指导老师弹窗相关
const mentorDialogVisible = ref(false)
const mentorFormRef = ref(null)
const mentorForm = reactive({
  teacherName: '',
  teacherDepartment: '',
  teacherNo: ''
})

const mentorRules = {
  // All mentor fields are now optional (Validation rules removed per user request)
}

// 团队成员弹窗相关
const memberDialogVisible = ref(false)
const memberFormRef = ref(null)
const memberForm = reactive({
  isExternal: false,
  studentName: '',
  studentDepartment: '',
  studentNumber: '',
  externalName: '',
  externalSchool: '',
  externalNumber: '',
  isLeader: false
})

const memberRules = computed(() => {
  if (memberForm.isExternal) {
    return {
      // 移除必填规则以适应快速录入需求
    }
  } else {
    return {
      // 移除必填规则以适应快速录入需求
    }
  }
})

// 个人赛和团体赛分别保存数据
const individualFormData = reactive({
  competitionId: null,       // 竞赛ID
  competitionName: '',       // 竞赛名称（用于显示）
  projectName: '',
  competitionLevel: '',
  awardLevel: '',
  awardRank: '',
  awardTime: '',
  contact: '',
  teachers: [],
  isTeam: false
})

const teamFormData = reactive({
  teamId: null,
  competitionId: null,       // 竞赛ID
  competitionName: '',       // 竞赛名称（用于显示）
  projectName: '',
  teamName: '',
  competitionLevel: '',
  awardLevel: '',
  awardRank: '',
  awardTime: '',
  contact: '',
  teachers: [],
  teamMembers: [],
  confirmMemberOrder: false,
  isTeam: true
})

// 当前使用的表单数据
const formData = ref(individualFormData)

const rules = {
  competitionName: [
    { required: true, message: '请输入竞赛名称', trigger: 'blur' }
  ],
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' }
  ],
  teamName: [
    { required: true, message: '请输入团队名称', trigger: 'blur' }
  ],
  competitionLevel: [
    { required: true, message: '请选择竞赛级别', trigger: 'change' }
  ],
  awardRank: [
    { required: true, message: '请选择竞赛后自动填充获奖等次', trigger: 'blur' }
  ],
  awardLevel: [
    { required: true, message: '请输入获奖等级', trigger: 'blur' }
  ],
  awardTime: [
    { required: true, message: '请选择获奖时间', trigger: 'change' }
  ],
  contact: [
    { required: true, message: '请输入联系方式', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

// 存储全量的竞赛列表
const allCompetitions = ref([])

// 预加载所有竞赛数据
const loadAllCompetitions = async () => {
  searchLoading.value = true
  try {
    const res = await request.get('/competition/all')
    
    // 该接口返回格式通常是 { code: "200", data: { list: [...], total: ... } } 或直接是 data 对象
    let targetArray = []
    
    if (res && res.code === '200' && res.data && Array.isArray(res.data.list)) {
      targetArray = res.data.list
    } else if (res && Array.isArray(res.list)) {
      targetArray = res.list
    } else if (res && res.code === '200' && Array.isArray(res.data)) {
      targetArray = res.data
    }
    
    allCompetitions.value = targetArray
    
  } catch (error) {
    console.error('加载竞赛列表失败:', error)
    ElMessage.error('无法加载竞赛列表源，您可以手动输入竞赛名称。')
  } finally {
    searchLoading.value = false
  }
}

// 页面加载时自动获取全量竞赛数据
onMounted(() => {
  loadAllCompetitions()
  request.get('/api/teams/locked').then(res => {
    if (res.code === '200') lockedTeams.value = res.data || []
  }).catch(() => {})
})

const applyLockedTeam = async (teamId) => {
  if (!teamId) {
    teamFormData.teamMembers = []
    return
  }
  const selected = lockedTeams.value.find(item => item.teamId === teamId)
  const res = await request.get(`/api/teams/${teamId}`)
  if (res.code !== '200') return ElMessage.error(res.msg)
  const team = res.data
  teamFormData.competitionId = team.competitionId
  teamFormData.competitionName = team.competitionName
  teamFormData.awardRank = selected?.awardRank || ''
  teamFormData.teamName = team.name
  teamFormData.teamMembers = (team.members || []).map((member, index) => ({
    isExternal: false,
    studentName: member.studentName,
    studentNumber: member.studentNumber,
    studentDepartment: member.college,
    isLeader: Boolean(member.isLeader),
    sortOrder: index + 1
  }))
  teamFormData.confirmMemberOrder = true
  ElMessage.success('已带入锁定队伍信息')
}

// ==================== 赛事下拉框搜索与推荐逻辑 ====================
const currentSearchKeyword = ref('')
const currentSelectOptions = ref([])

// 常见赛事名录
const commonCompetitionNames = [
  '全国大学生创新创业训练计划年会展示',
  '“挑战杯”全国大学生课外学术科技作品竞赛',
  '全国大学生电子设计竞赛',
  '全国大学生生物医学工程创新设计竞赛',
  '全国大学生数学建模竞赛',
  'ACM-ICPC国际大学生程序设计竞赛',
  '全国大学生嵌入式芯片与系统设计竞赛',
  '美国（国际）大学生数学建模竞赛（MCM/ICM）',
  '“蓝桥杯”全国软件和信息技术专业人才大赛'
]

const searchCompetitions = (query) => {
  currentSearchKeyword.value = query
  if (query) {
    const keyword = query.toLowerCase()
    currentSelectOptions.value = allCompetitions.value.filter(comp =>
      comp.competitionName && comp.competitionName.toLowerCase().includes(keyword)
    )
  } else {
    // 恢复默认推荐
    setCommonCompetitions()
  }
}

// ==================== 竞赛赛事定制弹窗逻辑 ====================
const competitionDialogVisible = ref(false)
const competitionSearchKeyword = ref('')
const tempSelectedCompetition = ref(null)

// 监听搜索词变化，如果是在列表以外纯手打且不点底下"未找到匹配"卡片，就暂时把手打项设为选中目标
watch(competitionDialogVisible, (val) => {
  if (val) {
    competitionSearchKeyword.value = ''
    tempSelectedCompetition.value = null
  }
})

const filteredCompetitions = computed(() => {
  if (!competitionSearchKeyword.value) {
    return allCompetitions.value // 弹窗初始展示全量数据
  }
  
  const keyword = competitionSearchKeyword.value.toLowerCase()
  return allCompetitions.value.filter(comp => 
    comp.competitionName && comp.competitionName.toLowerCase().includes(keyword)
  )
})

const openCompetitionDialog = () => {
  competitionDialogVisible.value = true
}

const selectTempCompetition = (comp) => {
  tempSelectedCompetition.value = comp
}

const selectCustomCompetition = () => {
  tempSelectedCompetition.value = {
    competitionId: null,
    competitionName: competitionSearchKeyword.value,
    awardRank: ''
  }
}

const confirmCompetitionSelection = () => {
  if (tempSelectedCompetition.value) {
    formData.value.competitionId = tempSelectedCompetition.value.competitionId
    formData.value.competitionName = tempSelectedCompetition.value.competitionName
    formData.value.awardRank = tempSelectedCompetition.value.awardRank || ''
  } else if (competitionSearchKeyword.value) {
    // 允许纯手工输入新的赛事并且直接点确认
    formData.value.competitionId = null
    formData.value.competitionName = competitionSearchKeyword.value
    formData.value.awardRank = ''
  } else {
    // 没选也没输
    return
  }
  
  competitionDialogVisible.value = false
  // 触发主表单验证
  if (formRef.value) {
    formRef.value.validateField('competitionName')
  }
}

const getRankTagType = (rank) => {
  if (rank === 'A') return 'danger'
  if (rank === 'B') return 'success'
  if (rank === 'C') return 'warning'
  return 'info'
}

// 选中赛事后，自动填充比赛级别等信息
const handleCompetitionSelectChange = (val) => {
  const comp = allCompetitions.value.find(c => c.competitionName === val)
  if (comp) {
    formData.value.competitionId = comp.competitionId
    if (comp.awardRank) {
      formData.value.awardRank = comp.awardRank
    }
  } else {
    // 纯手动输入的赛事
    formData.value.competitionId = null
    formData.value.awardRank = ''
  }
}

const handleTypeChange = () => {
  // 切换表单数据引用
  formData.value = applicationType.value === 'individual' ? individualFormData : teamFormData
  
  // 切换时清空表单验证
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const openMentorDialog = () => {
  mentorDialogVisible.value = true
}

const closeMentorDialog = () => {
  mentorDialogVisible.value = false
  // 重置表单
  mentorForm.teacherName = ''
  mentorForm.teacherDepartment = ''
  mentorForm.teacherNo = ''
  if (mentorFormRef.value) {
    mentorFormRef.value.clearValidate()
  }
}

const saveMentor = async () => {
  if (!mentorFormRef.value) return
  
  await mentorFormRef.value.validate((valid) => {
    if (valid) {
      // 检查工号是否重复
      const isDuplicate = formData.value.teachers.some(
        teacher => teacher.teacherNo === mentorForm.teacherNo
      )
      
      if (isDuplicate) {
        ElMessage.warning('该工号已存在，请勿重复添加')
        return
      }
      
      // 添加到当前表单的指导老师列表
      formData.value.teachers.push({
        teacherName: mentorForm.teacherName,
        teacherDepartment: mentorForm.teacherDepartment,
        teacherNo: mentorForm.teacherNo
      })
      
      ElMessage.success('添加成功')
      closeMentorDialog()
    } else {
      ElMessage.warning('请完善指导老师信息')
    }
  })
}

const removeMentor = (index) => {
  ElMessageBox.confirm('确定要删除该指导老师吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formData.value.teachers.splice(index, 1)
    formData.value.teachers.forEach((t, i) => {
      t.sortOrder = i + 1
    })
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const openMemberDialog = () => {
  memberDialogVisible.value = true
}

const closeMemberDialog = () => {
  memberDialogVisible.value = false
  // 重置表单
  memberForm.isExternal = false
  memberForm.studentName = ''
  memberForm.studentDepartment = ''
  memberForm.studentNumber = ''
  memberForm.externalName = ''
  memberForm.externalSchool = ''
  memberForm.externalNumber = ''
  memberForm.isLeader = false
  if (memberFormRef.value) {
    memberFormRef.value.clearValidate()
  }
}

const handleMemberTypeChange = () => {
  // 切换类型时清空验证
  if (memberFormRef.value) {
    memberFormRef.value.clearValidate()
  }
}

const saveMember = async () => {
  if (!memberFormRef.value) return
  
  await memberFormRef.value.validate((valid) => {
    if (valid) {
      // 检查学号是否重复
      const currentNumber = memberForm.isExternal ? memberForm.externalNumber : memberForm.studentNumber
      const isDuplicate = formData.value.teamMembers.some(member => {
        const memberNumber = member.isExternal ? member.externalNumber : member.studentNumber
        return memberNumber === currentNumber
      })
      
      if (isDuplicate) {
        ElMessage.warning('该学号已存在，请勿重复添加')
        return
      }
      
      // 构造新成员
      const newMember = {
        isExternal: memberForm.isExternal,
        studentName: memberForm.studentName,
        studentDepartment: memberForm.studentDepartment,
        studentNumber: memberForm.studentNumber,
        externalName: memberForm.externalName,
        externalSchool: memberForm.externalSchool,
        externalNumber: memberForm.externalNumber,
        isLeader: memberForm.isLeader,
        sortOrder: 0
      }
      
      if (memberForm.isLeader) {
        // 勾选队长：自动切换到按序号排序，并将该成员插入位置1（数组首位）
        formData.value.confirmMemberOrder = true
        formData.value.teamMembers.unshift(newMember)
      } else {
        formData.value.teamMembers.push(newMember)
      }
      
      // 刷新各成员 sortOrder
      formData.value.teamMembers.forEach((m, i) => { m.sortOrder = i + 1 })
      
      ElMessage.success(memberForm.isLeader ? '已添加队长并置于排序1' : '添加成功')
      closeMemberDialog()
    } else {
      ElMessage.warning('请完善团队成员信息')
    }
  })
}

const removeTeamMember = (index) => {
  ElMessageBox.confirm('确定要删除该团队成员吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formData.value.teamMembers.splice(index, 1)
    formData.value.teamMembers.forEach((m, i) => {
      m.sortOrder = i + 1
    })
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleFileChange = (file, files) => {
  // 验证文件大小（10MB = 10 * 1024 * 1024 bytes）
  const maxSize = 10 * 1024 * 1024
  
  if (file.size > maxSize) {
    ElMessage.error(`文件 ${file.name} 大小超过10MB，请选择更小的文件`)
    // 从文件列表中移除超大文件
    const index = files.indexOf(file)
    if (index > -1) {
      files.splice(index, 1)
    }
    // 根据申请类型更新对应的文件列表
    if (applicationType.value === 'individual') {
      individualFileList.value = files
    } else {
      teamFileList.value = files
    }
    return false
  }
  
  // 根据申请类型更新对应的文件列表
  if (applicationType.value === 'individual') {
    individualFileList.value = files
  } else {
    teamFileList.value = files
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }
    
    // 团体赛必须至少有两名团队成员
    if (applicationType.value === 'team') {
      if (!formData.value.teamId && (!formData.value.teamMembers || formData.value.teamMembers.length === 0)) {
        ElMessage.warning('团体赛请至少添加两名团队成员')
        return
      }
      if (!formData.value.teamId && formData.value.teamMembers.length < 2) {
        ElMessage.warning('团体赛至少需要两名团队成员')
        return
      }
      
      // 检查是否有队长
      const hasLeader = formData.value.teamId || formData.value.teamMembers.some(m => m.isLeader)
      if (!hasLeader) {
        ElMessage.warning('请指定一名队长')
        return
      }
    }
    
    // 团体赛需要确认排序选项
    if (applicationType.value === 'team' && !formData.value.teamId && formData.value.teamMembers && formData.value.teamMembers.length > 0) {
      const sortMessage = formData.value.confirmMemberOrder 
        ? '您已选择"组内有排序，序号对应成员排序"，请确认成员添加顺序是否正确？' 
        : '您已选择"组内无排序"，确认组内成员无先后顺序？'
      
      try {
        await ElMessageBox.confirm(sortMessage, '确认排序选项', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return
      }
    }
    
    try {
      await ElMessageBox.confirm('确认提交申请吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      submitting.value = true
      
      // 验证竞赛是否已选择
      if (!formData.value.competitionId) {
        ElMessage.error('请选择竞赛')
        submitting.value = false
        return
      }
      
      // 提取纯数据对象，避免循环引用
      const pureData = {
        competitionId: formData.value.competitionId,  // 传递竞赛ID
        projectName: formData.value.projectName,
        teamName: formData.value.teamName || '',
        competitionLevel: formData.value.competitionLevel,
        awardLevel: formData.value.awardLevel,
        awardRank: formData.value.awardRank,
        awardTime: formData.value.awardTime,
        contact: formData.value.contact,
        teachers: formData.value.teachers || [],
        teamMembers: formData.value.teamMembers || [],
        confirmMemberOrder: formData.value.confirmMemberOrder || false,
        isTeam: formData.value.isTeam
      }
      
      const formDataToSubmit = new FormData()
      formDataToSubmit.append('data', JSON.stringify(pureData))
      
      fileList.value.forEach(file => {
        formDataToSubmit.append('files', file.raw)
      })
      
      // 添加 studentId 参数（从登录用户信息获取）
      const currentUser = JSON.parse(localStorage.getItem('saims_user') || '{}')
      formDataToSubmit.append('studentId', currentUser.studentId)
      
      await request.post('/application/submit', formDataToSubmit, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      
      ElMessage.success('申请提交成功！')
      
    } catch (error) {
      if (error !== 'cancel') {
        console.error('提交失败:', error)
      }
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  // 重置表单验证
  if (formRef.value) {
    formRef.value.resetFields()
  }
  
  // 根据当前类型重置对应的表单数据
  if (applicationType.value === 'individual') {
    // 重置个人赛数据
    individualFormData.competitionId = null
    individualFormData.competitionName = ''
    individualFormData.projectName = ''
    individualFormData.competitionLevel = ''
    individualFormData.awardLevel = ''
    individualFormData.awardRank = ''
    individualFormData.awardTime = ''
    individualFormData.contact = ''
    individualFormData.teachers = []
  } else {
    // 重置团体赛数据
    teamFormData.competitionId = null
    teamFormData.teamId = null
    teamFormData.competitionName = ''
    teamFormData.projectName = ''
    teamFormData.teamName = ''
    teamFormData.competitionLevel = ''
    teamFormData.awardLevel = ''
    teamFormData.awardRank = ''
    teamFormData.awardTime = ''
    teamFormData.contact = ''
    teamFormData.teachers = []
    teamFormData.teamMembers = []
    teamFormData.confirmMemberOrder = false
  }
  
  // 重置文件列表
  fileList.value = []
  
  ElMessage.success('表单已重置')
}
</script>

<style scoped>
.student-award-page {
  min-height: 100vh;
  position: relative;
  padding-bottom: 40px;
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative; /* 确保内容在背景之上 */
  z-index: 10;
}

.form-card {
  margin-top: 20px;
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

/* 自定义头部样式 */
.premium-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 20px;
  margin-bottom: 10px;
}

.header-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.decorative-line {
  width: 4px;
  height: 24px;
  background: linear-gradient(to bottom, #6366f1, #a855f7);
  border-radius: 4px;
}

.card-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.5px;
}

.version-tag {
  border-radius: 6px;
  font-weight: 600;
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
  border: none;
}

/* 分段控制器风格的单选组 */
.custom-radio-group {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 4px;
  border-radius: 12px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02), 0 1px 2px rgba(0,0,0,0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

:deep(.custom-radio-group .el-radio-button__inner) {
  border: none !important;
  border-radius: 8px !important;
  background: transparent;
  color: #64748b;
  font-weight: 600;
  padding: 8px 24px;
  box-shadow: none !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.custom-radio-group .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #ffffff;
  color: #6366f1;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05) !important;
}

:deep(.custom-radio-group .el-radio-button__inner:hover) {
  color: #6366f1;
}

.application-form {
  margin-top: 30px;
}

/* 底部提交按钮区域 - 居中与玻璃态特效 */
.submit-action-row {
  margin-top: 40px !important;
  margin-bottom: 20px !important;
  display: flex;
  justify-content: center;
}

.submit-action-row :deep(.el-form-item__content) {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-left: 0 !important; /* 移除表单默认的 label 缩进，实现真正的居中 */
}

.premium-submit-btn {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%) !important;
  border: none !important;
  border-radius: 12px !important;
  padding: 12px 36px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  letter-spacing: 1px;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.premium-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4) !important;
  background: linear-gradient(135deg, #9366f9 0%, #6d70f5 100%) !important;
}

.premium-submit-btn:active {
  transform: translateY(1px);
}

.premium-reset-btn {
  background: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  color: #64748b !important;
  border-radius: 12px !important;
  padding: 12px 36px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.premium-reset-btn:hover {
  background: rgba(255, 255, 255, 1) !important;
  color: #6366f1 !important;
  border-color: #e0e7ff !important;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(99, 102, 241, 0.08) !important;
}

.premium-reset-btn:active {
  transform: translateY(1px);
}

.form-section {
  margin-bottom: 32px;
  padding: 24px 32px;
  background-color: rgba(255, 255, 255, 0.35); /* 更透明，透出色彩 */
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -1px rgba(0, 0, 0, 0.02);
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(to right, #6366f1, #a855f7);
  border-radius: 3px;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  color: #6366f1;
  font-size: 20px;
}

/* 增加全局表单项间距及错位修复 - 进一步拉大间距以确保红字完全展示 */
:deep(.el-form-item) {
  margin-bottom: 36px !important;
}

:deep(.el-form-item__error) {
  padding-top: 8px !important;
  font-size: 13px !important;
  font-weight: 500;
  line-height: 1.2;
}

/* 表单 Label 字体加大换色 */
:deep(.el-form-item__label) {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #475569 !important;
  padding-right: 20px !important;
}

/* 必填星号颜色 */
:deep(.el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label-wrap > .el-form-item__label::before), 
:deep(.el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::before) {
  color: #f43f5e !important;
  margin-right: 6px !important;
  font-weight: bold;
}

/* 竞赛下拉列表项自定义样式 */
.competition-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.comp-name {
  font-weight: 500;
  color: #334155;
}

.comp-rank {
  font-size: 13px;
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

/* 高级输入框组件样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-date-editor.el-input__wrapper),
:deep(.el-textarea__inner) {
  background-color: rgba(248, 250, 252, 0.6) !important;
  border-radius: 12px !important;
  padding: 8px 16px !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  transition: all 0.3s ease !important;
  min-height: 40px !important;
  box-sizing: border-box !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-select .el-input__wrapper:hover),
:deep(.el-date-editor.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #cbd5e1 inset !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-select .el-input__wrapper.is-focus),
:deep(.el-date-editor.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 2px #6366f1 inset !important;
}

:deep(.el-input__inner) {
  font-size: 15px !important;
  height: 24px !important;
  line-height: 24px !important;
}

/* 输入框字数统计颜色 */
:deep(.el-input__count),
:deep(.el-input__count .el-input__count-inner) {
  background: transparent !important;
  color: #475569 !important;
  font-weight: 600 !important;
  font-size: 13px !important;
}

/* 小段控制器风格 */
.custom-radio-group-small {
  background: rgba(255, 255, 255, 0.6);
  padding: 3px;
  border-radius: 8px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
  border: 1px solid rgba(255, 255, 255, 0.4);
  display: flex;
}

:deep(.custom-radio-group-small .el-radio-button) {
  flex: 1; /* 让内部按钮等分 */
}

:deep(.custom-radio-group-small .el-radio-button__inner) {
  border: none !important;
  border-radius: 6px !important;
  background: transparent;
  color: #64748b;
  font-weight: 600;
  box-shadow: none !important;
  width: 100%;
  padding: 6px 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.custom-radio-group-small .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #ffffff;
  color: #6366f1;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08) !important;
}

/* 移动端提示横条特调 */
@media screen and (max-width: 768px) {
  .member-instruction-row {
    flex-direction: column;
    align-items: stretch !important;
    gap: 12px;
    padding: 14px 16px !important;
  }
  
  .custom-radio-group-small {
    width: 100%;
  }
}

.empty-tip {
  padding: 30px;
  text-align: center;
}

/* 自定义拖拽上传区样式 */
.premium-upload-dragger :deep(.el-upload-dragger) {
  background: rgba(255, 255, 255, 0.5);
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 40px 20px;
}

.premium-upload-dragger :deep(.el-upload-dragger:hover) {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.05);
}

.premium-upload-dragger :deep(.el-upload-dragger.is-dragover) {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  transform: scale(1.02);
}

.premium-upload-dragger :deep(.el-icon--upload) {
  font-size: 56px;
  color: #818cf8;
  margin-bottom: 16px;
  line-height: 56px;
  transition: transform 0.3s;
}

.premium-upload-dragger :deep(.el-upload-dragger:hover .el-icon--upload) {
  transform: translateY(-5px);
}

.premium-upload-dragger :deep(.el-upload__text) {
  color: #475569;
  font-size: 15px;
  margin-bottom: 8px;
}

.premium-upload-dragger :deep(.el-upload__text em) {
  color: #6366f1;
  font-weight: 600;
  font-style: normal;
}

.premium-upload-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  margin-top: 12px;
}

.premium-upload-tip .el-icon {
  color: #8b5cf6;
  font-size: 14px;
}

.upload-form-item {
  display: flex;
  flex-direction: column;
}

.upload-form-item :deep(.el-form-item__label) {
  text-align: left;
  justify-content: flex-start;
  margin-bottom: 12px;
}

.upload-form-item :deep(.el-form-item__content) {
  display: block; /* 防止拖拽区在 flex 布局下变形 */
  width: 100%;
}

/* 自定义文件列表 */
.custom-file-list {
  list-style: none;
  padding: 0;
  margin: 16px 0 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.custom-file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.25s;
}

.custom-file-item:hover {
  border-color: #818cf8;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

/* 文件类型徽章 */
.file-type-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 5px;
  flex-shrink: 0;
  letter-spacing: 0.5px;
}

.badge-image { background: #dcfce7; color: #16a34a; }
.badge-pdf   { background: #fee2e2; color: #dc2626; }
.badge-word  { background: #dbeafe; color: #1d4ed8; }
.badge-other { background: #f1f5f9; color: #475569; }

/* 文件名（可点击） */
.file-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  transition: color 0.2s;
}

.file-name:hover {
  color: #6366f1;
  text-decoration: underline;
}

/* 文件大小 */
.file-size {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}

/* 预览按钮（紫色） */
.file-preview-btn {
  flex-shrink: 0;
  color: #818cf8 !important;
  border-color: #c7d2fe !important;
  background: #f0f4ff !important;
  transition: all 0.2s !important;
}

.file-preview-btn:hover {
  color: #fff !important;
  background: #6366f1 !important;
  border-color: #6366f1 !important;
}

/* 删除按钮（红色） */
.file-delete-btn {
  flex-shrink: 0;
  color: #f87171 !important;
  border-color: #fecaca !important;
  background: #fff5f5 !important;
  transition: all 0.2s !important;
}

.file-delete-btn:hover {
  color: #fff !important;
  background: #ef4444 !important;
  border-color: #ef4444 !important;
}

/* 文件列表进入/离开动画 */
.file-list-enter-active,
.file-list-leave-active {
  transition: all 0.3s ease;
}
.file-list-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.file-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* 团队卡片列表 */
.mentor-list, .member-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 拖拽手柄 */
.drag-handle {
  cursor: grab;
  font-size: 20px;
  color: #94a3b8;
  flex-shrink: 0;
  margin-right: 4px;
  transition: color 0.2s;
  user-select: none;
}

.drag-handle:hover {
  color: #6366f1;
}

.drag-handle:active {
  cursor: grabbing;
}

/* vuedraggable 拖拽幽灵（占位符）样式 */
.drag-ghost {
  opacity: 0.4;
  border: 2px dashed #6366f1 !important;
  background: #f0f1ff !important;
  box-shadow: none !important;
}

/* 当前正在被拖动的卡片样式 */
.drag-active {
  opacity: 0.95;
  box-shadow: 0 16px 32px rgba(99, 102, 241, 0.25) !important;
  border-color: rgba(99, 102, 241, 0.4) !important;
  transform: rotate(1deg) scale(1.02) !important;
  z-index: 9999;
}

.glass-card {
  background: rgba(255, 255, 255, 0.5); /* 降低卡片白度，使之更加毛玻璃 */
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.5);
  padding: 20px 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 24px;
}

.glass-card:hover {
  box-shadow: 0 12px 24px -8px rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateX(4px);
  background: #ffffff;
}

.mentor-title, .member-title {
  font-size: 15px;
  font-weight: 700;
  color: #6366f1;
  min-width: 80px;
  flex-shrink: 0;
  background: rgba(99, 102, 241, 0.1);
  padding: 6px 12px;
  border-radius: 8px;
  text-align: center;
}

.member-title {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.info-item {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.info-label {
  font-size: 15px;
  font-weight: 500;
  color: #64748b;
  margin-right: 8px;
  white-space: nowrap;
  flex-shrink: 0;
}

.info-value {
  font-size: 15px;
  color: #1e293b;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.glass-card .el-button {
  flex-shrink: 0;
  margin-left: auto;
  border-radius: 8px !important;
  font-weight: 600;
}

.premium-instruction {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(135deg, rgba(241, 245, 249, 0.8) 0%, rgba(226, 232, 240, 0.5) 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  margin-bottom: 24px;
}

.instruction-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.instruction-icon {
  color: #6366f1;
  font-size: 18px;
}

.instruction-text {
  font-size: 15px;
  font-weight: 600;
  color: #475569;
}

.upload-tip {
  color: #64748b;
  font-size: 13px;
  margin-top: 8px;
}

/* 按钮及交互重写 */
.premium-btn-small {
  border-radius: 8px !important;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%) !important;
  border: none !important;
  font-weight: 600 !important;
  box-shadow: 0 4px 6px -1px rgba(99, 102, 241, 0.2) !important;
  transition: all 0.3s ease !important;
  padding: 8px 16px !important;
}

.premium-btn-small:hover {
  box-shadow: 0 6px 12px -2px rgba(99, 102, 241, 0.3) !important;
  transform: translateY(-1px);
}

.submit-action-row {
  margin-top: 40px;
}

:deep(.submit-action-row .el-form-item__content) {
  justify-content: center;
  gap: 16px;
}

.premium-submit-btn {
  padding: 14px 48px !important;
  font-size: 18px !important;
  height: auto !important;
  border-radius: 14px !important;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%) !important;
  border: none !important;
  font-weight: 700 !important;
  box-shadow: 0 8px 16px -4px rgba(99, 102, 241, 0.4) !important;
  transition: all 0.3s ease !important;
}

.premium-submit-btn:hover {
  box-shadow: 0 12px 24px -6px rgba(99, 102, 241, 0.5) !important;
  transform: translateY(-2px);
}

.premium-reset-btn {
  padding: 14px 48px !important;
  font-size: 18px !important;
  height: auto !important;
  border-radius: 14px !important;
  background: white !important;
  border: 1px solid #cbd5e1 !important;
  color: #475569 !important;
  font-weight: 600 !important;
  transition: all 0.3s ease !important;
}

.premium-reset-btn:hover {
  border-color: #6366f1 !important;
  color: #6366f1 !important;
  background: #f8fafc !important;
}

.competition-select-trigger {
  display: flex;
  width: 100%;
  gap: 10px;
}
.flex-1-select {
  flex: 1;
}

/* ================= Mobile Responsive Tweaks ================= */
@media screen and (max-width: 768px) {
  /* 表单标签上方显示 */
  :deep(.el-form-item) {
    flex-direction: column !important;
    align-items: flex-start !important;
    margin-bottom: 24px !important;
  }
  
  :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left !important;
    justify-content: flex-start !important;
    margin-bottom: 8px !important;
    padding-right: 0 !important;
    line-height: 1.2 !important;
  }
  
  :deep(.el-form-item__content) {
    width: 100% !important;
  }

  /* 赛事名称输入框和按钮的堆叠 */
  .competition-select-trigger.mobile-stack {
    flex-direction: column;
    width: 100%;
    gap: 12px !important;
  }
  
  .competition-select-trigger.mobile-stack .flex-1-select,
  .competition-select-trigger.mobile-stack .premium-search-btn {
    width: 100% !important;
  }

  .premium-submit-btn,
  .premium-reset-btn {
    width: 100%;
    padding: 12px 0 !important;
    margin-left: 0 !important; /* 清除 Element Plus 相邻按钮默认增加的 12px 左间距 */
  }
  
  :deep(.submit-action-row .el-form-item__content) {
    flex-direction: column;
    gap: 12px;
  }

  /* 导师及成员卡片自适应缩放 */
  .glass-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
    position: relative;
  }
  
  .glass-card .info-item {
    width: 100%;
    margin-bottom: 4px;
  }

  .glass-card .el-button {
    width: 100%;
    margin-top: 8px;
    margin-left: 0;
  }

  .drag-handle {
    position: absolute;
    top: 16px;
    right: 16px;
  }
  
  .mentor-title, .member-title {
    margin-bottom: 8px;
  }

  /* 移动端弹窗宽度重写 */
  :deep(.custom-competition-dialog),
  :deep(.custom-mini-dialog) {
    width: 95% !important;
    margin: 0 auto !important;
  }
  
  .dialog-search-area {
    padding: 12px 16px;
  }
  
  .competition-list-container {
    padding: 12px 16px;
  }

  /* 表头与类型切换按钮移动端换行平铺，精简下方间距 */
  .card-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 16px;
  }
  
  :deep(.el-card__header) {
    padding-bottom: 12px;
    border-bottom: none; /* 去除双重横线 */
  }
  
  .premium-header {
    border-bottom: 1px solid rgba(0, 0, 0, 0.05); /* 保留自定横线即可 */
    padding-bottom: 16px;
    margin-bottom: 0; /* 抹平之前预留给 PC 的多余边距 */
  }
  
  :deep(.form-card .el-card__body) {
    padding-top: 10px; /* 拉近与头部的贴合感 */
  }
  
  .application-form {
    margin-top: 10px !important; /* 去除原本 30px 的巨大的表单顶部推远距离 */
  }
  
  .form-section {
    padding: 20px 16px !important; /* 缩小桌面端硕大的白盒子内边距 (原本32px) */
    margin-bottom: 20px !important;
  }
  
  .header-actions {
    width: 100%;
  }

  .header-actions .custom-radio-group {
    display: flex;
    width: 100%;
  }
  
  :deep(.header-actions .el-radio-button) {
    flex: 1;
    display: flex;
  }
  
  :deep(.header-actions .el-radio-button__inner) {
    width: 100%;
    justify-content: center;
    padding: 10px 0 !important;
  }
  
  .card-header h2 {
    font-size: 20px;
  }
  
  /* ================= 针对弹窗 (赛事搜索等) 的移动端极端优化 ================= */
  
  :deep(.custom-competition-dialog),
  :deep(.custom-mini-dialog) {
    border-radius: 12px !important;
    overflow: hidden !important;
    width: 95% !important;
    margin: 5vh auto !important; /* 不用 align-center，改用手动距顶 */
    height: auto !important;
  }
  
  :deep(.custom-competition-dialog .el-dialog__header),
  :deep(.custom-mini-dialog .el-dialog__header) {
    padding: 0 !important;
    margin: 0 !important;
  }
  
  :deep(.custom-competition-dialog .el-dialog__body),
  :deep(.custom-mini-dialog .el-dialog__body) {
    padding: 0 !important; /* 彻底清除移动端自带的导致挤压的巨大留白 */
    display: flex;
    flex-direction: column;
  }
  
  .dialog-body-content {
    /* 随下方列表自然延伸高度，不要无限撑开 */
    width: 100%;
    display: flex;
    flex-direction: column;
  }
  
  .dialog-custom-header {
    padding: 14px 18px !important;
  }
  .header-title {
    font-size: 16px !important;
  }
  
  .dialog-search-area {
    padding: 16px 18px !important; /* 增加搜索区边距增加呼吸感 */
  }
  
  .header-search-input .el-input-group__append {
    padding: 0 16px !important; /* 缩小追加按钮在手机下的左右边距避免挤压 */
    font-size: 14px !important;
  }
  
  .competition-list-container {
    padding: 12px 16px !important;
    max-height: 52vh !important; /* 允许列表更高 */
  }
  
  .competition-card {
    padding: 12px 14px !important;
    font-size: 14px !important;
  }
  .comp-card-name {
    font-size: 14px !important;
  }
  .comp-card-tag {
    padding: 2px 8px !important;
    font-size: 12px !important;
  }
  
  .dialog-footer {
    display: flex;
    justify-content: space-between;
    padding: 12px 16px !important;
  }
  .dialog-footer .el-button {
    flex: 1;
    margin: 0 4px !important;
    padding: 10px 0 !important;
  }
}

</style>

/* ================= Custom Competition Selector Dialog ================= */
<style>
.custom-competition-dialog {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

/* Header style override without scoped issues */
.custom-competition-dialog .el-dialog__header {
  padding: 0 !important;
  margin: 0 !important;
}

.custom-competition-dialog .el-dialog__body {
  padding: 0 !important;
  background: #f8fafc;
}

.custom-competition-dialog .el-dialog__footer {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  background: white;
}

/* ================= Custom Mini Form Dialog (Lighter & Softer) ================= */
.custom-mini-dialog {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.08);
}

.custom-mini-dialog .el-dialog__header {
  padding: 0 !important;
  margin: 0 !important;
}

.custom-mini-dialog .el-dialog__body {
  padding: 0 !important;
  background: white;
}

.custom-mini-dialog .el-dialog__footer {
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.premium-dialog-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.premium-dialog-footer .premium-cancel-btn {
  padding: 10px 24px !important;
  font-size: 14px !important;
  border-radius: 12px !important;
  height: 40px !important;
  background: white !important;
  border: 1px solid #e2e8f0 !important;
  color: #64748b !important;
  font-weight: 600 !important;
  transition: all 0.2s !important;
}

.premium-dialog-footer .premium-cancel-btn:hover {
  color: #1e293b !important;
  border-color: #cbd5e1 !important;
  background: #f1f5f9 !important;
}

.premium-dialog-footer .premium-confirm-btn {
  padding: 10px 24px !important;
  font-size: 14px !important;
  border-radius: 12px !important;
  height: 40px !important; /* Match cancel button size exactly */
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3) !important;
  transition: all 0.2s !important;
}

.premium-dialog-footer .premium-confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4) !important;
  opacity: 0.95;
}

.mini-dialog-header {
  background: white;
  padding: 20px 24px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}

/* ================= Premium Date Picker Customization ================= */
.premium-date-picker {
  border-radius: 16px !important;
  border: none !important;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12) !important;
  font-family: inherit !important;
}

/* Decorative top bar */
.premium-date-picker::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #6366f1, #a855f7, #ec4899);
  z-index: 10;
}

.premium-date-picker .el-date-picker__header {
  margin: 0 !important;
  padding: 20px 16px 12px 16px !important;
  background: linear-gradient(to bottom, #f8fafc, white);
  border-bottom: 1px solid #f1f5f9;
}

.premium-date-picker .el-date-picker__header-label {
  font-weight: 700;
  color: #1e293b;
  font-size: 16px;
}

.premium-date-picker .el-picker-panel__icon-btn {
  color: #64748b;
  transition: all 0.2s;
}

.premium-date-picker .el-picker-panel__icon-btn:hover {
  color: #6366f1;
  background: #f1f5f9;
  border-radius: 6px;
}

.premium-date-picker .el-date-picker__body {
  padding: 12px !important;
}

.premium-date-picker .el-date-table th {
  color: #94a3b8;
  font-weight: 500;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e2e8f0;
}

.premium-date-picker .el-date-table td {
  padding: 4px 0 !important;
}

.premium-date-picker .el-date-table td .el-date-table-cell {
  height: 36px;
}

.premium-date-picker .el-date-table td .el-date-table-cell .el-date-table-cell__text {
  width: 32px;
  height: 32px;
  line-height: 32px;
  border-radius: 10px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  color: #334155;
}

/* Hover state */
.premium-date-picker .el-date-table td.available:hover .el-date-table-cell__text {
  background: #f1f5f9;
  color: #6366f1;
}

/* Selected state */
.premium-date-picker .el-date-table td.current:not(.disabled) .el-date-table-cell__text {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  color: white !important;
  box-shadow: 0 4px 10px rgba(99, 102, 241, 0.4);
  font-weight: 600;
  transform: scale(1.05);
}

/* Today state */
.premium-date-picker .el-date-table td.today .el-date-table-cell__text {
  color: #6366f1;
  font-weight: 700;
  background: #e0e7ff;
}
.premium-date-picker .el-date-table td.today.current .el-date-table-cell__text {
  color: white;
}

.mini-header-close {
  cursor: pointer;
  font-size: 20px;
  color: #94a3b8;
  transition: all 0.2s;
}
.mini-header-close:hover {
  color: #ef4444;
  transform: rotate(90deg);
}

.dialog-custom-header {
  background: linear-gradient(135deg, #5b54e3 0%, #7c3aed 100%);
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.header-title .el-icon {
  font-size: 20px;
}

.header-close {
  cursor: pointer;
  font-size: 20px;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.2s;
}
.header-close:hover {
  color: white;
  transform: rotate(90deg);
}

.dialog-body-content {
  display: flex;
  flex-direction: column;
}

.dialog-search-area {
  padding: 20px 24px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
}

/* Override el-input styling to create a pill/softer shape and fuse append slot */
.header-search-input {
  display: flex;
  align-items: stretch;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  overflow: hidden;
  --el-input-border-color: transparent;
  --el-input-hover-border-color: transparent;
  --el-input-focus-border-color: transparent;
}

.header-search-input .el-input__wrapper {
  border-top-left-radius: 12px !important;
  border-bottom-left-radius: 12px !important;
  border-top-right-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
  padding: 8px 16px;
  background: #f8fafc;
  box-shadow: inset 0 0 0 1px #e2e8f0 !important;
  border: none !important;
  flex: 1;
}

.header-search-input .el-input__wrapper.is-focus {
  background: white;
  box-shadow: inset 0 0 0 1px #a78bfa !important;
  z-index: 1;
}

.header-search-input .el-input-group__append {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  border: none !important;
  color: #fff !important;
  border-radius: 0 !important;
  border-top-right-radius: 12px !important;
  border-bottom-right-radius: 12px !important;
  padding: 0 28px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: none !important;
  margin-left: -1px; /* Fuse the gap */
  position: relative;
  z-index: 2;
}

.header-search-input .el-input-group__append:hover {
  opacity: 0.9;
  box-shadow: inset 0 0 0 100px rgba(255, 255, 255, 0.1) !important;
}

.competition-list-container {
  max-height: 400px;
  min-height: 100px;
  overflow-y: auto;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.competition-list-container::-webkit-scrollbar {
  width: 6px;
}
.competition-list-container::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 8px;
}

.list-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #8b5cf6;
  padding: 4px 8px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.competition-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.competition-card:hover {
  border-color: #a78bfa;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.1);
  transform: translateY(-2px);
}

.competition-card.is-selected {
  border-color: #7c3aed;
  background-color: #f5f3ff;
  box-shadow: 0 0 0 1px #7c3aed;
}

.comp-card-name {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
  line-height: 1.5;
}

/* Category Badges */
.comp-card-tag {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.comp-card-tag.tag-A {
  background: #eff6ff;
  color: #3b82f6;
  border: 1px solid #bfdbfe;
}
.comp-card-tag.tag-B {
  background: #f0fdf4;
  color: #22c55e;
  border: 1px solid #bbf7d0;
}
.comp-card-tag.tag-C {
  background: #fefce8;
  color: #eab308;
  border: 1px solid #fef08a;
}
.comp-card-tag.tag-D {
  background: #f3f4f6;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}

.empty-card {
  justify-content: center;
  padding: 24px;
}
.empty-card .comp-card-name {
  color: #6366f1;
}

.empty-placeholder {
  padding: 40px 0;
}

.confirm-select-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  font-weight: 600;
  padding: 10px 24px;
}
.confirm-select-btn:hover {
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  opacity: 0.9;
}

/* ================= Premium Search Button for Dual Mode ================= */
.premium-search-btn {
  height: 48px !important;
  border-radius: 12px !important;
  padding: 0 24px !important;
  font-weight: 600 !important;
  font-size: 15px !important;
  background: #f8fafc !important;
  border: 1px solid #e2e8f0 !important;
  color: #6366f1 !important;
  transition: all 0.3s ease !important;
}

.premium-search-btn:hover {
  border-color: #a78bfa !important;
  color: #7c3aed !important;
  background: white !important;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.1) !important;
  transform: translateY(-1px);
}

/* ================= Premium Dialog Form Tweaks ================= */
.premium-dialog-form .premium-form-item {
  margin-bottom: 24px;
}
.premium-dialog-form .custom-label {
  font-size: 15px;
}
.premium-dialog-form .el-input__wrapper {
  background: #f8fafc;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02) !important;
  border: 1px solid #e2e8f0;
  padding: 6px 16px;
  transition: all 0.3s;
}
.premium-dialog-form .el-input__wrapper.is-focus {
  background: white;
  border-color: #a78bfa;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.1) !important;
}
.premium-dialog-form .el-input__inner {
  height: 40px;
  font-size: 15px;
}
</style>
