package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.model.Categoria;
import br.edu.ifpr.casalapp.dto.TransacaoRequest;
import br.edu.ifpr.casalapp.dto.TransacaoResponse;
import br.edu.ifpr.casalapp.model.Transacao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TransacaoController {

    private int proximoId = 4;

    private final List<Transacao> transacoes = new ArrayList<>(List.of(
            new Transacao(1, "Almoço", 35.90, 1),
            new Transacao(2, "Corrida de aplicativo", 18.50, 2),
            new Transacao(3, "Consulta médica", 150.00, 3)
    ));

    // Cópia dos dados de categorias, só para fazer o join.
    // Cada controller ainda tem o seu próprio "banco fake".
    private final List<Categoria> categorias = new ArrayList<>(List.of(
            new Categoria(1, "Alimentação", "prato"),
            new Categoria(2, "Transporte", "carro"),
            new Categoria(3, "Saúde", "coração")
    ));

    // INNER JOIN simples: para cada transação, percorre o vetor de categorias
    // procurando a categoria correspondente ao categoriaId.
    private TransacaoResponse toResponse(Transacao transacao) {
        String categoriaNome = null;
        for (Categoria categoria : categorias) {
            if (categoria.getId() == transacao.getCategoriaId()) {
                categoriaNome = categoria.getNome();
                break;
            }
        }

        return new TransacaoResponse(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getCategoriaId(),
                categoriaNome
        );
    }

    @GetMapping("/transacoes")
    public List<TransacaoResponse> listarTransacoes() {
        List<TransacaoResponse> resultado = new ArrayList<>();

        for (Transacao transacao : transacoes) {
            resultado.add(toResponse(transacao));
        }

        return resultado;
    }

    @GetMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> buscarTransacao(@PathVariable int id) {
        for (Transacao transacao : transacoes) {
            if (transacao.getId() == id) {
                return ResponseEntity.ok(toResponse(transacao));
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/transacoes")
    public ResponseEntity<TransacaoResponse> criarTransacao(@RequestBody TransacaoRequest request) {
        Transacao nova = new Transacao(proximoId, request.descricao(), request.valor(), request.categoriaId());
        proximoId++;
        transacoes.add(nova);
        return ResponseEntity.status(201).body(toResponse(nova));
    }

    @PutMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> atualizarTransacao(
            @PathVariable int id,
            @RequestBody TransacaoRequest request) {

        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId() == id) {
                Transacao atualizada = new Transacao(id, request.descricao(), request.valor(), request.categoriaId());
                transacoes.set(i, atualizada);
                return ResponseEntity.ok(toResponse(atualizada));
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> atualizarParcialTransacao(
            @PathVariable int id,
            @RequestBody TransacaoRequest request) {

        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId() == id) {
                Transacao existente = transacoes.get(i);

                String novaDescricao = request.descricao() != null ? request.descricao() : existente.getDescricao();
                Double novoValor = request.valor() != null ? request.valor() : existente.getValor();
                int novaCategoriaId = request.categoriaId() != null ? request.categoriaId() : existente.getCategoriaId();

                Transacao atualizada = new Transacao(id, novaDescricao, novoValor, novaCategoriaId);
                transacoes.set(i, atualizada);
                return ResponseEntity.ok(toResponse(atualizada));
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/transacoes/{id}")
    public ResponseEntity<Void> deletarTransacao(@PathVariable int id) {
        for (Transacao transacao : transacoes) {
            if (transacao.getId() == id) {
                transacoes.remove(transacao);
                return ResponseEntity.noContent().build();
            }
        }
        return ResponseEntity.notFound().build();
    }
}