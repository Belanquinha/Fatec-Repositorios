package repositorios.fatec.fatec_repositorios;

import java.util.Date;

public class aluno extends usuario {
    private Date dataNascimento;
    private int statusCurso;

    public aluno(String nome, String email, String fotoPerfil, Date dataNascimento, int statusCurso) {
        super(nome, email, fotoPerfil);
        this.dataNascimento = dataNascimento;
        this.statusCurso = statusCurso;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public int getStatusCurso() {
        return statusCurso;
    }

    public void setStatusCurso(int statusCurso) {
        this.statusCurso = statusCurso;
    }
}
