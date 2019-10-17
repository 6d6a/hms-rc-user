package ru.majordomo.hms.rc.user.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "database-user.mysql.session-variables")
@Component
public class MysqlSessionVariablesConfig {
    private List<String> charsets;
    private List<String> queryCacheTypes;
    private List<String> collations;
    private List<String> innodbStrictMode;
}
