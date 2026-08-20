package br.edu.ifpr.casalapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpr.casalapp.model.Casa;

public interface CasaRepository extends JpaRepository<Casa, Integer> {
}
