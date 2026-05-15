package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoriaController {

    private final List<CategoriaResponse> categorias = List.of(
        new CategoriaResponse(1, "Alimentação", "prato"),
        new CategoriaResponse(2, "Transporte", "carro"),
        new CategoriaResponse(3, "Saúde", "coração")
    );

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

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable int id) {
        for (CategoriaResponse categoria : categorias) {
            if (categoria.id() == id) {
                return ResponseEntity.noContent().build();
            }
        }
        return ResponseEntity.notFound().build();
    }
}