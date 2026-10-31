import { createRequire } from 'module'
import { defineConfig } from 'vitepress'

const require = createRequire(import.meta.url)
const pkg = require('vitepress/package.json')

export default defineConfig({
  lang: 'zh-CN',
  description: '安卓上基于内核的 root 方案。内核沿用 KernelSU，管理器整套重写。',

  themeConfig: {
    nav: nav(),

    lastUpdatedText: '最后更新',

    sidebar: {
      '/zh_CN/guide/': sidebarGuide()
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Cmry-mlc/FoxSu' }
    ],

    footer: {
        message: '在 GPL-3.0 许可证下发布。内核能力来自 KernelSU 及其社区。',
        copyright: 'Copyright © 2026 FoxSu 开发者'
    },

    editLink: {
        pattern: 'https://github.com/Cmry-mlc/FoxSu/edit/main/website/docs/:path',
        text: '在 GitHub 中编辑本页'
    }
  }
})

function nav() {
  return [
    { text: '指南', link: '/zh_CN/guide/what-is-kernelsu' },
    { text: '外观', link: '/zh_CN/features' },
    { text: '下载', link: 'https://github.com/Cmry-mlc/FoxSu/releases' },
  ]
}

function sidebarGuide() {
  return [
    {
        text: 'Guide',
        items: [
          { text: '什么是 KernelSU？', link: '/zh_CN/guide/what-is-kernelsu' },
          { text: 'KernelSU 模块与 Magisk 的差异', link: '/zh_CN/guide/difference-with-magisk' },
          { text: '安装', link: '/zh_CN/guide/installation' },
          { text: '如何构建？', link: '/zh_CN/guide/how-to-build' },
          { text: '如何为非 GKI 设备集成 KernelSU', link: '/zh_CN/guide/how-to-integrate-for-non-gki'},
          { text: '非官方支持设备', link: '/zh_CN/guide/unofficially-support-devices.md' },
          { text: '模块开发指南', link: '/zh_CN/guide/module.md' },
          { text: '元模块', link: '/zh_CN/guide/metamodule.md' },
          { text: '模块 Web 界面', link: '/zh_CN/guide/module-webui.md' },
          { text: '模块配置', link: '/zh_CN/guide/module-config.md' },
          { text: '一次性脚本', link: '/zh_CN/guide/module-script.md' },
          { text: 'App Profile', link: '/zh_CN/guide/app-profile.md' },
          { text: '救砖', link: '/zh_CN/guide/rescue-from-bootloop.md' },
          { text: 'x86_64 支持', link: '/zh_CN/guide/x86_64-support' },
          { text: '常见问题', link: '/zh_CN/guide/faq' },
          { text: '隐藏功能', link: '/zh_CN/guide/hidden-features' },
        ]
    }
  ]
}
