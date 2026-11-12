package com.bakouan.app.dto;

import jakarta.validation.constraints.*;

public final class PasswordAccessDtos {
    private PasswordAccessDtos(){}
    public record Reset(@NotNull Long version,@NotBlank @Size(max=1000) String motif){}
    public record Complete(String nouveau,String confirmer){
        @Override public String toString(){return "Complete{secrets=REDACTED}";}
    }
}
