package repositorios.fatec.fatec_repositorios;

import java.lang.reflect.Array;
import java.util.Date;

public class projetoDeSoftware extends projeto {
    String descricao;
    String linkGitHub;
    String arquivoDocumentacao;
    String arquivoProjeto;
    String imagemProjeto;
    int disponibilidadeGrupo;
    String tags;

    public projetoDeSoftware(String descricao, String linkGitHub,
                             String arquivoDocumentacao, String imagemProjeto,
                             int disponibilidadeGrupo,
                             String nomeProjeto,
                             Date dataPostagem, String imagemCapa,
                             Array linksContato, Array emailsGrupo
        ) {
        super(nomeProjeto, imagemCapa, dataPostagem, linksContato, emailsGrupo);
        this.


    }
}
