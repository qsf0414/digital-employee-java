<script setup lang="ts">
import { ref } from 'vue';
import SideBar from './components/SideBar.vue';
import TopNavbar from './components/TopNavbar.vue';

const isCollapsed = ref(false);

function toggleSidebar(): void {
  isCollapsed.value = !isCollapsed.value;
}
</script>

<template>
  <div class="main-layout">
    <SideBar :collapsed="isCollapsed" class="main-layout__sidebar" />
    <div
      class="main-layout__main"
      :style="{ marginLeft: isCollapsed ? '64px' : '220px' }"
    >
      <TopNavbar :collapsed="isCollapsed" class="main-layout__navbar" @toggle="toggleSidebar" />
      <div class="main-layout__content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.main-layout {
  position: relative;
  width: 100%;
  height: 100%;

  &__sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    transition: width $transition-duration;
  }

  &__main {
    display: flex;
    flex-direction: column;
    min-height: 100%;
    transition: margin-left $transition-duration;
  }

  &__navbar {
    position: sticky;
    top: 0;
    z-index: 90;
    height: $navbar-height;
  }

  &__content {
    flex: 1;
    padding: 20px;
    background-color: $color-bg-page;
  }
}
</style>
