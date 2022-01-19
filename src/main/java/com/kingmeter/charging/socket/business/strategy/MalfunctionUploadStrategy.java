package com.kingmeter.charging.socket.business.strategy;

import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.in.DockMalfunctionUploadRequestDto;
import com.kingmeter.dto.charging.v2.socket.out.DockMalfunctionUploadResponseDto;
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
public class MalfunctionUploadStrategy implements RequestStrategy {

    @Autowired
    private ChargingSiteService chargingSiteService;

    @Override
    public void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx) {

        DockMalfunctionUploadRequestDto requestDto =
                JSONObject.
                        parseObject(requestBody.getData(), DockMalfunctionUploadRequestDto.class);

        long siteId = requestDto.getSid();

        log.info(new KingMeterMarker("Socket,MalfunctionUpload,C901"),
                "{}|{}|{}|{}|{}|{}|{}|{}|{}|{}", siteId,
                requestDto.getKid(),requestDto.getKln(),
                requestDto.getBid(),requestDto.getCer(),
                requestDto.getBer(),requestDto.getDisp(),
                requestDto.getPer(),requestDto.getPerlk(),
                requestDto.getPerws());


        DockMalfunctionUploadResponseDto responseDto =
                new DockMalfunctionUploadResponseDto(requestDto.getKid(), 0);

        responseBody.setFunctionCodeArray(ServerFunctionCodeType.DockMalfunctionUpload);
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,MalfunctionUpload,C902"),
                "{}|{}|{}", siteId, responseDto.getKid(), responseDto.getSls());

        chargingSiteService.malfunctionUploadNotify(requestDto);
    }

}
