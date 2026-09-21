/** A reviewer retains the employee ability to revise their own submitted PPT. */
export function canResubmitDocument(userId: number | undefined, row: {
  submitterId?: number; projectId?: number; businessType?: string; status: string
}) {
  return userId != null && row.submitterId === userId && !!row.projectId
    && (row.businessType === 'SELECTION' || row.businessType === 'INITIATION')
    && row.status !== 'SUBMITTED'
}
