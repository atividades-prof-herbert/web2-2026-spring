package br.edu.ifpr.casalapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpr.casalapp.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
}
