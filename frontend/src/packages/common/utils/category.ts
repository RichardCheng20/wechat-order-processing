import type { CategoryItem } from '@common/api/product'

export interface CategorySidebarItem {
  key: string
  label: string
  categoryId?: number
  level: 0 | 1 | 2
}

export function flattenCategories(tree: CategoryItem[]): CategoryItem[] {
  const result: CategoryItem[] = []
  for (const parent of tree) {
    result.push(parent)
    if (parent.children?.length) {
      result.push(...parent.children)
    }
  }
  return result
}

export function getLeafCategories(tree: CategoryItem[]): CategoryItem[] {
  const leaves: CategoryItem[] = []
  for (const parent of tree) {
    if (parent.children?.length) {
      leaves.push(...parent.children)
    } else {
      leaves.push(parent)
    }
  }
  return leaves
}

export function getCategoryDisplayName(tree: CategoryItem[], categoryId: number): string {
  for (const parent of tree) {
    if (parent.id === categoryId) {
      return parent.name
    }
    for (const child of parent.children || []) {
      if (child.id === categoryId) {
        return `${parent.name} / ${child.name}`
      }
    }
  }
  const flat = flattenCategories(tree)
  return flat.find((c) => c.id === categoryId)?.name || ''
}

export function buildPrimarySidebar(tree: CategoryItem[]): CategorySidebarItem[] {
  const items: CategorySidebarItem[] = [
    { key: 'all', label: '全部', level: 0 },
  ]
  for (const parent of tree) {
    items.push({
      key: String(parent.id),
      label: parent.name,
      categoryId: parent.id,
      level: 1,
    })
  }
  items.push({ key: 'uncategorized', label: '未分类', level: 0 })
  return items
}

export function getParentCategory(tree: CategoryItem[], parentKey: string): CategoryItem | null {
  if (parentKey === 'all' || parentKey === 'uncategorized') return null
  const parentId = Number(parentKey)
  if (!parentId) return null
  return tree.find((p) => p.id === parentId) || null
}

export function buildCategorySidebar(tree: CategoryItem[]): CategorySidebarItem[] {
  const items: CategorySidebarItem[] = [
    { key: 'all', label: '全部', level: 0 },
  ]
  for (const parent of tree) {
    items.push({
      key: String(parent.id),
      label: parent.name,
      categoryId: parent.id,
      level: 1,
    })
    for (const child of parent.children || []) {
      items.push({
        key: String(child.id),
        label: child.name,
        categoryId: child.id,
        level: 2,
      })
    }
  }
  items.push({ key: 'uncategorized', label: '未分类', level: 0 })
  return items
}

export function matchCategoryFilter(
  productCategoryId: number | null | undefined,
  filterKey: string,
  tree: CategoryItem[],
): boolean {
  if (filterKey === 'all') return true
  if (filterKey === 'uncategorized') return !productCategoryId
  const categoryId = Number(filterKey)
  if (!categoryId) return true
  if (productCategoryId === categoryId) return true
  const parent = tree.find((p) => p.id === categoryId)
  if (parent?.children?.length) {
    return parent.children.some((child) => child.id === productCategoryId)
  }
  return false
}
