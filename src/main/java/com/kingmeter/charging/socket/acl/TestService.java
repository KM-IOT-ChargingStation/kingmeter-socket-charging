package com.kingmeter.charging.socket.acl;

import com.kingmeter.dto.charging.v2.socket.in.ForceUnLockRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.QueryDockBikeInfoRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.QueryDockInfoRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.ScanUnLockRequestDto;

/**
 * @description:
 * @author: crazyandy
 */
public interface TestService {
    default boolean dealWithDockBikeInfoNotify(QueryDockBikeInfoRequestDto requestDto) {
        return false;
    }
    default boolean dealWithQueryDockInfoNotify(QueryDockInfoRequestDto requestDto){
        return false;
    }
    default boolean dealWithScanUnlockNotify(ScanUnLockRequestDto requestDto){
        return false;
    }
    default boolean dealWithForceUnlockNotify(ForceUnLockRequestDto requestDto){
        return false;
    }
}
