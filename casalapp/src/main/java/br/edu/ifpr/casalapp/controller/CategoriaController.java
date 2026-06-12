package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaRequest;
import br.edu.ifpr.casalapp.dto.CategoriaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CategoriaController {

    private int proximoId = 4;

    private final List<CategoriaResponse> categorias = new ArrayList<>(List.of(
            new CategoriaResponse(1, "Alimentação", "prato"),
            new CategoriaResponse(2, "Transporte", "carro"),
            new CategoriaResponse(3, "Saúde", "coração")
    ));

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return categorias;
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> buscarCategoria(@PathVariable int id) {
        for (CategoriaResponse categoria : categorias) {
            if (categoria.id() == id) {
                return ResponseEntity.ok(categoria);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponse> criarCategoria(@RequestBody CategoriaRequest request) {
        CategoriaResponse nova = new CategoriaResponse(proximoId, request.nome(), request.icone());
        proximoId++;
        categorias.add(nova);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> atualizarCategoria(
            @PathVariable int id,
            @RequestBody CategoriaRequest request) {

        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).id() == id) {
                CategoriaResponse atualizada = new CategoriaResponse(id, request.nome(), request.icone());
                categorias.set(i, atualizada);
                return ResponseEntity.ok(atualizada);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> atualizarParcialCategoria(
            @PathVariable int id,
            @RequestBody CategoriaRequest request) {

        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).id() == id) {
                CategoriaResponse existente = categorias.get(i);

                String novoNome = request.nome() != null ? request.nome() : existente.nome();
                String novoIcone = request.icone() != null ? request.icone() : existente.icone();

                CategoriaResponse atualizada = new CategoriaResponse(id, novoNome, novoIcone);
                categorias.set(i, atualizada);
                return ResponseEntity.ok(atualizada);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable int id) {
        for (CategoriaResponse categoria : categorias) {
            if (categoria.id() == id) {
                categorias.remove(categoria);
                return ResponseEntity.noContent().build();
            }
        }
        return ResponseEntity.notFound().build();
    }
}
