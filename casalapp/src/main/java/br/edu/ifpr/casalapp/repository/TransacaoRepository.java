package br.edu.ifpr.casalapp.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import br.edu.ifpr.casalapp.model.Transacao;

@Repository
public class TransacaoRepository {

    private int proximoId = 4;

    private final List<Transacao> transacoes = new ArrayList<>(List.of(
            new Transacao(1, "Almoço", 35.90, 1),
            new Transacao(2, "Corrida de aplicativo", 18.50, 2),
            new Transacao(3, "Consulta médica", 150.00, 3)
    ));

    public List<Transacao> listar() {
        return transacoes;
    }

    public Transacao buscarPorId(int id) {
        for (Transacao transacao : transacoes) {
            if (transacao.getId() == id) {
                return transacao;
            }
        }
        return null;
    }

    public Transacao salvar(Transacao transacao) {
        Transacao nova = new Transacao(proximoId, transacao.getDescricao(), transacao.getValor(), transacao.getCategoriaId());
        proximoId++;
        transacoes.add(nova);
        return nova;
    }

    public Transacao atualizar(int id, Transacao atualizada) {
        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId() == id) {
                transacoes.set(i, atualizada);
                return atualizada;
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
