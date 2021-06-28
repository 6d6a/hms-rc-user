package ru.majordomo.hms.rc.user.resources.DTO;

import lombok.Data;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

/** Class-based Projections (DTOs) */
@Data
public class EntityIdOnly {
    @Nonnull
    @NotNull
    private String id;
}
