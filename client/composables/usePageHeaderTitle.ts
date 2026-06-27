/** 页面头部标题状态管理 - 供 BackButton 组件和页面间传递标题 */
export const usePageHeaderTitle = () => {
  const title = useState('page-header-title', () => '')
  const setTitle = (t: string) => { title.value = t }
  return { title, setTitle }
}
