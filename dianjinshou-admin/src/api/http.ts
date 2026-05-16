import axios, { AxiosError } from 'axios';
import { Message } from '@arco-design/web-vue';
import { useAuthStore } from '../stores/auth';
import router from '../router';
import { formatDateTime } from '../utils/format';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

const ISO_DATE_TIME_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/;

function normalizeDateTimes<T>(value: T): T {
  if (typeof value === 'string') {
    return (ISO_DATE_TIME_RE.test(value) ? formatDateTime(value) : value) as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => normalizeDateTimes(item)) as T;
  }
  if (value && typeof value === 'object') {
    Object.keys(value as Record<string, unknown>).forEach((key) => {
      (value as Record<string, unknown>)[key] = normalizeDateTimes((value as Record<string, unknown>)[key]);
    });
  }
  return value;
}

http.interceptors.response.use(
  (res) => {
    const data = normalizeDateTimes(res.data);
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0 || data.code === 200) {
        return data.data;
      }
      Message.error(data.message || '请求失败');
      return Promise.reject(new Error(data.message || 'API error'));
    }
    return data;
  },
  (err: AxiosError<any>) => {
    const status = err.response?.status;
    const msg = err.response?.data?.message || err.message || '网络错误';
    if (status === 401 || status === 403) {
      const auth = useAuthStore();
      auth.clear();
      if (router.currentRoute.value.name !== 'login') {
        router.push('/login');
      }
      Message.warning(status === 401 ? '登录已失效，请重新登录' : '无权限访问');
    } else {
      Message.error(msg);
    }
    return Promise.reject(err);
  }
);

export default http;
