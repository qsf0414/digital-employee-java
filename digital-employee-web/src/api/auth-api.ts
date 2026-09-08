import request from '@/utils/request';
import type { IApiResponse, ILoginParams, ICaptchaData, IMeData } from './types';

export function login(data: ILoginParams): Promise<IApiResponse<string>> {
  return request.post('/auth/login', data) as unknown as Promise<IApiResponse<string>>;
}

export function logout(): Promise<IApiResponse<void>> {
  return request.post('/auth/logout') as unknown as Promise<IApiResponse<void>>;
}

export function getCaptcha(): Promise<IApiResponse<ICaptchaData>> {
  return request.get('/auth/captcha') as unknown as Promise<IApiResponse<ICaptchaData>>;
}

export function getMeInfo(): Promise<IApiResponse<IMeData>> {
  return request.get('/auth/me') as unknown as Promise<IApiResponse<IMeData>>;
}
