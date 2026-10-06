<template>
  <div class="navbar">
    <div class="navbar-container">
      <div class="navbar-left" @click="router.push('/')">
        <div class="logo">
          <div class="logo-icon-wrapper">
            <el-icon class="logo-icon"><Trophy /></el-icon>
          </div>
          <span class="logo-text">学生获奖系统</span>
        </div>
      </div>
      
      <div class="navbar-center desktop-menu">
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/student/award" v-if="hasRole('student')">
            <span>申请填写</span>
          </el-menu-item>
          <el-menu-item index="/student/record" v-if="hasRole('student')">
            <span>申请记录</span>
          </el-menu-item>
          <el-menu-item index="/student/teams" v-if="hasRole('student')">
            <span>竞赛组队</span>
          </el-menu-item>
          <el-menu-item index="/student/honor" v-if="hasRole('student')">
            <span>荣誉中心</span>
          </el-menu-item>
          <el-sub-menu
            v-if="hasRole('admin') || hasRole('mentor')"
            index="admin-center"
            popper-class="admin-nav-submenu"
          >
            <template #title>
              <span>管理中心</span>
              <el-icon class="admin-menu-arrow"><ArrowDown /></el-icon>
            </template>
            <el-menu-item index="/admin/review">审核管理</el-menu-item>
            <el-menu-item index="/admin/statistics">数据统计</el-menu-item>
            <el-menu-item index="/admin/competition">竞赛管理</el-menu-item>
            <el-menu-item index="/admin/honor">荣誉管理</el-menu-item>
            <el-menu-item index="/admin/student">学生管理</el-menu-item>
            <el-menu-item index="/admin/duplicate">证书查重</el-menu-item>
            <el-menu-item index="/admin/risk-rule" v-if="hasRole('admin')">风险规则</el-menu-item>
            <el-menu-item index="/admin/student-import" v-if="hasRole('admin')">学生导入</el-menu-item>
            <el-menu-item index="/admin/teams" v-if="hasRole('admin')">组队治理</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
      
      <div class="navbar-right">
        <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99" class="notification-badge">
          <el-button circle text title="通知中心" @click="router.push('/notifications')">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
        <el-dropdown @command="handleCommand" class="premium-dropdown-trigger">
          <span class="user-dropdown">
            <div class="user-avatar" :style="{ background: userAvatarBg }">
              {{ userInitial }}
            </div>
            <span class="username">{{ userInfo?.name || userInfo?.username }}</span>
            <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled class="user-info-item">
                <div class="user-info">
                  <p class="user-detail">学号：{{ userInfo?.username }}</p>
                  <p class="role-tags">
                    <el-tag v-for="role in getRoleArray()" :key="role" size="small" effect="light" round>
                      {{ getRoleText(role) }}
                    </el-tag>
                  </p>
                </div>
              </el-dropdown-item>
              <el-dropdown-item divided command="changePassword" class="action-item">
                <el-icon><Lock /></el-icon>
                <span>修改密码</span>
              </el-dropdown-item>
              <el-dropdown-item command="logout" class="logout-item action-item">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <!-- 移动端汉堡菜单按钮 -->
        <div class="mobile-menu-btn" @click="drawerVisible = true">
          <el-icon><Expand /></el-icon>
        </div>
      </div>
    </div>
    
    <!-- 修改密码对话框 -->
    <el-dialog 
      v-model="passwordDialogVisible" 
      title="修改密码" 
      width="500px"
      append-to-body
      class="premium-dialog"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="95px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
          <div class="password-hint">密码需由字母和数字组合构成，长度不少于 8 位</div>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动端侧边抽屉菜单 -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      size="240px"
      append-to-body
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-header">
        <div class="logo-icon-wrapper">
          <el-icon class="logo-icon"><Trophy /></el-icon>
        </div>
        <span class="logo-text">获奖系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="mobile-el-menu"
        @select="handleMobileMenuSelect"
      >
        <el-menu-item index="/student/award" v-if="hasRole('student')">
          <div class="mobile-menu-inner">
            <span class="mobile-menu-text">申请填写</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/student/record" v-if="hasRole('student')">
          <div class="mobile-menu-inner">
            <span class="mobile-menu-text">申请记录</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/student/teams" v-if="hasRole('student')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">竞赛组队</span></div>
        </el-menu-item>
        <el-menu-item index="/student/honor" v-if="hasRole('student')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">荣誉中心</span></div>
        </el-menu-item>
        <el-menu-item index="/admin/review" v-if="hasRole('admin') || hasRole('mentor')">
          <div class="mobile-menu-inner">
            <span class="mobile-menu-text">审核管理</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/admin/statistics" v-if="hasRole('admin') || hasRole('mentor')">
          <div class="mobile-menu-inner">
            <span class="mobile-menu-text">数据统计</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/admin/competition" v-if="hasRole('admin') || hasRole('mentor')">
          <div class="mobile-menu-inner">
            <span class="mobile-menu-text">竞赛管理</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/admin/honor" v-if="hasRole('admin') || hasRole('mentor')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">荣誉管理</span></div>
        </el-menu-item>
        <el-menu-item index="/admin/student" v-if="hasRole('admin') || hasRole('mentor')">
          <div class="mobile-menu-inner">
            <span class="mobile-menu-text">学生管理</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/admin/duplicate" v-if="hasRole('admin') || hasRole('mentor')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">证书查重</span></div>
        </el-menu-item>
        <el-menu-item index="/admin/risk-rule" v-if="hasRole('admin')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">风险规则</span></div>
        </el-menu-item>
        <el-menu-item index="/admin/student-import" v-if="hasRole('admin')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">学生导入</span></div>
        </el-menu-item>
        <el-menu-item index="/admin/teams" v-if="hasRole('admin')">
          <div class="mobile-menu-inner"><span class="mobile-menu-text">组队治理</span></div>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { Trophy, User, ArrowDown, Lock, SwitchButton, Expand, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const userInfo = ref(null)
const activeMenu = ref('')
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const drawerVisible = ref(false)
const unreadCount = ref(0)

const handleMobileMenuSelect = (index) => {
  drawerVisible.value = false
  router.push(index)
}

const userInitial = computed(() => {
  const name = userInfo.value?.name || userInfo.value?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const userAvatarBg = computed(() => {
  const name = userInfo.value?.username || 'default'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  const hue1 = Math.abs(hash) % 360
  const hue2 = (hue1 + 40) % 360
  return `linear-gradient(135deg, hsl(${hue1}, 85%, 65%) 0%, hsl(${hue2}, 85%, 55%) 100%)`
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度不能少于8位'))
  } else if (!/(?=.*[A-Za-z])(?=.*\d)/.test(value)) {
    callback(new Error('密码需由字母和数字组合构成'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(async () => {
  const user = localStorage.getItem('saims_user')
  userInfo.value = user ? JSON.parse(user) : null
  activeMenu.value = route.path
  try {
    const res = await request.get('/api/notifications/unread-count')
    if (res.code === '200') unreadCount.value = Number(res.data || 0)
  } catch (_) {}
})

const hasRole = (role) => {
  if (!userInfo.value || !userInfo.value.role) return false
  return userInfo.value.role.includes(role)
}

const getRoleArray = () => {
  if (!userInfo.value || !userInfo.value.role) return []
  return userInfo.value.role.split(',').map(r => r.trim())
}

const getRoleText = (role) => {
  const roleMap = {
    'student': '学生',
    'mentor': '导师',
    'admin': '管理员'
  }
  return roleMap[role] || role
}

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'changePassword') {
    passwordDialogVisible.value = true
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'premium-message-box'
    })
    
    await request.post('/user/logout')
    localStorage.removeItem('saims_user')
    localStorage.removeItem('saims_token')
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
    }
  }
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.post('/user/changePassword', {
          username: userInfo.value.username,
          ...passwordForm.value
        })
        ElMessage.success('密码修改成功，请重新登录')
        passwordDialogVisible.value = false
        
        setTimeout(() => {
          localStorage.removeItem('saims_user')
          localStorage.removeItem('saims_token')
          router.push('/login')
        }, 1500)
      } catch (error) {
        console.error('修改密码失败:', error)
      }
    }
  })
}
</script>

