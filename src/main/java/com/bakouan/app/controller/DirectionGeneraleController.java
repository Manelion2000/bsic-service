package com.bakouan.app.controller;

import com.bakouan.app.dto.*;
import com.bakouan.app.service.DirectionGeneraleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/direction-generale")
public class DirectionGeneraleController {
    private final DirectionGeneraleService service;
    @GetMapping("/dg") public List<BaUserDto> getDg() { return service.getDg(); }
    @PostMapping("/dg") @ResponseStatus(HttpStatus.CREATED)
    public BaUserDto createDg(@Valid @RequestBody BaDirectionGeneraleRequestDto request,jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control","no-store");return service.createDg(request); }
    @GetMapping("/dga") public List<BaUserDto> getDga() { return service.getDga(); }
    @PostMapping("/dga") @ResponseStatus(HttpStatus.CREATED)
    public BaUserDto createDga(@Valid @RequestBody BaDirectionGeneraleRequestDto request,jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control","no-store");return service.createDga(request); }
    @GetMapping("/affectations-dga") public List<BaDgaPoleValidateurDto> getAssignments() { return service.getAssignments(); }
    @PostMapping("/affectations-dga") @ResponseStatus(HttpStatus.CREATED)
    public BaDgaPoleValidateurDto createAssignment(@RequestBody BaDgaPoleValidateurDto request) { return service.saveAssignment(null, request); }
    @PutMapping("/affectations-dga/{id}")
    public BaDgaPoleValidateurDto updateAssignment(@PathVariable String id, @RequestBody BaDgaPoleValidateurDto request) { return service.saveAssignment(id, request); }
    @DeleteMapping("/affectations-dga/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAssignment(@PathVariable String id) { service.deactivateAssignment(id); }
}
