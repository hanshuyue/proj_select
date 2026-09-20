import test from 'node:test'
import assert from 'node:assert/strict'
import { removeDeptFromTree } from '../deptTree.ts'

test('removeDeptFromTree removes the deleted node and keeps siblings', () => {
  const tree = [
    {
      id: 1,
      name: '集团',
      children: [
        { id: 2, parentId: 1, name: '研发中心' },
        {
          id: 3,
          parentId: 1,
          name: '测试中心',
          children: [
            { id: 4, parentId: 3, name: '自动化测试组' },
          ],
        },
      ],
    },
  ]

  const result = removeDeptFromTree(tree, 3)

  assert.equal(result.parentId, 1)
  assert.deepEqual(result.tree, [
    {
      id: 1,
      name: '集团',
      children: [
        { id: 2, parentId: 1, name: '研发中心' },
      ],
    },
  ])
})
