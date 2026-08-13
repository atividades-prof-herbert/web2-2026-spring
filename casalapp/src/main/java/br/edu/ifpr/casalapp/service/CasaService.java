package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.CasaRequestDTO;
import br.edu.ifpr.casalapp.dto.CasaResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;

@Service
public class CasaService {

    private int proximoId = 3;

    private final List<Casa> casas = new ArrayList<>(List.of(
            new Casa(1, "Casa da Praia", "CONVITE-1"),
            new Casa(2, "Apartamento Centro", "CONVITE-2")
    ));

    private CasaResponseDTO toResponse(Casa casa) {
        return new CasaResponseDTO(casa.getId(), casa.getNome(), casa.getCodigoConvite());
    }

    private String gerarCodigoConvite() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<CasaResponseDTO> listar() {
        List<CasaResponseDTO> resultado = new ArrayList<>();

        for (Casa casa : casas) {
            resultado.add(toResponse(casa));
        }

        return resultado;
    }

    public CasaResponseDTO buscarPorId(int id) {
        for (Casa casa : casas) {
            if (casa.getId() == id) {
                return toResponse(casa);
            }
        }
        return null;
    }

    public CasaResponseDTO criar(CasaRequestDTO request) {
        Casa nova = new Casa(proximoId, request.nome(), gerarCodigoConvite());
        proximoId++;
        casas.add(nova);
        return toResponse(nova);
    }

    public CasaResponseDTO atualizar(int id, CasaRequestDTO request) {
        for (int i = 0; i < casas.size(); i++) {
            if (casas.get(i).getId() == id) {
                Casa atualizada = new Casa(id, request.nome(), casas.get(i).getCodigoConvite());
                casas.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public CasaResponseDTO atualizarParcial(int id, CasaRequestDTO request) {
        for (int i = 0; i < casas.size(); i++) {
            if (casas.get(i).getId() == id) {
                Casa existente = casas.get(i);

                String novoNome = request.nome() != null ? request.nome() : existente.getNome();

                Casa atualizada = new Casa(id, novoNome, existente.getCodigoConvite());
                casas.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public boolean deletar(int id) {
        for (Casa casa : casas) {
            if (casa.getId() == id) {
                casas.remove(casa);
                return true;
            }
        }
        return false;
    }
}
