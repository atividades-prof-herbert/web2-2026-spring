package br.edu.ifpr.casalapp.dto;

public record TransacaoResponse(int id, String descricao, Double valor, int categoriaId, String categoriaNome) {}