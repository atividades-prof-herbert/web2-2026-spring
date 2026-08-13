package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaDTORequest;
import br.edu.ifpr.casalapp.dto.CategoriaDTOResponse;
import br.edu.ifpr.casalapp.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/categorias")
    public List<CategoriaDTOResponse> listarCategorias() {
        return categoriaService.listar();
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTOResponse> buscarCategoria(@PathVariable int id) {
        CategoriaDTOResponse categoria = categoriaService.buscarPorId(id);

        if (categoria != null) {
            return ResponseEntity.ok(categoria);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaDTOResponse> criarCategoria(@RequestBody CategoriaDTORequest request) {
        CategoriaDTOResponse nova = categoriaService.criar(request);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTOResponse> atualizarCategoria(
            @PathVariable int id,
            @RequestBody CategoriaDTORequest request) {

        CategoriaDTOResponse atualizada = categoriaService.atualizar(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTOResponse> atualizarParcialCategoria(
            @PathVariable int id,
            @RequestBody CategoriaDTORequest request) {

        CategoriaDTOResponse atualizada = categoriaService.atualizarParcial(id, request);

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
