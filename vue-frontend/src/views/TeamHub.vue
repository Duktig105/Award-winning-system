<template>
  <div class="team-page">
    <NavBar />
    <main class="team-shell">
      <header class="page-head">
        <div class="page-head-copy">
          <h1>竞赛组队</h1>
          <p>寻找互补的队友，组建可直接用于获奖申报的正式队伍</p>
        </div>
        <el-button class="head-cta" type="primary" size="large" :icon="Plus" @click="openCreate">创建队伍</el-button>
      </header>

      <el-tabs v-model="activeTab" class="team-tabs" @tab-change="tabChanged">
        <el-tab-pane label="组队广场" name="market">
          <div class="filters">
            <div class="filter-grid">
              <el-input v-model="filters.competition" placeholder="搜索竞赛或队伍" clearable :prefix-icon="Search" @keyup.enter="loadMarket" />
              <el-input v-model="filters.skill" placeholder="技能，如 Python" clearable @keyup.enter="loadMarket" />
              <el-input v-model="filters.major" placeholder="专业" clearable @keyup.enter="loadMarket" />
              <el-select v-model="filters.grade" placeholder="年级" clearable><el-option v-for="g in grades" :key="g" :label="g" :value="g" /></el-select>
              <el-select v-model="filters.status" placeholder="招募状态" clearable><el-option label="招募中" value="recruiting" /><el-option label="已关闭" value="closed" /><el-option label="已锁定" value="locked" /></el-select>
            </div>
            <div class="filter-actions"><el-button type="primary" :icon="Search" @click="loadMarket">筛选</el-button><el-button @click="clearFilters">清除</el-button></div>
          </div>
          <p class="result-count">共找到 {{ market.total }} 支队伍</p>
          <div v-loading="loading" class="team-grid">
            <article v-for="team in market.list" :key="team.teamId" class="team-card" :style="accentVars(team)">
              <div class="card-top">
                <div class="card-tags">
                  <el-tag class="cat-tag" effect="plain" round>{{ team.competitionName }}</el-tag>
                </div>
                <el-tag 
                  :type="team.status === 'recruiting' ? (isUrgent(team) ? 'danger' : 'success') : 'info'" 
                  effect="light" 
                  round
                  :class="{ 'urgent-tag': isUrgent(team), 'recruiting-tag': team.status === 'recruiting' && !isUrgent(team) }"
                >
                  <span v-if="team.status === 'recruiting' && !isUrgent(team)" class="pulse-dot"></span>
                  {{ team.status === 'recruiting' && isUrgent(team) ? '🔥 紧急扩充' : statusText(team.status) }}
                </el-tag>
              </div>
              
              <!-- 增加标签引导，解决空旷感 -->
              <h2 class="team-name"><span class="info-label">队伍名称：</span>{{ team.name }}</h2>
              <p class="description"><span class="info-label">队伍介绍：</span>{{ team.description || '负责人暂未填写队伍介绍' }}</p>
              
              <dl class="team-meta">
                <div><dt>负责人</dt><dd>{{ team.leaderName }}</dd></div>
                <div><dt>专业年级</dt><dd>{{ team.major || '-' }} · {{ team.grade || '-' }}</dd></div>
              </dl>

              <div class="slot-row" :title="`队伍 ${team.memberCount}/${team.targetSize} 人`">
                <span v-for="i in filledSlots(team)" :key="'m' + i" class="slot filled" :class="{ leader: i === 1 }" :title="i === 1 ? `${team.leaderName}（队长）` : '队员'">
                  <svg class="ava" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8.2" r="3.9" /><path d="M4.5 20.5c0-3.9 3.4-6.3 7.5-6.3s7.5 2.4 7.5 6.3z" /></svg>
                  <span v-if="i === 1" class="leader-badge">队长</span>
                </span>
                <span v-for="(h, idx) in heroSlots(team)" :key="'h' + idx" class="slot vacant" :title="`招募：队友`">+</span>
                <span class="slot-note">{{ vacancy(team) ? `虚位以待 x${vacancy(team)}` : '阵容已满' }}</span>
              </div>
              <div class="tag-row" v-if="skillTags(team).length"><span v-for="s in skillTags(team)" :key="s" class="tag skill">#{{ s }}</span></div>
              <div class="tag-row" v-if="vibeTags(team).length"><span v-for="t in vibeTags(team)" :key="t" class="tag vibe">{{ t }}</span></div>
              <!-- 状态B：队长意向职位部分已落实、仍有空缺，点击直达与队长的协商留言（复用申请附言通道） -->
              <div v-if="team.recruitState === 'partial' && team.canApply" class="intent-hint">
                <span class="intent-icon">💬</span>
                <span class="intent-text">队长意向职位部分已满，仍有空缺职位，<button type="button" class="intent-link" @click="contactLeader(team)">可与队长联系</button></span>
              </div>
              <div class="card-footer">
                <span class="deadline">截止：{{ formatTime(team.teamCloseTime) }}</span>
                <div class="card-actions">
                  <el-button @click="showDetail(team.teamId)">查看详情</el-button>
                  <el-button class="join-btn" type="primary" :disabled="!team.canApply" :title="team.unavailableReason || ''" @click="showDetail(team.teamId, true)">
                    <span class="btn-text">{{ team.canApply ? '申请加入' : team.unavailableReason }}</span>
                    <span v-if="team.canApply" class="fly-icon">🚀</span>
                  </el-button>
                </div>
              </div>
            </article>
            <el-empty v-if="!market.list.length && !loading" description="暂无符合条件的招募" />
          </div>
          <el-pagination v-if="market.total" layout="prev, pager, next" :total="market.total" :page-size="market.pageSize" v-model:current-page="market.page" @current-change="loadMarket" />
        </el-tab-pane>

        <el-tab-pane label="适合我的队伍" name="recommended">
          <p class="section-note">按公开岗位条件、你填写的竞赛简历和系统认证经历排序；技能标签为学生自填。</p>
          <div v-loading="loading" class="team-grid">
            <article v-for="item in recommendations" :key="`${item.teamId}-${item.positionId}`" class="team-card" :style="accentVars(item)">
              <div class="card-top">
                <el-tag class="cat-tag" effect="plain" round>{{ item.competitionName }}</el-tag>
                <strong class="match-score">{{ item.score }} 分</strong>
              </div>
              <h2 class="team-name"><span class="info-label">队伍名称：</span>{{ item.name }}</h2>
              <p class="description"><span class="info-label">推荐岗位：</span>{{ item.positionTitle }}</p>
              <div class="reason-list"><span v-for="reason in item.reasons" :key="reason">{{ reason }}</span></div>
              <div class="card-footer">
                <span class="deadline">{{ item.memberCount }}/{{ item.targetSize }} 人 · 截止 {{ formatTime(item.teamCloseTime) }}</span>
                <div class="card-actions">
                  <el-button @click="showDetail(item.teamId)">查看详情</el-button>
                  <el-button class="join-btn" type="primary" @click="showDetail(item.teamId,true,item.positionId)">
                    <span class="btn-text">申请岗位</span>
                    <span class="fly-icon">🚀</span>
                  </el-button>
                </div>
              </div>
            </article>
            <el-empty v-if="!recommendations.length && !loading" description="暂无适合的开放岗位，完善竞赛简历后再看看" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的队伍" name="mine">
          <div class="section-tools"><el-segmented v-model="mineScope" :options="[{label:'我负责的',value:'leader'},{label:'我参加的',value:'member'},{label:'历史档案',value:'history'}]" /><el-button :icon="Refresh" @click="loadMine">刷新</el-button></div>
          <el-table :data="visibleMine" v-loading="loading" stripe class="desktop-table">
            <el-table-column prop="competitionName" label="竞赛" min-width="220" />
            <el-table-column prop="name" label="队伍" min-width="160" />
            <el-table-column label="身份" width="100"><template #default="{row}"><el-tag :type="row.isLeader ? 'warning' : ''">{{ row.isLeader ? '负责人' : '成员' }}</el-tag></template></el-table-column>
            <el-table-column label="人数" width="90"><template #default="{row}">{{ row.memberCount }}/{{ row.targetSize }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{row}">{{ statusText(row.status) }}</template></el-table-column>
            <el-table-column label="操作" width="120"><template #default="{row}"><el-button link type="primary" @click="showDetail(row.teamId)">管理/查看</el-button></template></el-table-column>
          </el-table>
          <div class="mobile-list"><article v-for="row in visibleMine" :key="row.teamId" class="mobile-item"><strong>{{ row.name }}</strong><span>{{ row.competitionName }}</span><span>{{ row.isLeader ? '负责人' : '成员' }} · {{ statusText(row.status) }} · {{ row.memberCount }}/{{ row.targetSize }} 人</span><el-button type="primary" plain @click="showDetail(row.teamId)">查看队伍</el-button></article></div>
          <el-empty v-if="!visibleMine.length && !loading" description="这里还没有队伍" />
        </el-tab-pane>

        <el-tab-pane name="requests">
          <template #label><span class="tab-label">申请与邀请 <el-badge v-if="pendingCount" :value="pendingCount" /></span></template>
          <div class="request-toolbar">
            <el-segmented v-model="requestScope" :options="[{label:'待我处理',value:'mine'},{label:'队伍的申请',value:'managed'},{label:'收到的邀请',value:'invited'}]" @change="loadRequests" />
            <el-segmented v-model="requestView" :options="[{label:'待处理',value:'pending'},{label:'我发起的',value:'outgoing'},{label:'历史',value:'history'}]" class="request-view" />
            <span class="request-tip">点击任意一行查看申请人简历与完整留言</span>
          </div>
          <el-table :data="visibleRequests" v-loading="loading" stripe class="request-table desktop-table" :row-class-name="requestRowClass" @row-click="openRequestDetail">
            <el-table-column prop="competitionName" label="竞赛" min-width="200" /><el-table-column prop="teamName" label="队伍" min-width="140" />
            <el-table-column prop="studentName" label="学生" width="110" />
            <el-table-column label="岗位" width="150"><template #default="{row}"><span>{{ row.positionTitle || '队员' }}</span><el-tag v-if="row.positionVacancies" size="small" :type="positionFull(row) ? 'info' : 'success'" effect="plain" class="position-seat">{{ row.positionOccupied }}/{{ row.positionVacancies }}{{ positionFull(row) ? ' 已满' : '' }}</el-tag></template></el-table-column>
            <el-table-column label="留言" min-width="220" show-overflow-tooltip><template #default="{row}"><span :class="['req-message',{empty:!row.message}]">{{ row.message || '未填写留言' }}</span></template></el-table-column>
            <el-table-column label="类型" width="90"><template #default="{row}">{{ row.requestType === 'application' ? '申请' : '邀请' }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="requestTag(row.status)">{{ requestText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="230" align="center" header-align="center"><template #default="{row}"><el-button link @click.stop="openRequestDetail(row)">详情</el-button><template v-if="row.status === 'pending' && canDecide(row)"><el-button link type="success" :disabled="positionFull(row)" :title="positionFull(row) ? '该岗位名额已满或已关闭，不再接收新的加入请求' : ''" @click.stop="decide(row,'accept')">接受</el-button><el-button link type="danger" @click.stop="decide(row,'reject')">拒绝</el-button></template><el-button v-if="row.status==='pending' && canCancel(row)" link type="warning" @click.stop="decide(row,'cancel')">撤回</el-button></template></el-table-column>
          </el-table>
          <div class="mobile-list"><article v-for="row in visibleRequests" :key="row.requestId" class="mobile-item" :class="{ 'row-highlight': row.requestId === highlightRequestId }"><strong>{{ row.teamName }}</strong><span>{{ row.competitionName }} · {{ row.positionTitle || '队员' }}<template v-if="row.positionVacancies">（{{ row.positionOccupied }}/{{ row.positionVacancies }}{{ positionFull(row) ? ' 已满' : '' }}）</template></span><span>{{ row.requestType==='application'?'申请':'邀请' }} · {{ requestText(row.status) }} · {{ row.studentName }}</span><span v-if="row.message" class="req-message">留言：{{ row.message }}</span><span v-else class="req-message empty">未填写留言</span><div><el-button plain @click="openRequestDetail(row)">详情</el-button><el-button v-if="row.status==='pending' && canDecide(row)" type="success" plain @click="decide(row,'accept')">接受</el-button><el-button v-if="row.status==='pending' && canDecide(row)" type="danger" plain @click="decide(row,'reject')">拒绝</el-button><el-button v-if="row.status==='pending' && canCancel(row)" @click="decide(row,'cancel')">撤回</el-button></div></article></div>
        </el-tab-pane>

        <el-tab-pane label="竞赛简历" name="profile">
          <div class="profile-panel">
          <el-form :model="profile" label-position="top" class="profile-form" v-loading="loading">
            <div class="profile-summary"><div><strong>{{ profile.studentName }}</strong><span>{{ profile.studentNumber }}</span></div><div>{{ profile.grade }} · {{ profile.major }} · {{ profile.college }}</div></div>
            <el-form-item label="技能标签（学生自填）"><el-select v-model="profile.skills" multiple filterable allow-create default-first-option placeholder="输入技能后回车" /></el-form-item>
            <div class="two-col"><el-form-item label="擅长方向"><el-input v-model="profile.specialties" maxlength="500" /></el-form-item><el-form-item label="期望岗位"><el-input v-model="profile.preferredRoles" maxlength="500" /></el-form-item></div>
            <el-form-item label="个人简介"><el-input v-model="profile.bio" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
            <el-form-item label="竞赛经历"><el-input v-model="profile.competitionExperience" type="textarea" :rows="4" maxlength="1500" show-word-limit /></el-form-item>
            <div class="two-col"><el-form-item label="作品链接"><el-input v-model="profile.portfolioUrl" placeholder="https://" /></el-form-item><el-form-item label="每周可投入时间"><el-input-number v-model="profile.weeklyHours" :min="0" :max="168" /><span class="unit">小时</span></el-form-item></div>
            <div class="privacy-options"><strong>对其他学生公开</strong><el-switch v-model="profile.showBio" :active-value="1" :inactive-value="0" active-text="个人简介" /><el-switch v-model="profile.showPortfolio" :active-value="1" :inactive-value="0" active-text="作品链接" /><el-switch v-model="profile.showWeeklyHours" :active-value="1" :inactive-value="0" active-text="每周时间" /><p>默认不公开上述内容；完整学号仅队伍内部可见。</p></div>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存竞赛简历</el-button>
            <el-divider content-position="left">系统认证获奖经历</el-divider>
            <el-table :data="profile.approvedAwards || []" size="small"><el-table-column prop="competitionName" label="竞赛" /><el-table-column prop="projectName" label="项目" /><el-table-column prop="awardLevel" label="奖项" width="120" /></el-table>
          </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-dialog class="team-dialog" v-model="createVisible" title="创建竞赛队伍" width="min(680px, 94vw)">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="竞赛"><el-select v-model="createForm.competitionId" filterable placeholder="选择竞赛" style="width:100%"><el-option v-for="c in competitions" :key="c.competitionId" :label="c.competitionName" :value="c.competitionId" /></el-select></el-form-item>
        <div class="two-col">
          <el-form-item label="队伍名称"><el-input v-model="createForm.name" maxlength="50" /></el-form-item>
          <el-form-item label="目标人数"><el-input-number v-model="createForm.targetSize" :min="1" :max="50" /></el-form-item>
        </div>
        <el-form-item label="队伍介绍"><el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
        <div class="two-col">
          <el-form-item label="团队氛围标签">
            <el-select v-model="createForm.vibeTags" multiple filterable allow-create default-first-option placeholder="选择或输入团队标签" style="width:100%">
              <el-option v-for="v in PRESET_VIBES" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item>
          <el-form-item label="我的岗位"><el-input v-model="createForm.leaderRole" placeholder="如：项目负责人" /></el-form-item>
        </div>
        <div class="position-editor"><div class="position-head"><strong>招募岗位（硬技能）</strong><el-button link type="primary" :icon="Plus" @click="createForm.positions.push(blankPosition())">添加岗位</el-button></div><div v-for="(p,i) in createForm.positions" :key="i" class="position-row"><el-input v-model="p.title" placeholder="岗位名称" /><el-input-number v-model="p.vacancies" :min="1" :max="20" /><el-input v-model="p.requiredSkills" placeholder="所需技能，逗号分隔" /><el-button :icon="Delete" circle text type="danger" @click="createForm.positions.splice(i,1)" /></div></div>
      </el-form><template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="createTeam">创建并发布</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" class="team-drawer" size="min(720px, 96vw)" :title="detail.name || '队伍详情'">
      <template v-if="detail.teamId">
        <div class="detail-head"><el-tag effect="plain" round>{{ detail.competitionName }}</el-tag><el-tag :type="detail.status==='locked'?'warning':'success'" effect="light" round>{{ statusText(detail.status) }}</el-tag></div>
        <p class="drawer-desc">{{ detail.description || '暂无介绍' }}</p>
        <el-descriptions :column="2" border><el-descriptions-item label="人数">{{ detail.members?.length }}/{{ detail.targetSize }}</el-descriptions-item><el-descriptions-item label="截止">{{ formatTime(detail.teamCloseTime) }}</el-descriptions-item></el-descriptions>
        <section class="drawer-section">
          <h3>团队成员</h3>
          <el-table :data="detail.members" class="desktop-table"><el-table-column prop="studentName" label="姓名" /><el-table-column v-if="detail.isMember" prop="studentNumber" label="学号" /><el-table-column prop="roleName" label="岗位" /><el-table-column v-if="detail.isLeader && editable" label="管理" width="210"><template #default="{row}"><template v-if="!row.isLeader"><el-button link type="primary" @click="transferLeader(row)">转让</el-button><el-button link @click="editMemberRole(row)">调整岗位</el-button><el-button link type="danger" @click="removeMember(row)">移除</el-button></template></template></el-table-column></el-table>
        <div class="mobile-list"><article v-for="row in detail.members" :key="row.studentId" class="mobile-item"><strong>{{ row.studentName }} <el-tag v-if="row.isLeader" size="small">负责人</el-tag></strong><span v-if="detail.isMember">学号：{{ row.studentNumber }}</span><span>岗位：{{ row.roleName || '队员' }}</span><div v-if="detail.isLeader && editable && !row.isLeader"><el-button link type="primary" @click="transferLeader(row)">转让</el-button><el-button link @click="editMemberRole(row)">调整岗位</el-button><el-button link type="danger" @click="removeMember(row)">移除</el-button></div></article></div>
        </section>
        <section class="drawer-section">
          <div class="section-tools"><h3>招募岗位</h3><el-button v-if="detail.isLeader && editable" link type="primary" @click="openPosition()">新增岗位</el-button></div>
        <div v-for="p in detail.positions" :key="p.positionId" class="position-item"><div><strong>{{ p.title }}</strong><span>{{ p.occupied }}/{{ p.vacancies }} 人 · {{ p.requiredSkills || '不限技能' }} · {{ !editable?'历史岗位':p.status==='open'?(p.remainingSeats?'开放中':'已满员 · 不再接收申请与邀请'):'已关闭 · 不再接收申请与邀请' }}</span><p>{{ p.requirements || '暂无补充要求' }}</p></div><div class="position-actions"><el-button v-if="detail.isLeader && editable" link @click="openPosition(p)">编辑</el-button><el-button v-if="detail.isLeader && editable && p.status==='open'" link type="warning" @click="closePosition(p)">关闭</el-button><el-button v-if="!detail.isMember && detail.canApply && p.status==='open' && p.remainingSeats" type="primary" plain @click="openApply(p)">申请</el-button></div></div>
        <p v-if="detail.isLeader && editable && !availablePositions.length" class="section-note">所有招募岗位均已招满：不再接收新的申请，也无法再发出邀请；如需继续扩充，请新增岗位或调整名额后重开招募。</p>
        <p v-if="!detail.canApply && !detail.isMember" class="section-note">{{ detail.unavailableReason }}</p>
        </section>
        <div v-if="detail.isLeader && editable" class="leader-actions"><el-button @click="openEditTeam">编辑队伍</el-button><el-button :disabled="!detail.canApply || !availablePositions.length" :title="!availablePositions.length ? '所有招募岗位均已招满，无法再发出邀请' : (detail.unavailableReason || '')" @click="openInvite">邀请学生</el-button><el-button @click="loadCandidates">推荐队员</el-button><el-button @click="toggleRecruiting">{{ detail.recruiting ? '关闭招募' : '开启招募' }}</el-button><el-button type="warning" @click="lockTeam">锁定队伍</el-button><el-button type="danger" plain @click="dissolveTeam">解散</el-button></div>
        <div v-if="detail.isMember && editable" class="leader-actions"><el-button type="danger" plain @click="leaveTeam">退出队伍</el-button></div>
        <el-button v-if="!detail.isMember && !['removed','dissolved'].includes(detail.status)" text type="danger" @click="reportTeam">举报招募</el-button>
      </template>
    </el-drawer>

    <el-dialog class="team-dialog" v-model="applyVisible" :title="applyMode === 'contact' ? '联系队长' : '申请加入'" width="min(480px, 92vw)">
      <p>{{ applyMode === 'contact' ? '协商岗位' : '申请岗位' }}：{{ selectedPosition?.title }}</p>
      <p v-if="applyMode === 'contact'" class="apply-note">留言会以申请附言发送给队长，队长在申请列表即可回复你。</p>
      <el-input v-model="actionForm.message" type="textarea" :rows="4" maxlength="500" :placeholder="applyMode === 'contact' ? '想商量的岗位安排、每周可投入时间、能力情况等' : '介绍你的能力和参赛计划'" />
      <template #footer><el-button @click="applyVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitApply">{{ applyMode === 'contact' ? '发送留言' : '提交申请' }}</el-button></template>
    </el-dialog>
    <el-dialog class="team-dialog" v-model="inviteVisible" title="邀请学生" width="min(480px, 92vw)"><el-form label-position="top"><el-form-item v-if="actionForm.targetStudentId" label="邀请对象"><span>{{ actionForm.targetStudentName }}（系统内学生）</span></el-form-item><el-form-item v-else label="学生学号"><el-input v-model="actionForm.studentNumber" /></el-form-item><el-form-item label="岗位"><el-select v-model="actionForm.positionId" placeholder="选择仍有空缺的岗位" style="width:100%"><el-option v-for="p in availablePositions" :key="p.positionId" :label="`${p.title}（剩余 ${p.remainingSeats}）`" :value="p.positionId" /></el-select><div class="form-note">仅可邀请到仍有空缺的岗位；岗位一经招满即不再开放邀请与申请。</div></el-form-item><el-form-item label="邀请说明（对方可见的私信内容）"><el-input v-model="actionForm.message" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button @click="inviteVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitInvite">发送邀请</el-button></template></el-dialog>
    <el-dialog class="team-dialog request-detail" v-model="requestDetailVisible" title="申请详情" width="min(640px, 94vw)">
      <template v-if="requestDetail">
        <div class="rd-head">
          <div class="rd-person"><strong>{{ requestDetail.studentName }}</strong><span>{{ [requestApplicant.grade, requestApplicant.major, requestApplicant.college].filter(Boolean).join(' · ') || '学生' }}</span></div>
          <el-tag :type="requestTag(requestDetail.status)">{{ requestText(requestDetail.status) }}</el-tag>
        </div>
        <el-descriptions :column="2" size="small" border class="rd-meta">
          <el-descriptions-item label="竞赛">{{ requestDetail.competitionName }}</el-descriptions-item>
          <el-descriptions-item label="队伍">{{ requestDetail.teamName }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ requestDetail.positionTitle || '队员' }}<template v-if="requestDetail.positionVacancies">（{{ requestDetail.positionOccupied }}/{{ requestDetail.positionVacancies }}{{ positionFull(requestDetail) ? ' 已满' : ' 空缺' }}）</template></el-descriptions-item>
          <el-descriptions-item label="类型">{{ requestDetail.requestType === 'application' ? '学生申请' : '队长邀请' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ new Date(requestDetail.createTime).toLocaleString('zh-CN') }}</el-descriptions-item>
          <el-descriptions-item label="申请编号">#{{ requestDetail.requestId }}</el-descriptions-item>
        </el-descriptions>
        <section class="rd-block">
          <h4>{{ requestDetail.requestType === 'application' ? '申请人留言' : '邀请说明' }}</h4>
          <p v-if="requestDetail.message" class="rd-message">{{ requestDetail.message }}</p>
          <p v-else class="rd-empty">未填写留言</p>
        </section>
        <section class="rd-block">
          <h4>竞赛简历<span class="rd-hint">（对方未公开或未填写的内容不会显示）</span></h4>
          <div v-loading="requestProfileLoading" class="rd-profile">
            <div v-if="(requestApplicant.skills || []).length" class="rd-chips"><el-tag v-for="s in requestApplicant.skills" :key="s" size="small" effect="plain">{{ s }}</el-tag></div>
            <p v-else class="rd-empty">未填写技能标签</p>
            <p v-if="requestApplicant.specialties" class="rd-line"><b>擅长方向</b>{{ requestApplicant.specialties }}</p>
            <p v-if="requestApplicant.preferredRoles" class="rd-line"><b>期望岗位</b>{{ requestApplicant.preferredRoles }}</p>
            <p v-if="requestApplicant.bio" class="rd-line"><b>个人简介</b>{{ requestApplicant.bio }}</p>
            <p v-if="requestApplicant.portfolioUrl" class="rd-line"><b>作品链接</b><a :href="requestApplicant.portfolioUrl" target="_blank" rel="noopener">{{ requestApplicant.portfolioUrl }}</a></p>
            <p v-if="requestApplicant.weeklyHours" class="rd-line"><b>每周可投入</b>{{ requestApplicant.weeklyHours }} 小时</p>
            <p class="rd-privacy">未公开或未填写：{{ hiddenProfileFields }}</p>
            <el-table v-if="(requestApplicant.approvedAwards || []).length" :data="requestApplicant.approvedAwards" size="small" class="rd-awards"><el-table-column prop="competitionName" label="系统认证获奖竞赛" /><el-table-column prop="projectName" label="项目" /><el-table-column prop="awardLevel" label="奖项" width="100" /></el-table>
          </div>
        </section>
      </template>
      <template #footer>
        <el-button @click="closeAndOpenTeam">查看队伍</el-button>
        <el-button v-if="requestDetail?.status === 'pending' && canDecide(requestDetail)" type="success" :disabled="positionFull(requestDetail)" @click="decideFromDetail('accept')">接受</el-button>
        <el-button v-if="requestDetail?.status === 'pending' && canDecide(requestDetail)" type="danger" @click="decideFromDetail('reject')">拒绝</el-button>
        <el-button v-if="requestDetail?.status === 'pending' && canCancel(requestDetail)" type="warning" @click="decideFromDetail('cancel')">撤回</el-button>
        <el-button @click="requestDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <el-dialog class="team-dialog" v-model="editVisible" title="编辑队伍" width="min(520px,94vw)">
      <el-form label-position="top">
        <el-form-item label="队伍名称"><el-input v-model="editForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="队伍介绍"><el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
        <div class="two-col">
          <el-form-item label="目标人数"><el-input-number v-model="editForm.targetSize" :min="1" :max="50" /></el-form-item>
        </div>
        <el-form-item label="团队氛围标签">
          <el-select v-model="editForm.vibeTags" multiple filterable allow-create default-first-option placeholder="选择或输入团队标签" style="width:100%">
            <el-option v-for="v in PRESET_VIBES" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveTeam">保存</el-button></template>
    </el-dialog>
    <el-dialog class="team-dialog" v-model="positionVisible" :title="positionForm.positionId?'编辑岗位':'新增岗位'" width="min(520px,94vw)"><el-form label-position="top"><el-form-item label="岗位名称"><el-input v-model="positionForm.title" maxlength="50" /></el-form-item><el-form-item label="招募名额"><el-input-number v-model="positionForm.vacancies" :min="1" :max="50" /></el-form-item><el-form-item label="所需技能"><el-input v-model="positionForm.requiredSkills" placeholder="用逗号分隔" /></el-form-item><el-form-item label="补充要求"><el-input v-model="positionForm.requirements" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="positionVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="savePosition">保存</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import request from '../utils/request'

