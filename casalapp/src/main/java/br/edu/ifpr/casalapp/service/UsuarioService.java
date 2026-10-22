package br.edu.ifpr.casalapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ifpr.casalapp.dto.UsuarioRequestDTO;
import br.edu.ifpr.casalapp.dto.UsuarioResponseDTO;
import br.edu.ifpr.casalapp.model.Casa;
import br.edu.ifpr.casalapp.model.Usuario;
import br.edu.ifpr.casalapp.repository.CasaRepository;
import br.edu.ifpr.casalapp.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CasaRepository casaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, CasaRepository casaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.casaRepository = casaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        Casa casa = usuario.getCasa();
        int casaId = casa != null ? casa.getId() : 0;
        String casaNome = casa != null ? casa.getNome() : null;

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                casaId,
                casaNome
        );
    }

    public List<UsuarioResponseDTO> listar() {
        List<UsuarioResponseDTO> resultado = new ArrayList<>();

        for (Usuario usuario : usuarioRepository.findAll()) {
            resultado.add(toResponse(usuario));
        }

        return resultado;
    }

    public UsuarioResponseDTO buscarPorId(int id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            return toResponse(usuarioOpt.get());
        }
        return null;
    }

    public UsuarioResponseDTO criar(UsuarioRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            return null;
        }

        Casa casa = null;
        Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
        if (casaOpt.isPresent()) {
            casa = casaOpt.get();
        }

        String senhaHash = passwordEncoder.encode(request.senha());

        Usuario novo = new Usuario(0, request.nome(), request.email(), senhaHash, casa);
        Usuario salvo = usuarioRepository.save(novo);
        return toResponse(salvo);
    }

    public UsuarioResponseDTO atualizar(int id, UsuarioRequestDTO request) {
        if (!usuarioRepository.existsById(id)) {
            return null;
        }

        Casa casa = null;
        Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
        if (casaOpt.isPresent()) {
            casa = casaOpt.get();
        }

        String senhaHash = passwordEncoder.encode(request.senha());

        Usuario atualizado = new Usuario(id, request.nome(), request.email(), senhaHash, casa);
        Usuario salvo = usuarioRepository.save(atualizado);
        return toResponse(salvo);
    }

    public UsuarioResponseDTO atualizarParcial(int id, UsuarioRequestDTO request) {
        Optional<Usuario> existenteOpt = usuarioRepository.findById(id);
        if (existenteOpt.isEmpty()) {
            return null;
        }

        Usuario existente = existenteOpt.get();

        String novoNome = request.nome() != null ? request.nome() : existente.getNome();
        String novoEmail = request.email() != null ? request.email() : existente.getEmail();
        String novaSenhaHash = existente.getSenhaHash();
        if (request.senha() != null) {
            novaSenhaHash = passwordEncoder.encode(request.senha());
        }

        Casa novaCasa = existente.getCasa();
        if (request.casaId() != null) {
            novaCasa = null;
            Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
            if (casaOpt.isPresent()) {
                novaCasa = casaOpt.get();
            }
        }

        Usuario atualizado = new Usuario(id, novoNome, novoEmail, novaSenhaHash, novaCasa);
        Usuario salvo = usuarioRepository.save(atualizado);
        return toResponse(salvo);
    }

    public boolean deletar(int id) {
        if (!usuarioRepository.existsById(id)) {
            return false;
        }
        usuarioRepository.deleteById(id);
        return true;
    }
}
