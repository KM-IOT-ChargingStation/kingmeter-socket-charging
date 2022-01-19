package com.kingmeter.charging.socket.test;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.acl.TestService;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.charging.socket.test.dto.TestQueryDockBikeInfoDto;
import com.kingmeter.charging.socket.test.dto.TestQueryDockInfoDto;
import com.kingmeter.charging.socket.test.dto.TestUnLockDto;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.dto.charging.v2.socket.in.ForceUnLockRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.QueryDockBikeInfoRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.QueryDockInfoRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.ScanUnLockRequestDto;
import com.kingmeter.dto.charging.v2.socket.out.ForceUnLockResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.QueryDockBikeInfoResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.QueryDockInfoResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.ScanUnlockResponseDto;
import com.kingmeter.socket.framework.application.SocketApplication;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.HardWareUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.alibaba.fastjson.JSON.toJSON;

/**
 * @description:
 * @author: crazyandy
 */
@Slf4j
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private SocketApplication socketApplication;

    @Override
    public boolean dealWithDockBikeInfoNotify(QueryDockBikeInfoRequestDto requestDto) {
        long siteId = requestDto.getSid();
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        if (cacheUtil.getTestQueryDockBikeInfoMap().containsKey(siteId)) {

            TestQueryDockBikeInfoDto testQueryDockBikeInfoDto =
                    cacheUtil.getTestQueryDockBikeInfoMap().get(siteId);

            long currentTimeStamp = System.currentTimeMillis();

            log.info(new KingMeterMarker("Socket,QueryDockBikeInfo,CC01"),
                    "{}|{}|{}", requestDto.getSid(),
                    JSONObject.toJSONString(requestDto.getState()),
                    ((float) (currentTimeStamp - testQueryDockBikeInfoDto.getStartTimeStamp())) / 1000f);

            QueryDockBikeInfoResponseDto dto =
                    new QueryDockBikeInfoResponseDto(testQueryDockBikeInfoDto.getDockId());

            sleepBeforeDoingTest(testQueryDockBikeInfoDto.getIntervalTime());

            testQueryDockBikeInfoDto.setStartTimeStamp(System.currentTimeMillis());
            cacheUtil.getTestQueryDockBikeInfoMap().put(siteId, testQueryDockBikeInfoDto);

            log.info(new KingMeterMarker("Socket,QueryDockBikeInfo,CC02"),
                    "{}|{}", siteId, 0);

            socketApplication.sendSocketMsg(siteId,
                    ServerFunctionCodeType.QueryDockBikeInfo, JSONObject.toJSON(dto).toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean dealWithQueryDockInfoNotify(QueryDockInfoRequestDto requestDto) {
        long siteId = requestDto.getSid();
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        if (cacheUtil.getTestQueryDockInfoMap().containsKey(siteId)) {
            TestQueryDockInfoDto testQueryDockInfoDto = cacheUtil.getTestQueryDockInfoMap().get(siteId);
            long currentTimeStamp = System.currentTimeMillis();

            log.info(new KingMeterMarker("Socket,QueryDockInfo,C601"),
                    "{}|{}|{}|{}|{}|{}|{}", siteId,
                    requestDto.getMsv(), requestDto.getMhv(),
                    requestDto.getRsv(), requestDto.getRhv(),
                    JSONObject.toJSONString(requestDto.getState()),
                    ((float) (currentTimeStamp - testQueryDockInfoDto.getStartTimeStamp())) / 1000f);

            QueryDockInfoResponseDto queryDockInfoResponseDto =
                    new QueryDockInfoResponseDto(
                            testQueryDockInfoDto.getDockId()
                    );

            sleepBeforeDoingTest(testQueryDockInfoDto.getIntervalTime());

            testQueryDockInfoDto.setStartTimeStamp(System.currentTimeMillis());
            cacheUtil.getTestQueryDockInfoMap().put(siteId, testQueryDockInfoDto);

            log.info(new KingMeterMarker("Socket,QueryDockInfo,C602"),
                    "{}|{}", siteId, 0);
            socketApplication.sendSocketMsg(siteId,
                    ServerFunctionCodeType.QueryDockInfo,
                    toJSON(queryDockInfoResponseDto).toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean dealWithScanUnlockNotify(ScanUnLockRequestDto requestDto) {
        long siteId = requestDto.getSid();
        int timezone = getTimezone(siteId);
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        if (cacheUtil.getTestUnLockInfoMap().containsKey(siteId)) {
            TestUnLockDto unLockDto = cacheUtil.getTestUnLockInfoMap().get(siteId);
            long currentTimeStamp = System.currentTimeMillis();

            log.info(new KingMeterMarker("Socket,ScanUnLock,C101"),
                    "{}|{}|{}|{}|{}|{}|{}|{}",
                    siteId, requestDto.getKid(),
                    requestDto.getBid(), requestDto.getUid(), requestDto.getGbs(),
                    requestDto.getTim(),
                    HardWareUtils.getInstance()
                            .getLocalTimeByHardWareTimeStamp(
                                    timezone,
                                    requestDto.getTim()),
                    ((float) (currentTimeStamp - unLockDto.getStartTimeStamp())) / 1000f);


            sleepBeforeDoingTest(unLockDto.getIntervalTime());

            ScanUnlockResponseDto response = new
                    ScanUnlockResponseDto(requestDto.getKid(), unLockDto.getUserId(),
                    0,
                    HardWareUtils.getInstance().getUtcTimeStampOnDevice(
                            getTimezone(siteId)
                    ));


            unLockDto.setStartTimeStamp(System.currentTimeMillis());
            cacheUtil.getTestUnLockInfoMap().put(siteId, unLockDto);

            log.info(new KingMeterMarker("Socket,ScanUnLock,C102"),
                    "{}|{}|{}|{}|{}", siteId, unLockDto.getDockId(),
                    unLockDto.getUserId(), 0,
                    HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

            socketApplication.sendSocketMsg(siteId,
                    ServerFunctionCodeType.ScanUnLock,
                    toJSON(response).toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean dealWithForceUnlockNotify(ForceUnLockRequestDto requestDto) {
        long siteId = requestDto.getSid();
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        if (cacheUtil.getTestForceLockInfoMap().containsKey(siteId)) {
            int timezone = getTimezone(siteId);

            TestUnLockDto unLockDto = cacheUtil.getTestForceLockInfoMap().get(siteId);
            long currentTimeStamp = System.currentTimeMillis();

            log.info(new KingMeterMarker("Socket,ForceUnLock,C103"),
                    "{}|{}|{}|{}|{}|{}|{}|{}",
                    siteId, requestDto.getKid(),
                    requestDto.getBid(), requestDto.getUid(), requestDto.getGbs(),
                    requestDto.getTim(),
                    HardWareUtils.getInstance()
                            .getLocalTimeByHardWareTimeStamp(
                                    timezone,
                                    requestDto.getTim()),
                    ((float) (currentTimeStamp - unLockDto.getStartTimeStamp())) / 1000f);

            ForceUnLockResponseDto responseDto =
                    new ForceUnLockResponseDto(
                            requestDto.getKid(), unLockDto.getUserId(),
                            HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

            sleepBeforeDoingTest(unLockDto.getIntervalTime());

            unLockDto.setStartTimeStamp(System.currentTimeMillis());
            cacheUtil.getTestUnLockInfoMap().put(siteId, unLockDto);

            log.info(new KingMeterMarker("Socket,ForceUnLock,C104"),
                    "{}|{}|{}|{}",
                    siteId, unLockDto.getDockId(),
                    unLockDto.getUserId(), responseDto.getTim());

            socketApplication.sendSocketMsg(siteId,
                    ServerFunctionCodeType.ForceUnLock,
                    JSONObject.toJSON(responseDto).toString());
            return true;
        }
        return false;
    }

    private void sleepBeforeDoingTest(int intervalTime) {
        try {
            Thread.sleep(intervalTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getTimezone(long siteId) {
        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                siteId
        );
        if (siteMap == null) throw new KingMeterException(ResponseCode.Device_Not_Logon);

        return Integer.parseInt(siteMap.get("timezone"));
    }
}
