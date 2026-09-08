import { useUserStore } from '@/store/modules/user';
import { ALL_PERMISSION } from '@/constants/access-codes';

export function checkPermi(value: string[] | string): boolean {
  if (!value || (Array.isArray(value) && value.length === 0)) {
    return false;
  }
  const userStore = useUserStore();
  const allPermissions = userStore.permissions || [];
  const targetPerms = Array.isArray(value) ? value : [value];

  return allPermissions.some((perm) => {
    return perm === ALL_PERMISSION || targetPerms.includes(perm);
  });
}