<style scoped>
.navbar {
  /* 基础玻璃态与渐变微小叠加层 */
  background: rgba(255, 255, 255, 0.55);
  /* 以 60x60 的极简宽阔网格，辅佐一侧的流光亮斑，打造科技与极简之美，同时去除密集的波点感 */
  background-image: 
    linear-gradient(rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    radial-gradient(circle at 10% 50%, rgba(147, 51, 234, 0.05) 0%, transparent 40%);
  background-size: 60px 60px, 60px 60px, 100% 100%;
  background-position: 0 0, 0 0, 0 0;
  backdrop-filter: blur(24px) saturate(200%);
  -webkit-backdrop-filter: blur(24px) saturate(200%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 12px 48px -12px rgba(0, 0, 0, 0.08), inset 0 -1px 0 rgba(255, 255, 255, 0.5);
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 增加整体高度获得史诗级呼吸空间 */
.navbar-container {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 90px; /* 从 76px 进一步增大到 84px */
}

.navbar-left {
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.navbar-left:hover {
  transform: scale(1.02);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo-icon-wrapper {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #4f46e5 0%, #9333ea 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px -6px rgba(79, 70, 229, 0.5), inset 0 2px 4px rgba(255,255,255,0.3);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}

.logo:hover .logo-icon-wrapper {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 24px -8px rgba(79, 70, 229, 0.7), inset 0 2px 4px rgba(255,255,255,0.4);
}

.logo-icon {
  font-size: 22px;
  color: white;
}

.logo-text {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'Inter', system-ui, sans-serif;
  transition: background 0.3s ease;
}

.navbar-center {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: center;
  padding: 0 40px;
}

.navbar-right {
  flex-shrink: 0;
  min-width: max-content;
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.navbar-center .el-sub-menu__icon-arrow) {
  display: none;
}

.admin-menu-arrow {
  position: static;
  margin-left: 8px;
  font-size: 13px;
  color: #94a3b8;
  transition: transform 0.25s ease, color 0.25s ease;
}

:deep(.el-sub-menu:hover) .admin-menu-arrow,
:deep(.el-sub-menu.is-active) .admin-menu-arrow {
  color: #4f46e5;
}

:deep(.el-sub-menu.is-opened) .admin-menu-arrow {
  transform: rotate(180deg);
}

.mobile-menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.8);
  color: #1e293b;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.mobile-menu-btn:active {
  background: rgba(255, 255, 255, 0.8);
  transform: scale(0.95);
}

/* 抽屉样式 */
.drawer-header {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: 10px;
}
.drawer-header .logo-text {
  font-size: 18px;
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 700;
}

/* 抽屉弹出层底层修复，解决层叠背景污染与卡顿 */
:global(.mobile-drawer),
:global(.mobile-drawer .el-drawer),
:global(.mobile-drawer .el-drawer__body),
:global(.mobile-drawer .el-drawer__header) {
  background: #f8fafc !important; /* 强制使用不透明纯色替代渐变和玻璃态，彻底消灭渲染重叠 */
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

:global(.mobile-drawer.el-drawer__wrapper) {
  background-color: rgba(0, 0, 0, 0.4) !important; /* 加深蒙版遮挡底部 */
}

.mobile-el-menu {
  border-right: none !important;
  background: transparent !important;
}

/* 彻底覆盖和接管 el-menu-item 的原生布局 */
:deep(.mobile-el-menu .el-menu-item) {
  height: auto !important;
  line-height: normal !important;
  margin: 0 !important;
  padding: 0 !important;
  background: transparent !important;
  border: none !important;
}

:deep(.mobile-el-menu .el-menu-item.is-active) {
  background: transparent !important;
}

/* 移除原生伪类 */
:deep(.mobile-el-menu .el-menu-item::after),
:deep(.mobile-el-menu .el-menu-item::before) {
  display: none !important;
}

/* 用自定义盒子接管所有的交互视觉和排版 */
.mobile-menu-inner {
  width: calc(100% - 32px);
  margin: 8px 16px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 16px;
  transition: all 0.3s ease;
}

:deep(.mobile-el-menu .el-menu-item:hover) .mobile-menu-inner {
  background: rgba(241, 245, 249, 0.8);
  color: #0f172a;
}

:deep(.mobile-el-menu .el-menu-item.is-active) .mobile-menu-inner {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08) 0%, rgba(147, 51, 234, 0.04) 100%);
  color: #4f46e5;
  font-weight: 600;
}

.mobile-menu-text {
  position: relative;
  display: inline-block;
}

/* 自定义底部划线，纯粹依附于文本盒子内部居中 */
.mobile-menu-text::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  height: 3px;
  width: 24px;
  background: linear-gradient(90deg, #4f46e5, #9333ea);
  border-radius: 3px;
  transition: transform 0.3s ease;
}

:deep(.mobile-el-menu .el-menu-item:hover) .mobile-menu-text::after {
  transform: translateX(-50%) scaleX(0.5);
}

:deep(.mobile-el-menu .el-menu-item.is-active) .mobile-menu-text::after {
  transform: translateX(-50%) scaleX(1);
}

/* 响应式媒体查询 */
@media screen and (max-width: 768px) {
  .desktop-menu {
    display: none !important;
  }
  
  .mobile-menu-btn {
    display: flex;
  }
  
  .navbar-container {
    padding: 0 16px;
    height: 64px;
  }
  
  .logo-text {
    font-size: 18px;
  }
  
  .logo-icon-wrapper {
    width: 32px;
    height: 32px;
  }
  .logo-icon {
    font-size: 18px;
  }
  
  .username {
    display: none;
  }
  .user-dropdown {
    padding: 4px;
  }
  
  :deep(.el-menu-item) {
    height: 64px !important;
    line-height: 64px !important;
  }
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 16px 6px 6px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 24px -6px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255,255,255,1);
  transform: translateY(-1px);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 16px;
  box-shadow: 0 4px 10px -2px rgba(0,0,0,0.15), inset 0 2px 4px rgba(255,255,255,0.4);
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.username {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.2px;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  color: #94a3b8;
  font-size: 14px;
  transition: transform 0.3s ease;
}

.premium-dropdown-trigger[aria-expanded="true"] .dropdown-arrow {
  transform: rotate(180deg);
}

.user-info {
  padding: 5px 0;
}

.user-info p {
  margin: 5px 0;
  font-size: 14px;
}

.user-detail {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
  margin: 6px 0 !important;
}

.role-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px !important;
}

.el-menu--horizontal {
  background: transparent !important;
  border-bottom: none !important;
  height: 100%;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 90px !important; /* 跟随您设置的全新 90px 导航高度对齐 */
  line-height: 90px !important;
  padding: 0 24px !important;
  font-size: 16px !important;
  font-weight: 500 !important;
  color: #64748b !important;
  border-bottom: none !important;
  background: transparent !important;
  position: relative;
  transition: color 0.3s ease !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  color: #0f172a !important;
  background: transparent !important;
}

:deep(.el-menu-item.is-active),
:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #4f46e5 !important;
  font-weight: 600 !important;
  background: transparent !important;
}

/* 丝滑中心扩散发光下划线 */
:deep(.el-menu-item::after),
:deep(.el-sub-menu__title::after) {
  content: '';
  position: absolute;
  bottom: 24px; /* 适应 90px 超宽高度完美底距 */
  left: 50%;
  height: 4px;
  width: 44px; /* 与大空间匹配的指示线宽 */
  background: linear-gradient(135deg, #4f46e5, #9333ea);
  border-radius: 4px;
  transform: translateX(-50%) scaleX(0);
  transform-origin: center;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
  opacity: 0;
  box-shadow: 0 4px 12px -2px rgba(79, 70, 229, 0.5);
}

:deep(.el-menu-item:hover::after),
:deep(.el-sub-menu__title:hover::after) {
  transform: translateX(-50%) scaleX(0.6);
  opacity: 0.5;
}

:deep(.el-menu-item.is-active::after),
:deep(.el-sub-menu.is-active > .el-sub-menu__title::after) {
  transform: translateX(-50%) scaleX(1);
  opacity: 1;
}

@media screen and (max-width: 1100px) and (min-width: 769px) {
  .navbar-container { padding: 0 20px; }
  .navbar-center { padding: 0 16px; }
  .logo-text { display: none; }
  :deep(.el-menu-item), :deep(.el-sub-menu__title) { padding: 0 16px !important; }
}

</style>

<style>
/* 管理入口收纳菜单，与顶部导航的玻璃态视觉保持一致 */
.admin-nav-submenu.el-popper {
  border: 1px solid rgba(255, 255, 255, 0.85) !important;
  border-radius: 14px !important;
  background: rgba(255, 255, 255, 0.96) !important;
  box-shadow: 0 14px 32px -10px rgba(15, 23, 42, 0.2) !important;
  overflow: hidden;
}
.admin-nav-submenu .el-menu {
  min-width: 156px !important;
  padding: 8px !important;
  border: 0 !important;
  background: transparent !important;
}
.admin-nav-submenu .el-menu-item {
  height: 42px !important;
  line-height: 42px !important;
  margin: 2px 0 !important;
  padding: 0 16px !important;
  border-radius: 9px !important;
  color: #475569 !important;
  font-size: 14px !important;
}
.admin-nav-submenu .el-menu-item:hover {
  color: #4f46e5 !important;
  background: #eef2ff !important;
}
.admin-nav-submenu .el-menu-item.is-active {
  color: #4f46e5 !important;
  font-weight: 600 !important;
  background: #eef2ff !important;
}

/* 下拉菜单全局高级样式 */
.el-dropdown__popper.el-popper {
  border: none !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1) !important;
  border-radius: 16px !important;
}

.user-info-item {
  cursor: default !important;
  opacity: 1 !important;
  padding: 12px 20px !important;
  background: transparent !important;
}

.el-dropdown-menu__item.action-item {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  gap: 8px !important;
  padding: 12px 20px !important;
  font-size: 14px !important;
  color: #475569 !important;
  transition: all 0.2s ease;
  border-radius: 8px !important;
  margin: 2px 8px !important;
}

.el-dropdown-menu__item.action-item:hover {
  background-color: #f1f5f9 !important;
  color: #6366f1 !important;
}

/* 退出登录默认不红，悬浮时变红，增加 specificity 防止被覆盖 */
.el-dropdown-menu__item.action-item.logout-item:hover {
  background-color: #fee2e2 !important;
  color: #ef4444 !important;
}

/* 修改密码弹窗全局高级样式修正 */
.premium-dialog {
  border-radius: 12px !important;
  overflow: hidden;
  box-shadow: 0 20px 40px -8px rgba(0, 0, 0, 0.1) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
}

.premium-dialog .el-dialog__header {
  padding: 24px 28px 20px !important;
  margin-right: 0 !important;
  text-align: left;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.premium-dialog .el-dialog__title {
  font-size: 18px !important;
  font-weight: 600 !important;
  color: #1e293b !important;
  background: none !important;
  -webkit-text-fill-color: initial !important;
  letter-spacing: normal;
}

.premium-dialog .el-dialog__headerbtn {
  top: 22px !important;
  right: 28px !important;
}

.premium-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #94a3b8 !important;
  font-size: 16px !important;
  transition: all 0.2s ease !important;
}

.premium-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: #0f172a !important;
}

