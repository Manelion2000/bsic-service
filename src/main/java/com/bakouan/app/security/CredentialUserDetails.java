package com.bakouan.app.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public final class CredentialUserDetails extends User {
    private final long credentialVersion;
    private final boolean passwordChangeRequired;
    private final boolean temporaryPasswordUsed;
    public CredentialUserDetails(String username,String password,boolean enabled,boolean unlocked,
            Collection<? extends GrantedAuthority> authorities,long version,boolean required,boolean used) {
        super(username,password,enabled,true,true,unlocked,authorities);
        credentialVersion=version;passwordChangeRequired=required;temporaryPasswordUsed=used;
    }
    public long getCredentialVersion(){return credentialVersion;}
    public boolean isPasswordChangeRequired(){return passwordChangeRequired;}
    public boolean isTemporaryPasswordUsed(){return temporaryPasswordUsed;}
}
