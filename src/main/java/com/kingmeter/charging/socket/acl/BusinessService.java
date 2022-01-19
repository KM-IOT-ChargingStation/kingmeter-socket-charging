package com.kingmeter.charging.socket.acl;

import com.kingmeter.charging.socket.business.tracker.DockInfo4OTADto;
import com.kingmeter.dto.charging.v2.socket.in.*;
import com.kingmeter.dto.charging.v2.socket.out.BikeInDockResponseDto;
import com.kingmeter.dto.charging.v2.socket.out.LoginPermissionDto;
import com.kingmeter.dto.charging.v2.socket.out.SwingCardUnLockResponseDto;

import java.util.Map;

public interface BusinessService {
    LoginPermissionDto getLoginPermission(SiteLoginRequestDto requestDto);

    BikeInDockResponseDto createBikeInDockResponseDto(BikeInDockRequestDto requestDto);

    void forceUnlockNotify(ForceUnLockRequestDto requestDto);

    void heartBeatNotify(SiteHeartRequestDto requestDto);

    void malfunctionUploadNotify(DockMalfunctionUploadRequestDto requestDto);

    void malfunctionClearNotify(MalfunctionClearRequestDto requestDto);

    void dockBikeInfoNotify(QueryDockBikeInfoRequestDto requestDto);

    void dealWithQueryDockInfo(QueryDockInfoRequestDto requestDto);

    void dealWithScanUnLock(ScanUnLockRequestDto requestDto);

    SwingCardUnLockResponseDto dealWithSwingCardUnlock(SwingCardUnLockRequestDto requestDto);

    void dealWithSwingCardConfirm(SwingCardUnLockRequestConfirmDto requestDto);

    void queryDockLockStatusNotify(QueryDockLockStatusRequestDto requestDto);

    void siteSettingNotify(SiteSettingRequestDto requestDto);

    void offlineNotify(Long deviceId);

    void dealWithDockMonitorDataUpload(DockDataUploadRequestDto requestDto);

    void otaResponseNotify(OTARequestDto requestDto);

    void otaTrackerRecordUpload(Long siteId,boolean flag, Map<String, DockInfo4OTADto> dockIdAndBikeIdMap);
}
