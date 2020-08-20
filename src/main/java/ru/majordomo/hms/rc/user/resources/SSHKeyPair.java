package ru.majordomo.hms.rc.user.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSHKeyPair {
    private String privateKey;
    private String publicKey;
}
