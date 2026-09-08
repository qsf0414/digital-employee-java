<script setup lang="ts">
const props = defineProps<{
  total: number;
  page: number;
  pageSize: number;
}>();

const emit = defineEmits<{
  'update:page': [value: number];
  'update:pageSize': [value: number];
  change: [];
}>();

function handleCurrentChange(val: number): void {
  emit('update:page', val);
  emit('change');
}

function handleSizeChange(val: number): void {
  emit('update:pageSize', val);
  emit('update:page', 1);
  emit('change');
}
</script>

<template>
  <div class="table-pagination">
    <el-pagination
      :current-page="props.page"
      :page-size="props.pageSize"
      :total="props.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<style scoped lang="scss">
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
