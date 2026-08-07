package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaRequest;
import br.edu.ifpr.casalapp.dto.CategoriaResponse;
import br.edu.ifpr.casalapp.model.Categoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CategoriaController {

    private int proximoId = 4;

    private final List<Categoria> categorias = new ArrayList<>(List.of(
            new Categoria(1, "Alimentação", "prato"),
            new Categoria(2, "Transporte", "carro"),
            new Categoria(3, "Saúde", "coração")
    ));

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getIcone());
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        List<CategoriaResponse> resultado = new ArrayList<>();

        for (Categoria categoria : categorias) {
            resultado.add(toResponse(categoria));
        }

        return resultado;
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> buscarCategoria(@PathVariable int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                return ResponseEntity.ok(toResponse(categoria));
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponse> criarCategoria(@RequestBody CategoriaRequest request) {
        Categoria nova = new Categoria(proximoId, request.nome(), request.icone());
        proximoId++;
        categorias.add(nova);
        return ResponseEntity.status(201).body(toResponse(nova));
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> atualizarCategoria(
            @PathVariable int id,
            @RequestBody CategoriaRequest request) {

        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).id() == id) {
                Categoria atualizada = new Categoria(id, request.nome(), request.icone());
                categorias.set(i, atualizada);
                return ResponseEntity.ok(toResponse(atualizada));
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
                Categoria existente = categorias.get(i);

                String novoNome = request.nome() != null ? request.nome() : existente.getNome();
                String novoIcone = request.icone() != null ? request.icone() : existente.getIcone();

                Categoria atualizada = new Categoria(id, novoNome, novoIcone);
                categorias.set(i, atualizada);
                return ResponseEntity.ok(toResponse(atualizada));
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                categorias.remove(categoria);
                return ResponseEntity.noContent().build();
            }
        }
        return ResponseEntity.notFound().build();
    }
}
