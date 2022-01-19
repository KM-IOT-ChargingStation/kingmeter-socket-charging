package com.kingmeter.charging.socket.business.strategy;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.SiteSettingRequestDto;
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
public class SiteSettingStrategy implements RequestStrategy {

    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext channelHandlerContext) {
        SiteSettingRequestDto requestDto =
                JSONObject.
                        parseObject(requestBody.getData(), SiteSettingRequestDto.class);

        log.info(new KingMeterMarker("Socket,SiteSetting,CB01"),
                "{}|{}",requestDto.getSid(),requestDto.getSls());

        chargingSiteService.siteSettingNotify(requestDto);
    }
}
