package com.freightfox.tollplaza.controller;

import com.freightfox.tollplaza.dto.TollPlazaRequest;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.service.TollPlazaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TollPlazaController {

    private final TollPlazaService tollPlazaService;

    @PostMapping("/toll-plazas")
    public ResponseEntity<TollPlazaResponse> getTollPlazas(@Valid @RequestBody TollPlazaRequest request) {
        return ResponseEntity.ok(tollPlazaService.findTollPlazas(request));
    }
}
