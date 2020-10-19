package ru.majordomo.hms.rc.user.api.DTO.log;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ScrollRequest {
    @NotBlank
    private String scrollId;
}