const route=useRoute(),router=useRouter(),loading=ref(false),saving=ref(false)
const activeTab=ref(route.query.tab || 'market'), requestScope=ref('mine'),requestView=ref('pending'),highlightRequestId=ref(null),mineScope=ref('leader'), grades=['21级','22级','23级','24级','25级','26级']
const filters=reactive({competition:'',skill:'',major:'',grade:'',status:''}),market=reactive({list:[],total:0,page:1,pageSize:12}),mine=reactive({teams:[]}),requests=ref([]),profile=reactive({skills:[],weeklyHours:0,approvedAwards:[],showBio:0,showPortfolio:0,showWeeklyHours:0}),recommendations=ref([])
const competitions=ref([]),createVisible=ref(false),detailVisible=ref(false),applyVisible=ref(false),applyMode=ref('apply'),inviteVisible=ref(false),editVisible=ref(false),positionVisible=ref(false),candidateVisible=ref(false),candidateProfileVisible=ref(false),candidates=ref([]),candidateProfile=reactive({}),detail=reactive({}),selectedPosition=ref(null),actionForm=reactive({message:'',studentNumber:'',targetStudentId:null,targetStudentName:'',positionId:null})
/* 申请详情：点行打开，展示完整留言 + 申请人竞赛简历 */
const requestDetailVisible=ref(false),requestDetail=ref(null),requestApplicant=reactive({}),requestProfileLoading=ref(false)
/* 隐私过滤由后端完成，前端只列出缺失项，避免误标为「未公开」 */
const hiddenProfileFields=computed(()=>{const p=requestApplicant,miss=[];if(!p.bio)miss.push('个人简介');if(!p.portfolioUrl)miss.push('作品链接');if(!p.weeklyHours)miss.push('每周可投入时间');miss.push('竞赛经历');return miss.join('、')})
const positionForm=reactive({positionId:null,title:'',vacancies:1,requiredSkills:'',requirements:''})
const blankPosition=()=>({title:'',vacancies:1,requirements:'',requiredSkills:''})
const PRESET_VIBES = ['🎯 目标国奖', '🏃‍♂️ 效率至上', '☕ 佛系参与', '🤝 新人友好(包教)', '🔥 冲刺大厂', '💻 拒绝鸽王', '🌙 熬夜修仙', '🎈 主打陪伴']
const createForm=reactive({competitionId:null,name:'',description:'',targetSize:3,leaderRole:'负责人',vibeTags:[],positions:[blankPosition()]})
const editForm=reactive({name:'',description:'',targetSize:2,version:0,vibeTags:[]})
const pendingCount=computed(()=>requests.value.filter(v=>v.status==='pending').length)
const visibleMine=computed(()=>mine.teams.filter(t=>mineScope.value==='history'?t.historical:!t.historical&&(mineScope.value==='leader'?Boolean(t.isLeader):!t.isLeader)))
const currentUser=()=>JSON.parse(localStorage.getItem('saims_user')||'{}')
const visibleRequests = computed(() => requests.value.filter(r => {
  if (requestView.value === 'history') return r.status !== 'pending'
  if (requestView.value === 'outgoing') return r.status === 'pending' && r.operatorId === currentUser().userId
  return r.status === 'pending' && canDecide(r) &&
    (requestScope.value === 'mine' ? true
      : requestScope.value === 'managed' ? r.requestType === 'application'
      : r.requestType === 'invitation')
}))
const editable=computed(()=>['recruiting','closed'].includes(detail.status))
const availablePositions=computed(()=>(detail.positions||[]).filter(p=>p.status==='open'&&p.remainingSeats>0))
const unwrap=res=>{if(res.code!=='200')throw new Error(res.msg||'操作失败');return res.data}
const api=async(promise,success)=>{try{const data=unwrap(await promise);if(success)ElMessage.success(success);return data}catch(e){ElMessage.error(e.message||'操作失败');throw e}}
const statusText=s=>({recruiting:'招募中',closed:'已成队',locked:'已锁定',dissolved:'已解散',removed:'已下架'}[s]||s)
const requestText=s=>({pending:'待处理',accepted:'已接受',rejected:'已拒绝',cancelled:'已撤回',expired:'已失效'}[s]||s)
const requestTag=s=>({accepted:'success',rejected:'danger',pending:'warning',expired:'info'}[s]||'info')
/* 岗位是否已确认（名额已满或已关闭）：不再开放申请与邀请，队长也不能再接受该岗位的新申请 */
const positionFull=row=>row.positionVacancies!=null&&(row.positionOccupied>=row.positionVacancies||row.positionStatus&&row.positionStatus!=='open')
const splitSkills=v=>[...new Set(String(v).split(/[,，]/).map(s=>s.trim()).filter(Boolean))].slice(0,8)

