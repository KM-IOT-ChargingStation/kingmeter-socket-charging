package com.kingmeter.charging.socket.business.strategy;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.BikeInDockRequestDto;
import com.kingmeter.dto.charging.v2.socket.out.BikeInDockResponseDto;
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


@Slf4j
@Component
public class BikeInDockStrategy implements RequestStrategy {


    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {
        BikeInDockRequestDto requestDto = JSONObject.
                parseObject(requestBody.getData(), BikeInDockRequestDto.class);

        long siteId = requestDto.getSid();

        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);

        log.info(new KingMeterMarker("Socket,BikeInDock,C201"),
                "{}|{}|{}|{}|{}", siteId,
                requestDto.getKid(), requestDto.getBid(), requestDto.getTim(),
                HardWareUtils.getInstance()
                        .getLocalTimeByHardWareTimeStamp(
                                Integer.parseInt(siteMap.get("timezone")),
                                requestDto.getTim()));

        responseBody.setTokenArray(requestBody.getTokenArray());

        BikeInDockResponseDto responseDto =
                chargingSiteService.createBikeInDockResponseDto(requestDto,responseBody,ctx);
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        responseBody.setFunctionCodeArray(ServerFunctionCodeType.BikeInDock);
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,BikeInDock,C202"),
                "{}|{}|{}|{}|{}|{}", siteId, responseDto.getKid(),
                responseDto.getRet(), responseDto.getAcm(),
                responseDto.getCum(), responseDto.getRtm());
    }


}
