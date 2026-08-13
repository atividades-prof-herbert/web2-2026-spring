package br.edu.ifpr.casalapp.dto;

public record TransacaoRequestDTO(String descricao, Double valor, Integer categoriaId) {}