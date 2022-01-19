package com.kingmeter.charging.socket.business.utils;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.charging.socket.business.code.ServerFunctionCodeType;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.dto.charging.v2.socket.out.QueryDockInfoResponseDto;
import com.kingmeter.socket.framework.dto.ResponseBody;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SocketCommandUtils {

    @Async
    public void sendQueryDockInfoCommand(long siteId,long kid,
                                         ResponseBody responseBody, ChannelHandlerContext ctx){
        QueryDockInfoResponseDto responseDto =
                new QueryDockInfoResponseDto(kid);

        responseBody.setFunctionCodeArray(ServerFunctionCodeType.QueryDockInfo);
        responseBody.setData(JSONObject.toJSON(responseDto).toString());
        ctx.writeAndFlush(responseBody);

        log.info(new KingMeterMarker("Socket,QueryDockInfo,C602"),
                "{}|{}",siteId,responseDto.getKid());
    }

}
