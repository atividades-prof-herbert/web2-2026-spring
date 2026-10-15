package br.edu.ifpr.casalapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido") String email,
        @NotBlank(message = "senha é obrigatória") String senha) {
}