.premium-dialog .el-dialog__body {
  padding: 32px 40px 12px 20px !important;
}

.premium-dialog .el-form-item {
  margin-bottom: 24px !important;
}

.premium-dialog .el-form-item__label {
  font-weight: 500 !important;
  color: #475569 !important;
  font-size: 14px !important;
}

.premium-dialog .el-input__wrapper {
  background-color: rgba(248, 250, 252, 0.8) !important;
  border-radius: 6px !important;
  padding: 4px 12px !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  transition: all 0.2s ease !important;
  width: 100%;
}

.premium-dialog .el-input__wrapper:hover {
  background-color: #fff !important;
  box-shadow: 0 0 0 1px #cbd5e1 inset !important;
}

.premium-dialog .el-input__wrapper.is-focus {
  background-color: #fff !important;
  box-shadow: 0 0 0 2px #3b82f6 inset !important;
}

/* 密码提示文字紧贴下方，且不换行 */
.premium-dialog .password-hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
  line-height: 1.4;
  white-space: nowrap;
}

/* 避免错误信息把表单撑高 */
.premium-dialog .el-form-item__error {
  position: absolute;
  top: 100%;
  left: 0;
  padding-top: 4px;
}

.premium-dialog .el-dialog__footer {
  padding: 16px 28px 24px !important;
  border-top: none !important;
  display: flex !important;
  justify-content: flex-end !important;
  gap: 12px !important;
}