const isUrgent = (team) => {
  if (team.status !== 'recruiting' || !team.teamCloseTime) return false;
  const timeLeft = new Date(team.teamCloseTime).getTime() - Date.now();
  return timeLeft > 0 && timeLeft < 3 * 24 * 60 * 60 * 1000;
}

const CATEGORY_RULES=[
  {key:'art',words:['广告艺术','数字艺术','创新设计','三维数字化','设计周','美术','视觉','动画','摄影','服装','插画','音乐','舞蹈','戏剧','文创']},
  {key:'liberal',words:['外语','英语','演讲','诵写讲','经典','写作','辩论','翻译','汉语','中文','人文','社科','课外学术','模拟联合国','朗读','诗词']},
  {key:'biz',words:['创新创业','互联网+','创业','学创杯','电子商务','商业','营销','市场调查','企管','财务','金融','经贸','沙盘','管理']},
  {key:'tech',words:['程序设计','ACM','数学建模','蓝桥杯','ICT','电子设计','算法','软件','信息技术','人工智能','机器人','大数据','网络','物联网','智能车','物理','化学','力学','生物','医学','测绘','结构设计']},
]
function categoryOf(team){
  const name=String((team&&team.competitionName)||'')
  const hit=CATEGORY_RULES.find(r=>r.words.some(w=>name.includes(w)))
  return hit||{key:'fallback'}
}

