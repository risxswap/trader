<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style>
/* 修复inline表单中输入框宽度变化导致的布局抖动问题 */
.el-form--inline .el-form-item__content > .el-input,
.el-form--inline .el-form-item__content > .el-select,
.el-form--inline .el-form-item__content > .el-autocomplete,
.el-form--inline .el-form-item__content > .el-date-editor {
  width: 150px;
}

.el-form--inline .el-form-item__content > .el-date-editor.el-date-editor--daterange,
.el-form--inline .el-form-item__content > .el-date-editor.el-date-editor--datetimerange {
  width: 350px;
}
</style>
