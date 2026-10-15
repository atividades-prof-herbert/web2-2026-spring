package br.edu.ifpr.casalapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpr.casalapp.dto.ErroResponseDTO;
import br.edu.ifpr.casalapp.dto.LoginRequestDTO;
import br.edu.ifpr.casalapp.dto.LoginResponseDTO;
import br.edu.ifpr.casalapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Autentica um usuário a partir do email e da senha")
    @PostMapping("/auth/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO autenticado = authService.autenticar(request);

        if (autenticado == null) {
            return ResponseEntity.status(401).body(new ErroResponseDTO("email ou senha inválidos"));
        }

        return ResponseEntity.ok(autenticado);
    }
}
