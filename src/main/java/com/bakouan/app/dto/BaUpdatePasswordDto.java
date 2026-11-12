package com.bakouan.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Classe de mise à jour du mot de passe.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaUpdatePasswordDto implements Serializable {

  private String idUser;

  @lombok.ToString.Exclude
  private String ancien;

  @lombok.ToString.Exclude
  private String nouveau;

  @lombok.ToString.Exclude
  private String confirmer;

  // Les champs suivants sont utilisés
  // lors de la réinitialisation du mot de passe.

  private String username;

  private String telephone;

  @Email(message = "Le format de l'email est incorrect")
  private String email;

  @lombok.ToString.Exclude
  private String resetKey;

}
