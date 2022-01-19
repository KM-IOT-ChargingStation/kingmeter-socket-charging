package com.kingmeter.charging.socket.business.strategy;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.SiteHeartRequestDto;
import com.kingmeter.dto.charging.v2.socket.out.SiteHeartResponseDto;
import com.kingmeter.charging.socket.acl.ChargingSiteService;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.strategy.RequestStrategy;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class HeartBeatStrategy implements RequestStrategy {


    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {

        long siteId = Long.parseLong(requestBody.getDeviceId());

        SiteHeartResponseDto responseDto = chargingSiteService.createSiteHeartResponseDto(siteId);

        if (requestBody.getData().length() < 30) {
            //empty package 25
//            log.info(new KingMeterMarker("Socket,HeartBeat,C301"),
//                    "{}|0|0|0|0|0|0|0|0|{}", requestBody.getDeviceId(),
//                    requestBody.getData());
            sendHeartBeatResponse(responseBody, ctx, responseDto);
            return;
        }

        SiteHeartRequestDto requestDto =
                JSONObject.
                        parseObject(requestBody.getData(), SiteHeartRequestDto.class);

        log.info(new KingMeterMarker("Socket,HeartBeat,C301"),
                "{}|{}|{}", siteId,
                requestDto.getRpow(),
                JSONObject.toJSONString(requestDto.getState()));

        sendHeartBeatResponse(responseBody, ctx, responseDto);

        log.info(new KingMeterMarker("Socket,HeartBeat,C302"),
                "{}|{}|{}|{}", siteId, responseDto.getSls(),
                responseDto.getBnum(), responseDto.getTim());

        chargingSiteService.heartBeatNotify(requestDto, responseBody, ctx);
    }

    private void sendHeartBeatResponse(ResponseBody responseBody, ChannelHandlerContext ctx,
                                       SiteHeartResponseDto responseDto) {
        responseBody.setFunctionCodeArray(ServerFunctionCodeType.SiteHeartBeat);
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        ctx.writeAndFlush(responseBody);
    }

}
