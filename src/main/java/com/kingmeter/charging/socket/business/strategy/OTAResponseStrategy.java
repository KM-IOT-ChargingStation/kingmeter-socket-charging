package com.kingmeter.charging.socket.business.strategy;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.constant.OTAChargingType;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.OTARequestDto;
import com.kingmeter.charging.socket.acl.ChargingSiteService;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.strategy.RequestStrategy;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OTAResponseStrategy implements RequestStrategy {

    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {
        OTARequestDto requestDto =
                JSONObject.
                        parseObject(requestBody.getData(), OTARequestDto.class);

        long siteId = requestDto.getSid();

        log.info(new KingMeterMarker("Socket,OTA,CE01"),
                "{}|{}|{}|{}", siteId, OTAChargingType.getEnumByValue(requestDto.getParts()),
                requestDto.getPnum(), requestDto.getSls());

        chargingSiteService.OTAResponseNotify(requestDto);
    }
}
