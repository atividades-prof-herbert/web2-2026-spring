package br.edu.ifpr.casalapp.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        int usuarioId,
        String nome,
        String email,
        int casaId,
        String casaNome,
        @Schema(description = "token opaco a ser enviado no cabeçalho Authorization das próximas requisições")
        String token,
        LocalDateTime expiraEm) {
}
