package br.edu.ifpr.casalapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpr.casalapp.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}
