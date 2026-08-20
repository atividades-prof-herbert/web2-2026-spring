package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.CasaRequestDTO;
import br.edu.ifpr.casalapp.dto.CasaResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.repository.CasaRepository;

@Service
public class CasaService {

    private final CasaRepository casaRepository;

    public CasaService(CasaRepository casaRepository) {
        this.casaRepository = casaRepository;
    }

    private CasaResponseDTO toResponse(Casa casa) {
        return new CasaResponseDTO(casa.getId(), casa.getNome(), casa.getCodigoConvite());
    }

    private String gerarCodigoConvite() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<CasaResponseDTO> listar() {
        List<CasaResponseDTO> resultado = new ArrayList<>();

        for (Casa casa : casaRepository.findAll()) {
            resultado.add(toResponse(casa));
        }

        return resultado;
    }

    public CasaResponseDTO buscarPorId(int id) {
        Optional<Casa> casaOpt = casaRepository.findById(id);
        if (casaOpt.isPresent()) {
            return toResponse(casaOpt.get());
        }
        return null;
    }

    public CasaResponseDTO criar(CasaRequestDTO request) {
        Casa nova = new Casa(0, request.nome(), gerarCodigoConvite());
        Casa salva = casaRepository.save(nova);
        return toResponse(salva);
    }

    public CasaResponseDTO atualizar(int id, CasaRequestDTO request) {
        Optional<Casa> existente = casaRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }

        Casa atualizada = new Casa(id, request.nome(), existente.get().getCodigoConvite());
        Casa salva = casaRepository.save(atualizada);
        return toResponse(salva);
    }

    public CasaResponseDTO atualizarParcial(int id, CasaRequestDTO request) {
        Optional<Casa> existenteOpt = casaRepository.findById(id);
        if (existenteOpt.isEmpty()) {
            return null;
        }

        Casa existente = existenteOpt.get();
        String novoNome = request.nome() != null ? request.nome() : existente.getNome();

        Casa atualizada = new Casa(id, novoNome, existente.getCodigoConvite());
        Casa salva = casaRepository.save(atualizada);
        return toResponse(salva);
    }

    public boolean deletar(int id) {
        if (!casaRepository.existsById(id)) {
            return false;
        }
        casaRepository.deleteById(id);
        return true;
    }
}
