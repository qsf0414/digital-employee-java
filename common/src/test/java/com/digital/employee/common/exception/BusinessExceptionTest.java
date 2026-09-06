package com.digital.employee.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void shouldStoreCodeAndMessage() {
        BusinessException ex = new BusinessException("ERR_001", "test error");
        assertEquals("ERR_001", ex.getCode());
        assertEquals("test error", ex.getMessage());
    }

    @Test
    void shouldBeRuntimeException() {
        BusinessException ex = new BusinessException("X", "Y");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
