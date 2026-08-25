package br.edu.ifpr.casalapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequestDTO(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido") String email,
        @NotBlank(message = "senha é obrigatória") String senha,
        @NotNull(message = "casaId é obrigatório")
        @Schema(description = "id da casa à qual este usuário pertence") Integer casaId) {
}