.premium-dialog .el-button {
  border-radius: 6px !important;
  padding: 8px 24px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  border: none !important;
  transition: all 0.2s ease !important;
  margin: 0 !important;
  height: 36px !important;
}

.premium-dialog .el-button--default {
  background: #f1f5f9 !important;
  color: #475569 !important;
}

.premium-dialog .el-button--default:hover {
  background: #e2e8f0 !important;
  color: #0f172a !important;
}

.premium-dialog .el-button--primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25) !important;
}

.premium-dialog .el-button--primary:hover {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%) !important;
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.35) !important;
  transform: translateY(-1px);
}

/* ==================================
   全局 - 退出登录/消息确认框高级版 
================================== */
.premium-message-box {
  border-radius: 16px !important;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.2) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  padding-bottom: 8px !important;
  width: 420px !important;
}

/* 头部 Header */
.premium-message-box .el-message-box__header {
  padding: 24px 24px 12px 24px !important;
}
.premium-message-box .el-message-box__title {
  font-size: 18px !important;
  font-weight: 700 !important;
  color: #1e293b !important;
  justify-content: flex-start !important;
}
.premium-message-box .el-message-box__headerbtn {
  top: 24px !important;
  right: 24px !important;
}
.premium-message-box .el-message-box__headerbtn .el-message-box__close {
  color: #94a3b8 !important;
  font-size: 16px !important;
  transition: all 0.2s ease !important;
}
.premium-message-box .el-message-box__headerbtn:hover .el-message-box__close {
  color: #0f172a !important;
  transform: rotate(90deg);
}

