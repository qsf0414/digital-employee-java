import { computed } from 'vue';
import { useUserStore } from '@/store/modules/user';
import { useRouter, useRoute } from 'vue-router';
import { getStorage } from '@/utils/storage';
import { STORAGE_KEYS } from '@/constants/app-keys';

export function useAuth() {
  const userStore = useUserStore();
  const router = useRouter();
  const route = useRoute();

  const isLoggedIn = computed(() => !!getStorage<string>(STORAGE_KEYS.ACCESS_TOKEN));
  const userInfo = computed(() => userStore.userInfo);
  const roles = computed(() => userStore.roles);
  const permissions = computed(() => userStore.permissions);

  async function login(username: string, password: string, captchaKey: string, captchaCode: string) {
    await userStore.login({ username, password, captchaKey, captchaCode });
    const redirect = (route.query.redirect as string) || '/';
    router.push(redirect);
  }

  async function logout() {
    await userStore.logout();
    router.push('/login');
  }

  return { isLoggedIn, userInfo, roles, permissions, login, logout };
}
