package com.kingmeter.charging.socket.rest;

import lombok.Data;

import java.util.LinkedHashSet;

@Data
public class OTAFirmwareVersion {
    private String typeName;
    private LinkedHashSet<String> fileNameSet;
}
