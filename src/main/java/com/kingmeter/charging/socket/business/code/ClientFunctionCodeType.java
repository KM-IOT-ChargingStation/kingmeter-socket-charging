package com.kingmeter.charging.socket.business.code;



import com.kingmeter.charging.socket.business.strategy.*;

import java.util.HashMap;
import java.util.Map;

/**
 * wifimaster function code
 */
public enum ClientFunctionCodeType {

    LoginType(0xc001, LoginStrategy.class),//
    ScanUnLock(0xc101, ScanUnLockStrategy.class),//
    ForceUnLock(0xc103, ForceUnLockStrategy.class),//
    BikeInDock(0xc201, BikeInDockStrategy.class),//
    SiteHeartBeat(0xc301,HeartBeatStrategy.class),//
    SwingCardUnLock(0xc401,SwingCardUnLockStrategy.class),//
    SwingCardConfirm(0xc403,SwingCardConfirmStrategy.class),//
    QueryDockInfo(0xc601, QueryDockInfoStrategy.class),//
    CheckDockLockStatus(0xc701, CheckDockLockStatusStrategy.class),//
    MalfunctionUpload(0xc901,MalfunctionUploadStrategy.class),//
    MalfunctionClear(0xCA01,MalfunctionClearStrategy.class),//
    SiteSetting(0xCB01,SiteSettingStrategy.class),//
    QueryDockBikeInfo(0xCC01,QueryDockBikeInfoStrategy.class),//
    DockDataUpload(0xCD01,DockDataUploadStrategy.class),//
    OTAResponse(0xCE01,OTAResponseStrategy.class);//ota



    private int value;
    private Class className;

    ClientFunctionCodeType(int value, Class className) {
        this.value = value;
        this.className = className;
    }

    public int value() {
        return value;
    }

    public Class getClassName (){
        return className;
    }

    static Map<Integer, ClientFunctionCodeType> enumMap = new HashMap();

    static {
        for (ClientFunctionCodeType type : ClientFunctionCodeType.values()) {
            enumMap.put(type.value(), type);
        }
    }

    public static ClientFunctionCodeType getEnum(Integer value) {
        return enumMap.get(value);
    }

    public static boolean containsValue(Integer value) {
        return enumMap.containsKey(value);
    }
}
