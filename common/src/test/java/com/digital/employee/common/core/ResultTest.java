package com.digital.employee.common.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void successWithData() {
        Result<String> result = Result.success("hello");
        assertEquals("OK", result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void successWithoutData() {
        Result<Void> result = Result.success();
        assertEquals("OK", result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failWithCodeAndMessage() {
        Result<Void> result = Result.fail("ERR_001", "something went wrong");
        assertEquals("ERR_001", result.getCode());
        assertEquals("something went wrong", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void successWithNullData() {
        Result<Object> result = Result.success(null);
        assertEquals("OK", result.getCode());
        assertNull(result.getData());
    }

    @Test
    void failWithEmptyCode() {
        Result<Void> result = Result.fail("", "msg");
        assertEquals("", result.getCode());
        assertEquals("msg", result.getMessage());
    }

    @Test
    void successWithIntegerData() {
        Result<Integer> result = Result.success(42);
        assertEquals("OK", result.getCode());
        assertEquals(42, result.getData());
    }
}
