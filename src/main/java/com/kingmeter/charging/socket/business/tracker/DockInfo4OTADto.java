package com.kingmeter.charging.socket.business.tracker;


import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class DockInfo4OTADto {
    private long pre_kid;
    private long pre_bid;
    private long post_kid;
    private long post_bid;
}
