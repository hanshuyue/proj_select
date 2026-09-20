export interface DeptTreeNodeLike {
  id: number
  parentId?: number
  children?: DeptTreeNodeLike[]
  [key: string]: unknown
}

export function removeDeptFromTree<T extends DeptTreeNodeLike>(nodes: T[], deptId: number) {
  let parentId: number | undefined

  const walk = (items: T[]): T[] => {
    return items.reduce<T[]>((acc, item) => {
      if (item.id === deptId) {
        parentId = item.parentId
        return acc
      }
      if (item.children?.length) {
        acc.push({ ...item, children: walk(item.children as T[]) })
        return acc
      }
      acc.push(item)
      return acc
    }, [])
  }

  return { tree: walk(nodes), parentId }
}
