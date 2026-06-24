package com.fatecrepository.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String email;

    @ManyToOne
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    private String telefone;
    private String areaEnsino;

    public UUID getId(){
        return id;
    }
    public void setId(UUID id){
        this.id = id;
    }

    public String getNome(){
        return nome;
    }
    public void setNome(String nome){
        this.nome = nome;
    }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    public Instituicao getInstituicao(){
        return instituicao;
    }
    public void setInstituicao(Instituicao instituicao){
        this.instituicao = instituicao;
    }

    public String getTelefone(){
        return telefone;
    }
    public void setTelefone(String telefone){
        this.telefone = telefone;
    }

    public String getAreaEnsino(){
        return areaEnsino;
    }
    public void setAreaEnsino(String areaEnsino){
        this.areaEnsino = areaEnsino;
    }
}
