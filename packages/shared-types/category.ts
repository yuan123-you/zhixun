/** 分类视图对象 */
export interface CategoryVO {
  id: number
  name: string
  parentId: number
  sort: number
  articleCount?: number
  children?: CategoryVO[]
}
