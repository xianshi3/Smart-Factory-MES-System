import { describe, it, expect } from 'vitest'
import { mdToHtml } from '@/utils/markdown'

describe('mdToHtml', () => {
  it('转义 HTML 特殊字符，防止 XSS', () => {
    const html = mdToHtml('<script>alert(1)</script>')
    expect(html).not.toContain('<script>')
    expect(html).toContain('&lt;script&gt;')
  })

  it('标题按层级渲染（#→h2，####→h5）', () => {
    const html = mdToHtml('# 一级\n## 二级\n### 三级\n#### 四级\n##### 五级')
    expect(html).toContain('<h2>一级</h2>')
    expect(html).toContain('<h3>二级</h3>')
    expect(html).toContain('<h4>三级</h4>')
    expect(html).toContain('<h5>四级</h5>')
    expect(html).toContain('<h6>五级</h6>')
  })

  it('渲染加粗与行内代码', () => {
    const html = mdToHtml('**重要** 使用 `code` 命令')
    expect(html).toContain('<strong>重要</strong>')
    expect(html).toContain('<code>code</code>')
  })

  it('渲染有序列表为 <ol>', () => {
    const html = mdToHtml('1. 第一\n2. 第二')
    expect(html).toContain('<ol>')
    expect(html).toContain('<li>第一</li>')
    expect(html).not.toContain('<ul>')
  })

  it('渲染无序列表为 <ul>', () => {
    const html = mdToHtml('- 项A\n- 项B')
    expect(html).toContain('<ul>')
    expect(html).toContain('<li>项A</li>')
    expect(html).not.toContain('<ol>')
  })

  it('空输入返回空串', () => {
    expect(mdToHtml('')).toBe('')
    expect(mdToHtml(null as unknown as string)).toBe('')
  })
})
