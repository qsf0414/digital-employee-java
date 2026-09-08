import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getMeInfo, login as apiLogin, logout as apiLogout } from '@/api/auth-api';
import type { ILoginParams, IUserInfo, IMenuNode, IMeData } from '@/api/types';
import { getStorage, setStorage, clearStorage } from '@/utils/storage';
import { STORAGE_KEYS } from '@/constants/app-keys';

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getStorage<string>(STORAGE_KEYS.ACCESS_TOKEN) || '');
  const userInfo = ref<IUserInfo | null>(getStorage<IUserInfo>(STORAGE_KEYS.USER_INFO));
  const roles = ref<string[]>([]);
  const permissions = ref<string[]>([]);
  const menus = ref<IMenuNode[]>([]);

  async function login(params: ILoginParams): Promise<void> {
    const res = await apiLogin(params);
    token.value = res.data;
    setStorage(STORAGE_KEYS.ACCESS_TOKEN, res.data);
  }

  async function fetchUserInfo(): Promise<IMeData> {
    const res = await getMeInfo();
    const { user, roles: r, permissions: p, menus: m } = res.data;
    userInfo.value = user;
    roles.value = r;
    permissions.value = p;
    menus.value = m;
    setStorage(STORAGE_KEYS.USER_INFO, user);
    return res.data;
  }

  async function logout(): Promise<void> {
    try {
      await apiLogout();
    } finally {
      resetState();
    }
  }

  function resetState(): void {
    token.value = '';
    userInfo.value = null;
    roles.value = [];
    permissions.value = [];
    menus.value = [];
    clearStorage();
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    menus,
    login,
    fetchUserInfo,
    logout,
    resetState,
  };
});
