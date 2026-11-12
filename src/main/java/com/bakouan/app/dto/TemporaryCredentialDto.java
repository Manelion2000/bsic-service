package com.bakouan.app.dto;

import lombok.Getter;
import java.time.Instant;

/** Secret de remise uniquement dans la réponse de création ou réinitialisation. */
@Getter
public final class TemporaryCredentialDto {
    private final String username;
    private final String temporaryPassword;
    private final Instant expiresAt;
    private final long userVersion;
    public TemporaryCredentialDto(String username,String password,Instant expiresAt,long version) {
        this.username=username;this.temporaryPassword=password;this.expiresAt=expiresAt;this.userVersion=version;
    }
    @Override public String toString(){return "TemporaryCredentialDto{secret=REDACTED}";}
}
