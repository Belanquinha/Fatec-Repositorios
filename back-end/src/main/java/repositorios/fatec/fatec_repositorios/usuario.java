package repositorios.fatec.fatec_repositorios;

public abstract class usuario {
    private String nome;
    private String email;
    private String fotoPerfil;

    public usuario (String nome, String email, String fotoPerfil) {
        this.nome = nome;
        this.email = email;
        this.fotoPerfil = fotoPerfil;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}
