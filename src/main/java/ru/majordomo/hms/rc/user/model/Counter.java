package ru.majordomo.hms.rc.user.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Counter {
    @Id
    private String id;

    private Integer count = 0;

    @Indexed(unique = true)
    private String internalName;
}
