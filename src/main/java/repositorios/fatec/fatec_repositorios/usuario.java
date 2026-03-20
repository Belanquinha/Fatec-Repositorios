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
}
