export const ACCESS_CODES = {
  USER_MANAGE: 'admin:user:manage',
  USER_READONLY: 'admin:user:readonly',
  PERMISSION_MANAGE: 'admin:permission:manage',
  PERMISSION_READONLY: 'admin:permission:readonly',
  INVITE_CODE_MANAGE: 'admin:invite_code:manage',
  INVITE_CODE_READONLY: 'admin:invite_code:readonly',
  MENU_MANAGE: 'admin:menu:manage',
  MENU_READONLY: 'admin:menu:readonly',
  DATA_PLATFORM_DASHBOARD: 'admin:data_platform:dashboard',
  DATA_PLATFORM_DATA_ITEMS: 'admin:data_platform:data_items',
  DATA_PLATFORM_CONFIG: 'admin:data_platform:config',
  BOT_MANAGE: 'admin:bot:manage',
  BOT_READONLY: 'admin:bot:readonly',
  AGENT_MANAGE: 'admin:agent:manage',
  AGENT_READONLY: 'admin:agent:readonly',
  OBSERVABILITY_LOG_VIEW: 'admin:observability:log:view',
} as const;

export const ALL_PERMISSION = '*:*:*' as const;
