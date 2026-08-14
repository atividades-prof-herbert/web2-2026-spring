package br.edu.ifpr.casalapp.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import br.edu.ifpr.casalapp.model.Casa;

@Repository
public class CasaRepository {

    private final List<Casa> casas = new ArrayList<>(List.of(
            new Casa(1, "Casa da Praia", "CONVITE-1"),
            new Casa(2, "Apartamento Centro", "CONVITE-2")
    ));

    public List<Casa> listar() {
        return casas;
    }

    public Casa buscarPorId(int id) {
        for (Casa casa : casas) {
            if (casa.getId() == id) {
                return casa;
            }
        }
        return null;
    }
}
