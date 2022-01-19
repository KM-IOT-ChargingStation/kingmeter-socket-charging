package com.kingmeter.charging.socket.business.tracker;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.acl.ChargingSiteService;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.constant.OTAChargingType;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.rest.request.OTARequestRestDto;
import com.kingmeter.dto.charging.v2.socket.in.OTARequestDto;
import com.kingmeter.dto.charging.v2.socket.in.QueryDockBikeInfoRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.SiteLoginRequestDto;
import com.kingmeter.dto.charging.v2.socket.in.vo.DockStateFromQueryDockBikeInfoVO;
import com.kingmeter.dto.charging.v2.socket.out.OTAResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.QueryDockBikeInfoResponseDto;
import com.kingmeter.socket.framework.application.SocketApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Component
public class OTATracker {

    @Value("${kingmeter.ota.tracker}")
    private boolean trackerActive;

    @Autowired
    private SocketApplication socketApplication;

    @Autowired
    private ChargingSiteService chargingSiteService;


    private static final Logger logger = LoggerFactory.getLogger("ota_tracker");


    public void track_log(long siteId, OTATrackerStepType stepType, Object obj) {
        if (trackerActive) {
            Map<Long, QueryDockBikeInfoRequestDto> stepMap = OTACacheUtil.getInstance().getQueryInfoForOTAMap();
            if (stepType == OTATrackerStepType.QueryDockBikeInfoResponse_Before) {
                //we should judge whether this response data come ahead of login or after login
                if (stepMap.containsKey(siteId)) {
                    //after login
                    deaLWithSecondQueryInfo(siteId, obj);
                } else {
                    //ahead of login
                    dealWithFirstQueryInfo(siteId, obj);
                }
            } else if (stepType == OTATrackerStepType.OTA_Response) {
                //get ota response
                dealWithOTAResponse(siteId, obj);
            } else if (stepType == OTATrackerStepType.OTA_ReLogin) {
                //after we send OTA command , the wifimaster would relogin itself
                dealWithOTAReLogin(siteId, obj);
            } else if (stepType == OTATrackerStepType.QueryDockBikeInfoRequest_Before) {
                QueryDockBikeInfoResponseDto dto = (QueryDockBikeInfoResponseDto) obj;
                OTATrackerStepType currentStep = OTATrackerStepType.QueryDockBikeInfoRequest_Before;
                logger.info(new KingMeterMarker("Tracker," + currentStep),
                        "{}|{}|{}", siteId, currentStep.getStep(),
                        JSONObject.toJSON(dto).toString());
            }
        }
    }

