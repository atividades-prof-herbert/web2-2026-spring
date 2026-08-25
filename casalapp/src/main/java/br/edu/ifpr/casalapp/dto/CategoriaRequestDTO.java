package br.edu.ifpr.casalapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoriaRequestDTO(
        @NotBlank(message = "nome é obrigatório") String nome,
        String icone,
        @NotNull(message = "casaId é obrigatório")
        @Schema(description = "id da casa à qual esta categoria pertence") Integer casaId) {
}
