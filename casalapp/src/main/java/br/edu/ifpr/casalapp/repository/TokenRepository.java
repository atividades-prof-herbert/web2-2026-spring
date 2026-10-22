package br.edu.ifpr.casalapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpr.casalapp.model.Token;

public interface TokenRepository extends JpaRepository<Token, Integer> {

    Optional<Token> findByValor(String valor);
}
