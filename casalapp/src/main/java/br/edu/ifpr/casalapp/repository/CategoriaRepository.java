package br.edu.ifpr.casalapp.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import br.edu.ifpr.casalapp.model.Categoria;

@Repository
public class CategoriaRepository {

    private final List<Categoria> categorias = new ArrayList<>(List.of(
            new Categoria(1, "Alimentação", "prato", 1),
            new Categoria(2, "Transporte", "carro", 1),
            new Categoria(3, "Saúde", "coração", 2)
    ));

    public List<Categoria> listar() {
        return categorias;
    }

    public Categoria buscarPorId(int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                return categoria;
            }
        }
        return null;
    }
}
