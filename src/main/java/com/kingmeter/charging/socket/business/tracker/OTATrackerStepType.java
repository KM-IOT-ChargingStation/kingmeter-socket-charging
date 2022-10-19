package com.kingmeter.charging.socket.business.tracker;

public enum OTATrackerStepType {

    QueryDockBikeInfoRequest_Before(1),//httpChannelMap api to start OTA process
    QueryDockBikeInfoResponse_Before(2),//get dock and bike info first time
    OTA_Request(3),//ota httpChannelMap
    OTA_Response(4),//ota response
    OTA_ReLogin(5),//the wifimaster login again , we should move to #6
    QueryDockBikeInfoRequest_After(6),//send querying command
    QueryDockBikeInfoResponse_After(7),//get dock and bike info second time
    CompareTwiceQueryDockBikeInfo(8);//compare these two result.

    private int step;
    OTATrackerStepType(int step){
        this.step = step;
    }

    public int getStep(){
        return this.step;
    }
}
