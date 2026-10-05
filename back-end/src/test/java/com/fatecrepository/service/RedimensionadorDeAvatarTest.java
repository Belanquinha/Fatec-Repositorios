package com.fatecrepository.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A redução do avatar é o que impede que a foto do Microsoft Graph, que vem na resolução original,
 * encha o {@code localStorage} do navegador no caminho de login. Como esse caminho só é exercitado
 * com uma conta Microsoft real, esta suíte é a única rede de segurança sobre ele.
 */
@DisplayName("Redução da foto de perfil a avatar")
class RedimensionadorDeAvatarTest {

    @Test
    @DisplayName("Foto grande é reduzida ao lado máximo e devolvida como JPEG")
    void fotoGrandeEReduzida() throws Exception {
        // Ruído para que a compressão JPEG tenha trabalho real: uma cor sólida encolheria para
        // poucos bytes mesmo sem redução, e o teste passaria sem provar nada.
        byte[] grande = pngComRuido(1600, 1200);

        String dataUrl = RedimensionadorDeAvatar.paraDataUrl(grande);

        assertThat(dataUrl).startsWith("data:image/jpeg;base64,");

        BufferedImage resultante = decodificar(dataUrl);
        assertThat(resultante.getWidth()).isEqualTo(RedimensionadorDeAvatar.LADO_MAXIMO);
        assertThat(resultante.getHeight())
                .isEqualTo(RedimensionadorDeAvatar.LADO_MAXIMO * 1200 / 1600);
    }

    @Test
    @DisplayName("A foto reduzida é menor que a original em bytes")
    void reducaoRealmenteReduz() throws Exception {
        byte[] grande = pngComRuido(2000, 2000);

        String dataUrl = RedimensionadorDeAvatar.paraDataUrl(grande);

        int tamanhoFinal = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1)).length;
        assertThat(tamanhoFinal).isLessThan(grande.length);
    }

    @Test
    @DisplayName("Foto já pequena não é ampliada")
    void fotoPequenaNaoEAmpliada() throws Exception {
        byte[] pequena = pngComRuido(64, 48);

        BufferedImage resultante = decodificar(RedimensionadorDeAvatar.paraDataUrl(pequena));

        assertThat(resultante.getWidth()).isEqualTo(64);
        assertThat(resultante.getHeight()).isEqualTo(48);
    }

    @Test
    @DisplayName("PNG com transparência não vira fundo preto ao virar JPEG")
    void transparenciaNaoViraPreto() throws Exception {
        BufferedImage comAlfa = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = comAlfa.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, 300, 300);
        g.dispose();

        byte[] png = png(comAlfa);
        String dataUrl = RedimensionadorDeAvatar.paraDataUrl(png);

        BufferedImage resultante = decodificar(dataUrl);
        // Transparent vira branco porque o destino é preenchido com branco antes de desenhar.
        // O defeito seria vir PRETO, que é o sintoma de ler ARGB e escrever sem Alfa.
        assertThat(resultante.getRGB(10, 10)).isEqualTo(0xFFFFFFFF);
    }

    @Test
    @DisplayName("Bytes que não são imagem e são pequenos preservam a original")
    void originalPequenaNaoDecodificavelEhPreservada() {
        byte[] naoImagem = "conteudo qualquer".getBytes();

        String dataUrl = RedimensionadorDeAvatar.paraDataUrl(naoImagem);

        assertThat(dataUrl).isNotNull();
        assertThat(Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1)))
                .isEqualTo(naoImagem);
    }

    @Test
    @DisplayName("Bytes que não são imagem e são grandes são descartados")
    void originalGrandeNaoDecodificavelEhDescartada() {
        byte[] grandeNaoImagem = new byte[RedimensionadorDeAvatar.TAMANHO_MAXIMO_SEM_REDUCAO + 1];
        java.util.Arrays.fill(grandeNaoImagem, (byte) 7);

        // Avatar perdido é melhor do que meio megabyte de base64 no localStorage.
        assertThat(RedimensionadorDeAvatar.paraDataUrl(grandeNaoImagem)).isNull();
    }

    @Test
    @DisplayName("Entrada vazia ou nula devolve null")
    void entradaVaziaDevolveNulo() {
        assertThat(RedimensionadorDeAvatar.paraDataUrl(new byte[0])).isNull();
        assertThat(RedimensionadorDeAvatar.paraDataUrl(null)).isNull();
    }

    private BufferedImage decodificar(String dataUrl) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private byte[] pngComRuido(int largura, int altura) throws Exception {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            for (int y = 0; y < altura; y++) {
                for (int x = 0; x < largura; x++) {
                    // Padrão determinístico: o teste não pode depender de Random.
                    g.setColor(new Color((x * 31 + y * 17) % 256, (x * 7) % 256, (y * 13) % 256));
                    g.drawLine(x, y, x, y);
                }
            }
        } finally {
            g.dispose();
        }
        return png(imagem);
    }

    private byte[] png(BufferedImage imagem) throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }
}
