package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.TransacaoRequest;
import br.edu.ifpr.casalapp.dto.TransacaoResponse;
import br.edu.ifpr.casalapp.service.TransacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping("/transacoes")
    public List<TransacaoResponse> listarTransacoes() {
        return transacaoService.listar();
    }

    @GetMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> buscarTransacao(@PathVariable int id) {
        TransacaoResponse transacao = transacaoService.buscarPorId(id);

        if (transacao != null) {
            return ResponseEntity.ok(transacao);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/transacoes")
    public ResponseEntity<TransacaoResponse> criarTransacao(@RequestBody TransacaoRequest request) {
        TransacaoResponse nova = transacaoService.criar(request);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> atualizarTransacao(
            @PathVariable int id,
            @RequestBody TransacaoRequest request) {

        TransacaoResponse atualizada = transacaoService.atualizar(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> atualizarParcialTransacao(
            @PathVariable int id,
            @RequestBody TransacaoRequest request) {

        TransacaoResponse atualizada = transacaoService.atualizarParcial(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/transacoes/{id}")
    public ResponseEntity<Void> deletarTransacao(@PathVariable int id) {
        if (transacaoService.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
