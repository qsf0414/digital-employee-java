<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useUserStore } from '@/store/modules/user';

const props = defineProps<{ collapsed: boolean }>();

const route = useRoute();
const userStore = useUserStore();

const activeMenu = computed(() => route.path);

const menuItems = computed(() => {
  return userStore.menus.filter((m) => m.menuType !== 'F');
});

function resolveIcon(icon?: string): string {
  return icon || 'Menu';
}
</script>

<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': props.collapsed }">
    <div class="sidebar__logo">
      <span v-if="!props.collapsed" class="sidebar__logo-text">数字员工</span>
      <span v-else class="sidebar__logo-text--mini">DE</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="props.collapsed"
      :collapse-transition="false"
      background-color="#001529"
      text-color="#ffffffa6"
      active-text-color="#ffffff"
      router
    >
      <template v-for="menu in menuItems" :key="menu.id">
        <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path || String(menu.id)">
          <template #title>
            <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
            <span>{{ menu.title }}</span>
          </template>
          <el-menu-item
            v-for="child in menu.children"
            :key="child.id"
            :index="child.path ? `${menu.path}/${child.path}` : String(child.id)"
          >
            <el-icon v-if="child.icon"><component :is="resolveIcon(child.icon)" /></el-icon>
            <span>{{ child.title }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="menu.path || String(menu.id)">
          <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.sidebar {
  width: $sidebar-width;
  height: 100vh;
  background-color: #001529;
  transition: width $transition-duration;
  overflow: hidden;

  &--collapsed {
    width: $sidebar-collapsed-width;
  }

  &__logo {
    display: flex;
    align-items: center;
    justify-content: center;
    height: $navbar-height;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    border-bottom: 1px solid #ffffff1a;
  }

  &__logo-text--mini {
    font-size: 16px;
  }
}

.el-menu {
  border-right: none;
}
</style>