/* ============ 退回大受好评的“高级清透浅色渐变” ============ */
const CATEGORY_STYLES = {
  art: {
    accent: '#ec4899', soft: '#fce7f3', ink: '#be185d',
    bgGrad: 'linear-gradient(135deg, #ffffff 40%, #fdf2f8 100%)',
    metaBg: 'rgba(252, 231, 243, 0.4)',
    pattern: `url("data:image/svg+xml,%3Csvg width='120' height='40' viewBox='0 0 120 40' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 20 Q 30 0, 60 20 T 120 20' fill='none' stroke='%23ec4899' stroke-width='1.5' stroke-linecap='round'/%3E%3Cpath d='M0 30 Q 30 10, 60 30 T 120 30' fill='none' stroke='%23ec4899' stroke-width='1.5' stroke-linecap='round' opacity='0.5'/%3E%3C/svg%3E")`
  },
  tech: {
    accent: '#3b82f6', soft: '#dbeafe', ink: '#1d4ed8',
    bgGrad: 'linear-gradient(135deg, #ffffff 40%, #eff6ff 100%)',
    metaBg: 'rgba(219, 234, 254, 0.4)',
    pattern: `url("data:image/svg+xml,%3Csvg width='100' height='80' viewBox='0 0 100 80' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='20' cy='60' r='3' fill='%233b82f6'/%3E%3Ccircle cx='80' cy='30' r='4' fill='%233b82f6'/%3E%3Ccircle cx='50' cy='70' r='2' fill='%233b82f6'/%3E%3Cline x1='20' y1='60' x2='80' y2='30' stroke='%233b82f6' stroke-width='1' opacity='0.6'/%3E%3Cline x1='20' y1='60' x2='50' y2='70' stroke='%233b82f6' stroke-width='1' opacity='0.6'/%3E%3Cline x1='50' y1='70' x2='80' y2='30' stroke='%233b82f6' stroke-width='1' opacity='0.6'/%3E%3C/svg%3E")`
  },
  liberal: {
    accent: '#84cc16', soft: '#ecfccb', ink: '#4d7c0f',
    bgGrad: 'linear-gradient(135deg, #ffffff 40%, #f7fee7 100%)',
    metaBg: 'rgba(236, 252, 203, 0.4)',
    pattern: `url("data:image/svg+xml,%3Csvg width='100' height='80' viewBox='0 0 100 80' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M20 80 V 40 A 20 20 0 0 1 60 40 V 80' fill='none' stroke='%2384cc16' stroke-width='2' opacity='0.5'/%3E%3Cpath d='M40 80 V 50 A 10 10 0 0 1 60 50 V 80' fill='none' stroke='%2384cc16' stroke-width='2' opacity='0.3'/%3E%3Cpath d='M60 80 V 30 A 25 25 0 0 1 110 30 V 80' fill='none' stroke='%2384cc16' stroke-width='2' opacity='0.5'/%3E%3C/svg%3E")`
  },
  biz: {
    accent: '#f59e0b', soft: '#fef3c7', ink: '#b45309',
    bgGrad: 'linear-gradient(135deg, #ffffff 40%, #fffbeb 100%)',
    metaBg: 'rgba(254, 243, 199, 0.4)',
    pattern: `url("data:image/svg+xml,%3Csvg width='80' height='60' viewBox='0 0 80 60' xmlns='http://www.w3.org/2000/svg'%3E%3Crect x='10' y='40' width='12' height='20' rx='2' fill='%23f59e0b' opacity='0.4'/%3E%3Crect x='34' y='25' width='12' height='35' rx='2' fill='%23f59e0b' opacity='0.6'/%3E%3Crect x='58' y='10' width='12' height='50' rx='2' fill='%23f59e0b' opacity='0.8'/%3E%3C/svg%3E")`
  },
  fallback: {
    accent: '#64748b', soft: '#f1f5f9', ink: '#334155',
    bgGrad: 'linear-gradient(135deg, #ffffff 40%, #f8fafc 100%)',
    metaBg: 'rgba(241, 245, 249, 0.5)',
    pattern: 'none'
  }
};

const accentVars=team=>{
  const cat = categoryOf(team);
  const style = CATEGORY_STYLES[cat.key] || CATEGORY_STYLES.fallback;
  return {
    '--accent': style.accent,
    '--accent-soft': style.soft,
    '--accent-ink': style.ink,
    '--bg-grad': style.bgGrad,
    '--meta-bg': style.metaBg,
    '--pattern': style.pattern
  }
}

const vacancy=team=>Math.max((team.targetSize||0)-(team.memberCount||0),0)
const filledSlots=team=>Math.min(team.memberCount||0,5)

function heroSlots(team){
  const vac=Math.min(vacancy(team),5-filledSlots(team))
  if(vac<=0)return []
  return Array.from({ length: vac }, () => ({}))
}

const skillTags=team=>splitSkills(team.requiredSkills||'')
const vibeTags=team=>(team.vibeTags||'').split(',').filter(Boolean)
const formatTime=v=>v?new Date(v).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}):'长期开放'

