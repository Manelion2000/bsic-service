package com.bakouan.app.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * View Model object for storing a user's credentials.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaLoginDto {

    @NotBlank
    @Size(min = 1, max = 50)
    private String username;

    @NotBlank
    @Size(min = 1, max = 72)
    private String password;

    private Boolean rememberMe;

}
