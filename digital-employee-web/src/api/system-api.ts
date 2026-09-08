import request from '@/utils/request';
import type {
  IApiResponse,
  IPageResult,
  IPageQuery,
  ISysUser,
  ISysRole,
  ISysMenu,
} from './types';

export function getUserList(params: IPageQuery): Promise<IApiResponse<IPageResult<ISysUser>>> {
  return request.get('/system/user', { params }) as unknown as Promise<IApiResponse<IPageResult<ISysUser>>>;
}

export function getUserById(id: number): Promise<IApiResponse<ISysUser>> {
  return request.get(`/system/user/${id}`) as unknown as Promise<IApiResponse<ISysUser>>;
}

export function getRoleList(params: IPageQuery): Promise<IApiResponse<IPageResult<ISysRole>>> {
  return request.get('/system/role', { params }) as unknown as Promise<IApiResponse<IPageResult<ISysRole>>>;
}

export function getMenuTree(): Promise<IApiResponse<ISysMenu[]>> {
  return request.get('/system/menu/tree') as unknown as Promise<IApiResponse<ISysMenu[]>>;
}

export function assignRoleMenu(roleId: number, menuIds: number[]): Promise<IApiResponse<void>> {
  return request.put(`/system/role/${roleId}/menus`, { menuIds }) as unknown as Promise<IApiResponse<void>>;
}
