package com.kash.stord.tinyurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

/**
 * It is recommended to NOT use same entity classes for JPA and REST interface.
 * As JPA entities are modified by Spring/JPA outside our control.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class UrlMappingPojo {
    private static final Logger logger = LogManager.getLogger();
    private static ObjectMapper objectMapper = new ObjectMapper();

    UrlMappingPojo() {
        /* Needed for de/serialization as it's used in REST interface. */
    }

    @JsonProperty("longUrl")
    String longUrl;
    @JsonProperty("shortUrl")
    String shortUrl;
    @JsonProperty("message")
    String message;
    @JsonProperty("httpStatusCode")
    HttpStatus httpStatusCode;

    UrlMappingPojo withLongUrl(String newLongUrl) {
        this.longUrl = newLongUrl;
        return this;
    }

    UrlMappingPojo withShortUrl(String newShortUrl) {
        this.shortUrl = newShortUrl;
        return this;
    }

    UrlMappingPojo withMessage(String newMessage) {
        this.message = newMessage;
        return this;
    }

    UrlMappingPojo witHttpStatusCode(HttpStatus newHttpStatusCode) {
        this.httpStatusCode = newHttpStatusCode;
        return this;
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error("Unexpected error trying to serialize UrlMappingPojo.", e);
            return longUrl + " -- " + e;
        }
    }
}
