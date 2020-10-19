package ru.majordomo.hms.rc.user.api.DTO.log;

import lombok.Data;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

@Data
public class FindFtpLogRequest {
    /** elastic scrollId от предыдущего поиска который можно сразу удалить */
    @Nullable
    private String oldScrollId;
    @Nullable
    private String user;
    @Nullable
    private LocalDateTime startTimestamp;
    @Nullable
    private LocalDateTime endTimestamp;
    @Nullable
    private String remoteAddr;
}
