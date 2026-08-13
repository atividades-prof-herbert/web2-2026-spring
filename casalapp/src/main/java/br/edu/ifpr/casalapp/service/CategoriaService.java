package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.CasaResponseDTO;
import br.edu.ifpr.casalapp.dto.CategoriaRequestDTO;
import br.edu.ifpr.casalapp.dto.CategoriaResponseDTO;
import br.edu.ifpr.casalapp.model.Categoria;

@Service
public class CategoriaService {

    private final CasaService casaService;

    public CategoriaService(CasaService casaService) {
        this.casaService = casaService;
    }

    private int proximoId = 4;

    private final List<Categoria> categorias = new ArrayList<>(List.of(
            new Categoria(1, "Alimentação", "prato", 1),
            new Categoria(2, "Transporte", "carro", 1),
            new Categoria(3, "Saúde", "coração", 2)
    ));

    private CategoriaResponseDTO toResponse(Categoria categoria) {
        String casaNome = null;
        CasaResponseDTO casa = casaService.buscarPorId(categoria.getCasaId());
        if (casa != null) {
            casaNome = casa.nome();
        }

        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getIcone(),
                categoria.getCasaId(),
                casaNome
        );
    }

    public List<CategoriaResponseDTO> listar() {
        List<CategoriaResponseDTO> resultado = new ArrayList<>();

        for (Categoria categoria : categorias) {
            resultado.add(toResponse(categoria));
        }

        return resultado;
    }

    public CategoriaResponseDTO buscarPorId(int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                return toResponse(categoria);
            }
        }
        return null;
    }

    public CategoriaResponseDTO criar(CategoriaRequestDTO request) {
        Categoria nova = new Categoria(proximoId, request.nome(), request.icone(), request.casaId());
        proximoId++;
        categorias.add(nova);
        return toResponse(nova);
    }

    public CategoriaResponseDTO atualizar(int id, CategoriaRequestDTO request) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getId() == id) {
                Categoria atualizada = new Categoria(id, request.nome(), request.icone(), request.casaId());
                categorias.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public CategoriaResponseDTO atualizarParcial(int id, CategoriaRequestDTO request) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getId() == id) {
                Categoria existente = categorias.get(i);

                String novoNome = request.nome() != null ? request.nome() : existente.getNome();
                String novoIcone = request.icone() != null ? request.icone() : existente.getIcone();
                int novaCasaId = request.casaId() != null ? request.casaId() : existente.getCasaId();

                Categoria atualizada = new Categoria(id, novoNome, novoIcone, novaCasaId);
                categorias.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public boolean deletar(int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                categorias.remove(categoria);
                return true;
            }
        }
        return false;
    }
}
