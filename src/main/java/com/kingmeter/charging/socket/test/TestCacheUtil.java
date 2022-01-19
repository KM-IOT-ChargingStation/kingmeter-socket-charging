package com.kingmeter.charging.socket.test;

import com.kingmeter.charging.socket.test.dto.TestQueryDockBikeInfoDto;
import com.kingmeter.charging.socket.test.dto.TestQueryDockInfoDto;
import com.kingmeter.charging.socket.test.dto.TestUnLockDto;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Data
public class TestCacheUtil {
    private volatile static TestCacheUtil instance;

    private TestCacheUtil() {
    }

    public static TestCacheUtil getInstance() {
        if (instance == null) {
            synchronized (TestCacheUtil.class) {
                if (instance == null) {
                    instance = new TestCacheUtil();
                }
            }
        }
        return instance;
    }


    /**
     * key: siteId
     * value : TestUnLockDto
     */
    private volatile ConcurrentMap<Long, TestUnLockDto> testUnLockInfoMap = new ConcurrentHashMap<>();

    private volatile ConcurrentMap<Long, TestUnLockDto> testForceLockInfoMap = new ConcurrentHashMap<>();

    private volatile ConcurrentMap<Long, TestQueryDockInfoDto> testQueryDockInfoMap = new ConcurrentHashMap<>();

    private volatile ConcurrentMap<Long, TestQueryDockBikeInfoDto> testQueryDockBikeInfoMap = new ConcurrentHashMap<>();
}
