package repositorios.fatec.fatec_repositorios;

public class projeSoftware extends projeto {

    private String descricao;
    private String linkGithub;
    private String arquivoDocumentacao;
    private String arquivoProjeto;
    private String imagemProjeto;
    private String tags;
    private int disponibilidadeGrupo;

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    public String getLinkGithub() {
        return linkGithub;
    }
    public void setLinkGithub(String linkGithub) {
        this.linkGithub = linkGithub;
    }
    public String getArquivoDocumentacao() {
        return arquivoDocumentacao;
    }
    public void setArquivoDocumentacao(String arquivoDocumentacao) {
        this.arquivoDocumentacao = arquivoDocumentacao;
    }
    public String getArquivoProjeto() {
        return arquivoProjeto;
    }
    public void setArquivoProjeto(String arquivoProjeto) {
        this.arquivoProjeto = arquivoProjeto;
    }
    public String getImagemProjeto() {
        return imagemProjeto;
    }
    public void setImagemProjeto(String imagemProjeto) {
        this.imagemProjeto = imagemProjeto;
    }
    public String getTags() {
        return tags;
    }
    public void setTags(String tags) {
        this.tags = tags;
    }
    public int getDisponibilidadeGrupo() {
        return disponibilidadeGrupo;
    }
    public void setDisponibilidadeGrupo(int disponibilidadeGrupo) {
        this.disponibilidadeGrupo = disponibilidadeGrupo;
    }
}