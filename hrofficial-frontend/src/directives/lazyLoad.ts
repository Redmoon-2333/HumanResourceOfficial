import type { Directive, DirectiveBinding } from 'vue'

/**
 * 图片懒加载指令
 * 使用 Intersection Observer API 实现图片懒加载
 */
export const lazyLoad: Directive<HTMLImageElement> = {
  mounted(el: HTMLImageElement, binding: DirectiveBinding<string>) {
    const src = binding.value
    if (!src) return

    el.dataset.src = src
    el.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"%3E%3Crect fill="%23f5f5f5" width="400" height="300"/%3E%3Ctext fill="%23999" font-family="sans-serif" font-size="18" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E加载中...%3C/text%3E%3C/svg%3E'

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target as HTMLImageElement
            const realSrc = img.dataset.src
            if (realSrc) {
              img.src = realSrc
              img.removeAttribute('data-src')
              observer.unobserve(img)
            }
          }
        })
      },
      {
        rootMargin: '50px',
        threshold: 0.1
      }
    )

    observer.observe(el)
  }
}

export default lazyLoad
