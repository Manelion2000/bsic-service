package com.bakouan.app.service.impl;

import com.bakouan.app.dto.CircuitEtapeRequestDto;
import com.bakouan.app.dto.CircuitEtapeResponseDto;
import com.bakouan.app.dto.CircuitRequestDto;
import com.bakouan.app.dto.CircuitResponseDto;
import com.bakouan.app.enums.ECircuitStatut;
import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.CircuitMapper;
import com.bakouan.app.model.BaCircuit;
import com.bakouan.app.model.BaCircuitEtape;
import com.bakouan.app.model.BaDepartement;
import com.bakouan.app.model.BaService;
import com.bakouan.app.repositories.BaCircuitEtapeRepository;
import com.bakouan.app.repositories.BaCircuitRepository;
import com.bakouan.app.repositories.BaDepartementRepository;
import com.bakouan.app.repositories.BaRoleRepository;
import com.bakouan.app.repositories.BaServiceRepository;
import com.bakouan.app.service.CircuitService;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CircuitServiceImpl implements CircuitService {

    private final BaCircuitRepository circuitRepository;
    private final BaCircuitEtapeRepository circuitEtapeRepository;
    private final BaDepartementRepository departementRepository;
    private final BaServiceRepository serviceRepository;
    private final BaRoleRepository roleRepository;
    private final CircuitMapper circuitMapper;

    @Override
    public CircuitResponseDto createCircuit(final CircuitRequestDto request) {
        validateCircuitRequest(request);
        BaCircuit circuit = new BaCircuit();
        circuit.setId(BaUtils.randomUUID());
        circuit.setCode(generateCircuitCode());
        circuit.setLibelle(request.getLibelle().trim());
        circuit.setDescription(request.getDescription());
        applyCircuitStatus(circuit, request);
        BaCircuit saved = circuitRepository.save(circuit);

        if (request.getEtapes() != null && !request.getEtapes().isEmpty()) {
            replaceCircuitEtapes(saved.getId(), request.getEtapes());
        }
        assertCircuitCanBeActive(saved.getId(), saved.getStatu());
        return getCircuitById(saved.getId());
    }

    @Override
    public CircuitResponseDto updateCircuit(final String id, final CircuitRequestDto request) {
        validateCircuitRequest(request);
        BaCircuit circuit = getCircuitEntity(id);
        circuit.setLibelle(request.getLibelle().trim());
        circuit.setDescription(request.getDescription());
        applyCircuitStatus(circuit, request);
        circuitRepository.save(circuit);

        if (request.getEtapes() != null) {
            replaceCircuitEtapes(id, request.getEtapes());
        }
        assertCircuitCanBeActive(id, circuit.getStatu());
        return getCircuitById(id);
    }

    @Override
    public CircuitResponseDto getCircuitById(final String id) {
        BaCircuit circuit = getCircuitEntity(id);
        List<BaCircuitEtape> etapes = circuitEtapeRepository.findByCircuitIdOrderByOrdre(circuit.getId());
        return circuitMapper.toResponse(circuit, etapes);
    }

    @Override
    public List<CircuitResponseDto> getCircuits() {
        return circuitRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .map(c -> circuitMapper.toResponse(c, circuitEtapeRepository.findByCircuitIdOrderByOrdre(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CircuitResponseDto> getCircuitsActifs() {
        return circuitRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .filter(c -> c.getStatu() == ECircuitStatut.ACTIF)
                .map(c -> circuitMapper.toResponse(c, circuitEtapeRepository.findByCircuitIdOrderByOrdre(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public CircuitResponseDto activerCircuit(final String id) {
        BaCircuit circuit = getCircuitEntity(id);
        if (!testerCircuit(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le circuit est invalide et ne peut pas etre active.");
        }
        circuit.setStatu(ECircuitStatut.ACTIF);
        circuit.setActif(true);
        circuitRepository.save(circuit);
        return getCircuitById(id);
    }

    @Override
    public CircuitResponseDto desactiverCircuit(final String id) {
        BaCircuit circuit = getCircuitEntity(id);
        circuit.setStatu(ECircuitStatut.INACTIF);
        circuit.setActif(false);
        circuitRepository.save(circuit);
        return getCircuitById(id);
    }

    @Override
    public CircuitResponseDto dupliquerCircuit(final String id, final String nouveauNom) {
        if (BaUtils.isEmpty(nouveauNom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du nouveau circuit est obligatoire.");
        }
        BaCircuit original = getCircuitEntity(id);
        BaCircuit copy = new BaCircuit();
        copy.setId(BaUtils.randomUUID());
        copy.setCode(generateCircuitCode());
        copy.setLibelle(nouveauNom.trim());
        copy.setDescription("Duplication de: " + original.getLibelle());
        copy.setStatu(ECircuitStatut.EN_CONFIGURATION);
        copy.setActif(false);
        BaCircuit saved = circuitRepository.save(copy);
        List<CircuitEtapeRequestDto> etapes = circuitEtapeRepository.findByCircuitIdOrderByOrdre(id).stream()
                .map(this::toRequest)
                .collect(Collectors.toList());
        if (!etapes.isEmpty()) {
            replaceCircuitEtapes(saved.getId(), etapes);
        }
        return getCircuitById(saved.getId());
    }

    @Override
    public boolean testerCircuit(final String id) {
        try {
            getCircuitEntity(id);
            List<BaCircuitEtape> etapes = circuitEtapeRepository.findByCircuitIdOrderByOrdre(id);
            if (etapes.isEmpty()) {
                return false;
            }
            for (int i = 0; i < etapes.size(); i++) {
                BaCircuitEtape etape = etapes.get(i);
                if (etape.getOrdre() == null || etape.getOrdre() != i + 1) {
                    return false;
                }
                if (Boolean.FALSE.equals(etape.getActif())) {
                    return false;
                }
                if (etape.getType() == EHabilitationEtapeType.SERVICE && etape.getService() == null) {
                    return false;
                }
                if (etape.getType() == EHabilitationEtapeType.DEPARTEMENT && etape.getDepartement() == null) {
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void deleteCircuit(final String id) {
        BaCircuit circuit = getCircuitEntity(id);
        circuit.setActif(false);
        circuit.setStatu(ECircuitStatut.INACTIF);
        circuit.setStatut(EStatut.D);
        circuitRepository.save(circuit);
    }

    @Override
    public List<CircuitEtapeResponseDto> getCircuitEtapes(final String circuitId) {
        getCircuitEntity(circuitId);
        return circuitEtapeRepository.findByCircuitIdOrderByOrdre(circuitId).stream()
                .map(circuitMapper::toEtapeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CircuitEtapeResponseDto> replaceCircuitEtapes(final String circuitId,
                                                              final List<CircuitEtapeRequestDto> etapes) {
        BaCircuit circuit = getCircuitEntity(circuitId);
        validateEtapes(etapes);
        circuitEtapeRepository.deleteByCircuitId(circuitId);
        List<BaCircuitEtape> saved = persistEtapes(circuit, etapes);
        return saved.stream().map(circuitMapper::toEtapeResponse).collect(Collectors.toList());
    }

    @Override
    public CircuitEtapeResponseDto addCircuitEtape(final String circuitId, final CircuitEtapeRequestDto request) {
        BaCircuit circuit = getCircuitEntity(circuitId);
        validateSingleEtape(request);
        if (circuitEtapeRepository.existsByCircuitIdAndOrdre(circuitId, request.getOrdre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une etape existe deja pour cet ordre.");
        }
        BaCircuitEtape entity = buildEtapeEntity(circuit, request);
        return circuitMapper.toEtapeResponse(circuitEtapeRepository.save(entity));
    }

    @Override
    public CircuitEtapeResponseDto updateCircuitEtape(final String circuitId,
                                                      final String etapeId,
                                                      final CircuitEtapeRequestDto request) {
        getCircuitEntity(circuitId);
        validateSingleEtape(request);
        BaCircuitEtape etape = circuitEtapeRepository.findById(etapeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etape introuvable"));
        if (!circuitId.equals(etape.getCircuit().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etape non liee a ce circuit.");
        }

        List<BaCircuitEtape> siblings = circuitEtapeRepository.findByCircuitIdOrderByOrdre(circuitId);
        boolean ordreUsed = siblings.stream().anyMatch(e -> !e.getId().equals(etapeId) && request.getOrdre().equals(e.getOrdre()));
        if (ordreUsed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une etape existe deja pour cet ordre.");
        }

        etape.setOrdre(request.getOrdre());
        etape.setType(request.getType() == null ? EHabilitationEtapeType.DEPARTEMENT : request.getType());
        applyEtapeOptions(etape, request);
        if (etape.getType() == EHabilitationEtapeType.SERVICE) {
            etape.setService(resolveService(request.getServiceId()));
            etape.setDepartement(null);
        } else {
            etape.setDepartement(resolveDepartement(request.getDepartementId()));
            etape.setService(null);
        }
        return circuitMapper.toEtapeResponse(circuitEtapeRepository.save(etape));
    }

    @Override
    public void deleteCircuitEtape(final String circuitId, final String etapeId) {
        getCircuitEntity(circuitId);
        BaCircuitEtape etape = circuitEtapeRepository.findById(etapeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etape introuvable"));
        if (!circuitId.equals(etape.getCircuit().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etape non liee a ce circuit.");
        }
        circuitEtapeRepository.delete(etape);
    }

    private void validateCircuitRequest(final CircuitRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le payload du circuit est obligatoire.");
        }
        if (BaUtils.isEmpty(request.getLibelle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le libelle du circuit est obligatoire.");
        }
    }

    private void validateEtapes(final List<CircuitEtapeRequestDto> etapes) {
        if (etapes == null || etapes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le circuit doit contenir au moins une etape.");
        }
        Set<Integer> ordres = new HashSet<>();
        Set<String> targets = new HashSet<>();
        for (CircuitEtapeRequestDto etape : etapes) {
            validateSingleEtape(etape);
            if (!ordres.add(etape.getOrdre())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deux etapes ne peuvent pas avoir le meme ordre.");
            }
            String targetKey = (etape.getType() == EHabilitationEtapeType.SERVICE
                    ? "SERVICE:" + etape.getServiceId()
                    : "DEPARTEMENT:" + etape.getDepartementId());
            if (!targets.add(targetKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une etape ne peut apparaitre qu'une fois dans le circuit.");
            }
        }
    }

    private void validateSingleEtape(final CircuitEtapeRequestDto etape) {
        if (etape == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le payload d'etape est obligatoire.");
        }
        if (etape.getOrdre() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ordre de l'etape est obligatoire.");
        }
        if (BaUtils.isEmpty(etape.getRoleId()) && BaUtils.isEmpty(etape.getFonctionRequise())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le role validateur ou le role metier de l'etape est obligatoire.");
        }
        EHabilitationEtapeType type = etape.getType() == null ? EHabilitationEtapeType.DEPARTEMENT : etape.getType();
        if (type == EHabilitationEtapeType.SERVICE) {
            if (BaUtils.isEmpty(etape.getServiceId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le service de l'etape est obligatoire.");
            }
            if (!BaUtils.isEmpty(etape.getDepartementId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une etape SERVICE ne doit pas contenir de departement.");
            }
            resolveService(etape.getServiceId());
        } else {
            if (BaUtils.isEmpty(etape.getDepartementId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le departement de l'etape est obligatoire.");
            }
            if (!BaUtils.isEmpty(etape.getServiceId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une etape DEPARTEMENT ne doit pas contenir de service.");
            }
            resolveDepartement(etape.getDepartementId());
        }
    }

    private List<BaCircuitEtape> persistEtapes(final BaCircuit circuit, final List<CircuitEtapeRequestDto> etapes) {
        List<BaCircuitEtape> entities = etapes.stream()
                .sorted(Comparator.comparingInt(CircuitEtapeRequestDto::getOrdre))
                .map(etape -> buildEtapeEntity(circuit, etape))
                .collect(Collectors.toList());
        return circuitEtapeRepository.saveAll(entities).stream()
                .sorted(Comparator.comparingInt(BaCircuitEtape::getOrdre))
                .collect(Collectors.toList());
    }

    private BaCircuitEtape buildEtapeEntity(final BaCircuit circuit, final CircuitEtapeRequestDto request) {
        BaCircuitEtape entity = new BaCircuitEtape();
        entity.setId(BaUtils.randomUUID());
        entity.setCircuit(circuit);
        entity.setOrdre(request.getOrdre());
        EHabilitationEtapeType type = request.getType() == null ? EHabilitationEtapeType.DEPARTEMENT : request.getType();
        entity.setType(type);
        applyEtapeOptions(entity, request);
        if (type == EHabilitationEtapeType.SERVICE) {
            entity.setService(resolveService(request.getServiceId()));
            entity.setDepartement(null);
        } else {
            entity.setDepartement(resolveDepartement(request.getDepartementId()));
            entity.setService(null);
        }
        return entity;
    }

    private void applyCircuitStatus(final BaCircuit circuit, final CircuitRequestDto request) {
        ECircuitStatut status = request.getStatutCircuit();
        if (status == null) {
            status = Boolean.FALSE.equals(request.getActif()) ? ECircuitStatut.INACTIF : ECircuitStatut.EN_CONFIGURATION;
        }
        circuit.setStatu(status);
        circuit.setActif(status == ECircuitStatut.ACTIF);
    }

    private void assertCircuitCanBeActive(final String id, final ECircuitStatut status) {
        if (status == ECircuitStatut.ACTIF && !testerCircuit(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le circuit est invalide et ne peut pas etre active.");
        }
    }

    private void applyEtapeOptions(final BaCircuitEtape etape, final CircuitEtapeRequestDto request) {
        etape.setObligatoire(request.getObligatoire() == null || request.getObligatoire());
        etape.setActif(request.getActif() == null || request.getActif());
        etape.setFonctionRequise(BaUtils.isEmpty(request.getFonctionRequise()) ? null : request.getFonctionRequise().trim());
        etape.setDelaiValidationJours(request.getDelaiValidationJours());
        etape.setConditions(BaUtils.isEmpty(request.getConditions()) ? null : request.getConditions().trim());
        etape.setRole(BaUtils.isEmpty(request.getRoleId()) ? null : roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role introuvable: " + request.getRoleId())));
    }

    private CircuitEtapeRequestDto toRequest(final BaCircuitEtape etape) {
        CircuitEtapeRequestDto request = new CircuitEtapeRequestDto();
        request.setType(etape.getType());
        request.setOrdre(etape.getOrdre());
        request.setDepartementId(etape.getDepartement() == null ? null : etape.getDepartement().getId());
        request.setServiceId(etape.getService() == null ? null : etape.getService().getId());
        request.setRoleId(etape.getRole() == null ? null : etape.getRole().getId());
        request.setObligatoire(etape.getObligatoire());
        request.setActif(etape.getActif());
        request.setFonctionRequise(etape.getFonctionRequise());
        request.setDelaiValidationJours(etape.getDelaiValidationJours());
        request.setConditions(etape.getConditions());
        return request;
    }

    private BaDepartement resolveDepartement(final String departementId) {
        return departementRepository.findById(departementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable: " + departementId));
    }

    private BaService resolveService(final String serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable: " + serviceId));
    }

    private BaCircuit getCircuitEntity(final String id) {
        return circuitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit introuvable"));
    }

    private String generateCircuitCode() {
        int next = circuitRepository.countByCreatedDateBetween(
                Year.now().atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                Year.now().plusYears(1).atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        ) + 1;
        return String.format("CIR-%d-%03d", Year.now().getValue(), next);
    }
}
