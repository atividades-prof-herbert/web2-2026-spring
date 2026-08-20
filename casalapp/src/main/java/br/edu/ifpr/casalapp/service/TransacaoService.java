package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.TransacaoRequestDTO;
import br.edu.ifpr.casalapp.dto.TransacaoResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Categoria;
import br.edu.ifpr.casalapp.model.Transacao;
import br.edu.ifpr.casalapp.repository.CategoriaRepository;
import br.edu.ifpr.casalapp.repository.TransacaoRepository;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;

    public TransacaoService(TransacaoRepository transacaoRepository, CategoriaRepository categoriaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    private TransacaoResponseDTO toResponse(Transacao transacao) {
        Categoria categoria = transacao.getCategoria();
        int categoriaId = 0;
        String categoriaNome = null;
        String casaNome = null;

        if (categoria != null) {
            categoriaId = categoria.getId();
            categoriaNome = categoria.getNome();

            Casa casa = categoria.getCasa();
            if (casa != null) {
                casaNome = casa.getNome();
            }
        }

        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                categoriaId,
                categoriaNome,
                casaNome
        );
    }

    public List<TransacaoResponseDTO> listar() {
        List<TransacaoResponseDTO> resultado = new ArrayList<>();

        for (Transacao transacao : transacaoRepository.findAll()) {
            resultado.add(toResponse(transacao));
        }

        return resultado;
    }

    public TransacaoResponseDTO buscarPorId(int id) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(id);
        if (transacaoOpt.isPresent()) {
            return toResponse(transacaoOpt.get());
        }
        return null;
    }

    public TransacaoResponseDTO criar(TransacaoRequestDTO request) {
        Categoria categoria = null;
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(request.categoriaId());
        if (categoriaOpt.isPresent()) {
            categoria = categoriaOpt.get();
        }

        Transacao nova = new Transacao(0, request.descricao(), request.valor(), categoria);
        Transacao salva = transacaoRepository.save(nova);
        return toResponse(salva);
    }

    public TransacaoResponseDTO atualizar(int id, TransacaoRequestDTO request) {
        if (!transacaoRepository.existsById(id)) {
            return null;
        }

        Categoria categoria = null;
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(request.categoriaId());
        if (categoriaOpt.isPresent()) {
            categoria = categoriaOpt.get();
        }

        Transacao atualizada = new Transacao(id, request.descricao(), request.valor(), categoria);
        Transacao salva = transacaoRepository.save(atualizada);
        return toResponse(salva);
    }

    public TransacaoResponseDTO atualizarParcial(int id, TransacaoRequestDTO request) {
        Optional<Transacao> existenteOpt = transacaoRepository.findById(id);
        if (existenteOpt.isEmpty()) {
            return null;
        }

        Transacao existente = existenteOpt.get();

        String novaDescricao = request.descricao() != null ? request.descricao() : existente.getDescricao();
        Double novoValor = request.valor() != null ? request.valor() : existente.getValor();

        Categoria novaCategoria = existente.getCategoria();
        if (request.categoriaId() != null) {
            novaCategoria = null;
            Optional<Categoria> categoriaOpt = categoriaRepository.findById(request.categoriaId());
            if (categoriaOpt.isPresent()) {
                novaCategoria = categoriaOpt.get();
            }
        }

        Transacao atualizada = new Transacao(id, novaDescricao, novoValor, novaCategoria);
        Transacao salva = transacaoRepository.save(atualizada);
        return toResponse(salva);
    }

    public boolean deletar(int id) {
        if (!transacaoRepository.existsById(id)) {
            return false;
        }
        transacaoRepository.deleteById(id);
        return true;
    }
}
