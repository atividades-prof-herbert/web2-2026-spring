package br.edu.ifpr.casalapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpr.casalapp.config.AutenticacaoInterceptor;
import br.edu.ifpr.casalapp.dto.ErroResponseDTO;
import br.edu.ifpr.casalapp.dto.LoginRequestDTO;
import br.edu.ifpr.casalapp.dto.LoginResponseDTO;
import br.edu.ifpr.casalapp.dto.UsuarioResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Usuario;
import br.edu.ifpr.casalapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Autentica um usuário e emite um token de acesso. "
            + "A recusa usa sempre a mesma mensagem, para não revelar se o email existe")
    @PostMapping("/auth/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO autenticado = authService.autenticar(request);

        if (autenticado == null) {
            return ResponseEntity.status(401).body(new ErroResponseDTO("email ou senha inválidos"));
        }

        return ResponseEntity.ok(autenticado);
    }

    @Operation(summary = "Devolve o usuário dono do token enviado no cabeçalho Authorization")
    @GetMapping("/auth/eu")
    public UsuarioResponseDTO eu(
            @RequestAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO_LOGADO) Usuario usuarioLogado) {

        Casa casa = usuarioLogado.getCasa();
        int casaId = casa != null ? casa.getId() : 0;
        String casaNome = casa != null ? casa.getNome() : null;

        return new UsuarioResponseDTO(
                usuarioLogado.getId(),
                usuarioLogado.getNome(),
                usuarioLogado.getEmail(),
                casaId,
                casaNome
        );
    }

    @Operation(summary = "Invalida o token enviado, encerrando a sessão no servidor")
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.encerrarSessao(authorization.substring("Bearer ".length()));
        return ResponseEntity.noContent().build();
    }
}
