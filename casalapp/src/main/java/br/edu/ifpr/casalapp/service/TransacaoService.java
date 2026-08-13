package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.CategoriaResponseDTO;
import br.edu.ifpr.casalapp.dto.TransacaoRequestDTO;
import br.edu.ifpr.casalapp.dto.TransacaoResponseDTO;
import br.edu.ifpr.casalapp.model.Transacao;

@Service
public class TransacaoService {

    private final CategoriaService categoriaService;

    public TransacaoService(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    private int proximoId = 4;

    private final List<Transacao> transacoes = new ArrayList<>(List.of(
            new Transacao(1, "Almoço", 35.90, 1),
            new Transacao(2, "Corrida de aplicativo", 18.50, 2),
            new Transacao(3, "Consulta médica", 150.00, 3)
    ));

    private TransacaoResponseDTO toResponse(Transacao transacao) {
        String categoriaNome = null;
        CategoriaResponseDTO categoria = categoriaService.buscarPorId(transacao.getCategoriaId());
        if (categoria != null) {
            categoriaNome = categoria.nome();
        }

        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getCategoriaId(),
                categoriaNome
        );
    }

    public List<TransacaoResponseDTO> listar() {
        List<TransacaoResponseDTO> resultado = new ArrayList<>();

        for (Transacao transacao : transacoes) {
            resultado.add(toResponse(transacao));
        }

        return resultado;
    }

    public TransacaoResponseDTO buscarPorId(int id) {
        for (Transacao transacao : transacoes) {
            if (transacao.getId() == id) {
                return toResponse(transacao);
            }
        }
        return null;
    }

    public TransacaoResponseDTO criar(TransacaoRequestDTO request) {
        Transacao nova = new Transacao(proximoId, request.descricao(), request.valor(), request.categoriaId());
        proximoId++;
        transacoes.add(nova);
        return toResponse(nova);
    }

    public TransacaoResponseDTO atualizar(int id, TransacaoRequestDTO request) {
        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId() == id) {
                Transacao atualizada = new Transacao(id, request.descricao(), request.valor(), request.categoriaId());
                transacoes.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public TransacaoResponseDTO atualizarParcial(int id, TransacaoRequestDTO request) {
        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId() == id) {
                Transacao existente = transacoes.get(i);

                String novaDescricao = request.descricao() != null ? request.descricao() : existente.getDescricao();
                Double novoValor = request.valor() != null ? request.valor() : existente.getValor();
                int novaCategoriaId = request.categoriaId() != null ? request.categoriaId() : existente.getCategoriaId();

                Transacao atualizada = new Transacao(id, novaDescricao, novoValor, novaCategoriaId);
                transacoes.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public boolean deletar(int id) {
        for (Transacao transacao : transacoes) {
            if (transacao.getId() == id) {
                transacoes.remove(transacao);
                return true;
            }
        }
        return false;
    }
}
