package com.kingmeter.charging.socket.rest;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.charging.socket.business.tracker.OTACacheUtil;
import com.kingmeter.charging.socket.business.tracker.OTATracker;
import com.kingmeter.charging.socket.business.tracker.OTATrackerStepType;
import com.kingmeter.constant.OTAChargingType;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.dto.charging.v2.rest.request.OTARequestRestDto;
import com.kingmeter.dto.charging.v2.rest.request.ScanUnlockRequestRestDto;
import com.kingmeter.dto.charging.v2.rest.response.*;
import com.kingmeter.dto.charging.v2.socket.in.vo.DockStateInfoFromHeartBeatVO;
import com.kingmeter.dto.charging.v2.socket.out.*;
import com.kingmeter.socket.framework.application.SocketApplication;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.HardWareUtils;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.fastjson.JSON.toJSON;


@Slf4j
@Service
public class ChargingSocketApplication {

    @Autowired
    private SocketApplication socketApplication;

    @Autowired
    private OTATracker tracker;


    @Value("${kingmeter.ota.tracker}")
    private boolean trackerActive;

    /**
     * 扫码租车
     *
     * @param restDto
     */
    public ScanUnlockResponseRestDto scanUnlock(ScanUnlockRequestRestDto restDto) {
        long siteId = restDto.getSiteId();
        long dockId = restDto.getDockId();
        String userId = restDto.getUserId();

        int timezone = getTimezone(siteId);

        ScanUnlockResponseDto response = new
                ScanUnlockResponseDto(dockId, userId, restDto.getMinbsoc(),
                HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.ScanUnLock,
                toJSON(response).toString());

        String key = "scan_" + userId + "_" + dockId;
        log.info(new KingMeterMarker("Socket,ScanUnLock,C102"),
                "{}|{}|{}|{}|{}", siteId, dockId, userId, restDto.getMinbsoc(),
                HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

        return (ScanUnlockResponseRestDto) socketApplication.waitForPromiseResult(key, channel);
    }

    /**
     * 强制开锁
     *
     * @param siteId
     * @param dockId
     * @param userId
     * @return
     */
    public ForceUnLockResponseRestDto foreUnlock(long siteId, long dockId, String userId) {
        int timezone = getTimezone(siteId);
        ForceUnLockResponseDto responseDto =
                new ForceUnLockResponseDto(
                        dockId, userId,
                        HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));
        log.info(new KingMeterMarker("Socket,ForceUnLock,C104"),
                "{}|{}|{}|{}",
                siteId, dockId,
                userId, responseDto.getTim());

        String key = "force_" + userId;

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.ForceUnLock,
                JSONObject.toJSON(responseDto).toString());

