/**
 * Vite构建工具配置文件
 * 用于配置Vue3项目的构建、开发服务器、插件等选项
 * Vite是一个现代化的前端构建工具，提供快速的开发体验
 */
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 自动导入API和组件的插件，减少手动import的繁琐
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'

// Element Plus的解析器，用于自动导入Element Plus组件
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

/**
 * Vite配置对象
 * 定义项目的构建配置、插件、路径别名等
 */
export default defineConfig({
  // 插件配置：配置Vite使用的各种插件
  plugins: [
    vue(), // Vue3插件，支持Vue单文件组件
    // 自动导入配置：无需手动import即可使用Vue/Element API
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    // 自动注册组件：无需手动import Element Plus组件
    Components({
      resolvers: [
        // 配置Element Plus组件解析器，指定使用sass样式
        ElementPlusResolver({ importStyle: "sass" })
      ],
    }),
  ],

  // 路径解析配置
  resolve: {
    alias: {
      // 配置@别名指向src目录，简化路径写法
      // 例如：import App from '@/App.vue' 等同于 import App from './src/App.vue'
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
