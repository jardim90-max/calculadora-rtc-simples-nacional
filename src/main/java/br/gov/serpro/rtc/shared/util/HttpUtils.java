/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.shared.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Utilitário HTTP que monta a URL base da requisição atual para compor links
 * absolutos usados pela API.
 */
public final class HttpUtils {

    private HttpUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * URL base da aplicação na requisição atual: esquema, host, porta (quando
     * não for a padrão) e context path (ex.: {@code http://localhost:8082/api}),
     * para que os links gerados apontem para endpoints que de fato existem.
     */
    public static final String getBaseURL() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

}
