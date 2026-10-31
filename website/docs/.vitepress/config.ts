import { defineConfig, SiteConfig } from 'vitepress'
import locales from './locales'
import { readdir, writeFile } from 'fs/promises'
import { resolve } from 'path'

export default defineConfig({
    title: 'FoxSu',
    base: '/FoxSu/',
    locales: locales.locales,
    head: [
        ['meta', { property: 'og:type', content: 'website' }],
        ['meta', { property: 'og:title', content: 'FoxSu' }],
        ['meta', { property: 'og:image', content: '/FoxSu/logo.png' }],
        ['meta', {
            property: 'og:description',
            content: 'Root 在内核，体验在管理器。KernelSU 的内核能力，配一套从头重写的管理器。',
        }],
        ['meta', { name: 'theme-color', content: '#fdfbfa' }],
        ['link', { rel: 'icon', href: '/FoxSu/favicon.ico', sizes: 'any' }],
        ['link', { rel: 'apple-touch-icon', href: '/FoxSu/apple-touch-icon.png' }],
    ],
    sitemap: {
        hostname: 'https://cmry-mlc.github.io/FoxSu/',
    },
    // 模块仓库的模板索引：构建结束后把 dist/templates 下的文件列成 index.json
    buildEnd: async (config: SiteConfig) => {
        const templateDir = resolve(config.outDir, 'templates')
        const templateList = resolve(templateDir, 'index.json')
        let files: string[] = []
        try {
            files = await readdir(templateDir)
            files = files.filter(file => !file.startsWith('.') && file !== 'index.json')
        } catch (e) {
            // ignore
        }
        await writeFile(templateList, JSON.stringify(files))
    },
})
