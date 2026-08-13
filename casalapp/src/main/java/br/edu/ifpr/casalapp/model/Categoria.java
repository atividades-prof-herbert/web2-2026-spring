package br.edu.ifpr.casalapp.model;

public class Categoria {

    private int id;
    private String nome;
    private String icone;
    private int casaId;

    public Categoria(int id, String nome, String icone, int casaId) {
        this.id = id;
        this.nome = nome;
        this.icone = icone;
        this.casaId = casaId;
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

    public int getCasaId() {
        return casaId;
    }
}
