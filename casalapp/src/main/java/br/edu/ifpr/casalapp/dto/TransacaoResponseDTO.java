package br.edu.ifpr.casalapp.dto;

public record TransacaoResponseDTO(int id, String descricao, Double valor, int categoriaId, String categoriaNome, String casaNome) {}