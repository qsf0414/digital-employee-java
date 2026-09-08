import router from '@/router';
import { useUserStore } from '@/store/modules/user';
import { usePermissionStore } from '@/store/modules/permission';
import { getStorage } from '@/utils/storage';
import { STORAGE_KEYS, WHITE_LIST } from '@/constants/app-keys';

export function setupRouterGuard(): void {
  router.beforeEach(async (to, _from, next) => {
    const token = getStorage<string>(STORAGE_KEYS.ACCESS_TOKEN);
    const userStore = useUserStore();
    const permissionStore = usePermissionStore();

    if (token) {
      if (to.path === '/login') {
        next({ path: '/' });
      } else {
        if (userStore.roles.length === 0) {
          try {
            const { menus } = await userStore.fetchUserInfo();
            const accessRoutes = permissionStore.generateRoutes(menus);
            accessRoutes.forEach((route) => router.addRoute(route));
            next({ ...to, replace: true });
          } catch {
            await userStore.logout();
            next(`/login?redirect=${to.path}`);
          }
        } else {
          next();
        }
      }
    } else {
      if ((WHITE_LIST as readonly string[]).includes(to.path)) {
        next();
      } else {
        next(`/login?redirect=${to.path}`);
      }
    }
  });
}
