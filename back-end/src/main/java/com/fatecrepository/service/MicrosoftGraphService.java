package com.fatecrepository.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class MicrosoftGraphService {

    private static final String GRAPH_ME_URL = "https://graph.microsoft.com/v1.0/me";
    private static final String GRAPH_PHOTO_URL = "https://graph.microsoft.com/v1.0/me/photo/$value";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MicrosoftGraphService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * O Graph devolve campos que não interessam aqui — {@code @odata.context}, {@code @odata.type},
     * {@code businessPhones}, entre outros. Sem {@code ignoreUnknown = true} o Jackson aborta no
     * primeiro campo excedente e o login devolve 401 mesmo com um token válido.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class GraphUser {
        private String id;
        private String displayName;
        private String mail;
        @JsonProperty("userPrincipalName")
        private String userPrincipalName;
    }

    public GraphUser getUserInfo(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_ME_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                GraphUser user = objectMapper.readValue(response.body(), GraphUser.class);
                log.info("Dados obtidos do Microsoft Graph para: {}", user.getMail());
                return user;
            }

            log.error("Erro ao buscar dados do Microsoft Graph, status: {}", response.statusCode());
            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar dados do Microsoft Graph", e);
            return null;
        }
    }

    /**
     * Foto do Graph, reduzida a avatar pelo {@link RedimensionadorDeAvatar} antes de virar data-URL.
     */
    public String getPhotoUrl(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_PHOTO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200 && response.body().length > 0) {
                return RedimensionadorDeAvatar.paraDataUrl(response.body());
            }
        } catch (Exception e) {
            log.debug("Foto de perfil não encontrada no Microsoft Graph");
        }
        return null;
    }
}
