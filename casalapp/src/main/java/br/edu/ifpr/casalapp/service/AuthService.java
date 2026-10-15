package br.edu.ifpr.casalapp.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.LoginRequestDTO;
import br.edu.ifpr.casalapp.dto.LoginResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Usuario;
import br.edu.ifpr.casalapp.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());

        if (usuarioOpt.isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.getSenha().equals(request.senha())) {
            return null;
        }

        Casa casa = usuario.getCasa();
        int casaId = casa != null ? casa.getId() : 0;
        String casaNome = casa != null ? casa.getNome() : null;

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                casaId,
                casaNome
        );
    }
}
