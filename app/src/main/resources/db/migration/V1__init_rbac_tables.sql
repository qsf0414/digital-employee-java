CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_key VARCHAR(64) NOT NULL UNIQUE CHECK (role_key ~ '^[a-z0-9_]+$'),
    role_name VARCHAR(64) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20) UNIQUE,
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    status SMALLINT NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_user_role_id ON sys_user(role_id);
CREATE INDEX idx_sys_user_status ON sys_user(status);

CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(64) NOT NULL,
    menu_type CHAR(1) NOT NULL CHECK (menu_type IN ('M', 'C', 'F')),
    path VARCHAR(128),
    component VARCHAR(128),
    perms VARCHAR(128),
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    visible SMALLINT NOT NULL DEFAULT 1 CHECK (visible IN (0, 1)),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_menu_parent ON sys_menu(parent_id);
CREATE INDEX idx_sys_menu_perms ON sys_menu(perms);

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    menu_id BIGINT NOT NULL REFERENCES sys_menu(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_id)
);
CREATE INDEX idx_role_menu_mid ON sys_role_menu(menu_id);

CREATE TABLE sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    ip VARCHAR(64),
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    method VARCHAR(10),
    url VARCHAR(256),
    duration_ms INT,
    status SMALLINT NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    error_msg TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_log_user ON sys_audit_log(user_id);
CREATE INDEX idx_audit_log_created ON sys_audit_log(created_at);
