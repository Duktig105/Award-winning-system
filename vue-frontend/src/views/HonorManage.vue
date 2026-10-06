<template>
  <div class="honor-manage-page">
    <NavBar />

    <div class="content-container">
      <el-card class="manage-card">
        <template #header>
          <div class="card-header">
            <h2>荣誉系统管理</h2>
            <div class="header-actions">
              <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
              <el-button type="primary" :icon="Refresh" @click="recalcScores">重新计算积分</el-button>
            </div>
          </div>
        </template>

        <!-- 概览统计 -->
        <div class="stat-row">
          <div class="stat-card" v-for="s in summary" :key="s.key">
            <span class="stat-label">{{ s.label }}</span>
            <span class="stat-value">{{ s.value }}</span>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="manage-tabs">
          <!-- 类别 -->
          <el-tab-pane label="竞赛类别" name="category">
            <div class="action-bar">
              <el-button type="primary" :icon="Plus" @click="openCategoryDialog()">新增类别</el-button>
            </div>
            <el-table :data="categories" stripe>
              <el-table-column label="序号" type="index" width="80" />
              <el-table-column prop="categoryName" label="类别名称" min-width="160" />
              <el-table-column prop="description" label="说明" min-width="240" />
              <el-table-column prop="icon" label="图标" width="120">
                <template #default="{ row }">
                  <el-tag size="small" round>{{ row.icon || '无' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round size="small">
                    {{ row.status === 'enabled' ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openCategoryDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="disableCategory(row)">停用</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 技能 -->
          <el-tab-pane label="技能标签" name="skill">
            <div class="action-bar">
              <el-button type="primary" :icon="Plus" @click="openSkillDialog()">新增技能</el-button>
            </div>
            <el-table :data="skills" stripe>
              <el-table-column label="序号" type="index" width="80" />
              <el-table-column prop="name" label="技能名称" min-width="160" />
              <el-table-column prop="categoryName" label="所属方向" min-width="140" />
              <el-table-column prop="level" label="技能等级" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.level === 'advanced' ? 'danger' : (row.level === 'intermediate' ? 'warning' : 'info')" round size="small">
                    {{ levelText(row.level) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="自评" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.allowSelfEval ? 'success' : 'info'" round size="small">{{ row.allowSelfEval ? '允许' : '禁用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="仅获奖验证" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.awardOnly ? 'warning' : 'info'" round size="small">{{ row.awardOnly ? '是' : '否' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round size="small">{{ row.status === 'enabled' ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openSkillDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="disableSkill(row)">停用</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 竞赛-技能 -->
          <el-tab-pane label="竞赛-技能关联" name="relation">
            <div class="action-bar">
              <el-radio-group v-model="relationFilter" size="small" @change="loadRelations">
                <el-radio-button value="">全部映射</el-radio-button>
                <el-radio-button value="default">类别默认</el-radio-button>
                <el-radio-button value="override">竞赛级覆盖</el-radio-button>
              </el-radio-group>
              <el-button type="primary" :icon="Plus" @click="openRelationDialog()">新增映射</el-button>
              <span class="map-tip">竞赛级覆盖优先于类别默认；同一映射目标下权重之和应为 1.0（引擎对非 1 值按比例归一化兜底）</span>
            </div>
            <el-table :data="relations" stripe>
              <el-table-column label="序号" type="index" width="70" />
              <el-table-column prop="categoryName" label="竞赛类别" min-width="130" />
              <el-table-column label="映射层级" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.competitionId ? 'warning' : 'success'" round size="small">
                    {{ row.competitionId ? '竞赛级覆盖' : '类别默认' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="competitionName" label="竞赛（覆盖目标）" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.competitionName || '—（类别默认）' }}</template>
              </el-table-column>
              <el-table-column prop="skillName" label="技能" min-width="140" />
              <el-table-column label="权重" width="100" align="center">
                <template #default="{ row }">{{ (Number(row.weight) * 100).toFixed(0) }}%</template>
              </el-table-column>
              <el-table-column prop="contribution" label="贡献值" width="90" align="center" />
              <el-table-column label="操作" width="160" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openRelationDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteRelation(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 积分规则 -->
          <el-tab-pane label="积分规则" name="score">
            <div class="action-bar">
              <el-button type="primary" :icon="Plus" @click="openRuleDialog()">新增参数</el-button>
              <span class="map-tip">五因子：基础分S(等次×级别) × 层次L × 等级G × 角色R × 特殊K；技能经验 = 个人积分 × 权重 × 放大系数</span>
            </div>
            <el-table :data="rules" stripe>
              <el-table-column label="序号" type="index" width="70" />
              <el-table-column prop="paramGroup" label="参数组" min-width="140" />
              <el-table-column prop="paramKey" label="参数项" min-width="170" />
              <el-table-column prop="paramValue" label="取值" width="110" align="center" />
              <el-table-column prop="paramRemark" label="说明" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.paramRemark || '—' }}</template>
              </el-table-column>
              <el-table-column prop="ruleVersion" label="版本" width="90" align="center" />
              <el-table-column label="启用" width="90" align="center">
                <template #default="{ row }">
                  <el-switch :model-value="Number(row.enabled) === 1" @change="(v) => toggleRule(row, v)" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openRuleDialog(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 竞赛目录 -->
          <el-tab-pane label="竞赛目录" name="catalog">
            <div class="action-bar">
              <el-input v-model="catalogKeyword" placeholder="搜索竞赛名称" clearable style="width:240px" @keyup.enter="loadCompetitions" @clear="loadCompetitions" />
              <el-button :icon="Refresh" @click="loadCompetitions">搜索</el-button>
              <span class="map-tip">维护竞赛的类别归属、目录等次(A/B/C/D)与计分覆盖；基础分留空时按"等次基础分×级别系数"自动计算</span>
            </div>
            <el-table :data="competitions" stripe max-height="520">
              <el-table-column prop="competitionId" label="ID" width="70" />
              <el-table-column prop="competitionName" label="竞赛名称" min-width="260" show-overflow-tooltip />
              <el-table-column prop="categoryName" label="所属类别" min-width="120">
                <template #default="{ row }">{{ row.categoryName || '未归类' }}</template>
              </el-table-column>
              <el-table-column label="目录等次" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.grade || row.awardRank" :type="gradeTagType(row.grade || row.awardRank)" size="small">{{ row.grade || row.awardRank }}</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="计分覆盖" width="130" align="center">
                <template #default="{ row }">
                  <span v-if="row.overrideLevel || row.overrideGrade">{{ [row.overrideLevel, row.overrideGrade].filter(Boolean).join(' / ') }}</span>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column prop="baseScore" label="基础分" width="90" align="center">
                <template #default="{ row }">{{ row.baseScore ?? '—' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openCatalogDialog(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 获奖认定 -->
          <el-tab-pane label="获奖认定" name="mark">
            <div class="action-bar">
              <el-select v-model="markStatus" style="width:160px" @change="loadApplications">
                <el-option label="全部状态" value="" />
                <el-option label="待审核" value="pending" />
                <el-option label="已通过" value="approved" />
                <el-option label="已拒绝" value="rejected" />
              </el-select>
              <el-input v-model="markKeyword" placeholder="竞赛/姓名/学号/编号" clearable style="width:240px" @keyup.enter="loadApplications" @clear="loadApplications" />
              <el-button :icon="Refresh" @click="loadApplications">查询</el-button>
              <span class="map-tip">在审核通过前设置团队角色（队长1.0/技术骨干1.0/核心成员0.8/普通成员0.6）与"历史性突破奖"认定（特殊系数×1.5）</span>
            </div>
            <el-table :data="applications" stripe max-height="520">
              <el-table-column prop="applicationNumber" label="申请编号" width="120" />
              <el-table-column prop="studentName" label="学生" width="110">
                <template #default="{ row }">{{ row.studentName || row.studentNumber || '—' }}</template>
              </el-table-column>
              <el-table-column prop="competitionName" label="竞赛" min-width="220" show-overflow-tooltip />
              <el-table-column prop="competitionLevel" label="获奖层次" width="90" align="center" />
              <el-table-column prop="awardLevel" label="获奖等级" width="110" show-overflow-tooltip />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="({ approved: 'success', pending: 'warning', rejected: 'danger', returned: 'info' }[row.applicationStatus]) || 'info'" size="small" round>
                    {{ ({ approved: '已通过', pending: '待审核', rejected: '已拒绝', returned: '已打回' }[row.applicationStatus]) || row.applicationStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="memberRole" label="角色认定" width="110" align="center">
                <template #default="{ row }">{{ row.memberRole || '—（自动推导）' }}</template>
              </el-table-column>
              <el-table-column label="历史突破" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="Number(row.breakthrough) === 1" type="danger" size="small" round>已认定 ×1.5</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button size="small" type="primary" @click="openMarkDialog(row)">认定</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 勋章 -->
          <el-tab-pane label="勋章管理" name="badge">
            <div class="action-bar badge-action-bar">
              <el-button type="primary" :icon="Plus" @click="openBadgeDialog()">新增勋章</el-button>
              <el-input-number v-model="recheckStudentId" :min="1" :controls="false" placeholder="学生ID" style="width: 130px" />
              <el-button :icon="Refresh" @click="recheckBadges">按学生重算勋章</el-button>
            </div>
            <el-alert type="info" show-icon :closable="false" title="第一版采用固定条件类型 + 参数配置，预置8枚勋章可直接编辑解锁条件与阈值" style="margin-top: 12px" />
            <el-table :data="badges" stripe style="margin-top: 16px;">
              <el-table-column prop="badgeName" label="勋章名称" min-width="130" />
              <el-table-column prop="badgeCode" label="编码" width="130" />
              <el-table-column prop="description" label="说明" min-width="200" />
              <el-table-column label="解锁条件" min-width="220">
                <template #default="{ row }">
                  <span>{{ conditionTextOf(row) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="unlockThreshold" label="解锁阈值" width="100" align="center" />
              <el-table-column prop="sortOrder" label="展示顺序" width="100" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-switch :model-value="row.status" active-value="enabled" inactive-value="disabled" @change="(v) => toggleBadge(row, v)" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openBadgeDialog(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 荣誉标签 -->
          <el-tab-pane label="荣誉标签" name="tag">
            <div class="action-bar">
              <el-button type="primary" :icon="Plus" @click="openTagDialog()">新增标签</el-button>
            </div>
            <el-table :data="tags" stripe>
              <el-table-column prop="tagName" label="标签名称" min-width="140" />
              <el-table-column prop="tagType" label="类型" width="140">
                <template #default="{ row }">
                  <el-tag size="small" round>{{ typeText(row.tagType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明" min-width="240" />
              <el-table-column prop="conditionRule" label="生成条件" min-width="200" />
              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.statusComputed" active-value="enabled" inactive-value="disabled" @change="(v) => toggleTag(row, v)" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" align="center">
                <template #default="{ row }">
                  <el-button size="small" @click="openTagDialog(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- 类别对话框 -->
    <el-dialog v-model="categoryDialog.visible" :title="categoryDialog.form.categoryId ? '编辑竞赛类别' : '新增竞赛类别'" width="500px" class="premium-dialog">
      <el-form :model="categoryDialog.form" label-width="100px">
        <el-form-item label="类别名称"><el-input v-model="categoryDialog.form.categoryName" /></el-form-item>
        <el-form-item label="类别说明"><el-input v-model="categoryDialog.form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="图标">
          <el-select v-model="categoryDialog.form.icon" placeholder="选择图标标识" style="width:100%">
            <el-option label="monitor(编程)" value="monitor" />
            <el-option label="data-analysis(数学)" value="data-analysis" />
            <el-option label="magic-stick(创新)" value="magic-stick" />
            <el-option label="reading(科研)" value="reading" />
            <el-option label="cpu(电子)" value="cpu" />
            <el-option label="picture(视觉)" value="picture" />
            <el-option label="trophy(文体)" value="trophy" />
            <el-option label="star(综合)" value="star" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryDialog.form.sortOrder" :min="0" :max="999" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <!-- 技能对话框 -->
    <el-dialog v-model="skillDialog.visible" :title="skillDialog.form.skillId ? '编辑技能' : '新增技能'" width="500px" class="premium-dialog">
      <el-form :model="skillDialog.form" label-width="100px">
        <el-form-item label="技能名称"><el-input v-model="skillDialog.form.name" /></el-form-item>
        <el-form-item label="所属方向">
          <el-select v-model="skillDialog.form.skillCategoryId" placeholder="选择方向" style="width:100%">
            <el-option v-for="c in skillCategories" :key="c.skillCategoryId" :label="c.categoryName" :value="c.skillCategoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能等级">
          <el-radio-group v-model="skillDialog.form.level">
            <el-radio-button value="basic">基础</el-radio-button>
            <el-radio-button value="intermediate">中级</el-radio-button>
            <el-radio-button value="advanced">高级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="技能说明"><el-input v-model="skillDialog.form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="允许自评"><el-switch v-model="skillDialog.form.allowSelfEval" :disabled="skillDialog.form.awardOnly" /></el-form-item>
        <el-form-item label="仅获奖验证">
          <el-switch v-model="skillDialog.form.awardOnly" @change="(v) => { if (v) skillDialog.form.allowSelfEval = false }" />
          <span class="map-tip" style="margin-left:10px">开启后隐藏学生自评入口，等级仅由获奖记录驱动</span>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="skillDialog.form.sortOrder" :min="0" :max="999" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skillDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关联对话框 -->
    <el-dialog v-model="relationDialog.visible" title="竞赛-技能映射" width="560px" class="premium-dialog">
      <el-form :model="relationDialog.form" label-width="100px">
        <el-form-item label="映射层级">
          <el-radio-group v-model="relationDialog.mapType">
            <el-radio value="default">类别默认</el-radio>
            <el-radio value="override">竞赛级覆盖</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="relationDialog.mapType === 'override'" label="竞赛">
          <el-select v-model="relationDialog.form.competitionId" filterable remote :remote-method="searchCompetitions"
            :loading="competitionLoading" placeholder="搜索并选择竞赛" style="width:100%">
            <el-option v-for="c in competitionOptions" :key="c.competitionId" :label="`${c.competitionName}（${c.categoryName || '未归类'}）`" :value="c.competitionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="竞赛类别">
          <el-select v-model="relationDialog.form.categoryId" placeholder="选择类别" style="width:100%">
            <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能">
          <el-select v-model="relationDialog.form.skillId" filterable placeholder="选择技能" style="width:100%">
            <el-option v-for="s in skills" :key="s.skillId" :label="s.name" :value="s.skillId" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能权重">
          <el-input-number v-model="relationDialog.form.weight" :step="0.05" :min="0.05" :max="1" />
          <span class="map-tip" style="margin-left:10px">同一目标下权重之和应为 1.0</span>
        </el-form-item>
        <el-form-item label="贡献值"><el-input-number v-model="relationDialog.form.contribution" :min="1" :max="9999" /></el-form-item>
        <el-form-item label="团队角色"><el-input v-model="relationDialog.form.teamRole" placeholder="如：技术负责人（可空）" /></el-form-item>
        <el-form-item label="角色权重"><el-input-number v-model="relationDialog.form.teamRoleWeight" :min="0" :max="100" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relationDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRelation">保存</el-button>
      </template>
    </el-dialog>

    <!-- 积分规则对话框（v2.0 参数行） -->
    <el-dialog v-model="ruleDialog.visible" :title="ruleDialog.form.ruleId ? '编辑积分参数' : '新增积分参数'" width="520px" class="premium-dialog">
      <el-form :model="ruleDialog.form" label-width="100px">
        <el-form-item label="参数组">
          <el-select v-model="ruleDialog.form.paramGroup" filterable allow-create default-first-option style="width:100%" placeholder="选择或输入参数组">
            <el-option v-for="g in ruleGroups" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="参数项"><el-input v-model="ruleDialog.form.paramKey" placeholder="如 A等 / 国家级 / 历史性突破奖" /></el-form-item>
        <el-form-item label="参数取值"><el-input v-model="ruleDialog.form.paramValue" placeholder="数值或取值文本，如 100 / 1.5 / 取最高" /></el-form-item>
        <el-form-item label="参数说明"><el-input v-model="ruleDialog.form.paramRemark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="ruleDialog.form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 竞赛目录对话框 -->
    <el-dialog v-model="catalogDialog.visible" title="竞赛目录维护" width="520px" class="premium-dialog">
      <el-form :model="catalogDialog.form" label-width="110px">
        <el-form-item label="竞赛名称">
          <el-input :model-value="catalogDialog.form.competitionName" disabled />
        </el-form-item>
        <el-form-item label="所属类别">
          <el-select v-model="catalogDialog.form.categoryId" placeholder="选择类别" style="width:100%">
            <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="目录等次">
          <el-select v-model="catalogDialog.form.grade" style="width:100%" clearable>
            <el-option label="A等（基础分100）" value="A" />
            <el-option label="B等（基础分80）" value="B" />
            <el-option label="C等（基础分60）" value="C" />
            <el-option label="D等（基础分40）" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="计分级别覆盖">
          <el-select v-model="catalogDialog.form.overrideLevel" style="width:100%" clearable placeholder="学校文件特殊口径时设置">
            <el-option label="按国家级计分" value="国家级" />
            <el-option label="按省级计分" value="省级" />
          </el-select>
        </el-form-item>
        <el-form-item label="计分等次覆盖">
          <el-select v-model="catalogDialog.form.overrideGrade" style="width:100%" clearable placeholder="如大创年会：目录A等按C等计分">
            <el-option label="按A等计分" value="A" />
            <el-option label="按B等计分" value="B" />
            <el-option label="按C等计分" value="C" />
            <el-option label="按D等计分" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础分">
          <el-input-number v-model="catalogDialog.form.baseScore" :min="0" :max="999" :step="1" :precision="1" placeholder="留空自动计算" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catalogDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveCatalog">保存</el-button>
      </template>
    </el-dialog>

    <!-- 获奖认定对话框 -->
    <el-dialog v-model="markDialog.visible" title="获奖认定" width="480px" class="premium-dialog">
      <el-form :model="markDialog.form" label-width="110px">
        <el-form-item label="竞赛">
          <el-input :model-value="markDialog.form.competitionName" disabled />
        </el-form-item>
        <el-form-item label="团队角色">
          <el-select v-model="markDialog.form.memberRole" style="width:100%" clearable placeholder="留空则按团队信息自动推导">
            <el-option label="队长 / 第一完成人（×1.0）" value="队长" />
            <el-option label="技术骨干 / 第2-3完成人（×1.0）" value="技术骨干" />
            <el-option label="核心成员（×0.8）" value="核心成员" />
            <el-option label="普通成员（×0.6）" value="普通成员" />
          </el-select>
        </el-form-item>
        <el-form-item label="历史性突破">
          <el-switch v-model="markDialog.form.breakthrough" />
          <span class="map-tip" style="margin-left:10px">学校/学院首次获得该竞赛最高奖（特殊系数×1.5）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="markDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveMark">保存认定</el-button>
      </template>
    </el-dialog>

    <!-- 标签对话框 -->
    <el-dialog v-model="tagDialog.visible" :title="tagDialog.form.tagId ? '编辑标签' : '新增标签'" width="500px" class="premium-dialog">
      <el-form :model="tagDialog.form" label-width="100px">
        <el-form-item label="标签名称"><el-input v-model="tagDialog.form.tagName" /></el-form-item>
        <el-form-item label="标签类型">
          <el-select v-model="tagDialog.form.tagType" style="width:100%">
            <el-option label="获奖等级" value="award_level" />
            <el-option label="竞赛方向" value="competition_direction" />
            <el-option label="团队角色" value="role" />
            <el-option label="能力标签" value="ability" />
            <el-option label="成长标签" value="growth" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签说明"><el-input v-model="tagDialog.form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="生成条件"><el-input v-model="tagDialog.form.conditionRule" placeholder="JSON字符串,例如: {&quot;type&quot;:&quot;award_rank&quot;,&quot;award_rank&quot;:&quot;A&quot;}" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="tagDialog.form.sortOrder" :min="0" :max="999" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="tagDialog.form.statusComputed" active-value="enabled" inactive-value="disabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>

    <!-- 勋章对话框 -->
    <el-dialog v-model="badgeDialog.visible" :title="badgeDialog.form.badgeId ? '编辑勋章' : '新增勋章'" width="520px" class="premium-dialog">
      <el-form :model="badgeDialog.form" label-width="100px">
        <el-form-item label="勋章编码"><el-input v-model="badgeDialog.form.badgeCode" :disabled="!!badgeDialog.form.badgeId" placeholder="唯一编码，例如 rocket" /></el-form-item>
        <el-form-item label="勋章名称"><el-input v-model="badgeDialog.form.badgeName" /></el-form-item>
        <el-form-item label="勋章说明"><el-input v-model="badgeDialog.form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="图标">
          <el-select v-model="badgeDialog.form.icon" style="width:100%">
            <el-option label="star-on(星标)" value="star-on" />
            <el-option label="medal(奖章)" value="medal" />
            <el-option label="trophy(奖杯)" value="trophy" />
            <el-option label="user(用户)" value="user" />
            <el-option label="collection(合集)" value="collection" />
            <el-option label="guide(领航)" value="guide" />
            <el-option label="handshake(搭档)" value="handshake" />
            <el-option label="aim(目标)" value="aim" />
          </el-select>
        </el-form-item>
        <el-form-item label="解锁条件">
          <el-select v-model="badgeDialog.form.conditionType" style="width:100%">
            <el-option v-for="ct in badgeConditionTypes" :key="ct.value" :label="ct.label" :value="ct.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="['award_level','award_count'].includes(badgeDialog.form.conditionType)" label="竞赛级别">
          <el-select v-model="badgeDialog.form.competitionLevel" clearable placeholder="不选则不限级别" style="width:100%">
            <el-option label="国家级" value="国家级" />
            <el-option label="省级" value="省级" />
            <el-option label="市级" value="市级" />
            <el-option label="校级" value="校级" />
          </el-select>
        </el-form-item>
        <el-form-item label="解锁阈值"><el-input-number v-model="badgeDialog.form.unlockThreshold" :min="1" :max="999" /></el-form-item>
        <el-form-item label="展示顺序"><el-input-number v-model="badgeDialog.form.sortOrder" :min="0" :max="999" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="badgeDialog.form.statusComputed" active-value="enabled" inactive-value="disabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="badgeDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveBadge">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import NavBar from '@/components/NavBar.vue'

const activeTab = ref('category')
const categories = ref([])
const skills = ref([])
const skillCategories = ref([])
const relations = ref([])
const rules = ref([])
const badges = ref([])
const tags = ref([])
const competitions = ref([])
const applications = ref([])
const relationFilter = ref('')
const catalogKeyword = ref('')
const markStatus = ref('')
const markKeyword = ref('')
const competitionOptions = ref([])
const competitionLoading = ref(false)

const summary = computed(() => [
  { key: 'cat', label: '竞赛类别', value: categories.value.length },
  { key: 'skill', label: '技能标签', value: skills.value.length },
  { key: 'rule', label: '积分参数', value: rules.value.length },
  { key: 'tag', label: '荣誉标签', value: tags.value.length }
])

const levelText = (l) => ({ basic: '基础', intermediate: '中级', advanced: '高级' }[l] || l)
const typeText = (t) => ({ award_level: '获奖等级', competition_direction: '竞赛方向', role: '团队角色', ability: '能力', growth: '成长' }[t] || t)
const gradeTagType = (g) => ({ A: 'danger', B: 'warning', C: 'primary', D: 'info' }[g] || 'info')
const ruleGroups = computed(() => [...new Set(rules.value.map(r => r.paramGroup).filter(Boolean))])

const loadCategories = async () => { try { const res = await request.get('/api/honor/category/all'); if (res.code === '200') categories.value = res.data || [] } catch(e){} }
const loadSkills = async () => { try { const res = await request.get('/api/honor/skill/all'); if (res.code === '200') skills.value = res.data || [] } catch(e){} }
const loadSkillCategories = async () => { try { const res = await request.get('/api/honor/skill-category/list'); if (res.code === '200') skillCategories.value = res.data || [] } catch(e){} }
const loadRelations = async () => {
  try {
    const params = {}
    if (relationFilter.value) params.mapType = relationFilter.value
    const res = await request.get('/api/honor/competition-skill/list', { params })
    if (res.code === '200') relations.value = res.data || []
  } catch(e){}
}
const loadRules = async () => { try { const res = await request.get('/api/honor/score/rules'); if (res.code === '200') rules.value = res.data || [] } catch(e){} }
const loadBadges = async () => { try { const res = await request.get('/api/honor/badge/list'); if (res.code === '200') badges.value = res.data || [] } catch(e){} }
const loadTags = async () => { try { const res = await request.get('/api/honor/tag/list'); if (res.code === '200') tags.value = (res.data || []).map(t => ({...t, statusComputed: t.status})) } catch(e){} }
const loadCompetitions = async () => {
  try {
    const params = {}
    if (catalogKeyword.value.trim()) params.keyword = catalogKeyword.value.trim()
    const res = await request.get('/api/honor-manage/competition/options', { params })
    if (res.code === '200') competitions.value = res.data || []
  } catch(e){}
}
const searchCompetitions = async (kw) => {
  competitionLoading.value = true
  try {
    const params = kw ? { keyword: kw } : {}
    const res = await request.get('/api/honor-manage/competition/options', { params })
    if (res.code === '200') competitionOptions.value = res.data || []
  } catch(e){} finally { competitionLoading.value = false }
}
const loadApplications = async () => {
  try {
    const params = {}
    if (markStatus.value) params.status = markStatus.value
    if (markKeyword.value.trim()) params.keyword = markKeyword.value.trim()
    const res = await request.get('/api/honor-manage/application/list', { params })
    if (res.code === '200') applications.value = res.data || []
  } catch(e){}
}

const refreshAll = () => {
  loadCategories(); loadSkills(); loadSkillCategories(); loadRelations(); loadRules(); loadBadges(); loadTags()
  loadCompetitions(); loadApplications(); searchCompetitions('')
}

// 类别对话框
const categoryDialog = reactive({ visible: false, form: { categoryId: null, categoryName: '', description: '', icon: '', sortOrder: 0 } })
const openCategoryDialog = (row) => {
  if (row) categoryDialog.form = { ...row }
  else categoryDialog.form = { categoryId: null, categoryName: '', description: '', icon: '', sortOrder: 0 }
  categoryDialog.visible = true
}
const saveCategory = async () => {
  const url = categoryDialog.form.categoryId ? '/api/honor-manage/category' : '/api/honor-manage/category'
  const method = categoryDialog.form.categoryId ? 'put' : 'post'
  const res = await request[method](url, categoryDialog.form)
  if (res.code === '200') { ElMessage.success('已保存'); categoryDialog.visible = false; loadCategories() }
}
const disableCategory = async (row) => {
  await ElMessageBox.confirm(`确定停用【${row.categoryName}】吗？`, '提示', { type: 'warning', customClass: 'premium-message-box' }).catch(() => {})
  const res = await request.delete(`/api/honor-manage/category/${row.categoryId}`)
  if (res.code === '200') { ElMessage.success('已停用'); loadCategories() }
}

// 技能对话框
const skillDialog = reactive({ visible: false, form: { skillId: null, name: '', skillCategoryId: null, level: 'basic', description: '', allowSelfEval: true, awardOnly: false, sortOrder: 0 } })
const openSkillDialog = (row) => {
  if (row) skillDialog.form = { ...row, allowSelfEval: !!row.allowSelfEval, awardOnly: !!row.awardOnly }
  else skillDialog.form = { skillId: null, name: '', skillCategoryId: null, level: 'basic', description: '', allowSelfEval: true, awardOnly: false, sortOrder: 0 }
  skillDialog.visible = true
}
const saveSkill = async () => {
  const method = skillDialog.form.skillId ? 'put' : 'post'
  const res = await request[method]('/api/honor-manage/skill', skillDialog.form)
  if (res.code === '200') { ElMessage.success('已保存'); skillDialog.visible = false; loadSkills() }
}
const disableSkill = async (row) => {
  await ElMessageBox.confirm(`确定停用技能【${row.name}】吗？`, '提示', { type: 'warning', customClass: 'premium-message-box' }).catch(() => {})
  const res = await request.delete(`/api/honor-manage/skill/${row.skillId}`)
  if (res.code === '200') { ElMessage.success('已停用'); loadSkills() }
}

// 关联对话框（两层映射：类别默认 / 竞赛级覆盖）
const relationDialog = reactive({ visible: false, mapType: 'default', form: { id: null, categoryId: null, competitionId: null, skillId: null, weight: 0.3, contribution: 10, teamRole: '', teamRoleWeight: 50 } })
const openRelationDialog = (row) => {
  if (row) {
    relationDialog.mapType = row.competitionId ? 'override' : 'default'
    relationDialog.form = { ...row, weight: Number(row.weight) || 0.3 }
    if (row.competitionId && !competitionOptions.value.some(c => c.competitionId === row.competitionId)) {
      searchCompetitions(row.competitionName || '')
    }
  } else {
    relationDialog.mapType = relationFilter.value === 'override' ? 'override' : 'default'
    relationDialog.form = { id: null, categoryId: null, competitionId: null, skillId: null, weight: 0.3, contribution: 10, teamRole: '', teamRoleWeight: 50 }
  }
  relationDialog.visible = true
}
const saveRelation = async () => {
  const form = { ...relationDialog.form }
  if (relationDialog.mapType === 'default') form.competitionId = null
  else if (!form.competitionId) { ElMessage.warning('请选择要覆盖的竞赛'); return }
  if (!form.categoryId) { ElMessage.warning('请选择竞赛类别'); return }
  if (!form.skillId) { ElMessage.warning('请选择技能'); return }
  // 权重和提示：同一映射目标下现有权重合计 + 本次权重
  const sameTarget = relations.value.filter(r =>
    r.categoryId === form.categoryId &&
    (relationDialog.mapType === 'default' ? !r.competitionId : r.competitionId === form.competitionId) &&
    r.id !== form.id)
  const sum = sameTarget.reduce((s, r) => s + (Number(r.weight) || 0), 0) + (Number(form.weight) || 0)
  if (Math.abs(sum - 1) > 0.001) {
    const ok = await ElMessageBox.confirm(
      `该映射目标下权重合计为 ${(sum * 100).toFixed(0)}%（应为 100%），引擎将按比例归一化折算经验值，确定保存吗？`,
      '权重合计提示', { type: 'warning', confirmButtonText: '仍然保存', cancelButtonText: '返回修改', customClass: 'premium-message-box' }
    ).then(() => true).catch(() => false)
    if (!ok) return
  }
  const res = await request.post('/api/honor-manage/competition-skill', form)
  if (res.code === '200') { ElMessage.success('已保存'); relationDialog.visible = false; loadRelations() }
}
const deleteRelation = async (row) => {
  await ElMessageBox.confirm('确定删除该映射吗？', '提示', { type: 'warning', customClass: 'premium-message-box' }).catch(() => {})
  const res = await request.delete(`/api/honor-manage/competition-skill/${row.id}`)
  if (res.code === '200') { ElMessage.success('已删除'); loadRelations() }
}

// 积分规则（v2.0 参数行）
const ruleDialog = reactive({ visible: false, form: { ruleId: null, paramGroup: '竞赛等次基础分', paramKey: '', paramValue: '', paramRemark: '', enabled: true } })
const openRuleDialog = (row) => {
  if (row) ruleDialog.form = { ...row, enabled: Number(row.enabled) === 1 }
  else ruleDialog.form = { ruleId: null, paramGroup: '竞赛等次基础分', paramKey: '', paramValue: '', paramRemark: '', enabled: true }
  ruleDialog.visible = true
}
const saveRule = async () => {
  const res = await request.post('/api/honor-manage/score-rule', ruleDialog.form)
  if (res.code === '200') { ElMessage.success('已保存'); ruleDialog.visible = false; loadRules() }
}
const toggleRule = async (row, v) => {
  const res = await request.post(`/api/honor-manage/score-rule/${row.ruleId}/toggle`, null, { params: { enable: v } })
  if (res.code === '200') ElMessage.success(v ? '已启用' : '已停用')
}

// 竞赛目录维护
const catalogDialog = reactive({ visible: false, form: { competitionId: null, competitionName: '', categoryId: null, grade: null, overrideLevel: null, overrideGrade: null, baseScore: null } })
const openCatalogDialog = (row) => {
  catalogDialog.form = {
    competitionId: row.competitionId, competitionName: row.competitionName,
    categoryId: row.categoryId || null, grade: row.grade || null,
    overrideLevel: row.overrideLevel || null, overrideGrade: row.overrideGrade || null,
    baseScore: row.baseScore != null ? Number(row.baseScore) : null
  }
  catalogDialog.visible = true
}
const saveCatalog = async () => {
  const form = { ...catalogDialog.form }
  if (!form.categoryId) { ElMessage.warning('请选择所属类别'); return }
  const res = await request.post('/api/honor-manage/competition', form)
  if (res.code === '200') { ElMessage.success('已保存'); catalogDialog.visible = false; loadCompetitions() }
}

// 获奖认定（角色快照 + 历史性突破）
const markDialog = reactive({ visible: false, form: { applicationId: null, competitionName: '', memberRole: null, breakthrough: false } })
const openMarkDialog = (row) => {
  markDialog.form = {
    applicationId: row.applicationId, competitionName: row.competitionName,
    memberRole: row.memberRole || null, breakthrough: Number(row.breakthrough) === 1
  }
  markDialog.visible = true
}
const saveMark = async () => {
  const res = await request.post('/api/honor-manage/application/mark', markDialog.form)
  if (res.code === '200') { ElMessage.success('已认定'); markDialog.visible = false; loadApplications() }
}

const recalcScores = async () => {
  await ElMessageBox.confirm('将清除所有现有积分明细并重新计算所有学生积分，是否继续？', '重要提示', {
    type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消', customClass: 'premium-message-box'
  }).catch(() => { return })
  const res = await request.post('/api/honor-manage/score/recalc')
  if (res.code === '200') {
    ElMessage.success(`已重算：${res.data?.recalculatedStudents || 0} 名学生 / ${res.data?.totalApplications || 0} 条获奖`)
  }
}

// 标签
const tagDialog = reactive({ visible: false, form: { tagId: null, tagName: '', tagType: 'award_level', description: '', conditionRule: '{}', sortOrder: 0, statusComputed: 'enabled' } })
const openTagDialog = (row) => {
  if (row) tagDialog.form = { ...row, statusComputed: row.status }
  else tagDialog.form = { tagId: null, tagName: '', tagType: 'award_level', description: '', conditionRule: '{}', sortOrder: 0, statusComputed: 'enabled' }
  tagDialog.visible = true
}
const saveTag = async () => {
  const res = await request.post('/api/honor-manage/tag', { ...tagDialog.form, status: tagDialog.form.statusComputed })
  if (res.code === '200') { ElMessage.success('已保存'); tagDialog.visible = false; loadTags() }
}
const toggleTag = async (row, v) => {
  const res = await request.post(`/api/honor-manage/tag/${row.tagId}/toggle`, null, { params: { status: v } })
  if (res.code === '200') ElMessage.success('已更新')
}

// 勋章（固定条件类型 + 参数配置）
const badgeConditionTypes = [
  { value: 'first_award', label: '首次获奖（累计获奖次数）' },
  { value: 'award_level', label: '获奖等级（指定级别及以上获奖数）' },
  { value: 'award_count', label: '获奖数量（指定级别及以上获奖数）' },
  { value: 'team_award', label: '团队获奖（团队奖项次数）' },
  { value: 'team_leader_award', label: '团队领袖（以队长身份获奖次数）' },
  { value: 'category_count', label: '多面手（不同竞赛类别数）' },
  { value: 'team_partner_count', label: '黄金搭档（同一团队获奖次数）' },
  { value: 'continuous_semesters', label: '持续进步（不同获奖月份数）' }
]
const conditionTypeLabels = Object.fromEntries(badgeConditionTypes.map(ct => [ct.value, ct.label]))
const recheckStudentId = ref(null)

/** 从 unlock_condition JSON 中解析条件类型，兜底用预置勋章编码映射 */
const conditionTypeOf = (row) => {
  try {
    const m = /"type"\s*:\s*"([a-z_]+)"/.exec(String(row.unlockCondition || ''))
    if (m && conditionTypeLabels[m[1]]) return m[1]
  } catch (e) { /* ignore */ }
  const legacy = {
    first_award: 'first_award', national_honor: 'award_level', provincial_expert: 'award_count',
    team_star: 'team_leader_award', versatile: 'category_count', pioneer: 'award_count',
    golden_partner: 'team_partner_count', continuous_growth: 'continuous_semesters'
  }
  return legacy[row.badgeCode] || ''
}
const conditionTextOf = (row) => {
  const type = conditionTypeOf(row)
  const base = conditionTypeLabels[type] || row.unlockCondition || '固定条件'
  const m = /"competition_level"\s*:\s*"([^"]+)"/.exec(String(row.unlockCondition || ''))
  return m ? `${base} · ${m[1]}及以上` : base
}

const badgeDialog = reactive({ visible: false, form: { badgeId: null, badgeCode: '', badgeName: '', description: '', icon: 'medal', conditionType: 'first_award', competitionLevel: '', unlockThreshold: 1, sortOrder: 0, statusComputed: 'enabled' } })
const openBadgeDialog = (row) => {
  if (row) {
    badgeDialog.form = {
      badgeId: row.badgeId,
      badgeCode: row.badgeCode,
      badgeName: row.badgeName,
      description: row.description || '',
      icon: row.icon || 'medal',
      conditionType: conditionTypeOf(row) || 'first_award',
      competitionLevel: (/\"competition_level\"\s*:\s*\"([^\"]+)\"/.exec(String(row.unlockCondition || '')) || [])[1] || '',
      unlockThreshold: Number(row.unlockThreshold) || 1,
      sortOrder: Number(row.sortOrder) || 0,
      statusComputed: row.status || 'enabled'
    }
  } else {
    badgeDialog.form = { badgeId: null, badgeCode: '', badgeName: '', description: '', icon: 'medal', conditionType: 'first_award', competitionLevel: '', unlockThreshold: 1, sortOrder: 0, statusComputed: 'enabled' }
  }
  badgeDialog.visible = true
}
const saveBadge = async () => {
  const form = badgeDialog.form
  if (!form.badgeCode || !form.badgeName) { ElMessage.warning('勋章编码与名称不能为空'); return }
  const res = await request.post('/api/honor-manage/badge', { ...form, status: form.statusComputed })
  if (res.code === '200') { ElMessage.success('已保存'); badgeDialog.visible = false; loadBadges() }
}
const toggleBadge = async (row, v) => {
  const res = await request.post(`/api/honor-manage/badge/${row.badgeId}/toggle`, null, { params: { status: v } })
  if (res.code === '200') { ElMessage.success(v === 'enabled' ? '已启用' : '已停用'); loadBadges() }
}
const recheckBadges = async () => {
  if (!recheckStudentId.value) { ElMessage.warning('请先填写学生ID'); return }
  const res = await request.post(`/api/honor-manage/badge/recheck/${recheckStudentId.value}`)
  if (res.code === '200') ElMessage.success(`已重算学生 ${recheckStudentId.value} 的勋章`)
}

onMounted(refreshAll)
</script>

<style scoped>
.honor-manage-page { min-height: 100vh; padding-bottom: 40px; background: #f8fafc; }
.content-container { max-width: 1400px; margin: 0 auto; padding: 20px; }
.manage-card { border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h2 { margin: 0; font-size: 20px; color: #1e293b; }
.header-actions { display: flex; gap: 8px; }

.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 20px; }
.stat-card { padding: 14px 18px; background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.05)); border-radius: 12px; display: flex; flex-direction: column; border: 1px solid rgba(99,102,241,0.18); }
.stat-label { color: #64748b; font-size: 13px; }
.stat-value { color: #1e293b; font-size: 24px; font-weight: 700; margin-top: 4px; }

.manage-tabs { background: transparent; }
.action-bar { margin-bottom: 12px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.badge-action-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.map-tip { color: #94a3b8; font-size: 12px; line-height: 1.4; }
</style>