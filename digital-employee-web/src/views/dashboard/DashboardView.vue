<script setup lang="ts">
import { useUserStore } from '@/store/modules/user';

const userStore = useUserStore();
</script>

<template>
  <div class="dashboard-view">
    <el-card>
      <template #header>
        <span>工作台</span>
      </template>
      <p>欢迎回来，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</p>
      <p style="margin-top: 12px; color: #909399">
        角色：{{ userStore.roles.join(', ') }}
      </p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard-view {
  padding: 0;
}
</style>
