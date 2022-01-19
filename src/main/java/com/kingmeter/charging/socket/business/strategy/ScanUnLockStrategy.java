package com.kingmeter.charging.socket.business.strategy;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.ScanUnLockRequestDto;
import com.kingmeter.charging.socket.acl.ChargingSiteService;
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
public class ScanUnLockStrategy implements RequestStrategy {


    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext channelHandlerContext) {

        ScanUnLockRequestDto requestDto =
                JSONObject.
                        parseObject(requestBody.getData(), ScanUnLockRequestDto.class);

        chargingSiteService.dealWithScanUnLock(requestDto);
    }
}