/* 主干内容 Body */
.premium-message-box .el-message-box__content {
  padding: 16px 24px 24px 24px !important;
  font-size: 16px !important;
  color: #334155 !important;
}
.premium-message-box .el-message-box__container {
  align-items: center !important;
}
/* 警告标识Icon美化 */
.premium-message-box .el-message-box__status {
  font-size: 28px !important;
  color: #f59e0b !important; /* 琥珀橘，比默认黄更有质感 */
}
.premium-message-box .el-message-box__message {
  padding-left: 12px !important;
}

/* 底部操作区 Footer */
.premium-message-box .el-message-box__btns {
  padding: 12px 24px 16px 24px !important;
}

/* 按键重塑 */
.premium-message-box .el-message-box__btns .el-button {
  border-radius: 8px !important;
  padding: 8px 24px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  height: 38px !important;
  border: none !important;
  transition: all 0.3s ease !important;
  margin-left: 12px !important;
}
/* 取消按钮 */
.premium-message-box .el-message-box__btns .el-button--default {
  background: #f1f5f9 !important;
  color: #475569 !important;
}
.premium-message-box .el-message-box__btns .el-button--default:hover {
  background: #e2e8f0 !important;
  color: #0f172a !important;
}
/* 确定/警告按钮 */
.premium-message-box .el-message-box__btns .el-button--primary {
  background: linear-gradient(135deg, #f59e0b 0%, #ea580c 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25) !important;
}
.premium-message-box .el-message-box__btns .el-button--primary:hover {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%) !important;
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.35) !important;
  transform: translateY(-1px);
}
</style>
