package com.mes.common.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result / PageResult 单元测试
 */
class ResultTest {

    @Test
    void ok_withoutData() {
        Result<Void> r = Result.ok();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void ok_withData() {
        Result<String> r = Result.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void fail_withDefaultCode() {
        Result<Void> r = Result.fail("出错了");
        assertEquals(500, r.getCode());
        assertEquals("出错了", r.getMessage());
    }

    @Test
    void fail_withCustomCode() {
        Result<Void> r = Result.fail(400, "参数错误");
        assertEquals(400, r.getCode());
        assertEquals("参数错误", r.getMessage());
    }

    @Test
    void pageResult_builds() {
        PageResult<String> pr = PageResult.of(100, List.of("a", "b"), 3, 10);
        assertEquals(100, pr.getTotal());
        assertEquals(2, pr.getRecords().size());
        assertEquals(3, pr.getCurrent());
        assertEquals(10, pr.getSize());
    }
}
