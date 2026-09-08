export interface IApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
}

export interface ILoginParams {
  username: string;
  password: string;
  captchaKey: string;
  captchaCode: string;
}

export interface ICaptchaData {
  captchaKey: string;
  captchaImage: string;
  captchaType: string;
}

export interface IUserInfo {
  userId: number;
  username: string;
  nickname: string;
}

export interface IMenuNode {
  id: number;
  parentId: number;
  title: string;
  menuType: string;
  path?: string;
  component?: string;
  icon?: string;
  children: IMenuNode[];
}

export interface IMeData {
  user: IUserInfo;
  roles: string[];
  permissions: string[];
  menus: IMenuNode[];
}

export interface IPageQuery {
  page: number;
  pageSize: number;
}

export interface IPageResult<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ISysUser {
  id: number;
  username: string;
  nickname: string;
  phone?: string;
  roleId: number;
  status: number;
  mustChangePassword: boolean;
  createdAt: string;
}

export interface ISysRole {
  id: number;
  roleKey: string;
  roleName: string;
  status: number;
  createdAt: string;
}

export interface ISysMenu {
  id: number;
  parentId: number;
  title: string;
  menuType: string;
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  sortOrder: number;
  visible: number;
  createdAt: string;
}

export interface ISysMenuTreeNode extends ISysMenu {
  children: ISysMenuTreeNode[];
}

export interface UserCreateDTO {
  username: string;
  password: string;
  nickname: string;
  phone?: string;
  roleId: number;
}

export interface UserUpdateDTO {
  id: number;
  nickname: string;
  phone?: string;
  roleId: number;
  status: number;
}

export interface RoleCreateDTO {
  roleKey: string;
  roleName: string;
  menuIds: number[];
}

export interface RoleUpdateDTO {
  id: number;
  roleName: string;
  status: number;
  menuIds: number[];
}

export interface MenuCreateDTO {
  parentId: number;
  title: string;
  menuType: string;
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  sortOrder: number;
  visible: number;
}
