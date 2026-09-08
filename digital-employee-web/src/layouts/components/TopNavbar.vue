<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { useUserStore } from '@/store/modules/user';

defineProps<{ collapsed: boolean }>();
const emit = defineEmits<{ toggle: [] }>();

const router = useRouter();
const userStore = useUserStore();

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await userStore.logout();
    router.push('/login');
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="top-navbar">
    <div class="top-navbar__left">
      <el-icon class="top-navbar__trigger" @click="emit('toggle')">
        <component :is="collapsed ? 'Expand' : 'Fold'" />
      </el-icon>
    </div>
    <div class="top-navbar__right">
      <el-dropdown trigger="click">
        <span class="top-navbar__user">
          <el-icon><User /></el-icon>
          {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped lang="scss">
.top-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $navbar-height;
  padding: 0 16px;
  background-color: #fff;
  box-shadow: 0 1px 4px #00000014;

  &__left {
    display: flex;
    align-items: center;
  }

  &__trigger {
    font-size: 20px;
    cursor: pointer;
    color: $color-text-primary;

    &:hover {
      color: $color-primary;
    }
  }

  &__right {
    display: flex;
    align-items: center;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: $color-text-regular;

    &:hover {
      color: $color-primary;
    }
  }
}
</style>
