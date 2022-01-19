package com.kingmeter.charging.socket.business.code;

/**
 * socket server code
 */
public interface ServerFunctionCodeType {
    byte[] LoginType = {(byte) 192, (byte) 2};//("C0 02"),
    byte[] ScanUnLock = {(byte) 193, (byte) 2};//("C1 02"),
    byte[] ForceUnLock = {(byte) 193, (byte) 4};//("C1 04"),
    byte[] BikeInDock = {(byte) 194, (byte) 2};//("C2 02"),
    byte[] SiteHeartBeat = {(byte) 195, (byte) 2};//("C3 02"),
    byte[] SwingCardUnLock = {(byte) 196, (byte) 2};//("C4 02"),
    byte[] SwingCardConfirm = {(byte) 196, (byte) 4};//("C4 04"),
    byte[] QueryDockInfo = {(byte) 198, (byte) 2};//("C6 02"),
    byte[] CheckDockLockStatus = {(byte) 199, (byte) 2};//("C7 02"),
    byte[] DockMalfunctionUpload = {(byte) 201, (byte) 2};//("C9 02"),
    byte[] DockMalfunctionClear = {(byte) 202, (byte) 2};//("CA 02"),
    byte[] QueryDockBikeInfo = {(byte) 204, (byte) 2};//("CC 02"),
    byte[] SiteSetting = {(byte) 203, (byte) 2};//("CB 02"),
    byte[] DockDataUpload = {(byte) 205, (byte) 2};//("CD 02"),

    byte[] OTACommand = {(byte) 206, (byte) 2};//("CE 02"),
    byte[] OTAResultUploadResponse = {(byte) 207, (byte) 2};//("CF 02");

}
