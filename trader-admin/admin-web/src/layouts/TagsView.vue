<template>
  <div class="tags-view">
    <el-scrollbar>
      <div class="tags-wrap">
        <button
          v-for="tag in visitedViews"
          :key="tag.fullPath"
          type="button"
          class="tag-chip"
          :class="{ 'is-active': tag.fullPath === currentFullPath }"
          @click="onClickTag(tag)"
        >
          <span class="tag-chip__title">{{ tag.title }}</span>
          <span
            v-if="!tag.affix"
            class="tag-chip__close"
            @click.stop="onCloseTag(tag)">
            ×
          </span>
        </button>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
type TagView = {
  title: string
  fullPath: string
  path: string
  name: string
  affix: boolean
}

defineProps<{
  visitedViews: TagView[]
  currentFullPath: string
}>()

const emit = defineEmits<{
  clickTag: [tag: TagView]
  closeTag: [tag: TagView]
}>()

const onClickTag = (tag: TagView) => {
  emit('clickTag', tag)
}

const onCloseTag = (tag: TagView) => {
  emit('closeTag', tag)
}
</script>

<style scoped>
.tags-view {
  background: #f8fafc;
  border-bottom: 1px solid #e8edf5;
  padding: 10px 16px 12px;
}

.tags-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  white-space: nowrap;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d9e2ef;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.tag-chip:hover {
  color: #1e293b;
  border-color: #bfd1f7;
  background: #f8fbff;
}

.tag-chip.is-active {
  color: #1d4ed8;
  border-color: #bfdbfe;
  background: #eff6ff;
  box-shadow: 0 8px 20px rgba(29, 78, 216, 0.08);
}

.tag-chip__title {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-chip__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  color: inherit;
  font-size: 12px;
}

.tag-chip__close:hover {
  background: rgba(15, 23, 42, 0.08);
}

@media (max-width: 768px) {
  .tags-view {
    padding: 8px 12px 10px;
  }

  .tag-chip__title {
    max-width: 120px;
  }
}
</style>
