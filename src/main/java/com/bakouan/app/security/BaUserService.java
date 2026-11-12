package com.bakouan.app.security;


import com.bakouan.app.dto.*;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaProfil;
import com.bakouan.app.model.BaRole;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaProfilRepository;
import com.bakouan.app.repositories.BaRoleRepository;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.service.BaLogService;
import com.bakouan.app.service.BaMailService;
import com.bakouan.app.utils.BaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
@Slf4j
public class BaUserService {

    private final IUserDetailsFacade userDetailsFacade;
    private final BaUserRepository userRepository;
    private final BaProfilRepository profilRepository;
    private final BaRoleRepository roleRepository;
    private final com.bakouan.app.repositories.BaServiceRepository serviceRepository;
    private final com.bakouan.app.repositories.BaDepartementRepository departementRepository;
    private final com.bakouan.app.repositories.BaAgenceRepository agenceRepository;
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);
    private final PasswordEncoder passwordEncoder;
    private final BaLogService logService;
    private final BaMailService mailService;
    private final AccountCredentialService credentialService;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    /**
     * Avoir la liste des utilisateurs.
     *
     * @return liste des utilisateurs.
     */
    public List<BaUserDto> fetchUtilisateurs() {
        logService.log(new BaLogDto(EAction.V, "Utilisateurs"));

        return this.userRepository.fetchMulticrites(
                        EStatut.A.name(), "", "", "")
                .map(mapper::maps)
                .collect(Collectors.toList());
    }


    /**
     * Fonction de création d'un utilisateur.
     *
     * @param uDto
     * @return user
     */
    public BaUserDto createUser(final BaUserDto uDto) {
        BaUser currentUser = requireUserManager();
        log.info("Création d'un compte utilisateur.");
        logService.log(new BaLogDto(EAction.C, "Utilisateurs : " + uDto.getUsername()));
        // Assigner l'email comme nom d'utilisateur (priorite email, sinon email pro)
        if (!BaUtils.isEmpty(uDto.getEmail())) {
            uDto.setUsername(uDto.getEmail());
        } else if (!BaUtils.isEmpty(uDto.getEmailPro())) {
            uDto.setUsername(uDto.getEmailPro());
        }

        if (this.userRepository.existsByUsernameIgnoreCase(uDto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom d'utilisateur est déjà occupé.");
        }

        if (this.userRepository.existsByTelephoneIgnoreCase(uDto.getTelephone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de téléphone est déjà utilisé.");
        }

        if (uDto.getEmail() != null && this.userRepository.existsByEmailIgnoreCase(uDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email est déjà utilisé.");
        }

//        if (uDto.getCredentials() == null || BaUtils.isEmpty(uDto.getCredentials().getPassword())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous devez fournir le mot de passe");
//        }

        validatePrivilegeAssignment(currentUser, new BaUser(), uDto);
        BaUser user = this.mapper.maps(uDto);
        if (!BaUtils.isEmpty(uDto.getEmailPro())) {
            user.setEmailPro(uDto.getEmailPro());
        }
        user.setId(BaUtils.randomUUID());
        hydrateUserRelations(user, uDto);
        validateHierarchyRules(user);
        validateUserManagementScope(currentUser, user);
        String userPassword=credentialService.prepareTemporary(user);
        user.setActivated(Boolean.TRUE);
        validateUniqueDg(user);
        BaUser save=this.userRepository.saveAndFlush(user);
        BaUserDto result=this.mapper.maps(save);result.setInitialCredential(credentialService.receipt(save,userPassword));
        return result;
    }


    /**
     * Fonction de mise à jour d'un utilisateur.
     *
     * @param id   l'identifiant de l'utilisateur
     * @param uDto DTO Utilisateur
     */
    public void updateUser(final String id, final BaUserDto uDto) {
        BaUser currentUser = requireUserManager();
        log.info("Met à jour les informations d'un compte.");
        logService.log(new BaLogDto(EAction.U, "Utilisateurs : " + uDto.getUsername()));

        if (id == null || !this.userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur est introuvable");
        }

        BaUser original=this.userRepository.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"L’utilisateur est introuvable"));
        if(entityManager!=null)entityManager.refresh(original,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(original.getStatut()==EStatut.D)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"L’utilisateur est introuvable");
        validatePrivilegeAssignment(currentUser,original,uDto);
        if(!isAdministrator(currentUser)){uDto.setRoles(original.getRoles().stream().map(mapper::maps).collect(java.util.stream.Collectors.toSet()));uDto.setIdProfil(original.getProfil()==null?null:original.getProfil().getId());}
        uDto.setPassword(original.getPassword());uDto.setActivated(original.getActivated());uDto.setLocked(original.getLocked());
        uDto.setResetKey(original.getResetKey());uDto.setResetDate(original.getResetDate());

        if (this.userRepository.checkDuplicateTelephone(id, uDto.getTelephone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de téléphone est déjà utilisé.");
        }

        if (this.userRepository.checkDuplicateEmail(id, uDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email de téléphone est déjà utilisé.");
        }

        // Forcer le username à l'email (priorite email, sinon email pro)
        if (!BaUtils.isEmpty(uDto.getEmail())) {
            uDto.setUsername(uDto.getEmail());
        } else if (!BaUtils.isEmpty(uDto.getEmailPro())) {
            uDto.setUsername(uDto.getEmailPro());
        }

        uDto.setId(id);
        BaUser user = this.mapper.maps(uDto);
        user.setId(id);user.setVersion(original.getVersion());user.setCredentialVersion(original.getCredentialVersion());
        user.setPasswordChangeRequired(original.isPasswordChangeRequired());user.setTemporaryPasswordUsed(original.isTemporaryPasswordUsed());
        user.setTemporaryPasswordExpiresAt(original.getTemporaryPasswordExpiresAt());
        if (!BaUtils.isEmpty(uDto.getEmailPro())) {
            user.setEmailPro(uDto.getEmailPro());
        }
        hydrateUserRelations(user, uDto);
        validateHierarchyRules(user);
        validateUserManagementScope(currentUser, user);
        validateUniqueDg(user);
        user = this.userRepository.save(user);
        this.mapper.maps(user);
    }

    /**
     * Supprimer logiquement l'utilisateur.
     *
     * @param id identifiant de l'utilisateur
     */
    public void doDeleteUser(final String id) {
        BaUser currentUser = requireUserManager();
        logService.log(new BaLogDto(EAction.D, "Utilisateurs : " + id));

        log.info("Supprime un compte utilisateur. " + id);
        // À partir de là, on est sur de l'existence de l'élément,
        // donc inutile d'utiliser un optional, encore.
        this.userRepository.findById(id)
                .ifPresent(u -> {
                    validateUserManagementScope(currentUser, u);
                    ensureNotSelfAction(currentUser, u, "desactiver");
                    u.setStatut(EStatut.D);
                    u.setActivated(Boolean.FALSE);
                    u.setLocked(Boolean.TRUE);
                    this.userRepository.save(u);
                });
    }


    /**
     * Modifier le mot de passe de l'utilisateur actuellement connecté.
     *
     * @param pwDto DTO du mise à jour du mot de passe
     */
    public void changePassword(final BaUpdatePasswordDto pwDto) {credentialService.change(pwDto);}


    /**
     * Vérifie si un utilisateur est activé ou pas.
     *
     * @param username Nom d'utilisateur
     * @return un boolean
     */
    public Boolean isActivated(final String username) {
        log.info("Vérification de l'état du compte " + username + " à la connexion.");
        final BaUser ylUser = this.userRepository.findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "L'utilisateur n'existe pas."));
        return ylUser.getActivated();
    }

    /**
     * Recuperer le DTO de l'utilisateur connecte.
     *
     * @return le DTO ou BadRequest.
     */
    public BaUserDto getUserInfoWithMoreDetails() {
        Optional<BaUserDto> ou = this.getCurrentUser();
        var dto=ou.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur non connecté."));
        dto.setCanResetUserAccess(getCurrentUserEntity().map(credentialService::canResetAccess).orElse(false));
        return dto;
    }

    /**
     * Avoir la liste des roles.
     *
     * @return Liste des roles
     */
    public List<BaRoleDto> fetchRoles() {
        log.info("Recupère la liste des rôles.");
        logService.log(new BaLogDto(EAction.V, "Roles"));

        final List<BaRole> allRoles = roleRepository.findAll();
        final BaUserDto userInfoWithMoreDetails = this.getUserInfoWithMoreDetails();
        log.debug("Exclure les rôles liés à l'administrateur");
        // Exclure les rôles liés à l'administrateur.
        return allRoles.stream().parallel()
                .filter(Objects::nonNull)
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    /**
     * Pour ajouter un role.
     *
     * @param roleDto : le role à ajouter
     * @return BaRoleDto
     */
    public BaRoleDto addRole(final BaRoleDto roleDto) {
        requireAdministrator();
        logService.log(new BaLogDto(EAction.C, "Roles : " + roleDto.getLibelle()));
        BaRole role = mapper.maps(roleDto);
        if (BaUtils.isEmpty(role.getId())) {
            role.setId(BaUtils.randomUUID());
        }
        log.info("Ajoute un nouveau role.");
        return mapper.maps(roleRepository.save(role));
    }

    /**
     * Mettre à jour un role.
     *
     * @param pDto : Les modifications du role
     * @return Le role mis à jour
     */
    public BaRoleDto updateRole(final BaRoleDto pDto) {
        requireAdministrator();
        logService.log(new BaLogDto(EAction.U, "Roles : " + pDto.getLibelle()));

        BaRole role;
        if (roleRepository.existsById(pDto.getId())) {
            role = roleRepository.getReferenceById(pDto.getId());
            role.setLibelle(pDto.getLibelle());
            role.setCode(pDto.getCode());
            roleRepository.save(role);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle introuvable");
        }
        return mapper.maps(roleRepository.save(role));
    }

    public BaUserDto addRoleToUser(String userId, String roleId) {
        BaUser currentUser = requireAdministrator();
        BaUser user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable avec l'ID : " + userId));
        validateUserManagementScope(currentUser, user);

        BaRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle introuvable avec l'ID : " + roleId));

        // Comparer les identifiants pour prendre en charge les proxys Hibernate.
        // Le verrou sérialise les ajouts/retraits et une relance reste sans effet supplémentaire.
        if (user.getRoles().stream().anyMatch(assigned -> Objects.equals(assigned.getId(), roleId))) return mapper.maps(user);

        // Ajouter le rôle à l'utilisateur
        user.getRoles().add(role);
        userRepository.save(user);
        
        logService.log(new BaLogDto(EAction.U, "Ajout rôle à utilisateur : " + userId + " - " + roleId));
        
        return mapper.maps(user);
    }

    public BaUserDto removeRoleFromUser(String userId, String roleId) {
        BaUser currentUser = requireAdministrator();
        BaUser user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable avec l'ID : " + userId));
        validateUserManagementScope(currentUser, user);

        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle introuvable avec l'ID : " + roleId));
        if (!user.getRoles().removeIf(assigned -> Objects.equals(assigned.getId(), roleId))) return mapper.maps(user);
        userRepository.save(user);
        
        logService.log(new BaLogDto(EAction.U, "Suppression rôle de l'utilisateur : " + userId + " - " + roleId));
        
        return mapper.maps(user);
    }


    /**
     * Recupérer la liste des profils.
     *
     * @return Liste de ProfilDto: une liste de Dto
     */
    public List<BaProfilDto> fetchProfils() {
        log.info("Recupère les profils");
        logService.log(new BaLogDto(EAction.V, "Profils"));

        List<BaProfilDto> datas;
        datas = profilRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A)
                .stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
        return datas.stream()
                .collect(Collectors.toList());
    }

    /**
     * Ajouter un profil.
     *
     * @param profilDto : Le profil à ajouter
     * @return ProfilDto
     */
    public BaProfilDto addProfil(final BaProfilDto profilDto) {
        requireAdministrator();
        profilDto.setId(BaUtils.randomUUID());
        logService.log(new BaLogDto(EAction.C, "Profils" + profilDto.getLibelle()));

        log.info("Crée un nouveau profil " + profilDto.getLibelle());
        return mapper.maps(profilRepository.save(mapper.maps(profilDto)));
    }

    /**
     * Mettre à jour un profil.
     *
     * @param pDto : Les modifications du profil
     * @return ProfilDto: Le profil mis à jour
     */
    public BaProfilDto updateProfil(final BaProfilDto pDto) {
        requireAdministrator();
        log.info("Met à jour le profil " + pDto.getLibelle());
        logService.log(new BaLogDto(EAction.U, "Profils" + pDto.getLibelle()));

        BaProfil profil;
        if (profilRepository.existsById(pDto.getId())) {
            profil = profilRepository.getReferenceById(pDto.getId());
            profil.setRoles(pDto.getRoles()
                    .stream().map(mapper::maps)
                    .collect(Collectors.toSet()));
            profil.setLibelle(pDto.getLibelle());
            profil.setDescription(pDto.getDescription());
            profilRepository.save(profil);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BaProfil introuvable");
        }
        return mapper.maps(profilRepository.save(profil));
    }

    /**
     * Supprimer un profil.
     *
     * @param uuid : Le profil à supprimer
     */
    public void deleteProfil(final String uuid) {
        requireAdministrator();
        log.warn("Suppression du profil : {}", uuid);
        logService.log(new BaLogDto(EAction.D, "Profils" + uuid));

        if (profilRepository.existsById(uuid)) {
            profilRepository.findById(uuid)
                    .ifPresent(profil -> {
                        profil.setStatut(EStatut.D);
                        this.profilRepository.save(profil);
                    });
        }
    }

    /**
     * Supprimer un rôle.
     *
     * @param uuid : Le role à supprimer
     */
    public void deleteRole(final String uuid) {
        requireAdministrator();
        log.info("Suppression du role : {}", uuid);
        logService.log(new BaLogDto(EAction.D, "Roles" + uuid));

        if (roleRepository.existsById(uuid)) {
            roleRepository.deleteById(uuid);
        }
    }


    /**
     * Récupérer l'utilisateur connecte.
     *
     * @return Un optional
     */
    private Optional<BaUserDto> getCurrentUser() {
        Optional<String> usernameFromSecurityContext = BaAuditorAwareImpl.getCurrentUserLogin();
        if (usernameFromSecurityContext.isPresent()) {
            Optional<BaUserDto> fromContext = getOneByUsername(usernameFromSecurityContext.get());
            if (fromContext.isPresent()) {
                return fromContext;
            }
        }
        final UserDetails userDetails = userDetailsFacade.getUserDetails();
        if (userDetails == null || userDetails.getUsername() == null) {
            return Optional.empty();
        }
        return getOneByUsername(userDetails.getUsername());
    }

    /**
     * Recupere un utilisateur par son nom d'utilisateur.
     *
     * @param username
     * @return un optional de l'utilisateur
     */
    public Optional<BaUserDto> getOneByUsername(final String username) {
        final Optional<BaUser> opt = this.userRepository
                .findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
        return opt.map(entity -> {
            final BaUserDto dto = mapper.maps(entity);
            // Récupérer les rôles directement depuis l'entité user
            dto.setRoles(entity.getRoles().stream()
                    .map(mapper::maps)
                    .collect(Collectors.toSet()));
            return dto;
        });
    }

    public Optional<BaUserDto> getOneById(final String id) {
        final Optional<BaUser> opt = this.userRepository.findById(id)
                .filter(user -> EStatut.A.equals(user.getStatut()));
        return opt.map(entity -> {
            final BaUserDto dto = mapper.maps(entity);
            dto.setRoles(entity.getRoles().stream()
                    .map(mapper::maps)
                    .collect(Collectors.toSet()));
            return dto;
        });
    }

    private void validatePrivilegeAssignment(BaUser actor,BaUser target,BaUserDto input) {
        if(isAdministrator(actor))return;
        var requested=input.getRoles()==null?java.util.Set.<String>of():input.getRoles().stream().map(r->java.util.Objects.toString(r.getId(),r.getCode())).collect(java.util.stream.Collectors.toSet());
        var existing=target.getRoles()==null?java.util.Set.<String>of():target.getRoles().stream().map(r->java.util.Objects.toString(r.getId(),r.getCode())).collect(java.util.stream.Collectors.toSet());
        if(!requested.isEmpty()&&!requested.equals(existing)||input.getIdProfil()!=null&&(target.getProfil()==null||!input.getIdProfil().equals(target.getProfil().getId())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"L'attribution de rôles et de profils est réservée à l'administrateur.");
    }
    private void hydrateUserRelations(final BaUser user, final BaUserDto dto) {
        if (user == null || dto == null) {
            return;
        }
        user.setService(BaUtils.isEmpty(dto.getIdService()) ? null : serviceRepository.findById(dto.getIdService())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable")));
        user.setDepartement(BaUtils.isEmpty(dto.getIdDepartement()) ? null : departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        user.setAgence(BaUtils.isEmpty(dto.getIdAgence()) ? null : agenceRepository.findById(dto.getIdAgence())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable")));
        if (isServiceNotApplicable(user.getFonction())) {
            user.setService(null);
        }
        if (user.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL) {
            user.setService(null);
            user.setDepartement(null);
            user.setAgence(null);
        }
        if (user.getAgence() != null && user.getDepartement() != null
                && user.getAgence().getDepartement() != null
                && !user.getDepartement().getId().equals(user.getAgence().getDepartement().getId())) {
            log.warn("Agence {} non coherente avec le departement {} pour l'utilisateur {}. Association agence annulee.",
                    user.getAgence().getId(),
                    user.getDepartement().getId(),
                    user.getId());
            user.setAgence(null);
        }
        user.setSuperieur(BaUtils.isEmpty(dto.getIdSuperieur()) ? null : userRepository.findById(dto.getIdSuperieur())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Superieur introuvable")));
        user.setSuperieurSecondaire(BaUtils.isEmpty(dto.getIdSuperieurSecondaire()) ? null : userRepository.findById(dto.getIdSuperieurSecondaire())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Superieur secondaire introuvable")));
        if (!BaUtils.isEmpty(dto.getIdProfil())) {
            user.setProfil(profilRepository.findById(dto.getIdProfil())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profil introuvable")));
        }
        validateOrganisation(user);
        assignAgentHierarchy(user);
        // Gérer les rôles directement depuis le DTO
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<BaRole> userRoles = dto.getRoles().stream()
                    .map(roleDto -> roleRepository.findById(roleDto.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle introuvable avec l'ID : " + roleDto.getId())))
                    .collect(Collectors.toSet());
            user.setRoles(userRoles);
        }
    }

    private void validateOrganisation(final BaUser user) {
        if (user.getFonction() == EFonctionEmploye.AGENT && user.getDepartement() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le departement est obligatoire pour un AGENT.");
        }
        if (user.getFonction() == EFonctionEmploye.AGENT && user.getService() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le service est obligatoire pour un AGENT.");
        }
        if (user.getDepartement() != null && user.getDepartement().getStatut() != EStatut.A)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le departement selectionne est inactif.");
        if (user.getService() != null) {
            if (user.getService().getStatut() != EStatut.A)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le service selectionne est inactif.");
            if (user.getService().getDepartement() == null || user.getDepartement() == null
                    || !Objects.equals(user.getService().getDepartement().getId(), user.getDepartement().getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le service doit appartenir au departement selectionne.");
        }
    }

    private boolean isServiceNotApplicable(final EFonctionEmploye fonction) {
        return fonction == EFonctionEmploye.DIRECTEUR
                || fonction == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                || fonction == EFonctionEmploye.DIRECTEUR_GENERAL;
    }

    /** N+1 = chef du service ; N+2 = directeur du département. Aucun choix arbitraire. */
    private void assignAgentHierarchy(final BaUser user) {
        if (user.getFonction() != EFonctionEmploye.AGENT) return;
        BaUser chief = null, director = null;
        // Accepter les anciennes fiches inversées, puis enregistrer l'ordre canonique.
        for (BaUser superior : java.util.stream.Stream.of(user.getSuperieur(), user.getSuperieurSecondaire())
                .filter(Objects::nonNull).toList()) {
            if (Objects.equals(user.getId(), superior.getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un utilisateur ne peut pas etre son propre superieur.");
            if (!eligibleAgentSuperior(user, superior))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le responsable doit être actif et déverrouillé : chef du service de l'agent ou directeur de son département.");
            if (superior.getFonction() == EFonctionEmploye.CHEF_SERVICE) {
                if (chief != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez un seul chef de service comme supérieur N+1.");
                chief = superior;
            } else {
                if (director != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez un seul directeur comme supérieur N+2.");
                director = superior;
            }
        }
        if (chief == null) chief = uniqueAgentSuperior(user, userRepository.findByFonctionAndServiceIdAndStatut(
                EFonctionEmploye.CHEF_SERVICE, user.getService().getId(), EStatut.A));
        if (director == null) director = uniqueAgentSuperior(user, userRepository.findByFonctionAndDepartementIdAndStatut(
                EFonctionEmploye.DIRECTEUR, user.getDepartement().getId(), EStatut.A));
        user.setSuperieur(chief);
        user.setSuperieurSecondaire(director);
    }

    private BaUser uniqueAgentSuperior(BaUser agent, List<BaUser> candidates) {
        var eligible = candidates.stream().filter(u -> eligibleAgentSuperior(agent, u)).toList();
        return eligible.size() == 1 ? eligible.get(0) : null;
    }

    private boolean eligibleAgentSuperior(BaUser agent, BaUser superior) {
        if (!com.bakouan.app.service.CongeSecurity.available(superior) || Objects.equals(agent.getId(), superior.getId())) return false;
        var department = superior.getDepartement() != null ? superior.getDepartement()
                : superior.getService() == null ? null : superior.getService().getDepartement();
        if (department == null || !Objects.equals(agent.getDepartement().getId(), department.getId())) return false;
        return superior.getFonction() == EFonctionEmploye.DIRECTEUR
                || superior.getFonction() == EFonctionEmploye.CHEF_SERVICE && superior.getService() != null
                && Objects.equals(agent.getService().getId(), superior.getService().getId());
    }

    private void validateHierarchyRules(final BaUser user) {
        if (user == null || user.getFonction() == null) {
            return;
        }

        final BaUser sup1 = user.getSuperieur();
        final BaUser sup2 = user.getSuperieurSecondaire();
        final EFonctionEmploye fonction = user.getFonction();

        if (sup1 != null && user.getId() != null && user.getId().equals(sup1.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un utilisateur ne peut pas etre son propre superieur.");
        }
        if (sup2 != null && user.getId() != null && user.getId().equals(sup2.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un utilisateur ne peut pas etre son propre superieur secondaire.");
        }
        if (sup1 != null && sup2 != null && Objects.equals(sup1.getId(), sup2.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les deux superieurs doivent etre differents.");
        }

        switch (fonction) {
            case AGENT:
                if ((sup1 != null && sup1.getFonction() != EFonctionEmploye.CHEF_SERVICE)
                        || (sup2 != null && sup2.getFonction() != EFonctionEmploye.DIRECTEUR)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Le supérieur N+1 d'un agent est son chef de service ; le supérieur N+2 est le directeur de son département.");
                }
                break;
            case CHEF_SERVICE:
                if (sup1 != null && sup1.getFonction() != EFonctionEmploye.DIRECTEUR) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Le superieur facultatif d'un CHEF_SERVICE doit etre de type DIRECTEUR.");
                }
                if (sup2 != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Un CHEF_SERVICE ne peut pas avoir de superieur secondaire.");
                }
                break;
            case DIRECTEUR:
            case DIRECTEUR_GENERAL_ADJOINT:
                if (sup1 != null && sup1.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Le superieur facultatif d'un DIRECTEUR ou DGA doit etre de type DIRECTEUR_GENERAL.");
                }
                if (sup2 != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Ce profil ne peut pas avoir de superieur secondaire.");
                }
                break;
            case DIRECTEUR_GENERAL:
                if (sup1 != null || sup2 != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Le DIRECTEUR_GENERAL ne doit pas avoir de superieur direct.");
                }
                break;
            default:
                break;
        }
    }

    /**
     * Réinitialiser le mot de passe d'un utilisateur.
     * Juste vérifier que la clé d'activation est correcte.
     *
     * @param updatePasswordDto
     */
    public void completResetPassword(final BaUpdatePasswordDto input) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Demandez un accès temporaire à votre administrateur. La récupération par e-mail est désactivée.");
    }


    /**
     * Demander la réinitialisation du mot de passe.
     *
     * @param passwordDto Pour demande la réinitialisation, je prend en compte
     *                    l'email ou le nom d'utilisateur.
     */
    public void requestPasswordReset(final BaUpdatePasswordDto input) {
        // Réponse identique pour tous les comptes, sans envoi de mail ni modification.
    }


    /**
     * Activer un utilisateur.
     *
     * @param idUser identifiant de l'utilisateur
     */
    public void activateUser(final String idUser) {
        BaUser currentUser = requireUserManager();
        log.info("Try to activate user : {}", idUser);
        logService.log(new BaLogDto(EAction.U, "Activate un utilisateur " + idUser));

        this.userRepository.findById(idUser)
                .ifPresent(usr -> {
                    validateUserManagementScope(currentUser, usr);
                    usr.setActivated(Boolean.TRUE);
                    usr.setLocked(Boolean.FALSE);
                    usr.setStatut(EStatut.A);
                    validateUniqueDg(usr);
                    userRepository.save(usr);
                });
    }

    public void deactivateUser(final String idUser) {
        BaUser currentUser = requireUserManager();
        log.info("Try to deactivate user : {}", idUser);
        logService.log(new BaLogDto(EAction.U, "Deactivate un utilisateur " + idUser));

        BaUser user = this.userRepository.findById(idUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        validateUserManagementScope(currentUser, user);
        ensureNotSelfAction(currentUser, user, "desactiver");
        user.setActivated(Boolean.FALSE);
        user.setLocked(Boolean.TRUE);
        userRepository.save(user);
    }

    public BaUser requireAdministrator() {
        BaUser actor = getCurrentUserEntity().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecte."));
        if (!isAdministrator(actor) || !Boolean.TRUE.equals(actor.getActivated()) || Boolean.TRUE.equals(actor.getLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action reservee aux administrateurs actifs.");
        }
        return actor;
    }

    /** Verrou commun aux changements de direction, y compris sur une configuration vide. */
    public void lockDirectionGenerale() {
        roleRepository.findLockedByCode(BaRolesConstants.BA_CONNECT).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Le role BA_CONNECT doit etre configure."));
    }

    private void validateUniqueDg(final BaUser user) {
        if (user.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL || user.getStatut() != EStatut.A
                || !Boolean.TRUE.equals(user.getActivated()) || Boolean.TRUE.equals(user.getLocked())) return;
        lockDirectionGenerale();
        if (userRepository.findByStatut(EStatut.A).stream().anyMatch(other ->
                other.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL && Boolean.TRUE.equals(other.getActivated())
                && !Boolean.TRUE.equals(other.getLocked()) && !Objects.equals(other.getId(), user.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un DG actif existe deja. Desactivez son compte avant de nommer son remplacant.");
        }
    }

    private BaUser requireUserManager() {
        BaUser currentUser = getCurrentUserEntity()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecte."));
        if (isAdministrator(currentUser) || currentUser.getFonction() == EFonctionEmploye.CHEF_SERVICE) {
            return currentUser;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Action reservee aux administrateurs et aux chefs de service.");
    }

    private Optional<BaUser> getCurrentUserEntity() {
        Optional<String> usernameFromSecurityContext = BaAuditorAwareImpl.getCurrentUserLogin();
        if (usernameFromSecurityContext.isPresent()) {
            Optional<BaUser> fromContext = userRepository.findOneByUsernameIgnoreCaseAndStatut(
                    usernameFromSecurityContext.get(), EStatut.A);
            if (fromContext.isPresent()) {
                return fromContext;
            }
        }
        final UserDetails userDetails = userDetailsFacade.getUserDetails();
        if (userDetails == null || userDetails.getUsername() == null) {
            return Optional.empty();
        }
        return userRepository.findOneByUsernameIgnoreCaseAndStatut(userDetails.getUsername(), EStatut.A);
    }

    private void validateUserManagementScope(final BaUser actor, final BaUser target) {
        if (actor == null || target == null || isAdministrator(actor)) {
            return;
        }
        if (actor.getFonction() != EFonctionEmploye.CHEF_SERVICE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Action reservee aux administrateurs et aux chefs de service.");
        }
        if (target.getFonction() != EFonctionEmploye.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un chef de service ne peut gerer que les agents.");
        }
        if (actor.getService() == null || target.getService() == null
                || !Objects.equals(actor.getService().getId(), target.getService().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un chef de service ne peut gerer que les agents de son service.");
        }
    }

    private void ensureNotSelfAction(final BaUser actor, final BaUser target, final String action) {
        if (actor != null && target != null && Objects.equals(actor.getId(), target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas " + action + " votre propre compte.");
        }
    }

    private boolean isAdministrator(final BaUser user) {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getStatut() == EStatut.A && BaRolesConstants.BA_ADMIN.equalsIgnoreCase(role.getCode()));
    }
}
