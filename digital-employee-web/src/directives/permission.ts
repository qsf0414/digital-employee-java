import type { App, DirectiveBinding } from 'vue';
import { useUserStore } from '@/store/modules/user';
import { ALL_PERMISSION } from '@/constants/access-codes';

export const setupPermissionDirective = (app: App): void => {
  app.directive('hasPermi', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string[] | string>) {
      const { value } = binding;
      const userStore = useUserStore();
      const allPermissions = userStore.permissions || [];

      if (value && (Array.isArray(value) ? value.length > 0 : !!value)) {
        const targetPerms = Array.isArray(value) ? value : [value];

        const hasPermission = allPermissions.some((perm) => {
          return perm === ALL_PERMISSION || targetPerms.includes(perm);
        });

        if (!hasPermission && el.parentNode) {
          el.parentNode.removeChild(el);
        }
      }
    },
  });
};
