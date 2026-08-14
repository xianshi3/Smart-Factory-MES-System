/** 简易 Markdown → HTML 转换器（处理 AI 返回的格式化文本） */
export function mdToHtml(text: string): string {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  html = html.replace(/^#{5,}\s+(.+)$/gm, '<h6>$1</h6>')
  html = html.replace(/^####\s+(.+)$/gm, '<h5>$1</h5>')
  html = html.replace(/^###\s+(.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^##\s+(.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^#\s+(.+)$/gm, '<h2>$1</h2>')

  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  // Horizontal rules
  html = html.replace(/^(-{3,}|\*{3,})$/gm, '<hr>')

  // Lists: numbered and bullet
  // 有序列表先打标记，避免数字被消费后无法区分 ol/ul
  html = html.replace(/^(\d+)\.\s+(.+)$/gm, '<li data-ord="$1">$2</li>')
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')

  // Wrap consecutive <li> tags
  html = html.replace(/((?:<li.*?<\/li>\s*)+)/g, (match) => {
    if (match.includes('data-ord')) {
      return '<ol>' + match.replace(/\s+data-ord="\d+"/g, '') + '</ol>'
    }
    return '<ul>' + match + '</ul>'
  })

  // Double newlines → paragraph breaks
  html = html.replace(/\n\n+/g, '</p><p>')
  // Single newlines → <br>
  html = html.replace(/\n/g, '<br>')

  // Wrap in paragraph
  html = '<p>' + html + '</p>'

  // Clean up empty paragraphs and extra breaks
  html = html.replace(/<p>\s*<\/p>/g, '')
  html = html.replace(/<p><br>\s*(<h[2-6]|<ul|<ol|<hr)/g, '$1')
  html = html.replace(/(<\/h[2-6]>|<\/ul>|<\/ol>|<\/hr>)\s*<br>\s*<p>/g, '$1<p>')

  return html
}
