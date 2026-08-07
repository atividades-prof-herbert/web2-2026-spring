package br.edu.ifpr.casalapp.model;

public class Transacao {

    private int id;
    private String descricao;
    private Double valor;
    private int categoriaId;

    public Transacao(int id, String descricao, Double valor, int categoriaId) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.categoriaId = categoriaId;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getValor() {
        return valor;
    }

    public int getCategoriaId() {
        return categoriaId;
    }
}
