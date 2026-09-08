import axios, {
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { IApiResponse } from '@/api/types';
import { getStorage, clearStorage } from '@/utils/storage';
import { STORAGE_KEYS, HTTP_STATUS } from '@/constants/app-keys';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
});

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStorage<string>(STORAGE_KEYS.ACCESS_TOKEN);
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response: AxiosResponse<IApiResponse>) => {
    const { data } = response;
    if (data && data.code !== 'OK') {
      ElMessage.error(data.message || '请求失败');
      return Promise.reject(new Error(data.message));
    }
    return data as unknown as AxiosResponse;
  },
  (error) => {
    const { response } = error;
    if (response) {
      const { status, data } = response;
      if (status === HTTP_STATUS.UNAUTHORIZED) {
        if (data?.code === 'SESSION_REPLACED') {
          ElMessageBox.alert(
            '您的账号已在其他终端登录，当前会话已失效。',
            '下线提示',
            {
              confirmButtonText: '重新登录',
              type: 'warning',
              callback: () => {
                clearStorage();
                window.location.href = '/login';
              },
            },
          );
        } else {
          ElMessage.error(data?.message || '登录已失效，请重新登录');
          clearStorage();
          window.location.href = '/login';
        }
      } else if (status === HTTP_STATUS.FORBIDDEN) {
        ElMessage.error(data?.message || '暂无操作权限');
      } else if (status === HTTP_STATUS.TOO_MANY_REQUESTS) {
        ElMessage.error(data?.message || '请求过于频繁，请稍后再试');
      } else {
        ElMessage.error(data?.message || '网络连接异常');
      }
    } else {
      ElMessage.error('网络连接异常');
    }
    return Promise.reject(error);
  },
);

export default request;
