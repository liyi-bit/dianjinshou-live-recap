import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { AdminUserInfo } from '../api/adminAuth';

const TOKEN_KEY = 'dianjinshou_admin_token';
const USER_KEY = 'dianjinshou_admin_user';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '');
  const user = ref<AdminUserInfo | null>(
    localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY)!) : null
  );

  function setAuth(accessToken: string, info: AdminUserInfo) {
    token.value = accessToken;
    user.value = info;
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(info));
  }

  function clear() {
    token.value = '';
    user.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function isAdmin(): boolean {
    return user.value?.role === 'admin_super' || user.value?.role === 'admin_normal';
  }

  return { token, user, setAuth, clear, isAdmin };
});
