package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.TransacaoRequestDTO;
import br.edu.ifpr.casalapp.dto.TransacaoResponseDTO;
import br.edu.ifpr.casalapp.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @Operation(summary = "Lista todas as transações")
    @GetMapping("/transacoes")
    public List<TransacaoResponseDTO> listarTransacoes() {
        return transacaoService.listar();
    }

    @GetMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponseDTO> buscarTransacao(@PathVariable int id) {
        TransacaoResponseDTO transacao = transacaoService.buscarPorId(id);

        if (transacao != null) {
            return ResponseEntity.ok(transacao);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Cria uma transação vinculada a uma categoria existente")
    @PostMapping("/transacoes")
    public ResponseEntity<TransacaoResponseDTO> criarTransacao(@Valid @RequestBody TransacaoRequestDTO request) {
        TransacaoResponseDTO nova = transacaoService.criar(request);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponseDTO> atualizarTransacao(
            @PathVariable int id,
            @Valid @RequestBody TransacaoRequestDTO request) {

        TransacaoResponseDTO atualizada = transacaoService.atualizar(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponseDTO> atualizarParcialTransacao(
            @PathVariable int id,
            @RequestBody TransacaoRequestDTO request) {

        TransacaoResponseDTO atualizada = transacaoService.atualizarParcial(id, request);

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
