package com.kingmeter.charging.socket.test.dto;

import lombok.Data;

/**
 * @description:
 * @author: crazyandy
 */

@Data
public class TestUnLockDto {
    private long siteId;
    private long dockId;
    private String userId;
    private int intervalTime;
    private long startTimeStamp;
}
