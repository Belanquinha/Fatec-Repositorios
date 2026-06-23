package com.fatecrepository.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
public class Instituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String cnpj;
    private String email;
    private String telefone;

    @OneToMany(mappedBy = "instituicao")
    private List<Aluno> alunos;

    @OneToMany(mappedBy = "instituicao")
    private List<Professor> professores;

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

    public String getCnpj(){
        return cnpj;
    }
    public void setCnpj(String cnpj){
        this.cnpj = cnpj;
    }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    public String getTelefone(){
        return telefone;
    }
    public void setTelefone(String telefone){
        this.telefone = telefone;
    }

    public List<Aluno> getAlunos(){
        return alunos;
    }
    public void setAlunos(List<Aluno> alunos){
        this.alunos = alunos;
    }

    public List<Professor> getProfessores(){
        return professores;
    }
    public void setProfessores(List<Professor> professores){
        this.professores = professores;
    }
}
