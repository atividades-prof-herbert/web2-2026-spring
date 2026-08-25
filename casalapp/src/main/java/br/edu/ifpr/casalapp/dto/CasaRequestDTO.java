package br.edu.ifpr.casalapp.dto;

import jakarta.validation.constraints.NotBlank;

public record CasaRequestDTO(
        @NotBlank(message = "nome é obrigatório") String nome) {
}
