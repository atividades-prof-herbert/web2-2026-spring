package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.UsuarioRequestDTO;
import br.edu.ifpr.casalapp.dto.UsuarioResponseDTO;
import br.edu.ifpr.casalapp.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Lista todos os usuários")
    @GetMapping("/usuarios")
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioService.listar();
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarUsuario(@PathVariable int id) {
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);

        if (usuario != null) {
            return ResponseEntity.ok(usuario);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Cria um usuário vinculado a uma casa existente")
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponseDTO> criarUsuario(@Valid @RequestBody UsuarioRequestDTO request) {
        UsuarioResponseDTO novo = usuarioService.criar(request);
        return ResponseEntity.status(201).body(novo);
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizarUsuario(
            @PathVariable int id,
            @Valid @RequestBody UsuarioRequestDTO request) {

        UsuarioResponseDTO atualizado = usuarioService.atualizar(id, request);

        if (atualizado != null) {
            return ResponseEntity.ok(atualizado);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizarParcialUsuario(
            @PathVariable int id,
            @RequestBody UsuarioRequestDTO request) {

        UsuarioResponseDTO atualizado = usuarioService.atualizarParcial(id, request);

        if (atualizado != null) {
            return ResponseEntity.ok(atualizado);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable int id) {
        if (usuarioService.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
