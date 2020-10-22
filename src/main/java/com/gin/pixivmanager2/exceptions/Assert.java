package com.gin.pixivmanager2.exceptions;

public interface Assert {
    /**
     * 创建异常
     *
     * @return 异常
     */
    BaseException newException(String msg);

    /**
     * 断言对象非null 否则抛出异常
     *
     * @param obj 对象
     * @param e   抛出自定义异常
     * @throws RuntimeException 抛出异常
     */
    default void assertNotNull(Object obj, RuntimeException e) throws RuntimeException {
        if (obj == null) {
            throw e;
        }
    }

    default void assertNotNull(Object obj, String msg) throws RuntimeException {
        assertNotNull(obj, newException(msg));
    }

    /**
     * 断言对象为true 否则抛出异常
     *
     * @param b 对象
     * @param e 抛出自定义异常
     * @throws RuntimeException 抛出异常
     */
    default void assertTrue(Boolean b, RuntimeException e) throws RuntimeException {
        assertNotNull(b, e);
        if (!b) {
            throw e;
        }
    }

    default void assertTrue(Boolean b, String msg) throws RuntimeException {
        assertTrue(b, newException(msg));
    }

    /**
     * 断言字符串长度 否则抛出异常
     *
     * @param s      字符串
     * @param length 最小长度
     * @param e      异常对象
     * @throws RuntimeException 抛出异常
     */
    default void assertLength(String s, int length, RuntimeException e) throws RuntimeException {
        assertNotNull(s, e);
        if (s.length() < length) {
            throw e;
        }
    }

    default void assertLength(String s, int length, String msg) throws RuntimeException {
        assertLength(s, length, newException(msg));
    }

}
