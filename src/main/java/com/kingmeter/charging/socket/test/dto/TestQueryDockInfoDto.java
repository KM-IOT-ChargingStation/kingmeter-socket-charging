package com.kingmeter.charging.socket.test.dto;

import lombok.Data;

/**
 * @description:
 * @author: crazyandy
 */
@Data
public class TestQueryDockInfoDto {
    private long siteId;
    private long dockId;
    private int intervalTime;
    private long startTimeStamp;
}
