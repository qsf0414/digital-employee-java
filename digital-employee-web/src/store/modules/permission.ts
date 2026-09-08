import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import type { IMenuNode } from '@/api/types';

const modules = import.meta.glob('@/views/**/*.vue');

function resolveComponent(componentPath: string): (() => Promise<unknown>) | undefined {
  const key = `/src/views/${componentPath}.vue`;
  return modules[key] as (() => Promise<unknown>) | undefined;
}

function buildRoutes(menus: IMenuNode[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = [];

  for (const menu of menus) {
    if (menu.menuType === 'F') continue;

    const route: RouteRecordRaw = {
      path: menu.path || '',
      name: `menu_${menu.id}`,
      meta: {
        title: menu.title,
        icon: menu.icon,
      },
      children: [],
    };

    if (menu.component) {
      const componentFn = resolveComponent(menu.component);
      if (componentFn) {
        route.component = componentFn as () => Promise<unknown>;
      }
    }

    if (menu.children && menu.children.length > 0) {
      route.children = buildRoutes(menu.children);
    }

    routes.push(route);
  }

  return routes;
}

export const usePermissionStore = defineStore('permission', () => {
  const addedRoutes = ref<RouteRecordRaw[]>([]);

  function generateRoutes(menus: IMenuNode[]): RouteRecordRaw[] {
    const routes = buildRoutes(menus);
    addedRoutes.value = routes;
    return routes;
  }

  function resetRoutes(): void {
    addedRoutes.value = [];
  }

  return {
    addedRoutes,
    generateRoutes,
    resetRoutes,
  };
});
