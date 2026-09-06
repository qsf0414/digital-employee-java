package com.digital.employee.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.digital.employee.common.core.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotLoginExpired() {
        NotLoginException ex = new NotLoginException("token-expired", "login-type", NotLoginException.TOKEN_TIMEOUT);
        ResponseEntity<Result<Void>> response = handler.handleNotLogin(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
    }

    @Test
    void handleNotLoginSessionReplaced() {
        NotLoginException ex = new NotLoginException("token-be-replaced", "login-type", NotLoginException.BE_REPLACED);
        ResponseEntity<Result<Void>> response = handler.handleNotLogin(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("SESSION_REPLACED", response.getBody().getCode());
    }

    @Test
    void handleNotLoginDefault() {
        NotLoginException ex = new NotLoginException("token-invalid", "login-type", NotLoginException.INVALID_TOKEN);
        ResponseEntity<Result<Void>> response = handler.handleNotLogin(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
    }

    @Test
    void handleNotPermission() {
        NotPermissionException ex = new NotPermissionException("admin:user:manage");
        ResponseEntity<Result<Void>> response = handler.handleNotPermission(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("PERMISSION_DENIED", response.getBody().getCode());
    }

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException("USER_NOT_FOUND", "用户不存在");
        ResponseEntity<Result<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("USER_NOT_FOUND", response.getBody().getCode());
        assertEquals("用户不存在", response.getBody().getMessage());
    }

    @Test
    void handleGenericException() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<Result<Void>> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("SYSTEM_ERROR", response.getBody().getCode());
    }
}
