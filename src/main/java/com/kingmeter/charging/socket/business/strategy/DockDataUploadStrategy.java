package com.kingmeter.charging.socket.business.strategy;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.acl.ChargingSiteService;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.DockDataUploadRequestDto;
import com.kingmeter.dto.charging.v2.socket.out.DockDataUploadResponseDto;
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
public class DockDataUploadStrategy implements RequestStrategy {

    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody,
                        ChannelHandlerContext ctx) {
        DockDataUploadRequestDto requestDto = JSONObject.
                parseObject(requestBody.getData(), DockDataUploadRequestDto.class);

        long siteId = requestDto.getSid();

        log.info(new KingMeterMarker("Socket,DockMonitorDataUpload,CD01"),
                "{}|{}", siteId, JSONObject.toJSONString(requestDto.getState()));

        chargingSiteService.dealWithDockMonitorDataUpload(requestDto);

        responseBody.setTokenArray(requestBody.getTokenArray());
        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);
        if (siteMap == null) return;

        DockDataUploadResponseDto responseDto =
                new DockDataUploadResponseDto(0,
                        HardWareUtils.getInstance()
                                .getUtcTimeStampOnDevice(
                                        Integer.parseInt(siteMap.get("timezone"))
                                ));
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        responseBody.setFunctionCodeArray(ServerFunctionCodeType.DockDataUpload);
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,DockMonitorDataUpload,CD02"),
                "{}|{}", siteId, responseDto.getSls());
    }
}
