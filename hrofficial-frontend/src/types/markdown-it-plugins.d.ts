declare module 'markdown-it-task-lists' {
  import type MarkdownIt from 'markdown-it'
  function taskLists(md: MarkdownIt, options?: { enabled?: boolean; label?: boolean }): void
  export default taskLists
}

declare module 'markdown-it-emoji' {
  import type MarkdownIt from 'markdown-it'
  function emoji(md: MarkdownIt): void
  export default emoji
}

declare module 'markdown-it-container' {
  import type MarkdownIt from 'markdown-it'
  interface ContainerOptions {
    render?: (tokens: any[], idx: number, options: any, env: any, self: any) => string
  }
  function container(md: MarkdownIt, name: string, options?: ContainerOptions): void
  export default container
}

declare module 'markdown-it-anchor' {
  import type MarkdownIt from 'markdown-it'
  interface AnchorOptions {
    permalink?: boolean
    level?: number[]
  }
  function anchor(md: MarkdownIt, options?: AnchorOptions): void
  export default anchor
}

// 声明hljs全局变量
declare const hljs: {
  getLanguage: (lang: string) => any
  highlight: (code: string, options: { language: string; ignoreIllegals: boolean }) => { value: string }
} | undefined
