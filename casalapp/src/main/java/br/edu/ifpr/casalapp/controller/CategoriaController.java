package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaRequestDTO;
import br.edu.ifpr.casalapp.dto.CategoriaResponseDTO;
import br.edu.ifpr.casalapp.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Lista todas as categorias")
    @GetMapping("/categorias")
    public List<CategoriaResponseDTO> listarCategorias() {
        return categoriaService.listar();
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarCategoria(@PathVariable int id) {
        CategoriaResponseDTO categoria = categoriaService.buscarPorId(id);

        if (categoria != null) {
            return ResponseEntity.ok(categoria);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Cria uma categoria vinculada a uma casa existente")
    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponseDTO> criarCategoria(@Valid @RequestBody CategoriaRequestDTO request) {
        CategoriaResponseDTO nova = categoriaService.criar(request);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizarCategoria(
            @PathVariable int id,
            @Valid @RequestBody CategoriaRequestDTO request) {

        CategoriaResponseDTO atualizada = categoriaService.atualizar(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizarParcialCategoria(
            @PathVariable int id,
            @RequestBody CategoriaRequestDTO request) {

        CategoriaResponseDTO atualizada = categoriaService.atualizarParcial(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable int id) {
        if (categoriaService.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
