/** 页面头部标题状态管理 - 供 BackButton 组件和页面间传递标题 */
const pageHeaderTitle = ref('')

export const usePageHeaderTitle = () => {
  const title = pageHeaderTitle
  const setTitle = (t: string) => { title.value = t }
  return { title, setTitle }
}
