package com.kingmeter.charging.socket.business.strategy;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.SwingCardUnLockRequestDto;
import com.kingmeter.dto.charging.v2.socket.out.SwingCardUnLockResponseDto;
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
public class SwingCardUnLockStrategy implements RequestStrategy {

    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {

        SwingCardUnLockRequestDto requestDto = JSONObject.
                parseObject(requestBody.getData(), SwingCardUnLockRequestDto.class);
        long dockId = requestDto.getKid();
        long siteId = requestDto.getSid();
        String cardNo = HardWareUtils.getInstance().correctCardNumber(requestDto.getCard());

        Map<String, String> siteMap = CacheUtil.getInstance().getDeviceInfoMap().get(siteId);
        int timezone = Integer.parseInt(siteMap.get("timezone"));

        log.info(new KingMeterMarker("Socket,SwingCardUnLock,C401"),
                "{}|{}|{}|{}|{}|{}", siteId, dockId, requestDto.getBid(),
                cardNo, requestDto.getTim(),
                HardWareUtils.getInstance().getLocalTimeByHardWareTimeStamp(
                        timezone, requestDto.getTim()));

        requestDto.setCard(cardNo);
        SwingCardUnLockResponseDto responseDto =
                chargingSiteService.dealWithSwingCardUnlock(requestDto);

        responseBody.setTokenArray(requestBody.getTokenArray());
        responseBody.setFunctionCodeArray(ServerFunctionCodeType.SwingCardUnLock);
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,SwingCardUnLock,C402"),
                "{}|{}|{}|{}|{}|{}|{}|{}",
                siteId, responseDto.getKid(),
                responseDto.getAst(), responseDto.getAcm(), responseDto.getMinbsoc(),
                responseDto.getUid(), responseDto.getTim(),
                HardWareUtils.getInstance().getLocalTimeByHardWareTimeStamp(timezone, responseDto.getTim()));
    }


}
