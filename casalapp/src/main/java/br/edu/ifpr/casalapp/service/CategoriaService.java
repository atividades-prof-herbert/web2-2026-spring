package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.CategoriaRequestDTO;
import br.edu.ifpr.casalapp.dto.CategoriaResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Categoria;
import br.edu.ifpr.casalapp.repository.CasaRepository;
import br.edu.ifpr.casalapp.repository.CategoriaRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CasaRepository casaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, CasaRepository casaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.casaRepository = casaRepository;
    }

    private CategoriaResponseDTO toResponse(Categoria categoria) {
        Casa casa = categoria.getCasa();
        int casaId = casa != null ? casa.getId() : 0;
        String casaNome = casa != null ? casa.getNome() : null;

        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getIcone(),
                casaId,
                casaNome
        );
    }

    public List<CategoriaResponseDTO> listar() {
        List<CategoriaResponseDTO> resultado = new ArrayList<>();

        for (Categoria categoria : categoriaRepository.findAll()) {
            resultado.add(toResponse(categoria));
        }

        return resultado;
    }

    public CategoriaResponseDTO buscarPorId(int id) {
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);
        if (categoriaOpt.isPresent()) {
            return toResponse(categoriaOpt.get());
        }
        return null;
    }

    public CategoriaResponseDTO criar(CategoriaRequestDTO request) {
        Casa casa = null;
        Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
        if (casaOpt.isPresent()) {
            casa = casaOpt.get();
        }

        Categoria nova = new Categoria(0, request.nome(), request.icone(), casa);
        Categoria salva = categoriaRepository.save(nova);
        return toResponse(salva);
    }

    public CategoriaResponseDTO atualizar(int id, CategoriaRequestDTO request) {
        if (!categoriaRepository.existsById(id)) {
            return null;
        }

        Casa casa = null;
        Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
        if (casaOpt.isPresent()) {
            casa = casaOpt.get();
        }

        Categoria atualizada = new Categoria(id, request.nome(), request.icone(), casa);
        Categoria salva = categoriaRepository.save(atualizada);
        return toResponse(salva);
    }

    public CategoriaResponseDTO atualizarParcial(int id, CategoriaRequestDTO request) {
        Optional<Categoria> existenteOpt = categoriaRepository.findById(id);
        if (existenteOpt.isEmpty()) {
            return null;
        }

        Categoria existente = existenteOpt.get();

        String novoNome = request.nome() != null ? request.nome() : existente.getNome();
        String novoIcone = request.icone() != null ? request.icone() : existente.getIcone();

        Casa novaCasa = existente.getCasa();
        if (request.casaId() != null) {
            novaCasa = null;
            Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
            if (casaOpt.isPresent()) {
                novaCasa = casaOpt.get();
            }
        }

        Categoria atualizada = new Categoria(id, novoNome, novoIcone, novaCasa);
        Categoria salva = categoriaRepository.save(atualizada);
        return toResponse(salva);
    }

    public boolean deletar(int id) {
        if (!categoriaRepository.existsById(id)) {
            return false;
        }
        categoriaRepository.deleteById(id);
        return true;
    }
}