        //4,wait for lock response
        return  (ForceUnLockResponseRestDto)socketApplication.waitForPromiseResult(key, channel);
    }

    /**
     * 查询硬件版本
     *
     * @param siteId
     * @param dockId
     * @return
     */
    public QueryDockInfoResponseRestDto dealWithQueryDockInfo(long siteId, long dockId) {
        QueryDockInfoResponseDto queryDockInfoResponseDto =
                new QueryDockInfoResponseDto(dockId);
        String key = siteId + "_QueryDockInfo";

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockInfo,
                toJSON(queryDockInfoResponseDto).toString());

        log.info(new KingMeterMarker("Socket,QueryDockInfo,C602"),
                "{}|{}", siteId, dockId);

        return  (QueryDockInfoResponseRestDto)socketApplication.waitForPromiseResult(key, channel);
    }

    /**
     * 清除故障
     *
     * @param siteId
     * @param dockId
     * @param error
     * @return
     */
    public MalfunctionClearResponseRestDto clearMalfunction(long siteId, long dockId, int error) {
        DockMalfunctionClearResponseDto dto =
                new DockMalfunctionClearResponseDto(dockId, error);
        //3,send command to lock by socket
        String key = siteId + "_MalfunctionClear";

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.DockMalfunctionClear, JSONObject.toJSON(dto).toString());

        log.info(new KingMeterMarker("Socket,MalfunctionClear,CA02"),
                "{}|{}|{}", siteId, dockId, error);

        return (MalfunctionClearResponseRestDto)socketApplication.waitForPromiseResult(key, channel);
    }

    /**
     * 站点设置
     *
     * @param siteId
     * @param dataInterval
     * @param heartInterval
     * @param repeatTime
     * @param monitor
     */
    public SiteSettingResponseRestDto siteSetting(long siteId, int dataInterval,
                                                  int heartInterval,
                                                  int repeatTime,
                                                  int beep,
                                                  int monitor) {
        if (dataInterval < 5 || dataInterval > 600) {
            throw new KingMeterException(ResponseCode.BadParameters);
        }
        if (heartInterval != 0) {
            if (heartInterval < 5 || heartInterval > 600) {
                throw new KingMeterException(ResponseCode.BadParameters);
            }
        }
        if (repeatTime != -1 && repeatTime != 0) {
            throw new KingMeterException(ResponseCode.BadParameters);
        }
        if (beep != 0 && beep != 1) {
            throw new KingMeterException(ResponseCode.BadParameters);
        }
        if (monitor != 0) {
            if (monitor < 10 || monitor > 600) {
                throw new KingMeterException(ResponseCode.BadParameters);
            }
        }

        SiteSettingResponseDto dto =
                new SiteSettingResponseDto(dataInterval,
                        heartInterval, repeatTime, beep, monitor);

        //2,set heartbeat idle time
        socketApplication.setHeartBeatIdleTime(siteId, heartInterval);

        //3,send command to lock by socket
        String key = siteId + "_SiteSetting";

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.SiteSetting, JSONObject.toJSON(dto).toString());

        log.info(new KingMeterMarker("Socket,SiteSetting,CB02"),
                "{}|{}|{}|{}|{}|{}", siteId, dataInterval, heartInterval,
                repeatTime, beep, monitor);

        return (SiteSettingResponseRestDto)socketApplication.waitForPromiseResult(key, channel);
    }


    /**
     * 桩体车辆信息同步
     *
     * @param siteId
     * @param dockId
     */
    public QueryDockBikeInfoRequestRestDto queryDockAndBikeInfo(long siteId, long dockId) {
        QueryDockBikeInfoResponseDto dto =
                new QueryDockBikeInfoResponseDto(dockId);

        //3,send command to lock by socket
        String key = siteId + "_QueryDockBikeInfo";

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockBikeInfo, JSONObject.toJSON(dto).toString());

        log.info(new KingMeterMarker("Socket,QueryDockBikeInfo,CC02"),
                "{}|{}", siteId, dto.getKid());

        return (QueryDockBikeInfoRequestRestDto)socketApplication.waitForPromiseResult(key, channel);
    }

    /**
     * 查询桩体锁状态
     *
     * @param siteId
     * @param dockId
     * @param userId
     * @return
     */
    public QueryDockLockStatusResponseRestDto queryDockLockStatus(long siteId, long dockId, String userId) {
        QueryDockLockStatusResponseDto responseDto =
                new QueryDockLockStatusResponseDto(dockId,
                        userId);

        String key = siteId + "_checkDockLockStatus";

        SocketChannel channel = socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.CheckDockLockStatus,
                JSONObject.toJSON(responseDto).toString());

        log.info(new KingMeterMarker("Socket,CheckDockLockStatus,C702"),
                "{}|{}|{}", siteId, dockId, userId);

        return (QueryDockLockStatusResponseRestDto)socketApplication.waitForPromiseResult(key, channel);
    }


    /**
     * 设置IP，端口
     *
     * @param siteId
     * @param sls
     * @param pwd
     * @param url
     * @param port
     * @param companyCode
     * @param timezone
     */
    public void changeIpAndPort(long siteId, int sls,
                                String pwd, String url,
                                int port, String companyCode,
                                int timezone) {

        LoginResponseDto responseDto = new LoginResponseDto(sls, pwd, url, port,
                -1, -1, companyCode,
                HardWareUtils.getInstance().getUtcTimeStampOnDevice(timezone));

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.LoginType, JSONObject.toJSON(responseDto).toString());


        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                siteId
        );
        if (siteMap == null) throw new KingMeterException(ResponseCode.Device_Not_Logon);
        siteMap.put("timezone", String.valueOf(timezone));

        log.info(new KingMeterMarker("Socket,Login,C002"),
                "{}|{}|{}|{}|{}|{}|{}|{}|{}|{}", siteId,
                responseDto.getSls(), responseDto.getPwd(),
                responseDto.getUrl(), responseDto.getPot(),
                responseDto.getKnum(), responseDto.getBnum(),
                responseDto.getCid(), responseDto.getTim(),
                HardWareUtils.getInstance()
                        .getLocalTimeByHardWareTimeStamp(
                                timezone,
                                responseDto.getTim()));
    }

    /**
     * 查询 硬件设备列表
     *
     * @return
     */
    public List<ConnectionDto> queryConnection() {
        List<ConnectionDto> result = new ArrayList<>();
        Map<String, SocketChannel> map = CacheUtil.getInstance().getDeviceIdAndChannelMap();
        for (Map.Entry<String, SocketChannel> entry : map.entrySet()) {
            String deviceId = entry.getKey();
            SocketChannel channel = entry.getValue();
            result.add(new ConnectionDto(deviceId, channel.remoteAddress().getHostString(),
                    channel.remoteAddress().getPort()));
        }
        return result;
    }

    /**
     * ota 远程升级
     *
     * @param restDto
     * @return
     */
    public OTAResponseRestDto upgradeMultipleDevice(OTARequestRestDto restDto) {

        if (!OTAChargingType.containsName(restDto.getType())) {
            throw new KingMeterException(ResponseCode.BadParameters);
        }

        //check this site is online or not
        socketApplication.getTokenFromCache(String.valueOf(restDto.getSiteId()));

        OTAChargingType.validateFileName(OTAChargingType.getEnumByName(restDto.getType()),
                restDto.getFileName());

        OTAResponseDto responseDto = new OTAResponseDto(restDto.getFileUrl(), restDto.getHttpPort(),
                restDto.getFileName(), restDto.getVersion(), OTAChargingType.getEnumByName(restDto.getType()).value());

        long siteId = restDto.getSiteId();

        log.info(new KingMeterMarker("Socket,OTA,CE02"),
                "{}|{}|{}|{}|{}|{}", siteId, responseDto.getFurl(),
                responseDto.getFpot(),
                responseDto.getFname(),
                responseDto.getFver(),
                restDto.getType());

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

        String key = restDto.getSiteId() + "_OTA";
        SocketChannel channel = null;

        if (trackerActive) {
            OTACacheUtil otaCacheUtil = OTACacheUtil.getInstance();
            ConcurrentMap<Long, OTARequestRestDto> otaInfoMap =
                    otaCacheUtil.getOtaInfoMap();

            otaInfoMap.put(siteId, restDto);
            otaCacheUtil.setOtaInfoMap(otaInfoMap);

            QueryDockBikeInfoResponseDto requestDto =
                    new QueryDockBikeInfoResponseDto(0);

            channel = socketApplication.sendSocketMsg(siteId,
                    ServerFunctionCodeType.QueryDockBikeInfo,
                    toJSON(requestDto).toString());

            tracker.track_log(siteId, OTATrackerStepType.QueryDockBikeInfoRequest_Before,
                    requestDto);
        } else {
            channel = socketApplication.sendSocketMsg(siteId, ServerFunctionCodeType.OTACommand,
                    JSON.toJSON(responseDto).toString());
        }

        return (OTAResponseRestDto)socketApplication.waitForPromiseResult(key, channel);
    }


    private int getTimezone(long siteId) {
        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(
                siteId
        );
        if (siteMap == null) throw new KingMeterException(ResponseCode.Device_Not_Logon);

        return Integer.parseInt(siteMap.get("timezone"));
    }

}
