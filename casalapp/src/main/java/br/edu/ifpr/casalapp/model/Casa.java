package br.edu.ifpr.casalapp.model;

public class Casa {

    private int id;
    private String nome;
    private String codigoConvite;

    public Casa(int id, String nome, String codigoConvite) {
        this.id = id;
        this.nome = nome;
        this.codigoConvite = codigoConvite;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigoConvite() {
        return codigoConvite;
    }
}
