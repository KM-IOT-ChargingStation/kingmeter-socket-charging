package com.kingmeter.charging.socket.business.tracker;

import com.kingmeter.dto.charging.v2.rest.request.OTARequestRestDto;
import com.kingmeter.dto.charging.v2.socket.in.QueryDockBikeInfoRequestDto;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Data
public class OTACacheUtil {
    private volatile static OTACacheUtil instance;

    private OTACacheUtil() {
    }

    public static OTACacheUtil getInstance() {
        if (instance == null) {
            synchronized (OTACacheUtil.class) {
                if (instance == null) {
                    instance = new OTACacheUtil();
                }
            }
        }
        return instance;
    }


    /**
     * key: siteId
     * value : QueryDockInfoRequestDto
     */
    private volatile ConcurrentMap<Long, QueryDockBikeInfoRequestDto> queryInfoForOTAMap = new ConcurrentHashMap();

    /**
     * key: siteId
     * value : OTARequestRestDto
     */
    private volatile ConcurrentMap<Long, OTARequestRestDto> otaInfoMap = new ConcurrentHashMap<>();


    /**
     * key: siteId
     * value : OTARequestRestDto
     */
    private volatile ConcurrentMap<Long, Boolean> otaLoopMap = new ConcurrentHashMap<>();
}
