package br.edu.ifpr.casalapp.dto;

public record TransacaoRequest(String descricao, Double valor, Integer categoriaId) {}