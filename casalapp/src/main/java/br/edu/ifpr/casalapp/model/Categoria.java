package br.edu.ifpr.casalapp.model;

public class Categoria {

    private int id;
    private String nome;
    private String icone;

    public Categoria(int id, String nome, String icone) {
        this.id = id;
        this.nome = nome;
        this.icone = icone;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getIcone() {
        return icone;
    }
}
