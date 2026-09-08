import type { App, DirectiveBinding } from 'vue';
import { useUserStore } from '@/store/modules/user';
import { ALL_PERMISSION } from '@/constants/access-codes';

export const setupPermissionDirective = (app: App): void => {
  app.directive('hasPermi', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string[] | string>) {
      const { value } = binding;

      if (!value || (Array.isArray(value) && value.length === 0)) {
        throw new Error('v-hasPermi 必须绑定权限标识，例如：v-hasPermi="[\'admin:user:manage\']"');
      }

      const userStore = useUserStore();
      const allPermissions = userStore.permissions || [];
      const targetPerms = Array.isArray(value) ? value : [value];

      const hasPermission = allPermissions.some((perm) => {
        return perm === ALL_PERMISSION || targetPerms.includes(perm);
      });

      if (!hasPermission && el.parentNode) {
        el.parentNode.removeChild(el);
      }
    },
  });
};
