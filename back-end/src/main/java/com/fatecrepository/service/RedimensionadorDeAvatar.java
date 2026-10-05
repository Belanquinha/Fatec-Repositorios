package com.fatecrepository.service;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Reduz a foto do Microsoft Graph a um avatar antes de ela virar data-URL.
 *
 * <p>Existe por um motivo concreto: a foto chega com a resolução original e o avatar aparece num
 * círculo de ~40px. Devolvê-la crua colocava centenas de KB de base64 dentro do {@code localStorage}
 * do navegador, no caminho de login — escrita síncrona no main thread a cada entrada, contra um
 * armazenamento de ~5MB. Isso é risco de a página travar e de perder o resto da sessão, não só um
 * detalhe de segurança.
 *
 * <p>A política é deliberadamente conservadora e nunca piora o estado anterior. Se a redução não é
 * possível, cai na foto original <em>desde que ela já seja pequena</em>. Foto grande e ilegível é
 * descartada: ficar sem avatar é melhor que estourar a cota de storage.
 *
 * <p>Classe separada de {@link MicrosoftGraphService} porque é uma responsabilidade diferente — a
 * outra fala com o Graph, esta fala com pixels. Separação que também torna a redução testável sem
 * rede.
 */
@Slf4j
public final class RedimensionadorDeAvatar {

    /**
     * Lado maior do avatar depois da redução. 256px cobre exibição em 2x num círculo pequeno com
     * folga larga, e o JPEG resultante fica na casa das dezenas de KB.
     */
    static final int LADO_MAXIMO = 256;

    /**
     * Teto para aceitar a foto original quando não foi possível reduzir. Acima disto, o base64
     * passa de meio megabyte no {@code localStorage}.
     */
    static final int TAMANHO_MAXIMO_SEM_REDUCAO = 64 * 1024;

    private static final String PREFIXO_DATA_URL = "data:image/jpeg;base64,";

    private RedimensionadorDeAvatar() {
    }

    /**
     * @return data-URL JPEG reduzida, a original se já for pequena, ou {@code null} se nenhuma das
     *         duas for aceitável.
     */
    public static String paraDataUrl(byte[] original) {
        if (original == null || original.length == 0) {
            return null;
        }

        byte[] reduzida = tentaReduzir(original);

        if (reduzida != null) {
            return PREFIXO_DATA_URL + Base64.getEncoder().encodeToString(reduzida);
        }

        if (original.length <= TAMANHO_MAXIMO_SEM_REDUCAO) {
            return PREFIXO_DATA_URL + Base64.getEncoder().encodeToString(original);
        }

        log.debug("Foto de {} bytes ignorada: não foi possível reduzir e excede o limite",
                original.length);
        return null;
    }

    private static byte[] tentaReduzir(byte[] original) {
        try {
            BufferedImage fonte = ImageIO.read(new ByteArrayInputStream(original));
            if (fonte == null) {
                return null;
            }

            double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(fonte.getWidth(), fonte.getHeight()));

            int largura = Math.max(1, (int) Math.round(fonte.getWidth() * escala));
            int altura = Math.max(1, (int) Math.round(fonte.getHeight() * escala));

            // TYPE_INT_RGB de propósito: o JPEG não tem canal alfa, e um PNG transparente lido
            // como ARGB seria escrito com fundo preto em vez de transparente.
            BufferedImage destino = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            Graphics2D graficos = destino.createGraphics();
            try {
                graficos.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graficos.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graficos.setColor(Color.WHITE);
                graficos.fillRect(0, 0, largura, altura);
                graficos.drawImage(fonte, 0, 0, largura, altura, null);
            } finally {
                graficos.dispose();
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            if (!ImageIO.write(destino, "jpg", saida)) {
                return null;
            }
            return saida.toByteArray();
        } catch (Exception e) {
            log.debug("Não foi possível reduzir a foto do Graph: {}", e.getMessage());
            return null;
        }
    }
}
