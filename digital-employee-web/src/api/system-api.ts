import request from '@/utils/request';
import type {
  IApiResponse,
  IPageResult,
  IPageQuery,
  ISysUser,
  ISysRole,
  ISysMenu,
  ISysMenuTreeNode,
  UserCreateDTO,
  UserUpdateDTO,
  RoleCreateDTO,
  RoleUpdateDTO,
  MenuCreateDTO,
} from './types';

export function getUserList(params: IPageQuery): Promise<IApiResponse<IPageResult<ISysUser>>> {
  return request.get('/system/user', { params }) as unknown as Promise<IApiResponse<IPageResult<ISysUser>>>;
}

export function getUserById(id: number): Promise<IApiResponse<ISysUser>> {
  return request.get(`/system/user/${id}`) as unknown as Promise<IApiResponse<ISysUser>>;
}

export function createUser(data: UserCreateDTO): Promise<IApiResponse<void>> {
  return request.post('/system/user', data) as unknown as Promise<IApiResponse<void>>;
}

export function updateUser(data: UserUpdateDTO): Promise<IApiResponse<void>> {
  return request.put('/system/user', data) as unknown as Promise<IApiResponse<void>>;
}

export function deleteUser(id: number): Promise<IApiResponse<void>> {
  return request.delete(`/system/user/${id}`) as unknown as Promise<IApiResponse<void>>;
}

export function resetUserPassword(id: number): Promise<IApiResponse<void>> {
  return request.put(`/system/user/${id}/reset-password`) as unknown as Promise<IApiResponse<void>>;
}

export function getRoleList(params: IPageQuery): Promise<IApiResponse<IPageResult<ISysRole>>> {
  return request.get('/system/role', { params }) as unknown as Promise<IApiResponse<IPageResult<ISysRole>>>;
}

export function getRoleById(id: number): Promise<IApiResponse<ISysRole>> {
  return request.get(`/system/role/${id}`) as unknown as Promise<IApiResponse<ISysRole>>;
}

export function createRole(data: RoleCreateDTO): Promise<IApiResponse<void>> {
  return request.post('/system/role', data) as unknown as Promise<IApiResponse<void>>;
}

export function updateRole(data: RoleUpdateDTO): Promise<IApiResponse<void>> {
  return request.put('/system/role', data) as unknown as Promise<IApiResponse<void>>;
}

export function deleteRole(id: number): Promise<IApiResponse<void>> {
  return request.delete(`/system/role/${id}`) as unknown as Promise<IApiResponse<void>>;
}

export function assignRoleMenu(roleId: number, menuIds: number[]): Promise<IApiResponse<void>> {
  return request.put(`/system/role/${roleId}/menus`, { menuIds }) as unknown as Promise<IApiResponse<void>>;
}

export function getMenuTree(): Promise<IApiResponse<ISysMenuTreeNode[]>> {
  return request.get('/system/menu/tree') as unknown as Promise<IApiResponse<ISysMenuTreeNode[]>>;
}

export function getMenuList(): Promise<IApiResponse<ISysMenu[]>> {
  return request.get('/system/menu/list') as unknown as Promise<IApiResponse<ISysMenu[]>>;
}

export function getMenuById(id: number): Promise<IApiResponse<ISysMenu>> {
  return request.get(`/system/menu/${id}`) as unknown as Promise<IApiResponse<ISysMenu>>;
}

export function createMenu(data: MenuCreateDTO): Promise<IApiResponse<void>> {
  return request.post('/system/menu', data) as unknown as Promise<IApiResponse<void>>;
}

export function updateMenu(data: ISysMenu): Promise<IApiResponse<void>> {
  return request.put('/system/menu', data) as unknown as Promise<IApiResponse<void>>;
}

export function deleteMenu(id: number): Promise<IApiResponse<void>> {
  return request.delete(`/system/menu/${id}`) as unknown as Promise<IApiResponse<void>>;
}
