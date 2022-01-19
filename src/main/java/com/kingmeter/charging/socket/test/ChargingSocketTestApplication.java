package com.kingmeter.charging.socket.test;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.business.tracker.OTACacheUtil;
import com.kingmeter.charging.socket.business.tracker.OTATracker;
import com.kingmeter.charging.socket.business.tracker.OTATrackerStepType;
import com.kingmeter.charging.socket.test.dto.TestQueryDockBikeInfoDto;
import com.kingmeter.charging.socket.test.dto.TestQueryDockInfoDto;
import com.kingmeter.charging.socket.test.dto.TestUnLockDto;
import com.kingmeter.constant.OTAChargingType;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.dto.charging.v2.rest.request.OTARequestRestDto;
import com.kingmeter.dto.charging.v2.socket.in.vo.DockStateInfoFromHeartBeatVO;
import com.kingmeter.dto.charging.v2.socket.out.ForceUnLockResponseDto;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.dto.charging.v2.socket.out.QueryDockBikeInfoResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.QueryDockInfoResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.ScanUnlockResponseDto;
import com.kingmeter.socket.framework.application.SocketApplication;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.HardWareUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.fastjson.JSON.toJSON;


@Slf4j
@Service
public class ChargingSocketTestApplication {

    @Autowired
    private SocketApplication socketApplication;

    @Autowired
    private OTATracker tracker;


    public void stopUnlock(long siteId) {
        TestMemoryCache.getInstance().getUnlockFlag().remove(siteId);
    }


    public String batchUnlock(long siteId, int times,
                              long perSite, long perDock) {

        TestMemoryCache.getInstance().getUnlockFlag().put(siteId, true);

        new Thread(new TestUnlockPerTime(siteId, times, perSite, perDock)).start();

        return "batch unlock succeed";
    }


    public void stopCheckDockLock(long siteId) {
        TestMemoryCache.getInstance().getCheckLockFlag().remove(siteId);
    }

    public String batchCheckDockLock(long siteId, int times,
                                     long perSite, long perDock) {
        TestMemoryCache.getInstance().getCheckLockFlag().put(siteId, true);

        new Thread(new TestCheckDockLockPerTime(siteId, times, perSite, perDock)).start();

        return "batch check dock lock succeed";
    }

    class TestCheckDockLockPerTime implements Runnable {

        private long siteId;
        private int times;
        private long perSite;
        private long perDock;

        public TestCheckDockLockPerTime(long siteId, int times,
                                        long perSite, long perDock) {
            this.siteId = siteId;
            this.times = times;
            this.perSite = perSite;
            this.perDock = perDock;
        }

