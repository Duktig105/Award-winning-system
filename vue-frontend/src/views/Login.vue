<template>
  <div class="login-wrapper">
    <div class="login-container">
      <div class="login-card glass-effect">
        <!-- 卡片内部微妙的装饰元素 -->
        <div class="card-decoration top-left"></div>
        <div class="card-decoration bottom-right"></div>
        <div class="card-decoration line-top"></div>
        
        <div class="login-header">
          <div class="system-logo">
            <el-icon class="logo-inner"><Trophy /></el-icon>
          </div>
          <h2 class="login-title">学生竞赛获奖信息管理系统</h2>
          <p class="login-subtitle">Student Award Information Management System</p>
        </div>
        
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <el-form-item prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入学号/工号"
              size="large"
              prefix-icon="User"
              clearable
              class="custom-input"
            />
          </el-form-item>
          <el-form-item prop="password" class="mt-4">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
              class="custom-input"
            />
          </el-form-item>
          <el-form-item class="submit-item">
            <el-button 
              type="primary" 
              size="large" 
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登 录 中 ...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-footer">
          <p class="tips">忘记密码？<span>请联系指导老师</span></p>
        </div>
      </div>

      <!-- 卡片外部底部的防伪认证 -->
      <div class="outside-footer">
        <p>学生竞赛信息管理系统 &copy; 2026 | 设计：give me five</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from "@/utils/request.js";
import { Trophy } from '@element-plus/icons-vue'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = ref({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = () => {
  loginFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      request.post("/user/login", loginForm.value).then(res => {
        if (res.code === '200') {
          localStorage.setItem("saims_token", res.data.token)
          localStorage.setItem("saims_user", JSON.stringify(res.data.user || {}))
          
          ElMessage.success('登录成功')
          
          // 根据角色跳转
          const role = res.data.user?.role
          if (role && role.includes('mentor')) {
            // 导师跳转到审核页面
            router.push('/admin/review')
          } else if (role && role.includes('student')) {
            // 学生（包括学生兼管理员）跳转到申请填写页面
            router.push('/student/award')
          } else {
            router.push('/student/award')
          }
        } else {
          ElMessage.error(res.msg)
        }
      }).catch(error => {
        console.error('登录失败:', error)
        ElMessage.error('网络连接失败，请检查网络设置')
      }).finally(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style scoped>
/* 整个页面的外层包装，使用明亮/蓝紫浅色调渐变背景 */
.login-wrapper {
  position: relative;
  width: 100%;
  min-height: 100vh;
  /* background 交给 App.vue 处理 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 480px;
  padding: 0 20px;
}

/* 毛玻璃质感的登录卡片 - 明亮系 */
.glass-effect {
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  padding: 45px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.glass-effect:hover {
  transform: translateY(-5px);
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.15);
}

/* 内部极其细微的建筑/科技感装饰线条与角标 */
.card-decoration {
  position: absolute;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.4), rgba(59, 130, 246, 0.4));
  pointer-events: none;
}

/* 左上角折角点缀 */
.top-left {
  top: 15px;
  left: 15px;
  width: 20px;
  height: 20px;
  border-top: 2px solid rgba(139, 92, 246, 0.25);
  border-left: 2px solid rgba(139, 92, 246, 0.25);
  background: transparent;
}

/* 右下角折角点缀 */
.bottom-right {
  bottom: 15px;
  right: 15px;
  width: 20px;
  height: 20px;
  border-bottom: 2px solid rgba(96, 165, 250, 0.25);
  border-right: 2px solid rgba(96, 165, 250, 0.25);
  background: transparent;
}

/* 顶部细微发光带 */
.line-top {
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(168, 85, 247, 0.5), rgba(59, 130, 246, 0.5), transparent);
  border-radius: 0 0 4px 4px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.system-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #60a5fa, #a855f7); /* 蓝紫渐变 */
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 25px rgba(168, 85, 247, 0.3);
}

.logo-inner {
  color: #fff;
  font-size: 32px;
}

.login-title {
  color: #1e293b;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.login-subtitle {
  color: #64748b;
  font-size: 13px;
  margin: 0;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.login-form {
  margin-bottom: 30px;
}

/* 深度定制 Element Plus 输入框以匹配明亮毛玻璃风格 */
:deep(.custom-input .el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.8) inset !important;
  border-radius: 12px !important;
  padding: 8px 15px;
  transition: all 0.3s ease;
}

:deep(.custom-input .el-input__wrapper.is-focus) {
  background-color: rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 0 0 2px #a855f7 inset, 0 0 10px rgba(168, 85, 247, 0.2) !important;
}

:deep(.custom-input .el-input__inner) {
  color: #1e293b !important;
  height: 40px;
  padding-left: 5px;
}

:deep(.custom-input .el-input__inner::placeholder) {
  color: #94a3b8 !important;
}

:deep(.custom-input .el-icon) {
  color: #64748b !important;
  font-size: 18px;
}

.mt-4 {
  margin-top: 24px;
}

.submit-item {
  margin-top: 40px;
  margin-bottom: 0;
}

/* 蓝紫渐变按钮设计 */
.login-button {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  color: white;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border: none;
  box-shadow: 0 10px 20px -10px rgba(139, 92, 246, 0.6);
  transition: all 0.3s ease;
}

.login-button:hover, .login-button:focus {
  background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 100%);
  box-shadow: 0 15px 25px -10px rgba(139, 92, 246, 0.8);
  transform: translateY(-2px);
  color: white;
}

.login-button:active {
  transform: translateY(1px);
}

.login-footer {
  text-align: center;
  border-top: 1px solid rgba(0, 0, 0, 0.05); /* 适配浅色 */
  padding-top: 20px;
}

.tips {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

.tips span {
  color: #8b5cf6; /* 紫色提示文字 */
  cursor: pointer;
  transition: color 0.3s ease;
  margin-left: 4px;
}

.tips span:hover {
  color: #a78bfa;
  text-decoration: underline;
}

/* 适配移动端 */
@media screen and (max-width: 480px) {
  .glass-effect {
    padding: 35px 25px;
    border-radius: 20px;
  }
  .login-title {
    font-size: 20px;
  }
}

/* 外部底部的防伪认证样式 */
.outside-footer {
  margin-top: 25px;
  text-align: center;
  animation: fadeIn 1.5s ease-out;
}

.outside-footer p {
  color: #64748b;
  font-size: 12px;
  letter-spacing: 1px;
  opacity: 0.7;
  font-family: 'Inter', sans-serif;
  margin: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 0.7; transform: translateY(0); }
}
</style>
