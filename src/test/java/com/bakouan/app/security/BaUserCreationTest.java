package com.bakouan.app.security;

import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.service.BaLogService;
import com.bakouan.app.service.BaMailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaUserCreationTest {
    @Mock IUserDetailsFacade facade;
    @Mock BaUserRepository users;
    @Mock BaProfilRepository profiles;
    @Mock BaRoleRepository roles;
    @Mock BaServiceRepository services;
    @Mock BaDepartementRepository departments;
    @Mock BaAgenceRepository agencies;
    @Mock PasswordEncoder encoder;
    @Mock BaLogService log;
    @Mock BaMailService mail;
    @InjectMocks BaUserService service;
    BaDepartement department;
    BaService unit;
    BaUser actor;

    @BeforeEach void setup() {
        SecurityContextHolder.clearContext();
        org.springframework.test.util.ReflectionTestUtils.setField(service,"credentialService",new AccountCredentialService(users,encoder,new com.bakouan.app.config.PasswordAccessProperties(),log));
        actor=new BaUser("admin");actor.setUsername("admin");
        BaRole admin=new BaRole();admin.setCode("BA_ADMIN");actor.setRoles(Set.of(admin));
        when(facade.getUserDetails()).thenReturn(new User("admin","",List.of()));
        when(users.findOneByUsernameIgnoreCaseAndStatut("admin",EStatut.A)).thenReturn(Optional.of(actor));
        department=new BaDepartement();department.setId("department");
        unit=new BaService();unit.setId("service");unit.setDepartement(department);
        lenient().when(encoder.encode(anyString())).thenReturn("encoded-password");
        lenient().when(users.save(any())).thenAnswer(i->i.getArgument(0));
        lenient().when(users.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        lenient().when(roles.findLockedByCode("BA_CONNECT")).thenReturn(Optional.of(admin));
    }


    @Test void chiefCannotForgeAdministratorRightsInANewUserPayload() {
        actor.setFonction(EFonctionEmploye.CHEF_SERVICE);actor.setRoles(Set.of());actor.setService(new BaService());actor.getService().setId("service");
        BaUserDto input=request(EFonctionEmploye.AGENT);var elevated=new com.bakouan.app.dto.BaRoleDto();elevated.setCode("BA_ADMIN");elevated.setId("role-ba-admin");input.setRoles(Set.of(elevated));
        assertThatThrownBy(()->service.createUser(input)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        verify(users,never()).save(any());
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}

    @Test void createsAgentWhenNoDirectorOrChiefExists() {
        BaUserDto request=request(EFonctionEmploye.AGENT);existingStructure(request);
        BaUserDto result=service.createUser(request);
        assertThat(result.getId()).isNotBlank();
        assertThat(result.getInitialCredential().getTemporaryPassword()).hasSize(24);
        assertThat(result.isPasswordChangeRequired()).isTrue();verifyNoInteractions(mail);
        var saved=ArgumentCaptor.forClass(BaUser.class);verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getDepartement()).isSameAs(department);
        assertThat(saved.getValue().getService()).isSameAs(unit);
        assertThat(saved.getValue().getSuperieur()).isNull();assertThat(saved.getValue().getSuperieurSecondaire()).isNull();
        assertThat(saved.getValue().getActivated()).isTrue();assertThat(saved.getValue().getPassword()).isEqualTo("encoded-password");
        verify(users).findByFonctionAndDepartementIdAndStatut(EFonctionEmploye.DIRECTEUR,"department",EStatut.A);
        verify(users).findByFonctionAndServiceIdAndStatut(EFonctionEmploye.CHEF_SERVICE,"service",EStatut.A);
    }

    @ParameterizedTest @EnumSource(EFonctionEmploye.class)
    void noFunctionRequiresHigherLevelAccountsToExist(EFonctionEmploye function) {
        BaUserDto request=request(function);existingStructure(request);
        assertThatCode(()->service.createUser(request)).doesNotThrowAnyException();
    }

    @Test void agentCanHaveOnlyOneOptionalSuperior() {
        BaUserDto request=request(EFonctionEmploye.AGENT);existingStructure(request);request.setIdSuperieur("director");
        BaUser director=superior("director",EFonctionEmploye.DIRECTEUR);
        when(users.findById("director")).thenReturn(Optional.of(director));
        assertThatCode(()->service.createUser(request)).doesNotThrowAnyException();
        var saved=ArgumentCaptor.forClass(BaUser.class);verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSuperieur()).isNull();assertThat(saved.getValue().getSuperieurSecondaire()).isSameAs(director);
    }

    @Test void updatesAgentWithoutRequiringOrInventingSuperiors() {
        BaUserDto request=request(EFonctionEmploye.AGENT);existingStructure(request);
        BaUser existing=new BaUser("agent");existing.setPassword("original-password-hash");existing.setActivated(true);
        when(users.existsById("agent")).thenReturn(true);when(users.findLockedById("agent")).thenReturn(Optional.of(existing));
        service.updateUser("agent",request);
        var saved=ArgumentCaptor.forClass(BaUser.class);verify(users).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("original-password-hash");
        assertThat(saved.getValue().getSuperieur()).isNull();assertThat(saved.getValue().getSuperieurSecondaire()).isNull();
        verifyNoInteractions(mail);
    }

    @Test void serviceFromAnotherDepartmentIsRejected() {
        BaUserDto request=request(EFonctionEmploye.AGENT);existingStructure(request);
        BaDepartement foreign=new BaDepartement();foreign.setId("foreign");unit.setDepartement(foreign);
        assertThatThrownBy(()->service.createUser(request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("appartenir");
        verify(users,never()).save(any());verifyNoInteractions(mail);
    }

    @Test void inactiveServiceCannotBeAssigned() {
        BaUserDto request=request(EFonctionEmploye.AGENT);existingStructure(request);unit.setStatut(EStatut.D);
        assertThatThrownBy(()->service.createUser(request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("inactif");
        verify(users,never()).save(any());
    }

    @Test void agentStillRequiresBothDepartmentAndService() {
        BaUserDto request=request(EFonctionEmploye.AGENT);request.setIdService(null);existingStructure(request);
        assertThatThrownBy(()->service.createUser(request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("service est obligatoire");
        verify(users,never()).save(any());
    }

    @Test void ordinaryAgentCannotCreateAccountsThroughTheService() {
        actor.setRoles(Set.of());actor.setFonction(EFonctionEmploye.AGENT);
        assertThatThrownBy(()->service.createUser(request(EFonctionEmploye.AGENT))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verify(users,never()).save(any());
    }

    @Test void chiefStillCannotCreateAnAgentInAnotherService() {
        actor.setRoles(Set.of());actor.setFonction(EFonctionEmploye.CHEF_SERVICE);
        BaService foreign=new BaService();foreign.setId("other-service");actor.setService(foreign);
        BaUserDto request=request(EFonctionEmploye.AGENT);existingStructure(request);
        assertThatThrownBy(()->service.createUser(request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("son service");
        verify(users,never()).save(any());verifyNoInteractions(mail);
    }

    @Test void selfSuperiorIsStillRejectedOnUpdate() {
        BaUserDto request=request(EFonctionEmploye.AGENT);request.setIdSuperieur("agent");existingStructure(request);
        BaUser existing=new BaUser("agent");existing.setPassword("hash");
        when(users.existsById("agent")).thenReturn(true);when(users.findLockedById("agent")).thenReturn(Optional.of(existing));
        when(users.findById("agent")).thenReturn(Optional.of(existing));
        assertThatThrownBy(()->service.updateUser("agent",request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("propre superieur");
        verify(users,never()).save(any());
    }

    @Test void automaticallyAssignsOnlyChiefOfExactServiceThenDirectorOfDepartment() {
        var request=request(EFonctionEmploye.AGENT);existingStructure(request);
        var chief=superior("chief",EFonctionEmploye.CHEF_SERVICE);var director=superior("director",EFonctionEmploye.DIRECTEUR);
        when(users.findByFonctionAndServiceIdAndStatut(EFonctionEmploye.CHEF_SERVICE,"service",EStatut.A)).thenReturn(List.of(chief));
        when(users.findByFonctionAndDepartementIdAndStatut(EFonctionEmploye.DIRECTEUR,"department",EStatut.A)).thenReturn(List.of(director));
        var result=service.createUser(request);assertThat(result.getIdSuperieur()).isEqualTo("chief");assertThat(result.getIdSuperieurSecondaire()).isEqualTo("director");
        verifyNoInteractions(mail);
    }

    @Test void doesNotPickArbitrarilyWhenSeveralEligibleHoldersExist() {
        var request=request(EFonctionEmploye.AGENT);existingStructure(request);
        when(users.findByFonctionAndServiceIdAndStatut(EFonctionEmploye.CHEF_SERVICE,"service",EStatut.A)).thenReturn(List.of(superior("chief1",EFonctionEmploye.CHEF_SERVICE),superior("chief2",EFonctionEmploye.CHEF_SERVICE)));
        when(users.findByFonctionAndDepartementIdAndStatut(EFonctionEmploye.DIRECTEUR,"department",EStatut.A)).thenReturn(List.of(superior("director1",EFonctionEmploye.DIRECTEUR),superior("director2",EFonctionEmploye.DIRECTEUR)));
        var result=service.createUser(request);assertThat(result.getIdSuperieur()).isNull();assertThat(result.getIdSuperieurSecondaire()).isNull();
    }

    @Test void preservesExplicitEligibleChoicesInAnAmbiguousOrganigram() {
        var request=request(EFonctionEmploye.AGENT);existingStructure(request);request.setIdSuperieur("chief");request.setIdSuperieurSecondaire("director");
        when(users.findById("chief")).thenReturn(Optional.of(superior("chief",EFonctionEmploye.CHEF_SERVICE)));when(users.findById("director")).thenReturn(Optional.of(superior("director",EFonctionEmploye.DIRECTEUR)));
        var result=service.createUser(request);assertThat(result.getIdSuperieur()).isEqualTo("chief");assertThat(result.getIdSuperieurSecondaire()).isEqualTo("director");
    }

    @Test void excludesLockedInactiveOrInconsistentCandidatesFromAutomaticAssignment() {
        var request=request(EFonctionEmploye.AGENT);existingStructure(request);
        var locked=superior("locked",EFonctionEmploye.CHEF_SERVICE);locked.setLocked(true);
        var inactive=superior("inactive",EFonctionEmploye.CHEF_SERVICE);inactive.setActivated(false);
        var foreign=superior("foreign",EFonctionEmploye.CHEF_SERVICE);var dep=new BaDepartement();dep.setId("other");foreign.setDepartement(dep);
        var valid=superior("valid",EFonctionEmploye.CHEF_SERVICE);
        when(users.findByFonctionAndServiceIdAndStatut(EFonctionEmploye.CHEF_SERVICE,"service",EStatut.A)).thenReturn(List.of(locked,inactive,foreign,valid));
        assertThat(service.createUser(request).getIdSuperieur()).isEqualTo("valid");
    }

    @Test void rejectsExplicitChiefFromAnotherServiceRatherThanReplacingItSilently() {
        var request=request(EFonctionEmploye.AGENT);existingStructure(request);request.setIdSuperieur("chief");
        var chief=superior("chief",EFonctionEmploye.CHEF_SERVICE);var other=new BaService();other.setId("other");other.setDepartement(department);chief.setService(other);
        when(users.findById("chief")).thenReturn(Optional.of(chief));
        assertThatThrownBy(()->service.createUser(request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("chef du service");verify(users,never()).saveAndFlush(any());
    }

    private BaUser superior(String id,EFonctionEmploye function) {
        var u=new BaUser(id);u.setFonction(function);u.setActivated(true);u.setLocked(false);u.setDepartement(department);
        if(function==EFonctionEmploye.CHEF_SERVICE)u.setService(unit);
        return u;
    }

    private BaUserDto request(EFonctionEmploye function) {
        BaUserDto dto=new BaUserDto();dto.setNom("Agent");dto.setPrenom("Test");dto.setEmail("creation@example.test");dto.setTelephone("12345678");
        dto.setFonction(function);dto.setSexe(ESexe.MASCULIN);dto.setIdDepartement("department");
        if(function==EFonctionEmploye.AGENT||function==EFonctionEmploye.CHEF_SERVICE) dto.setIdService("service");return dto;
    }
    private void existingStructure(BaUserDto dto) {
        if(dto.getIdDepartement()!=null) when(departments.findById(dto.getIdDepartement())).thenReturn(Optional.of(department));
        if(dto.getIdService()!=null) when(services.findById(dto.getIdService())).thenReturn(Optional.of(unit));
    }
}