        public void run() {
            for (int i = 0; i < times; i++) {
                if (TestMemoryCache.getInstance().getCheckLockFlag().containsKey(siteId) &&
                        TestMemoryCache.getInstance().getCheckLockFlag().get(siteId)) {
                    Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                            siteId
                    );
                    List<DockStateInfoFromHeartBeatVO> stateList = JSON.parseArray(siteMap.get("dockArray"), DockStateInfoFromHeartBeatVO.class);

                    boolean flag = checkDockLockSingle(siteId, stateList, perDock);
                    if (!flag) {
                        TestMemoryCache.getInstance().getCheckLockFlag().remove(siteId);
                        break;
                    }
                    try {
                        Thread.sleep(perSite);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }
    }

    private boolean checkDockLockSingle(long siteId, List<DockStateInfoFromHeartBeatVO> stateList, long perDock) {
        try {
            String userId = "123";

            Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                    siteId
            );

            int timezone = Integer.parseInt(siteMap.get("timezone"));

            for (DockStateInfoFromHeartBeatVO vo : stateList) {
                ForceUnLockResponseDto response = new
                        ForceUnLockResponseDto(vo.getKid(), userId,
                        HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

                socketApplication.sendSocketMsg(siteId,
                        ServerFunctionCodeType.CheckDockLockStatus,
                        toJSON(response).toString());

                log.info(new KingMeterMarker("Socket,CheckDockLockStatus,C702"),
                        "{}|{}|{}",
                        siteId, vo.getKid(), userId);

                try {
                    Thread.sleep(perDock);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    class TestUnlockPerTime implements Runnable {

        private long siteId;
        private int times;
        private long perSite;
        private long perDock;

        public TestUnlockPerTime(long siteId, int times,
                                 long perSite, long perDock) {
            this.siteId = siteId;
            this.times = times;
            this.perSite = perSite;
            this.perDock = perDock;
        }

        public void run() {
            for (int i = 0; i < times; i++) {
                if (TestMemoryCache.getInstance().getUnlockFlag().containsKey(siteId) &&
                        TestMemoryCache.getInstance().getUnlockFlag().get(siteId)) {
                    Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                            siteId
                    );
                    List<DockStateInfoFromHeartBeatVO> stateList = JSON.parseArray(siteMap.get("dockArray"), DockStateInfoFromHeartBeatVO.class);

                    boolean flag = unlockSingle(siteId, stateList, perDock);
                    if (!flag) {
                        TestMemoryCache.getInstance().getUnlockFlag().remove(siteId);
                        break;
                    }
                    try {
                        Thread.sleep(perSite);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }
    }

    private boolean unlockSingle(long siteId, List<DockStateInfoFromHeartBeatVO> stateList, long perDock) {
        try {
            String userId = "123";

            Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                    siteId
            );

            int timezone = Integer.parseInt(siteMap.get("timezone"));

            for (DockStateInfoFromHeartBeatVO vo : stateList) {
                ForceUnLockResponseDto response = new
                        ForceUnLockResponseDto(vo.getKid(), userId,
                        HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

                socketApplication.sendSocketMsg(siteId,
                        ServerFunctionCodeType.ForceUnLock,
                        toJSON(response).toString());

                log.info(new KingMeterMarker("Socket,Force,C104"),
                        "{}|{}|{}|{}",
                        siteId, vo.getKid(),
                        userId, response.getTim());

                try {
                    Thread.sleep(perDock);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void testOTA(OTARequestRestDto restDto) {
        if (!OTAChargingType.containsName(restDto.getType())) {
            throw new KingMeterException(ResponseCode.BadParameters);
        }

        //check this site is online or not
        socketApplication.getTokenFromCache(String.valueOf(restDto.getSiteId()));

        OTAChargingType.validateFileName(OTAChargingType.getEnumByName(restDto.getType()),
                restDto.getFileName());

        long siteId = restDto.getSiteId();

        //3,send command to lock by socket
        if (OTAChargingType.getEnumByName(restDto.getType()) == OTAChargingType.ICPU ||
                OTAChargingType.getEnumByName(restDto.getType()) == OTAChargingType.DISPLAY ||
                OTAChargingType.getEnumByName(restDto.getType()) == OTAChargingType.FONTLIBRARY ||
                OTAChargingType.getEnumByName(restDto.getType()) == OTAChargingType.EICC) {
            Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);
            List<DockStateInfoFromHeartBeatVO> stateList = JSONObject.parseArray(
                    siteMap.get("dockArray")
                    , DockStateInfoFromHeartBeatVO.class);

            long timeOut = 120 + stateList.size() * 60 + 200;

            socketApplication.setHeartBeatIdleTime(siteId, timeOut);
        }

        OTACacheUtil otaCacheUtil = OTACacheUtil.getInstance();
        ConcurrentMap<Long, OTARequestRestDto> otaInfoMap =
                otaCacheUtil.getOtaInfoMap();

        otaInfoMap.put(siteId, restDto);
        otaCacheUtil.setOtaInfoMap(otaInfoMap);

        ConcurrentMap<Long,Boolean> otaLoopMap = otaCacheUtil.getOtaLoopMap();
        otaLoopMap.put(siteId,true);
        otaCacheUtil.setOtaLoopMap(otaLoopMap);

        QueryDockBikeInfoResponseDto requestDto =
                new QueryDockBikeInfoResponseDto(0);

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockBikeInfo,
                toJSON(requestDto).toString());

        tracker.track_log(siteId, OTATrackerStepType.QueryDockBikeInfoRequest_Before,
                requestDto);
    }


    public void deleteOTAFLag(long siteId) {
        OTACacheUtil otaCacheUtil = OTACacheUtil.getInstance();
        ConcurrentMap<Long, OTARequestRestDto> otaInfoMap =
                otaCacheUtil.getOtaInfoMap();

        if (otaInfoMap.containsKey(siteId)) {
            otaInfoMap.remove(siteId);
            otaCacheUtil.setOtaInfoMap(otaInfoMap);
        }
        ConcurrentMap<Long,Boolean> otaLoopMap = otaCacheUtil.getOtaLoopMap();
        if(otaLoopMap.containsKey(siteId)){
            otaInfoMap.remove(siteId);
            otaCacheUtil.setOtaLoopMap(otaLoopMap);
        }
    }


    public void testScanUnLock(TestUnLockDto unLockDto) {
        long siteId = unLockDto.getSiteId();
        int timezone = getTimezone(siteId);
        unLockDto.setStartTimeStamp(System.currentTimeMillis());
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestUnLockInfoMap().put(siteId, unLockDto);
        ScanUnlockResponseDto response = new
                ScanUnlockResponseDto(unLockDto.getDockId(), unLockDto.getUserId(),
                0,
                HardWareUtils.getInstance().getUtcTimeStampOnDevice(
                        getTimezone(siteId)
                ));

        log.info(new KingMeterMarker("Socket,ScanUnLock,C102"),
                "{}|{}|{}|{}|{}", siteId, unLockDto.getDockId(),
                unLockDto.getUserId(), 0,
                HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.ScanUnLock,
                toJSON(response).toString());
    }

    public void testForceUnLock(TestUnLockDto unLockDto) {
        long siteId = unLockDto.getSiteId();
        unLockDto.setStartTimeStamp(System.currentTimeMillis());
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestForceLockInfoMap().put(siteId, unLockDto);
        ForceUnLockResponseDto responseDto =
                new ForceUnLockResponseDto(
                        unLockDto.getDockId(), unLockDto.getUserId(),
                        HardWareUtils.getInstance().getUtcTimeStampOnDevice(
                                getTimezone(siteId)
                        ));

        log.info(new KingMeterMarker("Socket,ForceUnLock,C104"),
                "{}|{}|{}|{}",
                siteId, unLockDto.getDockId(),
                unLockDto.getUserId(), responseDto.getTim());

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.ForceUnLock,
                toJSON(responseDto).toString());
    }


    public void testQueryDockInfo(TestQueryDockInfoDto dockInfoDto) {
        long siteId = dockInfoDto.getSiteId();
        dockInfoDto.setStartTimeStamp(System.currentTimeMillis());
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestQueryDockInfoMap().put(siteId, dockInfoDto);
        QueryDockInfoResponseDto queryDockInfoResponseDto =
                new QueryDockInfoResponseDto(
                        dockInfoDto.getDockId()
                );

        log.info(new KingMeterMarker("Socket,QueryDockInfo,C602"),
                "{}|{}", siteId, 0);

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockInfo,
                toJSON(queryDockInfoResponseDto).toString());
    }


    public void testQueryDockBikeInfo(TestQueryDockBikeInfoDto dockBikeInfoDto) {
        long siteId = dockBikeInfoDto.getSiteId();
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        dockBikeInfoDto.setStartTimeStamp(System.currentTimeMillis());
        cacheUtil.getTestQueryDockBikeInfoMap().put(siteId, dockBikeInfoDto);
        QueryDockBikeInfoResponseDto dto =
                new QueryDockBikeInfoResponseDto(dockBikeInfoDto.getDockId());

        log.info(new KingMeterMarker("Socket,QueryDockBikeInfo,CC02"),
                "{}|{}", siteId, dto.getKid());

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockBikeInfo, JSONObject.toJSON(dto).toString());
    }

    public void deleteScanUnLockFlag(long siteId) {
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestUnLockInfoMap().remove(siteId);
    }

    public void deleteForceUnLockFlag(long siteId) {
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestForceLockInfoMap().remove(siteId);
    }

    public void deleteQueryDockInfoFlag(long siteId) {
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestQueryDockInfoMap().remove(siteId);
    }

    public void deleteQueryDockBikeInfoFlag(long siteId) {
        TestCacheUtil cacheUtil = TestCacheUtil.getInstance();
        cacheUtil.getTestQueryDockBikeInfoMap().remove(siteId);
    }


    private int getTimezone(long siteId) {
        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                siteId
        );
        if (siteMap == null) throw new KingMeterException(ResponseCode.Device_Not_Logon);

        return Integer.parseInt(siteMap.get("timezone"));
    }

}