async function loadMarket(){loading.value=true;try{const data=await api(request.get('/api/teams',{params:{...filters,page:market.page,pageSize:market.pageSize}}));Object.assign(market,data)}finally{loading.value=false}}
function clearFilters(){Object.assign(filters,{competition:'',skill:'',major:'',grade:'',status:''});market.page=1;loadMarket()}
async function loadRecommendations(){loading.value=true;try{recommendations.value=await api(request.get('/api/teams/recommendations'))}finally{loading.value=false}}
async function loadMine(){loading.value=true;try{const data=await api(request.get('/api/teams/mine'));mine.teams=data.teams||[]}finally{loading.value=false}}
async function loadRequests(){loading.value=true;try{const [mine,managed]=await Promise.all([api(request.get('/api/team-requests',{params:{scope:'mine'}})),api(request.get('/api/team-requests',{params:{scope:'managed'}}))]);requests.value=[...new Map([...mine,...managed].map(row=>[row.requestId,row])).values()]}finally{loading.value=false}}
async function loadProfile(){loading.value=true;try{Object.assign(profile,await api(request.get('/api/team-profiles/me')))}finally{loading.value=false}}
async function tabChanged(tab){router.replace({query:tab==='market'?{}:{tab}});if(tab==='market')loadMarket();if(tab==='recommended')loadRecommendations();if(tab==='mine')loadMine();if(tab==='requests')loadRequests();if(tab==='profile')loadProfile()}
async function openCreate(){if(!competitions.value.length)competitions.value=await api(request.get('/competition/all'));createVisible.value=true}
async function createTeam(){
  if(!createForm.competitionId||!createForm.name)return ElMessage.warning('请选择竞赛并填写队伍名称');
  saving.value=true;
  try{
    const payload = { ...createForm, vibeTags: createForm.vibeTags.join(',') }
    const data=await api(request.post('/api/teams',payload),'队伍已发布');
    createVisible.value=false;activeTab.value='mine';await loadMine();showDetail(data.teamId)
  }finally{saving.value=false}
}
async function showDetail(id,wantApply=false,positionId=null,mode='apply'){Object.keys(detail).forEach(k=>delete detail[k]);Object.assign(detail,await api(request.get(`/api/teams/${id}`)));detailVisible.value=true;candidateVisible.value=false;if(wantApply&&!detail.isMember&&detail.canApply&&availablePositions.value.length)openApply(availablePositions.value.find(p=>p.positionId===positionId)||availablePositions.value[0],mode)}
/* mode='apply' 常规申请入队；mode='contact' 复用申请附言通道，向队长发岗位协商留言 */
function openApply(position,mode='apply'){applyMode.value=mode;selectedPosition.value=position;actionForm.message=mode==='contact'?`你好，我看到队伍还有空缺岗位，想聊一下「${position.title}」的分工与要求（每周可投入时间 / 能力匹配），方便时回复我。`:'';applyVisible.value=true}
/* 状态B卡片上的「可与队长联系」：打开详情并直接落到协商留言框 */
function contactLeader(team){showDetail(team.teamId,true,null,'contact')}
async function submitApply(){if(saving.value)return;saving.value=true;try{await api(request.post(`/api/teams/${detail.teamId}/apply`,{positionId:selectedPosition.value.positionId,message:actionForm.message}),applyMode.value==='contact'?'留言已发送，队长在申请列表可回复你':'申请已发送');applyVisible.value=false;loadRequests()}finally{saving.value=false}}
/* 邀请（队长发起的私信）：只能投向仍有空缺的岗位；岗位已满即无任何私信入口 */
function openInvite(){
  if(!availablePositions.value.length) return ElMessage.warning('所有招募岗位均已招满，无法再发出邀请；如需继续招募请新增岗位或调整名额');
  Object.assign(actionForm,{studentNumber:'',targetStudentId:null,targetStudentName:'',positionId:availablePositions.value[0].positionId,message:''});inviteVisible.value=true
}
function inviteCandidate(candidate){Object.assign(actionForm,{studentNumber:'',targetStudentId:candidate.studentId,targetStudentName:candidate.studentName,positionId:candidate.positionId,message:''});inviteVisible.value=true}
async function submitInvite(){
  if(!actionForm.positionId) return ElMessage.warning('请选择要邀请的空缺岗位');
  const target=availablePositions.value.find(p=>p.positionId===actionForm.positionId);
  if(!target) return ElMessage.warning('该岗位已招满或已关闭，不再接收邀请，请改用其他空缺岗位');
  if(!target.remainingSeats) return ElMessage.warning(`「${target.title}」已招满，不再接收邀请`);
  if(saving.value)return;saving.value=true;try{await api(request.post(`/api/teams/${detail.teamId}/invite`,actionForm),'邀请已发送');inviteVisible.value=false;loadRequests()}finally{saving.value=false}
}
async function decide(row,action){if(action==='cancel')await ElMessageBox.confirm('撤回后对方将无法再接受，确定继续？','撤回请求');await api(request.post(`/api/team-requests/${row.requestId}/${action}`),action==='accept'?'已接受':action==='cancel'?'已撤回':'已拒绝');loadRequests();if(detail.teamId)showDetail(detail.teamId)}
/* 点行/点「详情」：展示该条申请或邀请的完整信息与申请人简历 */
async function openRequestDetail(row){
  requestDetail.value=row;Object.keys(requestApplicant).forEach(k=>delete requestApplicant[k])
  requestDetailVisible.value=true;requestProfileLoading.value=true
  try{Object.assign(requestApplicant,await api(request.get(`/api/team-profiles/${row.studentId}`)))}
  catch{/* 档案读取失败已在 api 内提示，弹窗保留申请本身的信息 */}
  finally{requestProfileLoading.value=false}
}
async function decideFromDetail(action){await decide(requestDetail.value,action);requestDetailVisible.value=false}
function closeAndOpenTeam(){const teamId=requestDetail.value?.teamId;requestDetailVisible.value=false;if(teamId)showDetail(teamId)}
function canDecide(row){const user=currentUser();return row.requestType==='invitation'?row.studentId===user.studentId:row.leaderId===user.studentId}
function canCancel(row){return row.operatorId===currentUser().userId}
async function saveProfile(){saving.value=true;try{Object.assign(profile,await api(request.put('/api/team-profiles/me',profile),'竞赛简历已保存'))}finally{saving.value=false}}
async function toggleRecruiting(){await api(request.post(`/api/teams/${detail.teamId}/recruiting`,{recruiting:!detail.recruiting}),'招募状态已更新');showDetail(detail.teamId)}
async function lockTeam(){await ElMessageBox.confirm('锁定后仅管理员可以解锁，确定继续？','锁定队伍',{type:'warning'});await api(request.post(`/api/teams/${detail.teamId}/lock`),'队伍已锁定');showDetail(detail.teamId)}
async function dissolveTeam(){await ElMessageBox.confirm('解散后不可恢复，确定解散？','解散队伍',{type:'error'});await api(request.post(`/api/teams/${detail.teamId}/dissolve`),'队伍已解散');detailVisible.value=false;loadMine()}
async function transferLeader(row){await ElMessageBox.confirm(`将负责人转让给 ${row.studentName}？`,'转让负责人');await api(request.post(`/api/teams/${detail.teamId}/transfer`,{studentId:row.studentId}),'负责人已转让');showDetail(detail.teamId)}
async function removeMember(row){await ElMessageBox.confirm(`确定移除 ${row.studentName}？`,'移除成员',{type:'warning'});await api(request.delete(`/api/teams/${detail.teamId}/members/${row.studentId}`),'成员已移除');showDetail(detail.teamId)}
function openEditTeam(){
  Object.assign(editForm,{
    name:detail.name,
    description:detail.description||'',
    targetSize:detail.targetSize,
    version:detail.version,
    vibeTags: detail.vibeTags ? detail.vibeTags.split(',') : []
  });
  editVisible.value=true
}
async function saveTeam(){
  saving.value=true;
  try{
    const payload = { ...editForm, vibeTags: editForm.vibeTags.join(',') }
    await api(request.put(`/api/teams/${detail.teamId}`,payload),'队伍已更新');
    editVisible.value=false;await showDetail(detail.teamId);loadMine()
  }finally{saving.value=false}
}
function openPosition(p){Object.assign(positionForm,p?{positionId:p.positionId,title:p.title,vacancies:p.vacancies,requiredSkills:p.requiredSkills||'',requirements:p.requirements||''}:{positionId:null,title:'',vacancies:1,requiredSkills:'',requirements:''});positionVisible.value=true}
async function savePosition(){saving.value=true;try{if(positionForm.positionId)await api(request.put(`/api/teams/${detail.teamId}/positions/${positionForm.positionId}`,positionForm),'岗位已更新');else await api(request.post(`/api/teams/${detail.teamId}/positions`,positionForm),'岗位已新增');positionVisible.value=false;showDetail(detail.teamId)}finally{saving.value=false}}
async function closePosition(p){await ElMessageBox.confirm(`关闭“${p.title}”后待处理请求将失效，确定继续？`,'关闭岗位',{type:'warning'});await api(request.post(`/api/teams/${detail.teamId}/positions/${p.positionId}/close`),'岗位已关闭');showDetail(detail.teamId)}
async function editMemberRole(row){const choices=(detail.positions||[]).filter(p=>p.status==='open');const {value}=await ElMessageBox.prompt(`填写 ${row.studentName} 的新岗位名称`,'调整岗位',{inputValue:row.roleName||'',inputValidator:v=>!!v.trim()||'请输入岗位名称'});const position=choices.find(p=>p.title===value.trim());await api(request.put(`/api/teams/${detail.teamId}/members/${row.studentId}/role`,{roleName:value.trim(),positionId:position?.positionId||null}),'岗位已调整');showDetail(detail.teamId)}
async function leaveTeam(){await ElMessageBox.confirm('退出后将失去队伍成员权限；若你是唯一成员，队伍将解散。确定继续？','退出队伍',{type:'warning'});await api(request.post(`/api/teams/${detail.teamId}/leave`),'已退出队伍');detailVisible.value=false;loadMine()}
async function loadCandidates(){candidates.value=await api(request.get(`/api/teams/${detail.teamId}/candidates`));candidateVisible.value=true}
async function viewCandidate(candidate){Object.keys(candidateProfile).forEach(k=>delete candidateProfile[k]);Object.assign(candidateProfile,await api(request.get(`/api/team-profiles/${candidate.studentId}`)));candidateProfileVisible.value=true}
async function reportTeam(){const {value}=await ElMessageBox.prompt('请说明举报原因','举报招募',{inputType:'textarea',inputValidator:v=>!!v.trim()||'请输入举报原因'});await api(request.post(`/api/teams/${detail.teamId}/reports`,{reason:value}),'举报已提交')}
/* 通知链接直达：tab=requests&requestId=xxx 时切到申请与邀请并高亮定位该条记录；兼容历史 ?teamId= 链接（打开详情抽屉） */
function focusRequestFromQuery(q){
  if(q.teamId&&q.tab!=='requests'){showDetail(q.teamId);return}
  const rid=Number(q.requestId); if(!rid)return;
  activeTab.value='requests';requestView.value='pending';highlightRequestId.value=rid;
  const row=requests.value.find(r=>r.requestId===rid);
  if(row){if(row.status!=='pending')requestView.value='history';requestScope.value=row.requestType==='application'?'managed':'invited'}
}
/* 桌面表格行高亮标记（通知定位的那条申请/邀请） */
function requestRowClass({row}){return row.requestId===highlightRequestId.value?'row-highlight':''}
onMounted(async()=>{await Promise.all([loadMarket(),loadRequests()]);focusRequestFromQuery({...route.query});if(activeTab.value!=='market')tabChanged(activeTab.value)})
/* 已停留在本页时点击另一条通知：仅 requestId 变化时重新加载并定位 */
watch(()=>route.query.requestId,()=>{if(route.query.requestId)loadRequests().then(()=>focusRequestFromQuery({...route.query}))})
</script>

