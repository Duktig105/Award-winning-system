# ============================================================
# 后端启动脚本（示例模板）
# ------------------------------------------------------------
# 使用方法：
#   1) 复制本文件为 start_backend.ps1
#        Copy-Item start_backend.sample.ps1 start_backend.ps1
#   2) 按自己的本机环境修改下面的数据库账号密码、JWT 密钥
#   3) 运行：  .\start_backend.ps1
#
# 说明：start_backend.ps1 含本机密钥，已在 .gitignore 中，
#       不会被提交到仓库，请勿手动上传。
# ============================================================

# ---------- 必填：MySQL 账号 ----------
$env:SAIMS_DB_USERNAME = 'root'
$env:SAIMS_DB_PASSWORD = '改成你自己的MySQL密码'

# ---------- 必填：JWT 签名密钥 ----------
# 任意随机字符串即可，建议 32 位以上；修改后已登录用户的 token 会全部失效
$env:SAIMS_JWT_SECRET = '改成你自己的随机字符串至少32位xxxxxxxxxxxxxxxx'

# ---------- 可选：OCR 智能预检（留空则自动转人工审核，不影响启动） ----------
# 如需启用：到百度智能云控制台开通「文字识别」，创建应用后填入下面两项
$env:SAIMS_OCR_PROVIDER   = 'baidu'
$env:SAIMS_OCR_ENDPOINT   = 'https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic'
$env:SAIMS_OCR_API_KEY    = ''
$env:SAIMS_OCR_SECRET_KEY = ''

Set-Location "$PSScriptRoot\springboot-backend"

if (Get-NetTCPConnection -LocalPort 9998 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host '端口 9998 已被占用，请先停止正在运行的后端进程'
    exit 1
}

Write-Host '正在启动后端：http://localhost:9998'
java -jar 'target\springboot-backend-0.0.1-SNAPSHOT.jar'
