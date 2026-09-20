export interface Column {
  key: string
  title?: string
  width?: number
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  className?: string
}

export interface SortState {
  key: string
  dir: 'asc' | 'desc'
}
