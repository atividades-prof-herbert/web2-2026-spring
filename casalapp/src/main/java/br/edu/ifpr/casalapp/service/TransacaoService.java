package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.TransacaoRequestDTO;
import br.edu.ifpr.casalapp.dto.TransacaoResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Categoria;
import br.edu.ifpr.casalapp.model.Transacao;
import br.edu.ifpr.casalapp.repository.CasaRepository;
import br.edu.ifpr.casalapp.repository.CategoriaRepository;
import br.edu.ifpr.casalapp.repository.TransacaoRepository;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CasaRepository casaRepository;

    public TransacaoService(
            TransacaoRepository transacaoRepository,
            CategoriaRepository categoriaRepository,
            CasaRepository casaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.casaRepository = casaRepository;
    }

    private TransacaoResponseDTO toResponse(Transacao transacao) {
        String categoriaNome = null;
        String casaNome = null;

        Categoria categoria = categoriaRepository.buscarPorId(transacao.getCategoriaId());
        if (categoria != null) {
            categoriaNome = categoria.getNome();

            Casa casa = casaRepository.buscarPorId(categoria.getCasaId());
            if (casa != null) {
                casaNome = casa.getNome();
            }
        }

        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getCategoriaId(),
                categoriaNome,
                casaNome
        );
    }

    public List<TransacaoResponseDTO> listar() {
        List<TransacaoResponseDTO> resultado = new ArrayList<>();

        for (Transacao transacao : transacaoRepository.listar()) {
            resultado.add(toResponse(transacao));
        }

        return resultado;
    }

    public TransacaoResponseDTO buscarPorId(int id) {
        Transacao transacao = transacaoRepository.buscarPorId(id);
        if (transacao != null) {
            return toResponse(transacao);
        }
        return null;
    }

    public TransacaoResponseDTO criar(TransacaoRequestDTO request) {
        Transacao nova = new Transacao(0, request.descricao(), request.valor(), request.categoriaId());
        Transacao salva = transacaoRepository.salvar(nova);
        return toResponse(salva);
    }

    public TransacaoResponseDTO atualizar(int id, TransacaoRequestDTO request) {
        Transacao existente = transacaoRepository.buscarPorId(id);
        if (existente == null) {
            return null;
        }

        Transacao atualizada = new Transacao(id, request.descricao(), request.valor(), request.categoriaId());
        transacaoRepository.atualizar(id, atualizada);
        return toResponse(atualizada);
    }

    public TransacaoResponseDTO atualizarParcial(int id, TransacaoRequestDTO request) {
        Transacao existente = transacaoRepository.buscarPorId(id);
        if (existente == null) {
            return null;
        }

        String novaDescricao = request.descricao() != null ? request.descricao() : existente.getDescricao();
        Double novoValor = request.valor() != null ? request.valor() : existente.getValor();
        int novaCategoriaId = request.categoriaId() != null ? request.categoriaId() : existente.getCategoriaId();

        Transacao atualizada = new Transacao(id, novaDescricao, novoValor, novaCategoriaId);
        transacaoRepository.atualizar(id, atualizada);
        return toResponse(atualizada);
    }

    public boolean deletar(int id) {
        return transacaoRepository.deletar(id);
    }
}
