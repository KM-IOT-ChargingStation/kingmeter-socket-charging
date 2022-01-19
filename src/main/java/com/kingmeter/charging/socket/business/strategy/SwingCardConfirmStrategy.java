package com.kingmeter.charging.socket.business.strategy;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.SwingCardUnLockRequestConfirmDto;
import com.kingmeter.dto.charging.v2.socket.out.SwingCardUnLockConfirmResponseDto;
import com.kingmeter.charging.socket.acl.ChargingSiteService;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.strategy.RequestStrategy;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.HardWareUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class SwingCardConfirmStrategy implements RequestStrategy {

    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {
        SwingCardUnLockRequestConfirmDto requestDto = JSONObject.
                parseObject(requestBody.getData(), SwingCardUnLockRequestConfirmDto.class);

        long siteId = requestDto.getSid();

        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);
        int timezone = Integer.parseInt(siteMap.get("timezone"));

        log.info(new KingMeterMarker("Socket,SwingCardUnLock,C403"),
                "{}|{}|{}|{}|{}|{}|{}|{}",
                siteId, requestDto.getKid(), requestDto.getBid(),
                requestDto.getCard(),requestDto.getGbs(), requestDto.getUid(), requestDto.getTim(),
                HardWareUtils.getInstance().getLocalTimeByHardWareTimeStamp(timezone, requestDto.getTim()));


        responseBody.setTokenArray(requestBody.getTokenArray());
        responseBody.setFunctionCodeArray(ServerFunctionCodeType.SwingCardConfirm);

        SwingCardUnLockConfirmResponseDto responseDto = createResponse(requestDto, timezone);
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,SwingCardUnLock,C404"),
                "{}|{}|{}|{}|{}", siteId, responseDto.getKid(),
                responseDto.getSls(), responseDto.getTim(),
                HardWareUtils.getInstance().getLocalTimeByHardWareTimeStamp(
                        timezone, responseDto.getTim()));

        chargingSiteService.dealWithSwingCardConfirm(requestDto);
    }


    private SwingCardUnLockConfirmResponseDto createResponse(
            SwingCardUnLockRequestConfirmDto requestConfirmDto, int timezone) {

        Map<String, String> siteMap = CacheUtil.getInstance()
                .getDeviceInfoMap()
                .getOrDefault(requestConfirmDto.getSid(), new ConcurrentHashMap<>());
        long temp = Long.parseLong(siteMap.getOrDefault("tempTime","0"));

        return new SwingCardUnLockConfirmResponseDto(requestConfirmDto.getKid(),
                System.currentTimeMillis()/1000+temp, 0);
    }

}
