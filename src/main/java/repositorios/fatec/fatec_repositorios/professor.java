package repositorios.fatec.fatec_repositorios;

import java.util.Date;

public class professor extends usuario{

    private int verificado;

    public professor(String nome, String email, String fotoPerfil, int verificado) {
        super(nome, email, fotoPerfil);
        this.verificado = verificado;
    }
}
