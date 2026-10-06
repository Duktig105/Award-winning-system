<template>
  <div class="notice-page">
    <NavBar />
    <main class="content-container">
      <section class="notice-card">
        <header class="page-header">
          <div>
            <h1>通知中心</h1>
            <p>{{ unread ? `${unread} 条通知等待查看` : '全部通知均已查看' }}</p>
          </div>
          <el-button class="read-all-btn" type="primary" :icon="CircleCheck" :disabled="!unread" @click="readAll">全部已读</el-button>
        </header>
        <div class="notice-list" v-loading="loading">
          <button v-for="item in list" :key="item.notificationId" class="notice" :class="{ unread: !item.isRead }" @click="open(item)">
            <span class="dot"></span>
            <div class="notice-content"><strong>{{ item.title }}</strong><p>{{ item.content }}</p><time>{{ format(item.createTime) }}</time></div>
            <el-icon class="notice-arrow"><ArrowRight /></el-icon>
          </button>
          <el-empty v-if="!list.length && !loading" description="暂无通知" />
        </div>
      </section>
    </main>
  </div>
</template>
<script setup>
import{onMounted,ref}from'vue';import{useRouter}from'vue-router';import{ArrowRight,CircleCheck}from'@element-plus/icons-vue';import NavBar from'../components/NavBar.vue';import request from'../utils/request';
const router=useRouter(),list=ref([]),unread=ref(0),loading=ref(false);async function load(){loading.value=true;try{const r=await request.get('/api/notifications');if(r.code==='200'){list.value=r.data.list;unread.value=Number(r.data.unread)}}finally{loading.value=false}}async function open(i){if(!i.isRead)await request.post(`/api/notifications/${i.notificationId}/read`);if(i.link)router.push(i.link);else load()}async function readAll(){await request.post('/api/notifications/read-all');load()}const format=v=>new Date(v).toLocaleString('zh-CN');onMounted(load)
</script>
<style scoped>
.notice-page{min-height:100vh;color:#1e293b}.content-container{max-width:1000px;margin:0 auto;padding:40px 20px}.notice-card{overflow:hidden;min-height:360px;background:rgba(255,255,255,.65);backdrop-filter:blur(20px);border:1px solid rgba(255,255,255,.85);border-radius:20px;box-shadow:0 10px 30px -10px rgba(15,23,42,.08)}.page-header{display:flex;justify-content:space-between;align-items:center;padding:26px 30px;border-bottom:1px solid rgba(226,232,240,.7)}h1{font-size:22px;margin:0 0 6px;font-weight:700;letter-spacing:0;color:#1e293b}.page-header p{margin:0;color:#64748b}.read-all-btn{border:0;border-radius:10px;background:linear-gradient(135deg,#6366f1,#818cf8);box-shadow:0 4px 12px -2px rgba(99,102,241,.28)}.notice-list{min-height:250px}.notice{width:100%;display:grid;grid-template-columns:10px minmax(0,1fr) 24px;text-align:left;gap:14px;align-items:center;padding:20px 28px;border:0;border-bottom:1px solid rgba(226,232,240,.65);background:rgba(255,255,255,.3);cursor:pointer;transition:background .2s ease}.notice:hover{background:rgba(248,250,252,.9)}.notice.unread{background:rgba(238,242,255,.65)}.dot{width:8px;height:8px;border-radius:50%;background:transparent}.unread .dot{background:#6366f1;box-shadow:0 0 0 4px rgba(99,102,241,.1)}.notice-content{min-width:0}.notice strong{font-size:15px;color:#334155}.notice p{margin:6px 0;color:#64748b;line-height:1.6;overflow-wrap:anywhere}.notice time{font-size:12px;color:#94a3b8}.notice-arrow{color:#94a3b8}@media(max-width:640px){.content-container{padding:18px 12px}.notice-card{border-radius:16px}.page-header{align-items:flex-start;padding:20px;gap:12px}.read-all-btn{padding:8px 12px}.notice{padding:17px 16px;gap:10px}}
</style>
