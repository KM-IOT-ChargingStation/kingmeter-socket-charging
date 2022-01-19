package com.kingmeter.charging.socket.acl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.business.tracker.DockInfo4OTADto;
import com.kingmeter.charging.socket.business.tracker.OTATracker;
import com.kingmeter.charging.socket.business.tracker.OTATrackerStepType;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.constant.OTAChargingType;
import com.kingmeter.dto.charging.v2.rest.response.*;
import com.kingmeter.dto.charging.v2.rest.response.vo.DockStateFromQueryDockBikeInfoVOForRest;
import com.kingmeter.dto.charging.v2.rest.response.vo.DockStateInfoFromQueryDockInfoVOForRest;
import com.kingmeter.dto.charging.v2.socket.in.*;
import com.kingmeter.dto.charging.v2.socket.in.vo.DockStateFromQueryDockBikeInfoVO;
import com.kingmeter.dto.charging.v2.socket.in.vo.DockStateInfoFromHeartBeatVO;
import com.kingmeter.dto.charging.v2.socket.in.vo.DockStateInfoFromQueryDockInfoVO;
import com.kingmeter.dto.charging.v2.socket.out.*;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.HardWareUtils;
import com.kingmeter.utils.MD5Util;
import com.kingmeter.utils.TokenResult;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChargingSiteService {

    @Value("${kingmeter.requestBusiness}")
    private boolean requestBusiness;

    @Value("${kingmeter.default.companyCode}")
    private String defaultCompanyCode;

    @Value("${kingmeter.default.timezone}")
    private int defaultTimezone;

    @Autowired
    private BusinessService business;

    @Autowired
    private OTATracker tracker;

    @Autowired
    private TestService testService;

    public LoginPermissionDto getSiteLoginPermission(SiteLoginRequestDto requestDto,
                                                     TokenResult tokenResult, SocketChannel channel) {
        String companyCode = defaultCompanyCode;
        int timezone = defaultTimezone;

        LoginResponseDto responseDto = new LoginResponseDto(0, "", "", 0,
                -1, -1,
                companyCode, HardWareUtils.getInstance()
                .getUtcTimeStampOnDevice(timezone));

        long siteId = requestDto.getSid();

        //100419
        byte[] passwordArray = {49, 48, 48, 52, 49, 57};
        String passwordMd5 = MD5Util.MD5Encode(passwordArray);

        if (!requestDto.getPwd().equals(passwordMd5)) {
            return null;
        }

        if (requestBusiness) {
            LoginPermissionDto permission = business.getLoginPermission(requestDto);
            if (permission == null) {
                return null;
            } else if (permission.getResponseDto().getSls() != 0) {
                return null;
            } else {
                responseDto = permission.getResponseDto();
                companyCode = permission.getCompanyCode();
                timezone = permission.getTimezone();
            }
        }

        Map<String, String> siteMap = CacheUtil.getInstance()
                .getDeviceInfoMap()
                .getOrDefault(siteId, new ConcurrentHashMap<>());

        siteMap.put("token", tokenResult.getToken());
        siteMap.put("bikeCount", "0");
        siteMap.put("dockCount", "0");
        siteMap.put("dockArray", "");
        siteMap.put("channelId", channel.id().asLongText());
        siteMap.put("pwd", requestDto.getPwd());
        siteMap.put("msv", requestDto.getMsv());
        siteMap.put("mhv", requestDto.getMhv());

        siteMap.put("count", "0");

        siteMap.put("companyCode", companyCode);
        siteMap.put("timezone", String.valueOf(timezone));

        CacheUtil.getInstance().getDeviceInfoMap().put(siteId, siteMap);
        CacheUtil.getInstance().dealWithLoginSucceed(String.valueOf(siteId),
                tokenResult.getToken(), tokenResult.getTokenArray(), channel);
        CacheUtil.getInstance().getDeviceResultMap().put(siteId + "_queryDockInfoFlag", new HashMap<>());

        return new LoginPermissionDto(responseDto, companyCode, timezone);
    }


    public BikeInDockResponseDto createBikeInDockResponseDto(BikeInDockRequestDto requestDto,
                                                             ResponseBody responseBody, ChannelHandlerContext ctx) {
        if (!requestBusiness) {
            return new BikeInDockResponseDto(requestDto.getKid(), 0, 0, 0, 0);
        }
        return business.createBikeInDockResponseDto(requestDto);
    }

    public void dealWithScanUnLock(ScanUnLockRequestDto requestDto) {
        if (testService.dealWithScanUnlockNotify(requestDto)) {
            return;
        }

        long siteId = requestDto.getSid();

        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);

        log.info(new KingMeterMarker("Socket,ScanUnLock,C101"),
                "{}|{}|{}|{}|{}|{}|{}|{}",
                siteId,requestDto.getKid(),
                requestDto.getBid(),requestDto.getUid(),requestDto.getGbs(),
                requestDto.getTim(),
                HardWareUtils.getInstance()
                        .getLocalTimeByHardWareTimeStamp(
                                Integer.parseInt(siteMap.get("timezone")),
                                requestDto.getTim()),0);


        Map<String, String> result = new HashMap<>();
        result.put("ScanUnlock",
                JSON.toJSONString(new ScanUnlockResponseRestDto(requestDto.getSid(),
                        requestDto.getKid(), requestDto.getBid(),
                        requestDto.getUid(), requestDto.getGbs(),
                        HardWareUtils.getInstance()
                                .getLocalTimeStampByHardWareUtcTimeStamp(
                                        Integer.parseInt(siteMap.get("timezone")),
                                        requestDto.getTim()))));

        CacheUtil.getInstance().getDeviceResultMap().put(
                "scan_" + requestDto.getUid(), result);

        if (requestBusiness) business.dealWithScanUnLock(requestDto);
    }

    public void forceUnlockNotify(ForceUnLockRequestDto requestDto) {
        if (testService.dealWithForceUnlockNotify(requestDto)) {
            return;
        }
        long siteId = requestDto.getSid();

        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);

        log.info(new KingMeterMarker("Socket,ForceUnLock,C103"),
                "{}|{}|{}|{}|{}|{}|{}|{}",
                siteId, requestDto.getKid(),
                requestDto.getBid(), requestDto.getUid(), requestDto.getGbs(),
                requestDto.getTim(),
                HardWareUtils.getInstance()
                        .getLocalTimeByHardWareTimeStamp(
                                Integer.parseInt(siteMap.get("timezone")),
                                requestDto.getTim()),0);

        Map<String, String> result = new HashMap<>();
        result.put("ForceUnlock",
                JSON.toJSONString(new ForceUnLockResponseRestDto(requestDto.getSid(),
                        requestDto.getKid(), requestDto.getBid(),
                        requestDto.getUid(), requestDto.getGbs(),
                        HardWareUtils.getInstance()
                                .getLocalTimeStampByHardWareUtcTimeStamp(
                                        Integer.parseInt(siteMap.get("timezone")),
                                        requestDto.getTim()))));

        CacheUtil.getInstance().getDeviceResultMap().put(
                "force_" + requestDto.getUid(), result);

        if (requestBusiness) business.forceUnlockNotify(requestDto);
    }


    public SiteHeartResponseDto createSiteHeartResponseDto(long siteId) {
        Map<String, String> siteMap = CacheUtil.getInstance()
                .getDeviceInfoMap()
                .getOrDefault(siteId, new ConcurrentHashMap<>());

        int timezone = Integer.parseInt(siteMap.getOrDefault("timezone", String.valueOf(defaultTimezone)));
        return new SiteHeartResponseDto(HardWareUtils.getInstance().getUtcTimeStampOnDevice(
                timezone),
                0, -1);
    }


    public void heartBeatNotify(SiteHeartRequestDto requestDto, ResponseBody responseBody, ChannelHandlerContext ctx) {
        long siteId = requestDto.getSid();
        DockStateInfoFromHeartBeatVO[] state = requestDto.getState();
        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);
        siteMap.put("dockArray", JSON.toJSONString(state));

        siteMap.put("rpow", String.valueOf(requestDto.getRpow()));

        CacheUtil.getInstance().getDeviceInfoMap().put(siteId, siteMap);

        if (requestBusiness) business.heartBeatNotify(requestDto);
    }

    /**
     * deal with dock malfunction upload
     *
     * @param requestDto
     */
    public void malfunctionUploadNotify(DockMalfunctionUploadRequestDto requestDto) {
        if (requestBusiness) business.malfunctionUploadNotify(requestDto);
    }

    public void malfunctionClearNotify(MalfunctionClearRequestDto requestDto) {
        MalfunctionClearResponseRestDto rest =
                new MalfunctionClearResponseRestDto(
                        requestDto.getSid(),
                        requestDto.getKid(),
                        requestDto.getSls()
                );

        Map<String, String> result = new HashMap<>();
        result.put("MalfunctionClear",
                JSON.toJSONString(rest));

        CacheUtil.getInstance().getDeviceResultMap().put(requestDto.getSid() + "_MalfunctionClear", result);

        if (requestBusiness) business.malfunctionClearNotify(requestDto);
    }

    public void dockBikeInfoNotify(QueryDockBikeInfoRequestDto requestDto) {
        if (testService.dealWithDockBikeInfoNotify(requestDto)) {
            return;
        }

        log.info(new KingMeterMarker("Socket,QueryDockBikeInfo,CC01"),
                "{}|{}|{}", requestDto.getSid(),
                JSONObject.toJSONString(requestDto.getState()), 0);

        DockStateFromQueryDockBikeInfoVOForRest[] stateForRest =
                new DockStateFromQueryDockBikeInfoVOForRest[requestDto.getState().length];
        for (int i = 0; i < requestDto.getState().length; i++) {
            DockStateFromQueryDockBikeInfoVO state = requestDto.getState()[i];
            stateForRest[i] = new DockStateFromQueryDockBikeInfoVOForRest(
                    state.getKid(), state.getKln(),
                    state.getBid(), state.getSls()
            );
        }

        String key = requestDto.getSid() + "_QueryDockBikeInfo";

        QueryDockBikeInfoRequestRestDto restDto =
                new QueryDockBikeInfoRequestRestDto(requestDto.getSid(), stateForRest);

        Map<String, String> result = new HashMap<>();
        result.put("QueryDockBikeInfo",
                JSON.toJSONString(restDto));

        CacheUtil.getInstance().getDeviceResultMap().put(key, result);

        tracker.track_log(requestDto.getSid(), OTATrackerStepType.QueryDockBikeInfoResponse_Before,
                requestDto);

        if (requestBusiness) business.dockBikeInfoNotify(requestDto);
    }

    public void dealWithQueryDockInfo(QueryDockInfoRequestDto requestDto) {
        if (testService.dealWithQueryDockInfoNotify(requestDto)) {
            return;
        }
        long siteId = requestDto.getSid();
        log.info(new KingMeterMarker("Socket,QueryDockInfo,C601"),
                "{}|{}|{}|{}|{}|{}|{}",siteId,
                requestDto.getMsv(),requestDto.getMhv(),
                requestDto.getRsv(),requestDto.getRhv(),
                JSONObject.toJSONString(requestDto.getState()),0);

        DockStateInfoFromQueryDockInfoVOForRest[] stateForRest =
                new DockStateInfoFromQueryDockInfoVOForRest[requestDto.getState().length];

        for (int i = 0; i < requestDto.getState().length; i++) {
            DockStateInfoFromQueryDockInfoVO state = requestDto.getState()[i];
            stateForRest[i] = new DockStateInfoFromQueryDockInfoVOForRest(
                    state.getKid(), state.getKln(),
                    state.getPsv(), state.getPhv(),
                    state.getDsv(), state.getDhv(),
                    state.getEsv(), state.getEhv(),
                    state.getSls()
            );
        }

        QueryDockInfoResponseRestDto rest =
                new QueryDockInfoResponseRestDto(requestDto.getSid(),
                        requestDto.getMsv(), requestDto.getMhv(),
                        requestDto.getRsv(), requestDto.getRhv(),
                        stateForRest);

        Map<String, String> result = new HashMap<>();
        result.put("dockArray",
                JSON.toJSONString(rest));

        CacheUtil.getInstance().getDeviceResultMap().put(requestDto.getSid() + "_QueryDockInfo", result);

        if (requestBusiness) business.dealWithQueryDockInfo(requestDto);
    }


    public SwingCardUnLockResponseDto dealWithSwingCardUnlock(SwingCardUnLockRequestDto requestDto) {
        if (!requestBusiness) {
            long siteId = requestDto.getSid();
            Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);

            int ast = Integer.parseInt(siteMap.getOrDefault("ast", "0"));
            int acm = Integer.parseInt(siteMap.getOrDefault("acm", "0"));
            int minbsoc = Integer.parseInt(siteMap.getOrDefault("minbsoc", "10"));
            String uid = siteMap.getOrDefault("uid", "0");

            return new SwingCardUnLockResponseDto(requestDto.getKid(),
                    ast, acm, minbsoc, uid,
                    HardWareUtils.getInstance().getUtcTimeStampOnDevice(
                            Integer.parseInt(siteMap.get("timezone"))
                    ));
        } else {
            return business.dealWithSwingCardUnlock(requestDto);
        }
    }

    public void dealWithSwingCardConfirm(SwingCardUnLockRequestConfirmDto requestDto) {
        if (requestBusiness) business.dealWithSwingCardConfirm(requestDto);
    }

    public void queryDockLockStatusNotify(QueryDockLockStatusRequestDto requestDto) {
        QueryDockLockStatusResponseRestDto restDto =
                new QueryDockLockStatusResponseRestDto(
                        requestDto.getSid(), requestDto.getKid(),
                        requestDto.getBid(), requestDto.getUid(),
                        requestDto.getLks()
                );

        Map<String, String> result = new HashMap<>();
        result.put("DockLockStatus",
                JSON.toJSONString(restDto));

        CacheUtil.getInstance().getDeviceResultMap().put(requestDto.getSid() + "_QueryDockLockStatus", result);

        if (requestBusiness) business.queryDockLockStatusNotify(requestDto);
    }

    public void siteSettingNotify(SiteSettingRequestDto requestDto) {
        Map<String, String> result = new HashMap<>();
        result.put("SiteSetting",
                JSON.toJSONString(new SiteSettingResponseRestDto(requestDto.getSid(),
                        requestDto.getSls())));

        CacheUtil.getInstance().getDeviceResultMap().put(requestDto.getSid() + "_SiteSetting", result);

        if (requestBusiness) business.siteSettingNotify(requestDto);
    }

    public void offlineNotify(Long deviceId) {
        if (requestBusiness) business.offlineNotify(deviceId);
    }

    public void dealWithDockMonitorDataUpload(DockDataUploadRequestDto requestDto) {
        if (requestBusiness) business.dealWithDockMonitorDataUpload(requestDto);
    }

    public void OTAResponseNotify(OTARequestDto requestDto) {
        Map<String, String> result = new HashMap<>();
        result.put("OTAResponse",
                JSON.toJSONString(new OTAResponseRestDto(requestDto.getSid(),
                        OTAChargingType.getEnumByValue(requestDto.getParts()).name(),
                        requestDto.getParts(),
                        requestDto.getSls())));

        CacheUtil.getInstance().getDeviceResultMap().put(
                requestDto.getSid() + "_OTA", result);

        tracker.track_log(requestDto.getSid(), OTATrackerStepType.OTA_Response, requestDto);

        if (requestBusiness) business.otaResponseNotify(requestDto);
    }

    public void otaTrackerRecordUpload(Long siteId, boolean flag, Map<String,
            DockInfo4OTADto> dockIdAndBikeIdMap) {
        if (requestBusiness) business.otaTrackerRecordUpload(siteId, flag, dockIdAndBikeIdMap);
    }


}
