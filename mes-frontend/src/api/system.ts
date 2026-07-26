import request from './index'

export function getUserList() { return request({ url: '/auth/user/list', method: 'get' }) }
export function createUser(data: any) { return request({ url: '/auth/user', method: 'post', data }) }
export function updateUser(id: number, data: any) { return request({ url: `/auth/user/${id}`, method: 'put', data }) }
export function deleteUser(id: number) { return request({ url: `/auth/user/${id}`, method: 'delete' }) }
export function assignUserRole(id: number, data: any) { return request({ url: `/auth/user/${id}/role`, method: 'put', data }) }

export function getRoleList() { return request({ url: '/auth/role/list', method: 'get' }) }
export function getRolePermissionsTree() { return request({ url: '/auth/role/permissions', method: 'get' }) }
export function getRolePermissions(id: number) { return request({ url: `/auth/role/${id}/permissions`, method: 'get' }) }
export function assignRolePermissions(id: number, data: any) { return request({ url: `/auth/role/${id}/permissions`, method: 'put', data }) }
export function createRole(data: any) { return request({ url: '/auth/role', method: 'post', data }) }
export function updateRole(id: number, data: any) { return request({ url: `/auth/role/${id}`, method: 'put', data }) }
export function deleteRole(id: number) { return request({ url: `/auth/role/${id}`, method: 'delete' }) }

export function getPermissionList() { return request({ url: '/auth/permission/list', method: 'get' }) }
export function createPermission(data: any) { return request({ url: '/auth/permission', method: 'post', data }) }
export function updatePermission(data: any) { return request({ url: '/auth/permission', method: 'put', data }) }
export function deletePermission(id: number) { return request({ url: `/auth/permission/${id}`, method: 'delete' }) }

export function getMenuList() { return request({ url: '/auth/menu/list', method: 'get' }) }
export function createMenu(data: any) { return request({ url: '/auth/menu', method: 'post', data }) }
export function updateMenu(data: any) { return request({ url: '/auth/menu', method: 'put', data }) }
export function deleteMenu(id: number) { return request({ url: `/auth/menu/${id}`, method: 'delete' }) }
