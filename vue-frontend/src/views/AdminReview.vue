<template>
  <div class="admin-review-page">
    <NavBar />
    
    <div class="content-container">
      <el-card class="review-card">
        <template #header>
          <div class="card-header">
            <h2>审核管理</h2>
          </div>
        </template>
        
        <!-- 大气优雅的数据看板 -->
        <div class="elegant-dashboard">
          <div class="elegant-card primary" @click="filterByStatus('')">
            <div class="card-info">
              <div class="card-title">总申请数</div>
              <div class="card-num">{{ statistics.totalCount }}</div>
            </div>
            <div class="card-bg-icon"><el-icon><Document /></el-icon></div>
          </div>
          
          <div class="elegant-card pending" @click="filterByStatus('pending')">
            <div class="card-info">
              <div class="card-title">待审核</div>
              <div class="card-num">{{ statistics.pendingCount }}</div>
            </div>
            <div class="card-bg-icon"><el-icon><Timer /></el-icon></div>
          </div>
          
          <div class="elegant-card approved" @click="filterByStatus('approved')">
            <div class="card-info">
              <div class="card-title">已通过</div>
              <div class="card-num">{{ statistics.approvedCount }}</div>
            </div>
            <div class="card-bg-icon"><el-icon><CircleCheck /></el-icon></div>
          </div>
          
          <div class="elegant-card rejected" @click="filterByStatus('rejected')">
            <div class="card-info">
              <div class="card-title">已拒绝</div>
              <div class="card-num">{{ statistics.rejectedCount }}</div>
            </div>
            <div class="card-bg-icon"><el-icon><CircleClose /></el-icon></div>
          </div>
          
          <div class="elegant-card returned" @click="filterByStatus('returned')">
            <div class="card-info">
              <div class="card-title">已打回</div>
              <div class="card-num">{{ statistics.returnedCount }}</div>
            </div>
            <div class="card-bg-icon"><el-icon><RefreshLeft /></el-icon></div>
          </div>
        </div>
        
        <!-- 操作栏 (高级重构) -->
        <div class="premium-filter-bar">
          <div class="left-actions">
            <el-button class="batch-btn batch-approve" @click="handleBatchReview('approved')">
              <el-icon><Check /></el-icon> 批量通过
            </el-button>
            <el-button class="batch-btn batch-return" @click="handleBatchReview('returned')">
              <el-icon><RefreshLeft /></el-icon> 批量打回
            </el-button>
            <el-button class="batch-btn batch-reject" @click="handleBatchReview('rejected')">
              <el-icon><Close /></el-icon> 批量拒绝
            </el-button>
            
            <div class="data-count-badge">
              <span>共 <b class="count-number">{{ pagination.total }}</b> 条信息</span>
            </div>
          </div>
          
          <div class="right-filters">
            <el-input
              v-model="searchStudentNumber"
              placeholder="请输入学号查询"
              clearable
              @input="handleSearch"
              class="premium-input search-number"
              :prefix-icon="Search"
            />
            <el-input
              v-model="searchCompetitionName"
              placeholder="请输入竞赛名称查询"
              clearable
              @input="handleSearch"
              class="premium-input search-name"
              :prefix-icon="Search"
            />
            <el-select
              v-model="filterStatus"
              placeholder="选择状态"
              clearable
              @change="handleSearch"
              class="premium-select"
            >
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
              <el-option label="已打回" value="returned" />
            </el-select>
          </div>
        </div>
        
        <!-- 申请列表 -->
        <el-table
          :data="applicationList"
          v-loading="loading"
          class="premium-table"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          :row-class-name="getRowClassName"
        >
          <el-table-column type="selection" width="55" align="center" :selectable="isSelectable" />
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="studentNumber" label="学生学号" width="120" />
          <el-table-column prop="competitionName" label="竞赛名称" min-width="180">
            <template #default="{ row }">
              <div class="multi-line-content">{{ row.competitionName }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="competitionLevel" label="竞赛级别" width="100" />
          <el-table-column label="获奖等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getAwardRankType(row.awardRank)">{{ row.awardRank }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="awardTime" label="获奖时间" width="100" />
          <el-table-column label="申请状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.applicationStatus)">
                {{ getStatusText(row.applicationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="95" align="center">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.riskLevel)" size="small" effect="dark">
                {{ riskText(row.riskLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="190">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="340" align="center" fixed="right">
            <template #default="{ row }">
              <div class="action-btn-group">
                <el-button 
                  size="small" 
                  class="action-btn view-btn"
                  @click="handleViewDetail(row)"
                >
                  查看详情
                </el-button>
                <template v-if="row.applicationStatus === 'pending'">
                  <el-button 
                    size="small" 
                    class="action-btn approve-btn"
                    @click="handleReview(row, 'approved')"
                  >
                    通过
                  </el-button>
                  <el-button 
                    size="small" 
                    class="action-btn return-btn"
                    @click="handleReview(row, 'returned')"
                  >
                    打回
                  </el-button>
                  <el-button 
                    size="small" 
                    class="action-btn reject-btn"
                    @click="handleReview(row, 'rejected')"
                  >
                    拒绝
                  </el-button>
                </template>
                <template v-else>
                  <el-button 
                    size="small" 
                    class="action-btn return-btn"
                    @click="handleReReviewFromTable(row)"
                  >
                    重新审核
                  </el-button>
                </template>
              </div>
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
          style="margin-top: 20px; justify-content: flex-end"
        />
      </el-card>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="申请详情"
      width="1000px"
      :destroy-on-close="true"
      class="premium-dialog"
      @close="handleDetailDialogClose"
    >
      <div v-loading="detailLoading" style="min-height: 200px">
        <div v-if="currentDetail.applicationId">
          <!-- 基本信息 -->
          <div class="premium-section">
            <h4 class="premium-title">基本信息</h4>
            <el-descriptions :column="2" border class="premium-desc">
              <el-descriptions-item label="申请编号">{{ currentDetail.applicationNumber }}</el-descriptions-item>
              <el-descriptions-item label="申请状态">
                <el-tag :type="getStatusType(currentDetail.applicationStatus)">
                  {{ getStatusText(currentDetail.applicationStatus) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="竞赛名称">{{ currentDetail.competitionName }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ currentDetail.projectName }}</el-descriptions-item>
              <el-descriptions-item label="竞赛类型">{{ currentDetail.competitionType }}</el-descriptions-item>
              <el-descriptions-item label="竞赛级别">{{ currentDetail.competitionLevel }}</el-descriptions-item>
              <el-descriptions-item label="获奖等次">
                <el-tag :type="getAwardRankType(currentDetail.awardRank)">{{ currentDetail.awardRank }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="获奖等级">{{ currentDetail.awardLevel }}</el-descriptions-item>
              <el-descriptions-item label="获奖时间">{{ currentDetail.awardTime }}</el-descriptions-item>
              <el-descriptions-item label="获奖数量">{{ currentDetail.awardQuantity }}</el-descriptions-item>
              <el-descriptions-item label="获奖人数">{{ currentDetail.awardPersonCount }}</el-descriptions-item>
              <el-descriptions-item label="联系方式">{{ currentDetail.contact }}</el-descriptions-item>
            </el-descriptions>
          </div>
          
          <!-- 团队成员 -->
          <div class="premium-section" v-if="currentDetail.teamMembers && currentDetail.teamMembers.length > 0">
            <h4 class="premium-title">团队成员</h4>
            <el-table :data="currentDetail.teamMembers" border class="premium-inner-table member-table">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="学号" width="140">
                <template #default="{ row }">{{ row.externalNumber || row.studentNumber || '-' }}</template>
              </el-table-column>
              <el-table-column label="姓名" width="100">
                <template #default="{ row }">{{ row.externalName || row.studentName || '-' }}</template>
              </el-table-column>
              <el-table-column label="学院" min-width="150">
                <template #default="{ row }">{{ row.college || '无' }}</template>
              </el-table-column>
              <el-table-column label="学校" min-width="150">
                <template #default="{ row }">{{ row.externalSchool || '本校' }}</template>
              </el-table-column>
              <el-table-column label="是否队长" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isLeader === 1 || row.isLeader === true" type="success" size="small">队长</el-tag>
                  <span v-else>队员</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <!-- 指导教师 -->
          <div class="premium-section" v-if="currentDetail.teachers && currentDetail.teachers.length > 0">
            <h4 class="premium-title">指导教师</h4>
            <el-table :data="currentDetail.teachers" border class="premium-inner-table teacher-table">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="teacherName" label="姓名" width="120" />
              <el-table-column prop="teacherNo" label="工号" width="120" />
              <el-table-column prop="teacherDepartment" label="所在单位" min-width="200" />
            </el-table>
          </div>

          <!-- 风险评估（证书查重 + OCR预检） -->
          <div class="premium-section" v-loading="riskLoading">
            <h4 class="premium-title">风险评估（证书查重 / OCR预检）</h4>
            <template v-if="riskInfo && riskInfo.risk">
              <div class="risk-summary">
                <el-tag :type="riskTagType(riskInfo.risk.riskLevel)" effect="dark">
                  {{ riskText(riskInfo.risk.riskLevel) }}
                </el-tag>
                <el-tag v-if="Number(riskInfo.risk.needManualReview) === 1" type="warning">需人工复核</el-tag>
                <el-tag v-if="riskInfo.risk.manualMark" :type="markTagType(riskInfo.risk.manualMark)">
                  {{ markText(riskInfo.risk.manualMark) }}（已人工处理）
                </el-tag>
              </div>
              <div class="risk-reasons" v-if="riskInfo.risk.riskReasons && riskInfo.risk.riskReasons.length">
                <div class="risk-reason-item" v-for="(reason, idx) in riskInfo.risk.riskReasons" :key="idx">
                  <span class="reason-dot">●</span>{{ reason }}
                </div>
              </div>
              <div v-else class="empty-text">暂无风险提示</div>
            </template>
            <div v-else class="empty-text">暂未进行预检（可在"证书查重"页面手动执行预检）</div>
          </div>

          <!-- OCR识别结果与申报字段对照 -->
          <div class="premium-section" v-if="riskInfo && riskInfo.ocrRecords && riskInfo.ocrRecords.length">
            <h4 class="premium-title">OCR识别结果与申报字段对照</h4>
            <div v-for="ocr in riskInfo.ocrRecords" :key="ocr.ocrId" class="ocr-block">
              <div class="ocr-file-line">
                <span class="ocr-file-name">{{ ocr.fileName || '证书文件' }}</span>
                <el-tag v-if="ocr.ocrStatus === 'success'" type="success" size="small">
                  识别成功（置信度 {{ Math.round((ocr.overallConfidence || 0) * 100) }}%）
                </el-tag>
                <el-tag v-else-if="ocr.ocrStatus === 'failed'" type="danger" size="small">
                  识别失败：{{ ocr.errorMessage }}（已转人工审核）
                </el-tag>
                <el-tag v-else type="info" size="small">{{ ocr.errorMessage || '未识别' }}</el-tag>
                <el-tag v-if="ocr.compareResult" :type="compareTagType(ocr.compareResult)" size="small">
                  比对：{{ compareText(ocr.compareResult) }}
                </el-tag>
              </div>
              <el-table
                v-if="ocr.compareDetail && ocr.compareDetail.length"
                :data="ocr.compareDetail"
                border
                size="small"
                class="premium-inner-table"
              >
                <el-table-column prop="label" label="比对字段" width="110" align="center" />
                <el-table-column prop="declared" label="申报字段" min-width="150">
                  <template #default="{ row }">
                    <span :class="{ 'declared-val': true, 'mismatch': row.result === 'inconsistent' }">{{ row.declared || '—' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="recognized" label="OCR识别字段" min-width="150">
                  <template #default="{ row }">
                    <span :class="{ 'mismatch': row.result === 'inconsistent' }">{{ row.recognized || '—' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="比对结果" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag :type="compareTagType(row.result)" size="small">{{ compareText(row.result) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="ocr.recognizedCertificateNo" class="ocr-cert-no">
                识别证书编号：<b>{{ ocr.recognizedCertificateNo }}</b>
              </div>
              <el-collapse v-if="ocr.rawText" class="ocr-raw-collapse">
                <el-collapse-item title="OCR识别原文" :name="ocr.ocrId">
                  <pre class="ocr-raw-text">{{ ocr.rawText }}</pre>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>

          <!-- 相似证书记录 -->
          <div class="premium-section" v-if="riskInfo && riskInfo.duplicateMatches && riskInfo.duplicateMatches.length">
            <h4 class="premium-title">相似证书记录（完全重复 / pHash相似）</h4>
            <el-table :data="riskInfo.duplicateMatches" border class="premium-inner-table">
              <el-table-column label="匹配类型" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.matchType === 'exact' ? 'danger' : 'warning'" size="small">
                    {{ row.matchType === 'exact' ? '完全相同' : 'pHash相似' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="相似度" width="90" align="center">
                <template #default="{ row }">
                  <span :class="{ 'sim-hot': row.similarity >= 0.9 }">{{ (row.similarity * 100).toFixed(1) }}%</span>
                </template>
              </el-table-column>
              <el-table-column label="关联学生" min-width="130">
                <template #default="{ row }">
                  <div>{{ row.matchedStudentName }}</div>
                  <div class="text-muted-small">{{ row.matchedStudentNumber }}</div>
                </template>
              </el-table-column>
              <el-table-column label="关联申请 / 团队" min-width="160">
                <template #default="{ row }">
                  <div>{{ row.matchedApplicationNumber }}</div>
                  <div class="text-muted-small">{{ row.matchedCompetitionName }}</div>
                  <div v-if="row.matchedTeamName" class="text-muted-small">团队：{{ row.matchedTeamName }}</div>
                </template>
              </el-table-column>
              <el-table-column label="团队成员" min-width="150">
                <template #default="{ row }">
                  <span v-if="row.matchedTeamMembers && row.matchedTeamMembers.length">
                    {{ row.matchedTeamMembers.map(m => (m.studentName || m.memberName) + (Number(m.isLeader) === 1 ? '（队长）' : '')).join('、') }}
                  </span>
                  <span v-else class="text-muted-small">个人申请</span>
                </template>
              </el-table-column>
              <el-table-column label="团队关系" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="Number(row.teamRelated) === 1 ? 'success' : 'danger'" size="small">
                    {{ Number(row.teamRelated) === 1 ? '同团队' : '无团队关系' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="匹配文件" width="80" align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="isImagePath(row.matchedFilePath)"
                    :src="getFilePreviewUrl(row.matchedFilePath)"
                    :preview-src-list="[getFilePreviewUrl(row.matchedFilePath)]"
                    preview-teleported
                    fit="cover"
                    style="width: 44px; height: 44px; border-radius: 6px"
                  />
                  <span v-else class="text-muted-small">文件</span>
                </template>
              </el-table-column>
              <el-table-column label="已处理" width="85" align="center">
                <template #default="{ row }">
                  <el-tag :type="Number(row.handled) === 1 ? 'success' : 'info'" size="small">
                    {{ Number(row.handled) === 1 ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 人工处理与最终意见 -->
          <div class="premium-section">
            <h4 class="premium-title">人工处理与最终意见</h4>
            <div class="manual-mark-row">
              <el-button type="success" plain @click="handleMark('normal_reuse')">标记正常复用</el-button>
              <el-button type="danger" plain @click="handleMark('abnormal_duplicate')">标记异常重复</el-button>
              <el-button type="warning" plain @click="handleMark('undetermined')">标记无法判断</el-button>
            </div>
            <div class="manual-info" v-if="riskInfo && riskInfo.risk && riskInfo.risk.manualMark">
              当前标记：{{ markText(riskInfo.risk.manualMark) }}
              <template v-if="riskInfo.risk.manualReviewer">｜处理人：{{ riskInfo.risk.manualReviewer }}</template>
              <template v-if="riskInfo.risk.manualTime">｜时间：{{ formatDateTime(riskInfo.risk.manualTime) }}</template>
            </div>
            <el-input
              v-model="markRemark"
              placeholder="人工处理备注（选填，点击标记按钮时保存）"
              maxlength="200"
              class="mt-10"
            />
            <el-input
              v-model="finalOpinion"
              type="textarea"
              :rows="3"
              placeholder="审核人员最终意见"
              maxlength="500"
              class="mt-10"
            />
            <div class="mt-10">
              <el-button type="primary" size="small" @click="handleSaveOpinion" :loading="opinionSaving">
                保存最终意见
              </el-button>
            </div>
          </div>
          
          <!-- 证明材料 -->
          <div class="premium-section">
            <h4 class="premium-title">证明材料</h4>
            <div v-if="currentDetail.files && currentDetail.files.length > 0">
              <div class="file-list">
                <div v-for="file in currentDetail.files" :key="file.fileId" class="premium-file-item" @click="handlePreviewFile(file)">
                  <div class="file-info">
                    <el-icon class="file-icon"><Document /></el-icon>
                    <div>
                      <div class="file-name">{{ file.fileName }}</div>
                      <div class="file-meta">
                        {{ formatFileSize(file.fileSize) }} | {{ formatDateTime(file.uploadTime) }}
                      </div>
                    </div>
                  </div>
                  <el-button type="primary" size="small" class="action-btn view-btn" @click.stop="handlePreviewFile(file)" plain>预览</el-button>
                </div>
              </div>
            </div>
            <div v-else class="empty-text">无附件材料</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="audit-action-bar">
          <div class="audit-re-review" v-if="currentDetail.applicationStatus === 'approved' || currentDetail.applicationStatus === 'rejected' || currentDetail.applicationStatus === 'returned'">
            <el-button 
              class="action-btn return-btn" 
              @click="handleReReview"
            >
              重新审核
            </el-button>
          </div>
          <div class="audit-status-btns">
            <template v-if="currentDetail.applicationStatus === 'pending'">
              <el-button class="action-btn reject-btn" @click="handleReviewSingle('rejected')">拒绝</el-button>
              <el-button class="action-btn return-btn" @click="handleReviewSingle('returned')">打回</el-button>
              <el-button class="action-btn approve-btn" @click="handleReviewSingle('approved')">通过</el-button>
            </template>
            <el-button class="action-btn" @click="detailDialogVisible = false">关 闭</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 文件预览弹窗 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="previewFileData.fileName"
      width="800px"
      class="preview-dialog"
      @close="handlePreviewDialogClose"
    >
      <div class="preview-container">
        <img 
          v-if="isPreviewImage" 
          :src="getFilePreviewUrl(previewFileData.filePath)" 
          class="preview-image"
          alt="预览图片" 
        />
        <iframe 
          v-else-if="previewFileData.filePath"
          :src="getFilePreviewUrl(previewFileData.filePath)"
          class="preview-iframe"
        ></iframe>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document, Check, Close, RefreshLeft } from '@element-plus/icons-vue'
import axios from 'axios'
import NavBar from '../components/NavBar.vue'

const API_BASE_URL = 'http://localhost:9998/api/review'

const loading = ref(false)
const detailLoading = ref(false)
const detailDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const searchStudentNumber = ref('')
const searchCompetitionName = ref('')
const filterStatus = ref('')
const selectedIds = ref([])

const statistics = reactive({
  totalCount: 0,
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  returnedCount: 0
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const applicationList = ref([])
const currentDetail = ref({})
const previewFileData = reactive({
  fileName: '',
  filePath: '',
  previewUrl: ''
})

// ===== 证书查重 / OCR预检 / 风险评估 =====
const riskInfo = ref(null)
const riskLoading = ref(false)
const opinionSaving = ref(false)
const markRemark = ref('')
const finalOpinion = ref('')

const riskTextMap = { high: '高风险', medium: '中风险', low: '低风险', none: '无风险' }
const riskText = (level) => riskTextMap[level] || '未评估'
const riskTagType = (level) => ({ high: 'danger', medium: 'warning', low: 'info', none: 'success' }[level] || 'info')

const markTextMap = { normal_reuse: '正常复用', abnormal_duplicate: '异常重复', undetermined: '无法判断' }
const markText = (mark) => markTextMap[mark] || mark
const markTagType = (mark) => ({ normal_reuse: 'success', abnormal_duplicate: 'danger', undetermined: 'warning' }[mark] || 'info')

const compareTextMap = { consistent: '一致', inconsistent: '不一致', undetermined: '无法判断' }
const compareText = (r) => compareTextMap[r] || r
const compareTagType = (r) => ({ consistent: 'success', inconsistent: 'danger', undetermined: 'warning' }[r] || 'info')

const isImagePath = (path) => /\.(jpe?g|png|gif|bmp|webp)$/i.test(path || '')

const loadRiskInfo = async (applicationId) => {
  riskInfo.value = null
  riskLoading.value = true
  try {
    const response = await axios.get(`${API_BASE_URL}/risk/${applicationId}`)
    if (response.data && response.data.code === '200') {
      riskInfo.value = response.data.data
      finalOpinion.value = riskInfo.value?.risk?.finalOpinion || ''
      markRemark.value = riskInfo.value?.risk?.manualRemark || ''
    }
  } catch (error) {
    console.error('加载风险信息失败:', error)
  } finally {
    riskLoading.value = false
  }
}

const handleMark = async (markType) => {
  try {
    await ElMessageBox.confirm(
      `确定将此申请标记为「${markTextMap[markType]}」吗？`,
      '人工处理确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const response = await axios.post(`${API_BASE_URL}/risk/mark`, {
      applicationId: currentDetail.value.applicationId,
      markType,
      remark: markRemark.value
    })
    if (response.data && response.data.code === '200') {
      ElMessage.success(`已标记为${markTextMap[markType]}`)
      riskInfo.value = response.data.data
      loadApplicationList()
    } else {
      ElMessage.error(response.data?.msg || '标记失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('标记失败')
      console.error('标记失败:', error)
    }
  }
}

const handleSaveOpinion = async () => {
  opinionSaving.value = true
  try {
    const response = await axios.post(`${API_BASE_URL}/risk/opinion`, {
      applicationId: currentDetail.value.applicationId,
      finalOpinion: finalOpinion.value
    })
    if (response.data && response.data.code === '200') {
      ElMessage.success('最终意见已保存')
    } else {
      ElMessage.error(response.data?.msg || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存意见失败:', error)
  } finally {
    opinionSaving.value = false
  }
}

const statusTextMap = {
  'pending': '待审核',
  'approved': '已通过',
  'rejected': '已拒绝',
  'returned': '已打回'
}

onMounted(() => {
  loadStatistics()
  loadApplicationList()
})

const loadStatistics = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/statistics`)
    Object.assign(statistics, response.data)
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadApplicationList = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE_URL}/list`, {
      params: {
        status: filterStatus.value,
        competitionName: searchCompetitionName.value,
        studentNumber: searchStudentNumber.value,
        page: pagination.page,
        pageSize: pagination.pageSize
      }
    })
    applicationList.value = response.data.list || []
    pagination.total = response.data.total || 0
  } catch (error) {
    ElMessage.error('加载申请列表失败')
    console.error('加载申请列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadApplicationList()
}

const handlePageChange = (page) => {
  pagination.page = page
  loadApplicationList()
}

const handleSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadApplicationList()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.applicationId)
}

const isSelectable = (row) => {
  return row.applicationStatus === 'pending'
}

const filterByStatus = (status) => {
  filterStatus.value = status
  handleSearch()
}

const getRowClassName = ({ row }) => {
  if (row.applicationStatus === 'approved') {
    return 'row-approved'
  } else if (row.applicationStatus === 'returned') {
    return 'row-returned'
  } else if (row.applicationStatus === 'rejected') {
    return 'row-rejected'
  }
  return ''
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

const getStatusText = (status) => {
  return statusTextMap[status] || status
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

const handleViewDetail = async (row) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  riskInfo.value = null
  
  try {
    const response = await axios.get(`${API_BASE_URL}/detail/${row.applicationId}`)
    currentDetail.value = {
      ...response.data,
      competitionName: row.competitionName
    }
    loadRiskInfo(row.applicationId)
  } catch (error) {
    ElMessage.error('加载详情失败')
    console.error('加载详情失败:', error)
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleReview = async (row, status) => {
  const statusText = statusTextMap[status]
  
  try {
    await ElMessageBox.confirm(
      `确定要将申请"${row.competitionName}"${statusText}吗？`,
      '审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.post(`${API_BASE_URL}/single`, {
      applicationId: row.applicationId,
      status: status
    })
    
    ElMessage.success(`${statusText}成功`)
    loadStatistics()
    loadApplicationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${statusText}失败`)
      console.error('审核失败:', error)
    }
  }
}

const handleReviewSingle = async (status) => {
  const statusText = statusTextMap[status]
  
  try {
    await ElMessageBox.confirm(
      `确定要将申请"${currentDetail.value.competitionName}"${statusText}吗？`,
      '审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.post(`${API_BASE_URL}/single`, {
      applicationId: currentDetail.value.applicationId,
      status: status
    })
    
    ElMessage.success(`${statusText}成功`)
    detailDialogVisible.value = false
    loadStatistics()
    loadApplicationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${statusText}失败`)
      console.error('审核失败:', error)
    }
  }
}

const handleBatchReview = async (status) => {
  const statusText = statusTextMap[status]
  
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要审核的申请')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedIds.value.length} 个申请${statusText}吗？`,
      '批量审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.post(`${API_BASE_URL}/batch`, {
      applicationIds: selectedIds.value,
      status: status
    })
    
    ElMessage.success(`批量${statusText}成功`)
    selectedIds.value = []
    loadStatistics()
    loadApplicationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`批量${statusText}失败`)
      console.error('批量审核失败:', error)
    }
  }
}

const handleDetailDialogClose = () => {
  currentDetail.value = {}
  riskInfo.value = null
  markRemark.value = ''
  finalOpinion.value = ''
}

const handleReReview = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要将此申请重新设置为待审核状态吗？',
      '重新审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.post(`${API_BASE_URL}/single`, {
      applicationId: currentDetail.value.applicationId,
      status: 'pending'
    })
    
    ElMessage.success('已重新设置为待审核状态')
    detailDialogVisible.value = false
    loadStatistics()
    loadApplicationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重新审核失败')
      console.error('重新审核失败:', error)
    }
  }
}

const handleReReviewFromTable = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要将申请"${row.competitionName}"重新设置为待审核状态吗？`,
      '重新审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.post(`${API_BASE_URL}/single`, {
      applicationId: row.applicationId,
      status: 'pending'
    })
    
    ElMessage.success('已重新设置为待审核状态')
    loadStatistics()
    loadApplicationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重新审核失败')
      console.error('重新审核失败:', error)
    }
  }
}

// 判断是否为图片
const isPreviewImage = computed(() => {
  if (!previewFileData.filePath) return false;
  const ext = previewFileData.filePath.split('.').pop().toLowerCase();
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext);
});

const handlePreviewFile = async (file) => {
  previewFileData.fileName = file.fileName
  previewFileData.filePath = file.filePath
  if (previewFileData.previewUrl) URL.revokeObjectURL(previewFileData.previewUrl)
  try {
    const response = await axios.get(`http://localhost:9998${file.filePath}`, { responseType: 'blob' })
    previewFileData.previewUrl = URL.createObjectURL(response.data)
  } catch (error) {
    ElMessage.error('附件加载失败')
    return
  }
  previewDialogVisible.value = true
}

const getFilePreviewUrl = (filePath) => {
  if (!filePath) return ''
  // 如果filePath已经包含完整URL，直接返回
  if (filePath.startsWith('http')) {
    return filePath
  }
  // 否则拼接base URL
  return previewFileData.previewUrl
}

const handlePreviewDialogClose = () => {
  previewFileData.fileName = ''
  previewFileData.filePath = ''
  if (previewFileData.previewUrl) URL.revokeObjectURL(previewFileData.previewUrl)
  previewFileData.previewUrl = ''
}
</script>

<style scoped>
.admin-review-page {
  min-height: 100vh;
}

.content-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.review-card {
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

/* 大气优雅的数据看板 */
.elegant-dashboard {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.elegant-card {
  position: relative;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 30px 24px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03), inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
  justify-content: center;
  cursor: pointer;
}

.elegant-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.06), inset 0 0 0 1px rgba(255, 255, 255, 1);
  background: rgba(255, 255, 255, 0.9);
}

.card-info {
  position: relative;
  z-index: 2;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

.card-title .el-icon {
  font-size: 18px;
}

.card-num {
  font-size: 46px;
  font-weight: 800;
  color: #0f172a;
  font-family: 'Inter', -apple-system, sans-serif;
  line-height: 1;
  letter-spacing: -1px;
}

.card-bg-icon {
  position: absolute;
  right: -24px;
  bottom: -30px;
  font-size: 140px;
  color: #cbd5e1;
  opacity: 0.15;
  z-index: 1;
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.elegant-card:hover .card-bg-icon {
  transform: scale(1.1) rotate(-8deg);
  opacity: 0.25;
}

/* 专属色彩点缀：高级渐变与异色 */
.elegant-card.primary .card-num {
  background: linear-gradient(135deg, #4f46e5, #9333ea);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.elegant-card.primary .card-bg-icon { color: #818cf8; }

.elegant-card.pending .card-num { color: #d97706; }
.elegant-card.pending .card-bg-icon { color: #fcd34d; }

.elegant-card.approved .card-num { color: #059669; }
.elegant-card.approved .card-bg-icon { color: #6ee7b7; }

.elegant-card.rejected .card-num { color: #dc2626; }
.elegant-card.rejected .card-bg-icon { color: #fca5a5; }

.elegant-card.returned .card-num { color: #475569; }
.elegant-card.returned .card-bg-icon { color: #94a3b8; }

/* 高级重构工具栏 */
.premium-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.left-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.batch-btn {
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  border-radius: 8px !important;
  padding: 8px 16px !important;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1) !important;
}
.batch-btn .el-icon {
  margin-right: 4px;
}

.batch-approve { background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important; box-shadow: 0 4px 10px rgba(16, 185, 129, 0.2) !important; }
.batch-approve:hover { transform: translateY(-2px) !important; box-shadow: 0 6px 15px rgba(16, 185, 129, 0.35) !important; opacity: 0.95 !important; }

.batch-return { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%) !important; box-shadow: 0 4px 10px rgba(245, 158, 11, 0.2) !important; }
.batch-return:hover { transform: translateY(-2px) !important; box-shadow: 0 6px 15px rgba(245, 158, 11, 0.35) !important; opacity: 0.95 !important; }

.batch-reject { background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important; box-shadow: 0 4px 10px rgba(239, 68, 68, 0.2) !important; }
.batch-reject:hover { transform: translateY(-2px) !important; box-shadow: 0 6px 15px rgba(239, 68, 68, 0.35) !important; opacity: 0.95 !important; }

.data-count-badge {
  margin-left: 10px;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  font-size: 14px;
  color: #64748b;
  border: 1px solid rgba(226, 232, 240, 0.8);
}
.data-count-badge .count-number {
  color: #6366f1;
  font-size: 16px;
  font-weight: 700;
  margin: 0 4px;
}

.right-filters {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.premium-input .el-input__wrapper), 
:deep(.premium-select .el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.02) !important;
  border-radius: 8px;
  transition: all 0.3s;
}

:deep(.premium-input .el-input__wrapper:hover), 
:deep(.premium-select .el-input__wrapper:hover),
:deep(.premium-input .el-input__wrapper.is-focus),
:deep(.premium-select .el-input__wrapper.is-focus) {
  background-color: #ffffff;
  box-shadow: 0 0 0 1px #6366f1 !important;
}

.search-number, .search-name { width: 180px; }
.premium-select { width: 130px; }

.detail-section .section-title {
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
}

.file-info {
  display: flex;
  align-items: center;
}

/* 行颜色 */
:deep(.row-approved) {
  background-color: #f0f9ff !important;
}

:deep(.row-returned) {
  background-color: #fffbf0 !important;
}

:deep(.row-rejected) {
  background-color: #fef0f0 !important;
}

:deep(.row-approved:hover) {
  background-color: #e1f3ff !important;
}

:deep(.row-returned:hover) {
  background-color: #fff4d9 !important;
}

:deep(.row-rejected:hover) {
  background-color: #fde2e2 !important;
}
/* ===== 现代高级透明表格核心逻辑 ===== */
.premium-table {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  background: white !important;
  /* 去除默认边框 */
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #f8fafc;
  margin-top: 15px;
}

:deep(.premium-table .el-table__header-wrapper th) {
  background-color: #f8fafc !important;
  color: #64748b;
  font-weight: 600;
  font-size: 14px;
  height: 54px;
  border-bottom: 1px solid #e2e8f0 !important;
}

/* 添加全行动画基础 */
:deep(.premium-table .el-table__row) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: white !important;
}

:deep(.premium-table .el-table__row td) {
  padding: 16px 0;
  border-bottom: 1px solid transparent !important;
}

/* 原有的特定审核状态上色层与悬停效果完美兼容 */
:deep(.premium-table .row-approved td) { background-color: #f0fdf4 !important; }
:deep(.premium-table .row-returned td) { background-color: #fffbeb !important; }
:deep(.premium-table .row-rejected td) { background-color: #fef2f2 !important; }

/* 动态悬浮突起发光：核心吸睛点 */
:deep(.premium-table .el-table__row:hover td) {
  background-color: #eef2ff !important;
}

:deep(.premium-table .el-table__row:hover) {
  transform: scale(1.002) translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.1), 0 4px 6px -4px rgba(99, 102, 241, 0.05);
  position: relative;
  z-index: 10;
}

/* 隐藏表格根部杂线 */
:deep(.premium-table::before) { display: none; }

/* 重新美化状态标签 */
:deep(.premium-table .el-tag) {
  border: none;
  border-radius: 8px;
  padding: 4px 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

:deep(.premium-table .el-tag--warning) { background: #fef3c7; color: #d97706; }
:deep(.premium-table .el-tag--success) { background: #d1fae5; color: #059669; }
:deep(.premium-table .el-tag--danger) { background: #fee2e2; color: #dc2626; }
:deep(.premium-table .el-tag--info) { background: #f1f5f9; color: #475569; }

/* ===== 炫酷动画操作按钮 - 药丸风格组群 ===== */
.action-btn-group {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.action-btn {
  border: 1px solid rgba(0, 0, 0, 0.04) !important;
  border-radius: 6px !important; /* 恢复为常规圆角长方形 */
  padding: 6px 14px !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  letter-spacing: 0.5px;
  background: #ffffff !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02) !important;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

/* 查看详情 -> 科技紫 */
.action-btn.view-btn { color: #6366f1 !important; }
.action-btn.view-btn:hover {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  color: white !important; border-color: transparent !important;
  transform: scale(1.05) !important; box-shadow: 0 4px 15px rgba(99, 102, 241, 0.35) !important;
}

/* 通过 -> 清新绿 */
.action-btn.approve-btn { color: #10b981 !important; }
.action-btn.approve-btn:hover {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  color: white !important; border-color: transparent !important;
  transform: scale(1.05) !important; box-shadow: 0 4px 15px rgba(16, 185, 129, 0.35) !important;
}

/* 打回/重审 -> 阳光橙 */
.action-btn.return-btn { color: #f59e0b !important; }
.action-btn.return-btn:hover {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%) !important;
  color: white !important; border-color: transparent !important;
  transform: scale(1.05) !important; box-shadow: 0 4px 15px rgba(245, 158, 11, 0.35) !important;
}

/* 拒绝 -> 魅影红 */
.action-btn.reject-btn { color: #ef4444 !important; }
.action-btn.reject-btn:hover {
  background: linear-gradient(135deg, #ef4444 0%, #f43f5e 100%) !important;
  color: white !important; border-color: transparent !important;
  transform: scale(1.05) !important; box-shadow: 0 4px 15px rgba(239, 68, 68, 0.35) !important;
}

.action-btn:active {
  transform: scale(0.95) !important;
}

/* ======== 多选框组件(Checkbox)果冻悬浮特效 (全向弥散光圈) ======== */
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

/* 强制重写组件原来的 transform 以确保绝对居中，避免 Element Plus 的默认覆盖 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  transform: rotate(45deg) scaleY(1) !important;
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

/* ===== 高品质卡片式详情弹窗 ===== */
:deep(.premium-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

:deep(.premium-dialog .el-dialog__header) {
  margin: 0;
  padding: 24px 24px 20px;
  background: linear-gradient(to right, #f8fafc, #ffffff);
  border-bottom: 1px solid #f1f5f9;
}

:deep(.premium-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

:deep(.premium-dialog .el-dialog__body) {
  padding: 20px 24px;
  background-color: #f8fafc; /* 使用灰色底衬托白色的内容卡片 */
}

:deep(.premium-dialog .el-dialog__footer) {
  padding: 16px 24px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
}

/* 详情卡片块 */
.premium-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.02);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.premium-section:last-child {
  margin-bottom: 0;
}

.premium-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 20px;
  margin-top: 0;
}

.premium-title::before {
  content: '';
  display: block;
  width: 4px;
  height: 18px;
  background: linear-gradient(to bottom, #6366f1, #a855f7);
  border-radius: 4px;
  margin-right: 10px;
}

/* 美化描述列表表单 */
:deep(.premium-desc) {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

:deep(.premium-desc .el-descriptions__body) {
  border-radius: 8px;
}

:deep(.premium-desc .el-descriptions__label) {
  background-color: #f8fafc !important;
  color: #64748b;
  font-weight: 600;
  width: 120px;
}

/* 内部小表格美化 */
:deep(.premium-inner-table) {
  border-radius: 8px;
  overflow: hidden;
  --el-table-border-color: #e2e8f0;
}

:deep(.premium-inner-table th) {
  background-color: #f8fafc !important;
  color: #475569;
  font-weight: 600;
}

/* 附件项美化 */
.file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.premium-file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: pointer;
}

.premium-file-item:hover {
  background-color: #f8fafc;
  border-color: #cbd5e1;
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
}

.file-info {
  display: flex;
  align-items: center;
}

.file-icon {
  font-size: 32px;
  color: #6366f1;
  margin-right: 16px;
  opacity: 0.9;
}

.file-name {
  font-weight: 600;
  color: #334155;
  margin-bottom: 4px;
  font-size: 14px;
}

.file-meta {
  font-size: 12px;
  color: #94a3b8;
}

.empty-text {
  color: #94a3b8;
  padding: 20px 0;
  text-align: center;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px dashed #e2e8f0;
}

/* ===== 风险评估 / OCR对照 / 人工处理 ===== */
.risk-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.risk-reasons {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
}

.risk-reason-item {
  color: #b91c1c;
  font-size: 13px;
  line-height: 1.9;
}

.reason-dot {
  margin-right: 8px;
  font-size: 8px;
  vertical-align: 2px;
}

.ocr-block {
  background: #fafbfe;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
}

.ocr-file-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.ocr-file-name {
  font-weight: 600;
  color: #334155;
  font-size: 14px;
}

.ocr-cert-no {
  margin-top: 8px;
  font-size: 13px;
  color: #475569;
}

.ocr-raw-collapse {
  margin-top: 8px;
  border-top: none;
}

.ocr-raw-text {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 12px;
  color: #64748b;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 180px;
  overflow-y: auto;
  margin: 0;
}

.mismatch {
  color: #dc2626;
  font-weight: 700;
}

.declared-val {
  color: #334155;
}

.text-muted-small {
  color: #94a3b8;
  font-size: 12px;
}

.sim-hot {
  color: #dc2626;
  font-weight: 700;
}

.manual-mark-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.manual-info {
  font-size: 13px;
  color: #475569;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
}

.mt-10 {
  margin-top: 10px;
}

/* ======= 文件预览弹窗专用 ======= */
.preview-container {
  text-align: center;
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8fafc;
  border-radius: 8px;
  overflow: hidden;
}
.preview-image {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.preview-iframe {
  width: 100%;
  height: 65vh;
  border: none;
}

/* ========================================= */
/* 📱 移动端深度适配 (AdminReview)         */
/* ========================================= */
@media screen and (max-width: 768px) {
  /* 基础容器 */
  .admin-review-page.login-wrapper-style { padding-bottom: 20px; }
  .content-container { padding: 10px; }
  .review-card { border-radius: 16px !important; margin-top: 10px; }
  :deep(.review-card .el-card__body) { padding: 16px 12px; }

  /* 📊 数据看板换列瀑布流 */
  .elegant-dashboard { grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 20px; }
  .elegant-card.primary { grid-column: 1 / -1; }
  .elegant-card { padding: 20px 16px; border-radius: 16px; min-height: 90px; }
  .card-num { font-size: 32px; }
  .card-bg-icon { font-size: 80px; right: -15px; bottom: -20px; }

  /* 🎛️ 筛选栏：高定级小组件化排版 */
  .premium-filter-bar {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
    gap: 16px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.85);
  }
  
  /* 移动端由于卡片化隐藏了复选框，这里同步隐藏所有批量操作按钮 */
  .left-actions { 
    display: none !important;
  }
  
  /* 数据计数徽标改为胶囊式，并占满宽度 */
  .data-count-badge { 
    width: 100%;
    display: flex;
    justify-content: center; 
    padding: 10px;
    margin-top: 0;
    border: none;
    background: #f8fafc;
    border-radius: 30px;
    box-sizing: border-box;
  }
  
  .right-filters { 
    display: flex;
    flex-direction: column; 
    width: 100%; 
    gap: 12px; 
  }
  
  .premium-input, .premium-select { 
    width: 100% !important; 
    margin: 0 !important; 
  }
  
  /* 让移动端输入框胖乎乎一点，手感更好 */
  :deep(.right-filters .el-input__wrapper) {
    padding: 6px 15px;
    border-radius: 10px;
  }

  /* 📑 彻底重构审核表格为“数据流卡片式” */
  .premium-table { background: transparent !important; box-shadow: none !important; }
  .premium-table :deep(.el-table__header-wrapper) { display: none !important; }
  .premium-table :deep(.el-table__body-wrapper) { overflow: visible !important; }
  .premium-table :deep(table), .premium-table :deep(tbody) { display: block; width: 100% !important; }
  
  .premium-table :deep(.el-table__row) {
    display: flex !important; flex-direction: column !important;
    width: 100%; 
    background-color: #ffffff !important;
    --el-table-tr-bg-color: #ffffff !important;
    border-radius: 12px !important; margin-bottom: 16px !important;
    padding: 12px 16px !important; box-sizing: border-box;
    border: 1px solid #e2e8f0 !important;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05) !important;
  }
  
  /* 状态卡片：整卡高亮变色系统 */
  .premium-table :deep(.el-table__row.row-approved) {
    background-color: #f0fdf4 !important; /* 护眼淡绿 */
    --el-table-tr-bg-color: #f0fdf4 !important;
    border-color: #bbf7d0 !important;
  }
  .premium-table :deep(.el-table__row.row-returned) {
    background-color: #fffbeb !important; /* 温和浅黄 */
    --el-table-tr-bg-color: #fffbeb !important;
    border-color: #fde68a !important;
  }
  .premium-table :deep(.el-table__row.row-rejected) {
    background-color: #fef2f2 !important; /* 警告淡红 */
    --el-table-tr-bg-color: #fef2f2 !important;
    border-color: #fecaca !important;
  }

  .premium-table :deep(.el-table__row td) { 
    display: flex !important; width: 100% !important; padding: 4px 0 !important; 
    border: none !important; text-align: left !important; align-items: flex-start !important; 
    background-color: transparent !important; /* 强制透出父级的纯白，杜绝格子感 */
  }
  .premium-table :deep(.el-table__row td:hover), .premium-table :deep(.el-table__row--striped td) { background-color: transparent !important; }
  .premium-table :deep(.cell) { padding: 0 !important; display: flex; width: 100%; flex-wrap: wrap; }

  /* 表格卡片引导注入 */
  .premium-table :deep(.el-table__row td:nth-child(1)) { display: none !important; } /* 多选框暂隐藏或特定处理 */
  .premium-table :deep(.el-table__row td:nth-child(2)) { display: none !important; } /* 序号隐藏 */
  .premium-table :deep(.el-table__row td:nth-child(3)::before) { content: "学号："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; }
  .premium-table :deep(.el-table__row td:nth-child(4)) { font-size: 16px; font-weight: 700; color: #1e293b; padding: 8px 0 !important; border-bottom: 1px dashed #f1f5f9 !important; margin-bottom: 4px; }
  .premium-table :deep(.el-table__row td:nth-child(4)::before) { content: "赛事："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; margin-top: 2px; }
  .premium-table :deep(.el-table__row td:nth-child(5)::before) { content: "级别："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; }
  .premium-table :deep(.el-table__row td:nth-child(6)::before) { content: "等次："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; }
  .premium-table :deep(.el-table__row td:nth-child(7)::before) { content: "获奖时间："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; }
  .premium-table :deep(.el-table__row td:nth-child(8)::before) { content: "状态："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; }
  .premium-table :deep(.el-table__row td:nth-child(9)::before) { content: "提交时间："; color: #94a3b8; font-size: 13px; font-weight: 500; min-width: 70px; }

  /* 审核操作组 (铺满卡片底部区域) */
  .premium-table :deep(.el-table__row td:last-child) { margin-top: 12px; padding-top: 12px !important; border-top: 1px solid #f1f5f9 !important; }
  .premium-table :deep(.el-table__row td:last-child .cell) { justify-content: flex-end; }
  .premium-table :deep(.action-btn-group) { width: 100%; display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; }
  .premium-table :deep(.action-btn-group .el-button) { flex: 1 1 auto; margin-left: 0 !important; padding: 12px 16px !important; }

  /* 🔍 详情审核弹窗重塑 */
  :deep(.detail-dialog) { width: 95% !important; margin: 5vh auto !important; border-radius: 16px !important; height: auto !important; }
  :deep(.detail-dialog .el-dialog__body) { padding: 16px !important; }

  /* 🌟 详情弹窗数据项重构：iOS 风格左右分栏 */
  :deep(.premium-desc), :deep(.premium-desc .el-descriptions__body) { border: none !important; background: transparent !important; }
  :deep(.premium-desc .el-descriptions__body table), :deep(.premium-desc .el-descriptions__body tbody) { display: block; width: 100%; border: none !important; }
  :deep(.premium-desc .el-descriptions__body tr) { display: grid !important; grid-template-columns: 85px 1fr; width: 100%; border: none !important; }
  :deep(.premium-desc .el-descriptions__label), :deep(.premium-desc .el-descriptions__content), :deep(.premium-desc.is-bordered .el-descriptions__body .el-descriptions__cell) {
    display: flex; align-items: flex-start; border: none !important; border-bottom: 1px solid #f1f5f9 !important; box-sizing: border-box; text-align: left !important; background-color: transparent !important; padding: 16px 4px !important; line-height: 1.5;
  }
  :deep(.premium-desc .el-descriptions__label) { color: #64748b; font-size: 14px; font-weight: 500; }
  :deep(.premium-desc .el-descriptions__content) { color: #0f172a; font-size: 14px; font-weight: 600; }
  :deep(.premium-desc .el-descriptions__body tr:last-child .el-descriptions__label:last-of-type), :deep(.premium-desc .el-descriptions__body tr:last-child .el-descriptions__content:last-of-type) { border-bottom: none !important; }

  /* 🌟 内部团队/老师卡片流 */
  .premium-inner-table { background: transparent !important; border: none !important; --el-table-border-color: transparent !important; --el-table-bg-color: transparent !important; }
  .premium-inner-table :deep(.el-table__inner-wrapper::before) { display: none !important; }
  .premium-inner-table :deep(.el-table__inner-wrapper) { background: transparent !important; }
  .premium-inner-table :deep(.el-table__header-wrapper) { display: none !important; }
  .premium-inner-table :deep(.el-table__body-wrapper) { overflow: visible !important; }
  .premium-inner-table :deep(table), .premium-inner-table :deep(tbody) { display: block; width: 100% !important; }
  .premium-inner-table :deep(.el-table__row) { display: flex !important; flex-direction: column; background: #ffffff !important; border-radius: 12px; padding: 12px 16px; margin-bottom: 12px; border: 1px solid #e2e8f0; box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.02) !important; }
  .premium-inner-table :deep(.el-table__row td) { display: flex; align-items: center; padding: 6px 0 !important; border: none !important; background: transparent !important; }
  .premium-inner-table :deep(.cell) { padding: 0 !important; display: flex; width: 100%; color: #1e293b; font-size: 14px; font-weight: 500; }
  .premium-inner-table :deep(.el-table__row td::before) { color: #64748b; font-size: 13px; font-weight: 400; width: 65px; flex-shrink: 0; }

  /* 区分配置注入 */
  .premium-inner-table :deep(.el-table__row td:nth-child(1)) { display: none !important; }
  .premium-inner-table :deep(.el-table__row td:nth-child(2)::before) { content: "姓名"; }
  .premium-inner-table :deep(.el-table__row td:nth-child(3)::before) { content: "学号"; }

  /* 审核底部工具栏 (审核状态与意见提交部分) */
  .detail-footer-content { margin-top: 10px; }
  .audit-action-bar { flex-direction: column; gap: 12px; }
  .audit-radio-group { width: 100%; }
  .audit-radio-group :deep(.el-radio-button) { flex: 1; }
  .audit-radio-group :deep(.el-radio-button__inner) { width: 100%; }

  /* 🖼️ 预览弹窗 */
  :deep(.preview-dialog) { width: 95% !important; margin: 5vh auto !important; border-radius: 12px !important; }
  :deep(.preview-dialog .el-dialog__body) { padding: 10px !important; }
  .preview-container { min-height: 60vh; }
  .preview-iframe { height: 70vh; }

  /* 📄 底部翻页栏适配 */
  :deep(.el-pagination) {
    display: flex;
    flex-wrap: wrap;
    justify-content: center !important;
    gap: 10px;
    padding: 10px 0;
  }
  :deep(.el-pagination button),
  :deep(.el-pagination span:not([class*=suffix])),
  :deep(.el-pagination .el-select) {
    margin: 0 !important;
  }
}
</style>
