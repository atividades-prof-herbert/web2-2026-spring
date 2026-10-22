package br.edu.ifpr.casalapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpr.casalapp.model.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Integer> {

    List<Transacao> findByCategoriaCasaId(int casaId);
}
