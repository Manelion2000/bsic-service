package com.bakouan.app.controller;


import com.bakouan.app.dto.BaJWTTokenDto;
import com.bakouan.app.dto.BaLoginDto;
import com.bakouan.app.dto.BaProfilDto;
import com.bakouan.app.dto.BaRoleDto;
import com.bakouan.app.dto.BaUpdatePasswordDto;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.security.BaUserDetailsService;
import com.bakouan.app.security.jwt.JWTFilter;
import com.bakouan.app.security.jwt.RevokedTokenService;
import com.bakouan.app.security.jwt.TokenProvider;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.utils.BaConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping(BaConstants.URL.BASE_URL)
@RequiredArgsConstructor
@Slf4j
public class BaUserController {

    private final BaUserService userService;

    private final TokenProvider tokenProvider;

    private final BaUserDetailsService userDetailsService;
    private final RevokedTokenService revokedTokenService;


    /**
     * Endpoint d'authentification.
     *
     * @param loginVM
     * @return Le token, si l'utilisation est authentifié
     */
    @PostMapping(BaConstants.URL.AUTHENTICATE)
    public ResponseEntity<BaJWTTokenDto> authorize(final @Valid @RequestBody BaLoginDto loginVM) {

        this.userDetailsService.checkValiditeCompte(loginVM.getUsername());
        HttpHeaders httpHeaders = new HttpHeaders();
        BaJWTTokenDto jwt;
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());
            Authentication authentication = this.userService.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            boolean rememberMe = Boolean.TRUE.equals(loginVM.getRememberMe());
            jwt = tokenProvider.createToken(authentication, rememberMe);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe et le nom d'utilisateur "
                    + "ne correspondent pas.");
        }
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt.getToken());
        return new ResponseEntity<>(jwt, httpHeaders, HttpStatus.OK);
    }

    /**
     * Endpoint de deconnexion.
     * Revoque le token courant (si valide) et efface le contexte de securite.
     *
     * @param authorizationHeader header Authorization
     * @return {@link ResponseEntity}
     */
    @PostMapping(BaConstants.URL.LOGOUT)
    public ResponseEntity<Void> logout(
            @RequestHeader(name = JWTFilter.AUTHORIZATION_HEADER, required = false) final String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (tokenProvider.validateToken(token)) {
                revokedTokenService.revokeToken(token, tokenProvider.extractExpiration(token));
            }
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }


    /**
     * Récuperer tout les users.
     *
     * @return List de BaUserDto {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.USER)
    public ResponseEntity<List<BaUserDto>> getUsers() {
        return new ResponseEntity<>(userService.fetchUtilisateurs(), HttpStatus.OK);
    }

    /**
     * Ajouter un user.
     *
     * @param userDto : le user.
     * @return {@link ResponseEntity}p
     */
    @PostMapping(BaConstants.URL.USER)
    public ResponseEntity<Void> postUser(final @Valid @RequestBody BaUserDto userDto) {
        userService.createUser(userDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Mise à jour d'un user.
     *
     * @param uuid    : id du user
     * @param userDto : le user
     * @return {@link ResponseEntity}
     */
    @PutMapping(BaConstants.URL.USER + "/{code}")
    public ResponseEntity<Void> putUser(
            @PathVariable(name = "code") final String uuid,
            @RequestBody @Valid final BaUserDto userDto) {
        userService.updateUser(uuid, userDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Supprimer un user.
     *
     * @param code : id du user
     * @return {@link ResponseEntity}
     */
    @DeleteMapping(BaConstants.URL.USER + "/{code}")
    public ResponseEntity<String> deleteUser(@PathVariable("code") final String code) {
        userService.doDeleteUser(code);
        return new ResponseEntity<>("User supprimé", HttpStatus.OK);
    }


    /**
     * Récupère les informations de l'utilisateur courant.
     *
     * @return Information de l'utilisateur courant.  {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.USER + "/details")
    public ResponseEntity<BaUserDto> getUserInfo() {
        final BaUserDto userInfo = userService.getUserInfoWithMoreDetails();
        log.debug("Fetch User Info : {} ", userInfo);
        return new ResponseEntity<>(userInfo, HttpStatus.OK);
    }


    /**
     * Récuperer tous les roles du système.
     *
     * @return List de BaRoleDto {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.ROLE)
    public ResponseEntity<List<BaRoleDto>> fetchRoles() {
        return new ResponseEntity<>(userService.fetchRoles(), HttpStatus.OK);
    }

    /**
     * Ajouter un role.
     *
     * @param roleDto : le role.
     * @return {@link ResponseEntity}
     */
    @PostMapping(BaConstants.URL.ROLE)
    public ResponseEntity<BaRoleDto> addRole(final @Valid @RequestBody BaRoleDto roleDto) {
        return new ResponseEntity<>(userService.addRole(roleDto), HttpStatus.CREATED);
    }

    /**
     * Ajouter un nouveau profil.
     *
     * @param profilDto : Le nouveau profil
     * @return {@link ResponseEntity}
     */
    @PostMapping(BaConstants.URL.PROFIL)
    public ResponseEntity<BaProfilDto> addProfil(final @Valid @RequestBody BaProfilDto profilDto) {
        return new ResponseEntity<>(userService.addProfil(profilDto), HttpStatus.CREATED);
    }

    /**
     * Avoir la liste des profils.
     *
     * @return List of {@link BaProfilDto}
     */
    @GetMapping(BaConstants.URL.PROFIL)
    public ResponseEntity<List<BaProfilDto>> fetchProfils() {
        return new ResponseEntity<>(userService.fetchProfils(), HttpStatus.OK);
    }

    /**
     * Mise à jour du profil.
     *
     * @param profilDto : Le profil et les roles.
     * @return {@link ResponseEntity}
     */
    @PutMapping(BaConstants.URL.PROFIL)
    public ResponseEntity<BaProfilDto> updateProfil(@RequestBody @Valid final BaProfilDto profilDto) {
        return new ResponseEntity<>(userService.updateProfil(profilDto), HttpStatus.OK);
    }


    /**
     * Mise à jour du role.
     *
     * @param roleDto : Le role.
     * @return {@link ResponseEntity}
     */
    @PutMapping(BaConstants.URL.ROLE)
    public ResponseEntity<BaRoleDto> updateRole(@RequestBody @Valid final BaRoleDto roleDto) {
        return new ResponseEntity<>(userService.updateRole(roleDto), HttpStatus.OK);
    }


    /**
     * Supprimer un profil.
     *
     * @param uuid : Le profil à supprimer
     * @return {@link ResponseEntity}
     */
    @DeleteMapping(BaConstants.URL.PROFIL + "/{id}")
    public ResponseEntity<String> deleteProfil(@PathVariable("id") final String uuid) {
        userService.deleteProfil(uuid);
        return new ResponseEntity<>("Profil supprimé", HttpStatus.OK);
    }


    /**
     * Supprimer un role.
     *
     * @param uuid : Le role à supprimer
     * @return {@link ResponseEntity}
     */
    @DeleteMapping(BaConstants.URL.ROLE + "/{id}")
    public ResponseEntity<String> deleteRole(@PathVariable("id") final String uuid) {
        userService.deleteRole(uuid);
        return new ResponseEntity<>("Rôle supprimé", HttpStatus.OK);
    }

    /**
     * Changement de mot de passe par l'utilisateur lui-même connecté.
     *
     * @param updatePasswordDto : le user
     * @return {@link ResponseEntity}
     */
    @PutMapping(BaConstants.URL.USER + "/change-password")
    public ResponseEntity<Void> putUser(@RequestBody @Valid final BaUpdatePasswordDto updatePasswordDto) {
        this.userService.changePassword(updatePasswordDto);
        return ResponseEntity.ok().build();
    }


    /**
     * Terminer la réinitialisation du mot de passe.
     *
     * @param updatePasswordDto Contient les informations sur le code de réinitialisation et d'activation
     * @return {@link ResponseEntity}
     */
    @PutMapping(BaConstants.URL.USER + "/complete-reset-password")
    public ResponseEntity<Void> completeResetPassword(
            @RequestBody @Valid final BaUpdatePasswordDto updatePasswordDto) {
        this.userService.completResetPassword(updatePasswordDto);
        return ResponseEntity.ok().build();
    }

    /**
     * Demander le changement de mot de passe.
     *
     * @param passwordDto
     * @return {@link ResponseEntity}
     */
    @PutMapping(BaConstants.URL.USER + "/request-reset-password")
    public ResponseEntity<Void> requestResetPassword(
            @RequestBody final BaUpdatePasswordDto passwordDto) {
        this.userService.requestPasswordReset(passwordDto);
        return ResponseEntity.ok().build();
    }


    /**
     * Retrieve csrf token.
     * The CSRF TOKEN IN PUTTED IN HEADER RESPONSE
     * AUTOMATICALLY.
     */
    @GetMapping(BaConstants.URL.CSRF_TOKEN)
    public void retrieveCsrf() {
        log.info("Getting CSRF TOKEN");
    }


    /**
     * Activer un utilisateur.
     *
     * @param idUser
     * @return La liste des utilisateurs.  {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.USER + "/{id}/activate")
    public ResponseEntity<String> activateUser(@PathVariable(name = "id") final String idUser) {
        userService.activateUser(idUser);
        return new ResponseEntity<>("L'activation a reussi", HttpStatus.OK);
    }

    // ===== GESTION DES RÔLES UTILISATEURS =====

    /**
     * Ajouter un rôle à un utilisateur.
     *
     * @param userId : ID de l'utilisateur
     * @param roleId : ID du rôle
     * @return {@link ResponseEntity}
     */
    @PostMapping(BaConstants.URL.USER + "/{userId}/roles/{roleId}")
    public ResponseEntity<BaUserDto> addRoleToUser(@PathVariable(name = "userId") final String userId, 
                                                   @PathVariable(name = "roleId") final String roleId) {
        try {
            BaUserDto updatedUser = userService.addRoleToUser(userId, roleId);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du rôle {} à l'utilisateur {}: {}", roleId, userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Supprimer un rôle d'un utilisateur.
     *
     * @param userId : ID de l'utilisateur
     * @param roleId : ID du rôle
     * @return {@link ResponseEntity}
     */
    @DeleteMapping(BaConstants.URL.USER + "/{userId}/roles/{roleId}")
    public ResponseEntity<BaUserDto> removeRoleFromUser(@PathVariable(name = "userId") final String userId, 
                                                        @PathVariable(name = "roleId") final String roleId) {
        try {
            BaUserDto updatedUser = userService.removeRoleFromUser(userId, roleId);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du rôle {} de l'utilisateur {}: {}", roleId, userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Récupérer les rôles d'un utilisateur.
     *
     * @param userId : ID de l'utilisateur
     * @return {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.USER + "/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable(name = "userId") final String userId) {
        BaUserDto user = userService.getOneById(userId)
                .or(() -> userService.getOneByUsername(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return new ResponseEntity<>(user.getRoles(), HttpStatus.OK);
    }

}
