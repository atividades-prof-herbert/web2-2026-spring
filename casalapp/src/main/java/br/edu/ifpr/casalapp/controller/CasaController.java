package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CasaRequestDTO;
import br.edu.ifpr.casalapp.dto.CasaResponseDTO;
import br.edu.ifpr.casalapp.service.CasaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CasaController {

    private final CasaService casaService;

    public CasaController(CasaService casaService) {
        this.casaService = casaService;
    }

    @GetMapping("/casas")
    public List<CasaResponseDTO> listarCasas() {
        return casaService.listar();
    }

    @GetMapping("/casas/{id}")
    public ResponseEntity<CasaResponseDTO> buscarCasa(@PathVariable int id) {
        CasaResponseDTO casa = casaService.buscarPorId(id);

        if (casa != null) {
            return ResponseEntity.ok(casa);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/casas")
    public ResponseEntity<CasaResponseDTO> criarCasa(@RequestBody CasaRequestDTO request) {
        CasaResponseDTO nova = casaService.criar(request);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/casas/{id}")
    public ResponseEntity<CasaResponseDTO> atualizarCasa(
            @PathVariable int id,
            @RequestBody CasaRequestDTO request) {

        CasaResponseDTO atualizada = casaService.atualizar(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/casas/{id}")
    public ResponseEntity<CasaResponseDTO> atualizarParcialCasa(
            @PathVariable int id,
            @RequestBody CasaRequestDTO request) {

        CasaResponseDTO atualizada = casaService.atualizarParcial(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/casas/{id}")
    public ResponseEntity<Void> deletarCasa(@PathVariable int id) {
        if (casaService.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