    private void dealWithOTAResponse(long siteId, Object o) {
        OTARequestDto requestDto = (OTARequestDto) o;
        OTATrackerStepType currentStep = OTATrackerStepType.OTA_Response;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}|{}|{}|{}", siteId, currentStep.getStep(),
                OTAChargingType.getEnumByValue(requestDto.getParts()),
                requestDto.getPnum(), requestDto.getSls(),
                JSONObject.toJSON(requestDto).toString());
    }

    private void deaLWithSecondQueryInfo(long siteId, Object content) {
        OTACacheUtil otaCacheUtil = OTACacheUtil.getInstance();

        //compare these two response data
        QueryDockBikeInfoRequestDto firstInfoDto =
                otaCacheUtil.getQueryInfoForOTAMap().get(siteId);
        QueryDockBikeInfoRequestDto secondInfoDto = (QueryDockBikeInfoRequestDto) content;

        OTATrackerStepType currentStep = OTATrackerStepType.QueryDockBikeInfoResponse_After;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}", siteId, currentStep.getStep(),
                JSONObject.toJSON(secondInfoDto.getState()).toString());

        compareTwiceQueryInfo(firstInfoDto, secondInfoDto);

        //判断是否需要下一次升级，如果需要，则发送第一次查询
        ConcurrentMap<Long, OTARequestRestDto> otaInfoMap =
                otaCacheUtil.getOtaInfoMap();
        if (!otaInfoMap.containsKey(siteId)) {
            return;
        }

        otaCacheUtil.getQueryInfoForOTAMap().remove(siteId);

        ConcurrentMap<Long,Boolean> otaLoopMap = otaCacheUtil.getOtaLoopMap();
        if(otaLoopMap.containsKey(siteId)){
            if(otaLoopMap.get(siteId).booleanValue()){
                startNewOTA(siteId);
            }
        }
    }

    private void startNewOTA(long siteId) {
        OTATrackerStepType currentStep;//开始新的升级
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        QueryDockBikeInfoResponseDto dto =
                new QueryDockBikeInfoResponseDto(0);
        currentStep = OTATrackerStepType.QueryDockBikeInfoRequest_Before;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}", siteId, currentStep.getStep(),
                JSONObject.toJSON(dto).toString());

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockBikeInfo, JSONObject.toJSON(dto).toString());
    }

    private void compareTwiceQueryInfo(QueryDockBikeInfoRequestDto firstInfoDto, QueryDockBikeInfoRequestDto secondInfoDto) {
        boolean flag = false;
        DockStateFromQueryDockBikeInfoVO[] firstState = firstInfoDto.getState();
        DockStateFromQueryDockBikeInfoVO[] secondState = secondInfoDto.getState();

        Map<String, DockInfo4OTADto> dockIdAndBikeIdMap = new HashMap<>();
        for (DockStateFromQueryDockBikeInfoVO vo : firstState) {
            dockIdAndBikeIdMap.put(String.valueOf(vo.getKln()), new DockInfo4OTADto(vo.getKid(), vo.getBid(), 0, 0));
        }
        for (DockStateFromQueryDockBikeInfoVO vo : secondState) {
            String kln = String.valueOf(vo.getKln());
            if (dockIdAndBikeIdMap.containsKey(kln)) {
                DockInfo4OTADto allDto = dockIdAndBikeIdMap.get(kln);
                allDto.setPost_kid(vo.getKid());
                allDto.setPost_bid(vo.getBid());
                dockIdAndBikeIdMap.put(kln, allDto);
                if (allDto.getPre_kid() == allDto.getPost_kid() &&
                        allDto.getPre_bid() == allDto.getPost_bid()) {
                    flag = true;
                }
            } else {
                dockIdAndBikeIdMap.put(kln,
                        new DockInfo4OTADto(0, 0, vo.getKid(), vo.getKid()));
            }
        }
        long siteId = firstInfoDto.getSid();
        OTATrackerStepType currentStep = OTATrackerStepType.CompareTwiceQueryDockBikeInfo;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}|{}", siteId, currentStep.getStep(), flag,
                JSONObject.toJSON(dockIdAndBikeIdMap).toString());

        chargingSiteService.otaTrackerRecordUpload(siteId, flag, dockIdAndBikeIdMap);
    }


    private void dealWithOTAReLogin(long siteId, Object o) {
        OTACacheUtil otaCacheUtil = OTACacheUtil.getInstance();
        ConcurrentMap<Long, OTARequestRestDto> otaInfoMap =
                otaCacheUtil.getOtaInfoMap();
        if (!otaInfoMap.containsKey(siteId)) {
            return;
        }
        SiteLoginRequestDto otaResultDto =
                (SiteLoginRequestDto) o;

        OTATrackerStepType currentStep = OTATrackerStepType.OTA_ReLogin;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}|{}|{}", siteId, currentStep.getStep(),
                otaResultDto.getMhv(), otaResultDto.getMsv(),
                JSONObject.toJSON(otaResultDto).toString());

        //to send query dock and bike info after login
        QueryDockBikeInfoResponseDto dto =
                new QueryDockBikeInfoResponseDto(0);

        currentStep = OTATrackerStepType.QueryDockBikeInfoRequest_After;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}", siteId, currentStep.getStep(),
                JSONObject.toJSON(dto).toString());

        socketApplication.sendSocketMsg(siteId,
                ServerFunctionCodeType.QueryDockBikeInfo, JSONObject.toJSON(dto).toString());
    }

    private void dealWithFirstQueryInfo(long siteId, Object o) {
        OTACacheUtil otaCacheUtil = OTACacheUtil.getInstance();
        ConcurrentMap<Long, OTARequestRestDto> otaInfoMap =
                otaCacheUtil.getOtaInfoMap();
        if (!otaInfoMap.containsKey(siteId)) {
            return;
        }

        QueryDockBikeInfoRequestDto firstQueryInfoDto = (QueryDockBikeInfoRequestDto) o;
        ConcurrentMap<Long, QueryDockBikeInfoRequestDto> map =
                otaCacheUtil.getQueryInfoForOTAMap();
        map.put(siteId, firstQueryInfoDto);
        otaCacheUtil.setQueryInfoForOTAMap(map);

        OTATrackerStepType currentStep = OTATrackerStepType.QueryDockBikeInfoResponse_Before;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}", siteId, currentStep.getStep(),
                JSONObject.toJSON(firstQueryInfoDto).toString());

        OTARequestRestDto otaRequestRestDto = otaInfoMap.get(siteId);
        currentStep = OTATrackerStepType.OTA_Request;
        logger.info(new KingMeterMarker("Tracker," + currentStep),
                "{}|{}|{}|{}", siteId, currentStep.getStep(),
                otaRequestRestDto.getType(),
                JSONObject.toJSON(firstQueryInfoDto).toString());
        sendOTACommand(otaRequestRestDto);
    }

    private void sendOTACommand(OTARequestRestDto restDto) {
        OTAResponseDto responseDto = new OTAResponseDto(restDto.getFileUrl(), restDto.getHttpPort(),
                restDto.getFileName(), restDto.getVersion(), OTAChargingType.getEnumByName(restDto.getType()).value());
        socketApplication.sendSocketMsg(restDto.getSiteId(),
                ServerFunctionCodeType.OTACommand,
                JSON.toJSON(responseDto).toString());
    }


}