<style scoped>
.team-page{
  --brand:#6366f1;--brand-2:#818cf8;--brand-deep:#4f46e5;--brand-soft:#eef2ff;
  --ink:#1e293b;--ink-2:#475569;--muted:#64748b;--faint:#94a3b8;--line:#e8ecf3;
  min-height:100vh;color:var(--ink);
  background:transparent;
}
.team-shell{max-width:1280px;margin:0 auto;padding:36px 24px 72px}

.page-head{
  position:relative;overflow:hidden;
  display:flex;align-items:center;justify-content:space-between;gap:20px;
  padding:26px 30px;margin-bottom:22px;
  background:linear-gradient(120deg,#fff 0%,#f7f8ff 58%,#eef2ff 100%);
  border:1px solid rgba(99,102,241,.16);border-radius:20px;
  box-shadow:0 26px 54px -36px rgba(30,41,59,.55);
}
.page-head::after{
  content:'';position:absolute;top:-90px;right:-60px;width:260px;height:260px;border-radius:50%;
  background:radial-gradient(circle at center,rgba(99,102,241,.22),transparent 68%);pointer-events:none;
}
.page-head-copy{position:relative;padding-left:16px}
.page-head-copy::before{
  content:'';position:absolute;left:0;top:5px;bottom:5px;width:4px;border-radius:4px;
  background:linear-gradient(180deg,#6366f1,#22d3ee);
}
.page-head h1{margin:0;font-size:26px;font-weight:800;letter-spacing:-.3px;color:var(--ink)}
.page-head p{margin:8px 0 0;color:var(--muted);font-size:13.5px;line-height:1.6}
.head-cta{position:relative;flex-shrink:0}

.team-page :deep(.el-button--primary:not(.is-link):not(.is-plain):not(.is-text):not(.join-btn)){
  border:0;border-radius:11px;
  background:linear-gradient(135deg,#6366f1,#818cf8);
  box-shadow:0 10px 22px -14px rgba(99,102,241,.95);
}
.team-page :deep(.el-button--primary:not(.is-link):not(.is-plain):not(.is-text):not(.join-btn):hover){background:linear-gradient(135deg,#5459e6,#7b86f6)}
.team-page :deep(.el-button:not(.is-link):not(.is-text)){border-radius:11px}

.team-tabs{
  padding:6px 28px 30px;
  background:rgba(255,255,255,.86);backdrop-filter:blur(18px);
  border:1px solid rgba(255,255,255,.9);border-radius:24px;
  box-shadow:0 32px 70px -48px rgba(30,41,59,.6),0 2px 8px rgba(15,23,42,.03);
}
.team-tabs :deep(.el-tabs__header){margin:0 0 24px}
.team-tabs :deep(.el-tabs__nav-wrap::after){height:1px;background:var(--line)}
.team-tabs :deep(.el-tabs__item){height:58px;padding:0 22px;font-size:15px;font-weight:600;color:var(--muted);transition:color .2s ease}
.team-tabs :deep(.el-tabs__item:hover){color:var(--brand-2)}
.team-tabs :deep(.el-tabs__item.is-active){color:var(--brand-deep)}
.team-tabs :deep(.el-tabs__active-bar){height:3px;border-radius:3px;background:linear-gradient(90deg,#6366f1,#8b5cf6)}
.tab-label{display:inline-flex;align-items:center;gap:4px}

.filters{
  display:flex;flex-direction:column;gap:14px;
  padding:18px 20px;margin-bottom:20px;
  background:linear-gradient(180deg,#fbfcff,#f5f7fd);
  border:1px solid var(--line);border-radius:18px;
}
.filter-grid{display:grid;grid-template-columns:2fr 1.2fr 1.2fr 1fr 1fr;gap:12px}
.filter-grid>*{min-width:0}
.filter-actions{display:flex;justify-content:flex-end;gap:10px;padding-top:14px;border-top:1px dashed var(--line)}
.filter-actions :deep(.el-button){min-width:106px;margin-left:0}
.filters :deep(.el-input__wrapper),.filters :deep(.el-select__wrapper){
  border-radius:12px;background:#fff;box-shadow:0 0 0 1px #e4e9f2 inset;transition:box-shadow .2s ease;
}
.filters :deep(.el-input__wrapper.is-focus),.filters :deep(.el-select__wrapper.is-focused){
  box-shadow:0 0 0 1px var(--brand) inset,0 0 0 4px rgba(99,102,241,.12);
}
.result-count{
  display:inline-flex;align-items:center;gap:8px;
  margin:0 0 18px;padding:7px 14px;
  background:var(--brand-soft);color:var(--brand-deep);
  border-radius:999px;font-size:12.5px;font-weight:600;
}
.result-count::before{content:'';width:6px;height:6px;border-radius:50%;background:currentColor}

.team-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:20px;min-height:240px}
.team-grid :deep(.el-empty){grid-column:1/-1}

.team-card{
  --accent:#64748b;--accent-soft:#f1f5f9;--accent-ink:#475569;
  position:relative;overflow:hidden;display:flex;flex-direction:column;min-width:0;
  padding:22px;border:1px solid rgba(255,255,255,0.8);border-radius:16px;
  background: var(--bg-grad, #ffffff);
  box-shadow: 0 4px 14px -4px rgba(0,0,0,0.03), inset 0 0 0 1px rgba(255,255,255,0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

/* 花纹位置整体往上移：将 bottom 调至 40px */
.team-card::after {
  content: '';
  position: absolute;
  bottom: 40px; 
  right: 16px;
  width: 140px;
  height: 90px;
  background-image: var(--pattern);
  background-repeat: no-repeat;
  background-position: bottom right;
  opacity: 0.15;
  pointer-events: none;
  z-index: 0;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.team-card:hover{
  transform: translateY(-2px); 
  box-shadow:0 12px 24px -8px rgba(0,0,0,0.08), inset 0 0 0 1px rgba(255,255,255,1);
}
.team-card:hover::after {
  opacity: 0.25;
  transform: scale(1.02);
}

.card-top,.card-actions,.section-tools,.detail-head,.position-head{display:flex;justify-content:space-between;align-items:center;gap:10px;position:relative;z-index:1}
.card-top{align-items:flex-start}
.card-tags{display:flex;align-items:center;gap:8px;flex-wrap:wrap;min-width:0}
.card-tags :deep(.el-tag.cat-tag){
  background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(4px);
  border-color: var(--accent); color: var(--accent-ink); font-weight: 600;
}
.card-top :deep(.el-tag){font-weight:500;display:inline-flex;align-items:center}

.pulse-dot {
  display: inline-block; width: 6px; height: 6px; border-radius: 50%;
  background-color: #10b981; margin-right: 6px; vertical-align: middle;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulse-green 2s infinite;
}
@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}
.urgent-tag {
  border-color: #fca5a5 !important; background-color: #fef2f2 !important;
  color: #ef4444 !important; font-weight: 700 !important;
}

/* 增加了引导文本前缀后的标题和介绍样式 */
.info-label {
  font-weight: 500;
  color: #94a3b8;
  font-size: 13.5px;
  margin-right: 4px;
}
.team-name{
  margin:14px 0 6px;
  font-size:16px;
  font-weight:700;
  letter-spacing:0;
  color:#111827;
  position:relative;
  z-index:1;
  display: flex;
  align-items: baseline;
}
.description{
  margin:0;color:#6b7280;font-size:13px;line-height:1.7;position:relative;z-index:1;
  display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;
}

.team-meta{
  display:grid;gap:8px;margin:16px 0 0;padding:12px 14px;
  background: var(--meta-bg);
  border:1px solid rgba(255,255,255,0.7);border-radius:10px;
  backdrop-filter: blur(4px);
  position:relative;z-index:1;
}
.team-meta>div{display:grid;grid-template-columns:64px 1fr;gap:10px;align-items:baseline;font-size:13px}
.team-meta dt{color:#64748b}
.team-meta dd{margin:0;color:#1e293b;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}

.slot-row{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin:18px 0 0;position:relative;z-index:1}
.slot{position:relative;width:34px;height:34px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;flex-shrink:0}
.slot.filled{background:#ffffff;border:1.5px solid #e5e7eb;color:#9ca3af;box-shadow:0 2px 6px rgba(0,0,0,0.03)}
.slot.filled.leader{background:#ffffff;border-color:var(--accent);color:var(--accent-ink)}
.slot .ava{width:20px;height:20px;fill:currentColor}

/* 队长徽章：跟随赛道主色（--accent） */
.leader-badge {
  position: absolute; right: -8px; bottom: -4px;
  background: var(--accent);
  color: #fff; font-size: 10px; font-weight: 700;
  padding: 2px 6px; border-radius: 999px;
  line-height: 1.1; border: 1.5px solid #fff;
  box-shadow: 0 2px 4px var(--accent-soft);
  letter-spacing: 0.5px;
}

.slot.vacant {
  border: 1.5px dashed var(--accent); background: rgba(255, 255, 255, 0.4); 
  color: var(--accent); font-size: 16px; font-weight: 500;
}

.slot-note{font-size:12.5px;color:#64748b;font-weight:600}

.tag-row{display:flex;flex-wrap:wrap;gap:6px;margin:12px 0 0;position:relative;z-index:1}
.tag-row .tag{padding:4px 10px;border-radius:8px;font-size:11.5px;font-weight:600;line-height:1.7;white-space:nowrap}
.tag-row .tag.skill{border:1px solid rgba(255,255,255,0.8);color:var(--accent-ink);background:rgba(255,255,255,0.6);backdrop-filter:blur(4px)}
.tag-row .tag.vibe{border:1px solid transparent;background:var(--accent-soft);color:var(--accent-ink)}
/* 状态B提示条：意向职位部分已落实，仍有空缺（浅底虚线框，跟随类别强调色） */
.intent-hint{display:flex;align-items:flex-start;gap:8px;margin:12px 0 0;padding:9px 12px;border-radius:8px;background:var(--accent-soft);border:1px dashed var(--accent);color:var(--accent-ink);font-size:12px;line-height:1.6;position:relative;z-index:1}
.intent-hint .intent-icon{flex-shrink:0}
.intent-hint .intent-text{font-weight:500}
/* 提示条内的行动入口：无按钮外观、仅下划线，避免抢占「申请加入」主按钮的注意力 */
.intent-hint .intent-link{padding:0;margin:0;border:0;background:none;font:inherit;font-weight:700;color:var(--accent-ink);text-decoration:underline;text-underline-offset:3px;cursor:pointer;transition:opacity .2s}
.intent-hint .intent-link:hover{opacity:.68}
.apply-note{margin:4px 0 10px;font-size:12px;color:#94a3b8;line-height:1.6}

.deadline{display:flex;align-items:center;gap:7px;margin-top:auto;padding-top:14px;font-size:12px;color:#9ca3af}
.deadline::before{content:'';width:6px;height:6px;border-radius:50%;background:var(--accent);opacity:.85;flex-shrink:0}
.card-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-top:auto;padding-top:16px;border-top:1px solid rgba(255,255,255,0.6);position:relative;z-index:1}
.card-footer .deadline{margin:0;padding:0}
.card-actions{justify-content:flex-end;gap:8px}
.card-actions :deep(.el-button){height:34px;padding:0 14px;font-size:13px;margin-left:0}
.card-actions .el-button{white-space:normal;height:auto;min-height:34px}
.card-actions :deep(.el-button--default){background:#ffffff;border-color:#e5e7eb;color:#374151}
.card-actions :deep(.el-button--default:hover){background:#f3f4f6;border-color:#d1d5db;color:#111827}

.card-actions .join-btn{
  --el-button-bg-color:var(--accent);--el-button-border-color:var(--accent);
  --el-button-hover-bg-color:var(--accent-ink);--el-button-hover-border-color:var(--accent-ink);
  --el-button-active-bg-color:var(--accent-ink);--el-button-active-border-color:var(--accent-ink);
  --el-button-disabled-bg-color:rgba(255,255,255,0.6);--el-button-disabled-border-color:transparent;--el-button-disabled-text-color:#9ca3af;
  border:0;color:#fff;
}
.join-btn:deep(span){ display: inline-flex; align-items: center; justify-content: center; overflow: hidden; }
.fly-icon {
  display: inline-block; max-width: 0; opacity: 0; transform: translateX(-8px) translateY(1px) scale(0.5);
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  white-space: nowrap; margin-left: 0;
}
.join-btn:not(.is-disabled):hover .fly-icon {
  max-width: 20px; opacity: 1; transform: translateX(0) translateY(1px) scale(1.1); margin-left: 6px;
}

.reason-list{display:flex;flex-wrap:wrap;gap:6px;margin:12px 0 0;position:relative;z-index:1}
.reason-list span{padding:5px 10px;border-radius:8px;background:var(--accent-soft);color:var(--accent-ink);font-size:12px;font-weight:500}
.match-score{font-size:15px;font-weight:700;color:var(--accent-ink);background:var(--accent-soft);padding:4px 11px;border-radius:999px}

.section-tools{margin:4px 0 18px;color:var(--muted)}
.section-tools :deep(.el-segmented){
  --el-segmented-bg-color:#f1f4fa;
  --el-segmented-item-selected-color:#ffffff;
  border-radius:12px;padding:4px
}
.section-tools :deep(.el-segmented__item){font-weight:600;color:#64748b}
.section-tools :deep(.el-segmented__item.is-selected),
.section-tools :deep(.el-segmented__item.is-selected.is-disabled){color:#ffffff}
.request-toolbar{
  display:flex;align-items:center;gap:14px;flex-wrap:wrap;
  padding:14px 16px;margin-bottom:18px;
  background:#f8fafc;border:1px solid var(--line);border-radius:16px;
}
.request-toolbar :deep(.el-segmented){--el-segmented-bg-color:#eceff6;border-radius:12px;padding:4px}
.request-toolbar .request-view{margin-left:auto}
.request-table{margin-top:0}
/* 申请/邀请留言：队长查看应聘者附言，单行省略 + 悬浮查看全文 */
.req-message{color:var(--ink);font-size:12.5px}
.req-message.empty{color:var(--faint);font-style:italic}
/* 通知定位的申请/邀请行：高亮提示队长"留言在这里" */
.request-table :deep(tr.row-highlight>td){background:#e0e7ff!important}
.request-table :deep(tr.row-highlight:hover>td){background:#c7d2fe!important}
/* 申请详情弹窗 */
.request-tip{margin-left:12px;color:var(--faint);font-size:12px}
.request-table :deep(.el-table__row){cursor:pointer}
.rd-head{display:flex;justify-content:space-between;align-items:center;gap:12px}
.rd-person strong{margin-right:8px;font-size:16px;color:var(--ink)}
.rd-person span{color:var(--faint);font-size:12.5px}
.rd-meta{margin:14px 0 4px}
.rd-block{margin-top:16px}
.rd-block h4{margin:0 0 8px;font-size:13.5px;color:var(--ink)}
.rd-hint{margin-left:6px;font-weight:400;color:var(--faint);font-size:12px}
.rd-message{margin:0;padding:12px 14px;background:#f5f7fb;border:1px solid var(--line);border-radius:12px;color:var(--ink);font-size:13px;line-height:1.75;white-space:pre-wrap;overflow-wrap:anywhere}
.rd-empty{margin:0;color:var(--faint);font-size:13px;font-style:italic}
.rd-profile{min-height:40px}
.rd-chips{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:8px}
.rd-line{margin:6px 0;color:var(--muted);font-size:13px;line-height:1.7}
.rd-line b{margin-right:8px;color:var(--ink);font-weight:600}
.rd-line a{color:var(--accent-ink);text-decoration:underline;overflow-wrap:anywhere}
.rd-privacy{margin:8px 0 0;color:var(--faint);font-size:12px}
.rd-awards{margin-top:10px}
@media(max-width:900px){.request-tip{display:none}}
.mobile-item.row-highlight{border-color:#6366f1;box-shadow:0 0 0 2px rgba(99,102,241,.22)}
.form-note{margin-top:6px;color:var(--faint);font-size:12px;line-height:1.6}
.position-seat{margin-left:6px;vertical-align:middle}
.section-note{
  position:relative;margin:0 0 18px;padding:12px 16px;
  background:#f8fafc;border-radius:12px;color:var(--muted);font-size:12.5px;line-height:1.75;
}
.section-note::before{
  content:'';position:absolute;left:0;top:12px;bottom:12px;width:3px;border-radius:3px;
  background:linear-gradient(180deg,#818cf8,#22d3ee);
}

.team-tabs :deep(.el-table){
  border-radius:16px;overflow:hidden;background:transparent;
  --el-table-border-color:rgba(226,232,240,.6);
  --el-table-row-hover-bg-color:#f7f9ff;
}
.team-tabs :deep(.el-table th.el-table__cell){
  height:50px;background:#f8fafc;border-bottom:1px solid var(--line);
  color:#64748b;font-size:13px;font-weight:600;
}
.team-tabs :deep(.el-table td.el-table__cell){padding:13px 0;border-bottom:1px solid #f2f4f9;color:var(--ink-2);font-size:13.5px}
.team-tabs :deep(.el-table__inner-wrapper::before){display:none}
.team-tabs :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell){background:#fcfdff}
.team-tabs :deep(.el-table .el-button.is-link){height:30px;padding:0 12px;border-radius:9px;font-weight:600;background:#f1f5f9;color:#475569}
.team-tabs :deep(.el-table .el-button.is-link:hover){filter:brightness(.965)}
.team-tabs :deep(.el-table .el-button.is-link.el-button--primary){background:#eef2ff;color:#4f46e5}
.team-tabs :deep(.el-table .el-button.is-link.el-button--success){background:#e8f8ee;color:#15a34a}
.team-tabs :deep(.el-table .el-button.is-link.el-button--danger){background:#fdecec;color:#e11d48}
.team-tabs :deep(.el-table .el-button.is-link.el-button--warning){background:#fff4e6;color:#d97706}
.team-tabs :deep(.el-empty){padding:34px 0}
.team-tabs :deep(.el-pagination){justify-content:center;margin-top:26px}
.team-tabs :deep(.el-tag){border-radius:999px}

.profile-panel{max-width:920px;margin:0 auto}
.profile-form{padding:2px 0}
.profile-form :deep(.el-form-item){margin-bottom:20px}
.profile-form :deep(.el-form-item__label){font-weight:600;color:var(--ink-2);padding-bottom:8px}
.profile-summary{
  display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;
  padding:20px 22px;margin-bottom:24px;
  background:linear-gradient(120deg,#eef2ff,#f5f3ff 58%,#ecfeff);
  border:1px solid #e0e7ff;border-radius:18px;
}
.profile-summary strong{font-size:20px;font-weight:700;color:#3730a3;margin-right:10px}
.profile-summary span,.unit{color:var(--muted);font-size:13px}
.profile-summary span{margin-left:8px}
.two-col{display:grid;grid-template-columns:1fr 1fr;gap:16px 20px}
.privacy-options{
  display:flex;align-items:center;flex-wrap:wrap;gap:16px;
  padding:18px 20px;margin-bottom:20px;
  background:#f8fafc;border:1px solid var(--line);border-radius:16px;
}
.privacy-options strong{font-size:13.5px;color:var(--ink-2)}
.privacy-options p{width:100%;margin:0;color:var(--faint);font-size:12.5px}

.detail-head{justify-content:flex-start;margin-bottom:14px}
.drawer-desc{margin:0 0 18px;color:var(--muted);font-size:13.5px;line-height:1.75}
.drawer-section{margin-top:26px;padding-top:22px;border-top:1px dashed var(--line)}
.team-page h3{display:flex;align-items:center;gap:9px;margin:0 0 14px;font-size:15px;font-weight:700;color:var(--ink)}
.team-page h3::before{content:'';width:3px;height:15px;border-radius:3px;background:linear-gradient(180deg,#6366f1,#22d3ee)}
.position-item{
  display:flex;align-items:flex-start;justify-content:space-between;gap:16px;
  padding:16px 18px;margin-bottom:10px;
  background:#fff;border:1px solid var(--line);border-radius:14px;
  transition:border-color .2s ease,box-shadow .2s ease;
}
.position-item:hover{border-color:#c7d2fe;box-shadow:0 18px 36px -30px rgba(79,70,229,.6)}
.position-item strong{font-size:14.5px;color:var(--ink)}
.position-item span{display:block;margin-top:6px;color:var(--muted);font-size:12.5px}
.position-item p{margin:8px 0 0;color:var(--faint);font-size:12.5px;line-height:1.7}
.position-actions{display:flex;flex-wrap:wrap;gap:8px;justify-content:flex-end;flex-shrink:0}
.leader-actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:26px;padding-top:20px;border-top:1px dashed var(--line)}
.leader-actions :deep(.el-button){margin-left:0}
.candidate-list{margin-top:26px;padding-top:22px;border-top:1px dashed var(--line)}
.candidate-item{
  padding:16px 18px;margin:12px 0;
  background:#fff;border:1px solid var(--line);border-radius:16px;
  transition:border-color .2s ease,box-shadow .2s ease;
}
.candidate-item:hover{border-color:#c7d2fe;box-shadow:0 18px 36px -30px rgba(79,70,229,.6)}
.candidate-item strong,.candidate-item>span{display:block}
.candidate-item strong{font-size:14.5px;color:var(--ink)}
.candidate-item>span{margin-top:6px;color:var(--muted);font-size:13px}
.candidate-actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}

.position-editor{margin-top:6px;padding:16px;background:#f8fafc;border:1px solid var(--line);border-radius:16px}
.position-head{margin-bottom:12px}
.position-head strong{font-size:14px;color:var(--ink-2)}
.position-row{display:grid;grid-template-columns:1fr 100px 1.4fr 40px;gap:10px;align-items:center;margin-top:10px}

.mobile-list{display:none}
.mobile-item{
  display:grid;gap:10px;padding:16px;
  background:#fff;border:1px solid var(--line);border-radius:16px;
  box-shadow:0 16px 34px -32px rgba(30,41,59,.7);
}
.mobile-item strong{font-size:15px;color:var(--ink)}
.mobile-item span{color:var(--muted);font-size:13px;line-height:1.6}
.mobile-item>div{display:flex;flex-wrap:wrap;gap:8px;justify-content:flex-end}

@media(max-width:1200px){
  .team-grid{grid-template-columns:repeat(auto-fill,minmax(300px,1fr))}
  .filter-grid{grid-template-columns:repeat(3,minmax(0,1fr))}
}
@media(max-width:900px){
  .team-shell{padding:24px 18px 56px}
  .page-head{padding:22px}
  .team-tabs{padding:4px 20px 24px;border-radius:20px}
  .filter-grid{grid-template-columns:repeat(2,minmax(0,1fr))}
  .team-grid{grid-template-columns:1fr}
  .request-toolbar .request-view{margin-left:0}
}
@media(max-width:640px){
  .team-shell{padding:16px 12px 48px}
  .page-head{flex-direction:column;align-items:flex-start;gap:16px;padding:20px 18px;border-radius:18px}
  .page-head-copy{padding-left:14px}
  .page-head h1{font-size:22px}
  .page-head p{display:none}
  .head-cta{width:100%;justify-content:center}
  .team-tabs{padding:0 12px 20px;border-radius:18px}
  .team-tabs :deep(.el-tabs__nav-wrap.is-scrollable){padding:0}
  .team-tabs :deep(.el-tabs__nav-prev),.team-tabs :deep(.el-tabs__nav-next){display:none}
  .team-tabs :deep(.el-tabs__nav-scroll){overflow:visible}
  .team-tabs :deep(.el-tabs__nav){display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr));width:100%;transform:none!important}
  .team-tabs :deep(.el-tabs__item){justify-content:center;height:48px;padding:0 6px;font-size:13.5px}
  .team-tabs :deep(.el-tabs__active-bar){display:none}
  .team-tabs :deep(.el-tabs__item.is-active){box-shadow:inset 0 -2px var(--brand)}
  .team-grid{grid-template-columns:1fr}
  .filter-grid{grid-template-columns:1fr}
  .filters{padding:14px;border-radius:16px}
  .filter-actions{justify-content:stretch}
  .filter-actions :deep(.el-button){flex:1;min-width:0}
  .two-col{grid-template-columns:1fr}
  .profile-summary{display:block}
  .profile-summary>div:last-child{margin-top:8px}
  .position-row{grid-template-columns:1fr 90px}
  .position-row :nth-child(3){grid-column:1/-1}
  .page-head :deep(.el-button){flex-shrink:0}
  .desktop-table{display:none}
  .mobile-list{display:grid;gap:12px}
  .request-toolbar{padding:12px;gap:10px}
  .request-toolbar :deep(.el-segmented){width:100%}
  .request-toolbar .request-view{margin-left:0}
  .section-tools{flex-wrap:wrap;gap:10px}
  .position-item{flex-direction:column;align-items:flex-start;gap:12px}
  .position-actions{justify-content:flex-start}
  .leader-actions{gap:8px}
  .team-page h3{font-size:14.5px}
  .result-count{width:100%;justify-content:center}
}
</style>

<style>
.team-dialog{border-radius:20px;overflow:hidden;box-shadow:0 40px 90px -50px rgba(15,23,42,.75)}
.team-dialog .el-dialog__header{padding:24px 26px 4px;margin-right:0}
.team-dialog .el-dialog__title{font-size:17px;font-weight:700;color:#1e293b}
.team-dialog .el-dialog__headerbtn{top:16px;right:14px}
.team-dialog .el-dialog__body{padding:14px 26px 4px;color:#475569;font-size:13.5px;line-height:1.78}
.team-dialog .el-dialog__body>p{margin:0 0 12px}
.team-dialog .el-dialog__footer{padding:16px 26px 22px;margin-top:16px;border-top:1px solid #f1f4f9}
.team-dialog .el-form-item__label{font-weight:600;color:#475569}
.team-dialog .el-button{border-radius:11px}
.team-dialog .el-button--primary{border:0;background:linear-gradient(135deg,#6366f1,#818cf8);box-shadow:0 10px 22px -14px rgba(99,102,241,.95)}
.team-dialog .el-input__wrapper,.team-dialog .el-textarea__inner,.team-dialog .el-select__wrapper{border-radius:12px}
.team-dialog .position-editor{margin-top:8px}
.team-dialog h3{display:flex;align-items:center;gap:9px;margin:18px 0 12px;font-size:15px;font-weight:700;color:#1e293b}
.team-dialog h3::before{content:'';width:3px;height:15px;border-radius:3px;background:linear-gradient(180deg,#6366f1,#22d3ee)}

.team-drawer .el-drawer__header{padding:22px 26px 14px;margin-bottom:0;border-bottom:1px solid #eef2f7;color:#1e293b;font-size:17px;font-weight:700}
.team-drawer .el-drawer__body{padding:22px 26px 34px}
.team-drawer .el-button{border-radius:11px}
.team-drawer .el-descriptions__label{color:#64748b;font-weight:600;background:#f8fafc}
.team-drawer .el-descriptions__content{color:#475569}

@media(max-width:640px){
  .team-dialog .el-dialog__header{padding:20px 18px 0}
  .team-dialog .el-dialog__body{padding:12px 18px 0}
  .team-dialog .el-dialog__footer{padding:14px 18px 18px}
  .team-drawer .el-drawer__header{padding:18px 16px 12px}
  .team-drawer .el-drawer__body{padding:16px 16px 28px}
}
</style>