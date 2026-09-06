INSERT INTO sys_role (role_key, role_name, status)
VALUES
    ('super_admin', '超级管理员', 1),
    ('admin', '管理员', 1),
    ('user', '普通用户', 1)
ON CONFLICT (role_key) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, title, menu_type, path, component, perms, icon, sort_order)
VALUES
    (1, 0, '系统管理', 'M', '/system', NULL, NULL, 'setting', 1),
    (2, 1, '用户管理', 'C', '/system/user', 'system/user/UserManage', 'admin:user:readonly', 'user', 1),
    (3, 2, '用户新增', 'F', NULL, NULL, 'admin:user:manage', NULL, 1),
    (4, 2, '用户编辑', 'F', NULL, NULL, 'admin:user:manage', NULL, 2),
    (5, 2, '用户删除', 'F', NULL, NULL, 'admin:user:manage', NULL, 3),
    (6, 1, '角色管理', 'C', '/system/role', 'system/role/RoleManage', 'admin:permission:readonly', 'peoples', 2),
    (7, 6, '角色新增', 'F', NULL, NULL, 'admin:permission:manage', NULL, 1),
    (8, 6, '角色编辑', 'F', NULL, NULL, 'admin:permission:manage', NULL, 2),
    (9, 6, '角色删除', 'F', NULL, NULL, 'admin:permission:manage', NULL, 3),
    (10, 1, '菜单管理', 'C', '/system/menu', 'system/menu/MenuManage', 'admin:menu:readonly', 'tree-table', 3),
    (11, 10, '菜单新增', 'F', NULL, NULL, 'admin:menu:manage', NULL, 1),
    (12, 10, '菜单编辑', 'F', NULL, NULL, 'admin:menu:manage', NULL, 2),
    (13, 10, '菜单删除', 'F', NULL, NULL, 'admin:menu:manage', NULL, 3),
    (14, 1, '邀请码管理', 'C', '/system/invite', 'system/invite/InviteManage', 'admin:invite_code:readonly', 'ticket', 4),
    (15, 14, '邀请码新增', 'F', NULL, NULL, 'admin:invite_code:manage', NULL, 1),
    (16, 14, '邀请码删除', 'F', NULL, NULL, 'admin:invite_code:manage', NULL, 2),
    (17, 0, '数据平台', 'M', '/data', NULL, NULL, 'data-line', 2),
    (18, 17, '仪表盘', 'C', '/data/dashboard', 'data/DashboardView', 'admin:data_platform:dashboard', 'monitor', 1),
    (19, 17, '数据项', 'C', '/data/items', 'data/DataItems', 'admin:data_platform:data_items', 'list', 2),
    (20, 17, '平台配置', 'C', '/data/config', 'data/DataConfig', 'admin:data_platform:config', 'tools', 3),
    (21, 0, 'AI 中心', 'M', '/ai', NULL, NULL, 'cpu', 3),
    (22, 21, 'Bot 管理', 'C', '/ai/bot', 'ai/BotManage', 'admin:bot:readonly', 'robot', 1),
    (23, 22, 'Bot 新增', 'F', NULL, NULL, 'admin:bot:manage', NULL, 1),
    (24, 22, 'Bot 编辑', 'F', NULL, NULL, 'admin:bot:manage', NULL, 2),
    (25, 22, 'Bot 删除', 'F', NULL, NULL, 'admin:bot:manage', NULL, 3),
    (26, 21, 'Agent 管理', 'C', '/ai/agent', 'ai/AgentManage', 'admin:agent:readonly', 'connection', 2),
    (27, 26, 'Agent 新增', 'F', NULL, NULL, 'admin:agent:manage', NULL, 1),
    (28, 26, 'Agent 编辑', 'F', NULL, NULL, 'admin:agent:manage', NULL, 2),
    (29, 26, 'Agent 删除', 'F', NULL, NULL, 'admin:agent:manage', NULL, 3),
    (30, 0, '可观测性', 'M', '/observe', NULL, NULL, 'view', 4),
    (31, 30, '日志查看', 'C', '/observe/logs', 'observe/LogView', 'admin:observability:log:view', 'log', 1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('sys_menu_id_seq', (SELECT COALESCE(MAX(id), 0) FROM sys_menu));

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key = 'super_admin'
ON CONFLICT DO NOTHING;
