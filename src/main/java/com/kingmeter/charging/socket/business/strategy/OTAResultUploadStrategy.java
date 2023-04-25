package com.kingmeter.charging.socket.business.strategy;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.constant.OTAChargingType;
import com.kingmeter.dto.charging.v2.socket.in.OTAResultUploadRequestDto;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.strategy.RequestStrategy;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OTAResultUploadStrategy implements RequestStrategy {

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {
        OTAResultUploadRequestDto requestDto =
                JSONObject.
                        parseObject(requestBody.getData(), OTAResultUploadRequestDto.class);

        long siteId = requestDto.getSid();

        log.info(new KingMeterMarker("Socket,OTA,CF01"),
                "{}|{}|{}|{}", siteId, OTAChargingType.getEnumByValue(requestDto.getParts()),
                requestDto.getPro(), requestDto.getSls());


        responseBody.setTokenArray(requestBody.getTokenArray());
        responseBody.setFunctionCodeArray(ServerFunctionCodeType.OTAResultUploadResponse);
        responseBody.setData("");
        responseBody.setDeviceId(siteId);
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,OTA,CF02"),
                "{}", siteId);
    }
}
