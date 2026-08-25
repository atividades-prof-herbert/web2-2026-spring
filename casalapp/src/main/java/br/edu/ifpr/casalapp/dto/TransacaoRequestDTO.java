package br.edu.ifpr.casalapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransacaoRequestDTO(
        @NotBlank(message = "descricao é obrigatória") String descricao,
        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor deve ser maior que zero") Double valor,
        @NotNull(message = "categoriaId é obrigatório")
        @Schema(description = "id da categoria existente à qual esta transação pertence") Integer categoriaId) {
}
