package br.edu.ifpr.casalapp.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.LoginRequestDTO;
import br.edu.ifpr.casalapp.dto.LoginResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Token;
import br.edu.ifpr.casalapp.model.Usuario;
import br.edu.ifpr.casalapp.repository.TokenRepository;
import br.edu.ifpr.casalapp.repository.UsuarioRepository;

@Service
public class AuthService {

    private static final int HORAS_DE_VALIDADE = 8;

    private final UsuarioRepository usuarioRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());

        if (usuarioOpt.isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            return null;
        }

        Token token = emitirToken(usuario);

        Casa casa = usuario.getCasa();
        int casaId = casa != null ? casa.getId() : 0;
        String casaNome = casa != null ? casa.getNome() : null;

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                casaId,
                casaNome,
                token.getValor(),
                token.getExpiraEm()
        );
    }

    private Token emitirToken(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now();

        Token token = new Token(
                0,
                UUID.randomUUID().toString(),
                usuario,
                agora,
                agora.plusHours(HORAS_DE_VALIDADE)
        );

        return tokenRepository.save(token);
    }

    public Usuario resolverUsuario(String valorDoToken) {
        if (valorDoToken == null) {
            return null;
        }

        Optional<Token> tokenOpt = tokenRepository.findByValor(valorDoToken);

        if (tokenOpt.isEmpty()) {
            return null;
        }

        Token token = tokenOpt.get();

        if (token.estaExpirado()) {
            tokenRepository.delete(token);
            return null;
        }

        return token.getUsuario();
    }

    public void encerrarSessao(String valorDoToken) {
        Optional<Token> tokenOpt = tokenRepository.findByValor(valorDoToken);

        if (tokenOpt.isPresent()) {
            tokenRepository.delete(tokenOpt.get());
        }
    }
}
