package repositorios.fatec.fatec_repositorios;

import java.lang.reflect.Array;
import java.util.Date;

public abstract class projeto {
    String nomeProjeto;
    Date dataPostagem;
    String imagemCapa;
    Array linksContato;
    
    public String getNomeProjeto() {
        return nomeProjeto;
    }
    public void setNomeProjeto(String nomeProjeto) {
        this.nomeProjeto = nomeProjeto;
    }
    public Date getDataPostagem() {
        return dataPostagem;
    }
    public void setDataPostagem(Date dataPostagem) {
        this.dataPostagem = dataPostagem;
    }
    public String getImagemCapa() {
        return imagemCapa;
    }
    public void setImagemCapa(String imagemCapa) {
        this.imagemCapa = imagemCapa;
    }
    public Array getLinksContato() {
        return linksContato;
    }
    public void setLinksContato(Array linksContato) {
        this.linksContato = linksContato;
    }
    public Array getEmailsGrupo() {
        return emailsGrupo;
    }
    public void setEmailsGrupo(Array emailsGrupo) {
        this.emailsGrupo = emailsGrupo;
    }
    Array emailsGrupo;
}
