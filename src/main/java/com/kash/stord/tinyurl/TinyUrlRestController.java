package com.kash.stord.tinyurl;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import javassist.NotFoundException;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.metrics.annotation.Timed;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for incoming REST API calls for resource "/tinyUrl" and
 * frontend at "/"
 */
@RestController
@Timed
public class TinyUrlRestController {
    private static final Logger logger = LogManager.getLogger();
    public static final String X_CORRELATION_ID = "X-CORRELATION-ID";
    public static final String CORRELATION_ID = "correlation-id";
    private static final String INTERNAL_SERVER_ERROR_WITH_CORRELATION_ID =
        "An unexpected error occurred. If problem persists, please contact support with correlation id: ";

    private final TinyUrlRepository repository;

    @Autowired
    public TinyUrlRestController(TinyUrlRepository repo) {
        this.repository = repo;
    }

    /**
     * Create a new short URL from a long URL.<br/>
     * - Creates a new entry/mapping in DB (id <-> long URL).<br/>
     * - Converts the id (number) of new mapping to alpha-numeric short URL (string) using a bijective function.<br/>
     * - Returns the short URL.<br/>
     *
     * @param body              JSON deserialized by Spring. Only longUrl is required.
     * @param userCorrelationId optional correlation id in headers for this transaction, if not provided
     *                          a new one would be created & used. Always returned in headers.
     * @return A new mapping if successful, 400 for bad input. 500 with error message  otherwise.
     */
    @CrossOrigin
    @PostMapping(path = "/tinyurl")
    public ResponseEntity<UrlMappingPojo> createTinyurl(@RequestBody UrlMappingPojo body,
                                                        @RequestHeader(value = X_CORRELATION_ID, required = false)
                                                            String userCorrelationId) {
        // FIXME: if user provided a correlation id in header (X-Correlation-Id: <some unique id>) then use that
        //        else generate one of our own. This is used in logging.pattern (%X{correlation-id}) in
        //        application.properties, so it gets embedded in every log message. Required to find right
        //        logs in a multi-threaded cloud environment.
        String correlationId = Strings.isNotEmpty(userCorrelationId) ? userCorrelationId : UUID.randomUUID().toString();
        ThreadContext.put(CORRELATION_ID, correlationId);

        // FIXME: In a boundary function like this, always provide a log at entry and all exits with as many
        //        variables/params as possible.
        if (logger.isDebugEnabled()) {
            // FIXME: If traffic is high, logging every call at info level can cost money for log/event processing.
            //        Put a debug level log so we can get it if needed without deploying code with new logs.
            logger.debug("body: {}, correlationId: {}", body, correlationId);
        }

        try {
            String longUrl = body.longUrl;
            if (!UrlValidator.getInstance().isValid(longUrl)) {
                // FIXME: Would be nice to specify what's wrong with URL and not just say it's bad URL.
                //        But there is not standard validator and Apache one only returns bool.
                String errMsg = String.format("Supplied longUrl (%s) is not a valid URL according to Apache Commons" +
                    " UrlValidator. Ensure it has valid Scheme, Authority, Path, Query, Fragment.", longUrl);
                logger.warn(errMsg);
                return ResponseEntity.badRequest().header(X_CORRELATION_ID, correlationId)
                    .body(body.withMessage(errMsg).witHttpStatusCode(HttpStatus.BAD_REQUEST));
            }

            UrlMapping newMapping = repository.save(new UrlMapping(longUrl));
            String shortUrl = NumToStrBijectiveConverter.numToStr(newMapping.getId());

            logger.info("returning newMapping.getId(): {}, shortUrl: {}", newMapping.getId(), shortUrl);
            return ResponseEntity.ok().header(X_CORRELATION_ID, correlationId)
                .body(new UrlMappingPojo().withShortUrl(shortUrl).withLongUrl(longUrl));

        } catch (Exception e) {
            // FIXME: In a boundary function provide a catch all, so user never receives HTTP 500 with useless
            //        message "Internal server error".
            logger.error("Unexpected exception handling body: '{}'", body, e);
            // FIXME: If a correlation id is part of every bug report, it makes it easier to debug.
            return ResponseEntity.internalServerError().header(X_CORRELATION_ID, correlationId).body(body.withMessage(
                INTERNAL_SERVER_ERROR_WITH_CORRELATION_ID +
                    correlationId).witHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Resolves a short URL created earlier to corresponding long URL. Does NOT redirect.
     * See {@link #resolveAndRedirect(String, String)} for more.
     * Converts the alpha-numeric short URL (string) to id (number) using a bijective function.
     * Lookup mapping for this id in DB (id <-> long URL).
     * Returns the long URL.
     *
     * @param shortUrl          a path variable. Short URL to be resolved, must've been created using POST earlier.
     * @param userCorrelationId optional header, used for traceability
     * @return An existing mapping corresponding to the shortUrl, if it exists. 404 otherwise.
     */
    @CrossOrigin
    @GetMapping(path = "/tinyurl/{shortUrl}")
    public ResponseEntity<UrlMappingPojo> resolveTinyurl(@PathVariable String shortUrl,
                                                         @RequestHeader(value = X_CORRELATION_ID, required = false)
                                                             String userCorrelationId) {
        String correlationId = Strings.isNotEmpty(userCorrelationId) ? userCorrelationId : UUID.randomUUID().toString();
        ThreadContext.put(CORRELATION_ID, correlationId);
        logger.info("shortUrl: {}, correlationId: {}", shortUrl, correlationId);
        try {
            String longUrl = resolveToLongUrl(shortUrl);
            return ResponseEntity.ok().header(X_CORRELATION_ID, correlationId)
                .body(new UrlMappingPojo().withLongUrl(longUrl).withShortUrl(shortUrl));

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).header(X_CORRELATION_ID, correlationId).body(
                new UrlMappingPojo().withShortUrl(shortUrl).withMessage(e.getMessage())
                    .witHttpStatusCode(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            logger.error("Unexpected exception handling shortUrl: '{}'", shortUrl, e);
            return ResponseEntity.internalServerError().header(X_CORRELATION_ID, correlationId).body(
                new UrlMappingPojo().withMessage(
                    INTERNAL_SERVER_ERROR_WITH_CORRELATION_ID +
                        correlationId).withShortUrl(shortUrl).witHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Resolves given short URL and responds with 301 to redirect caller to resolved/long URL.
     *
     * @param shortUrl          short URL to resolve from path variables
     * @param userCorrelationId optional correlation id in headers for this transaction
     * @return 301 if resolved, 404 if not found. 500 with error message  otherwise.
     */
    @GetMapping(value = "/{shortUrl}")
    public ResponseEntity<String> resolveAndRedirect(@PathVariable String shortUrl,
                                                     @RequestHeader(value = X_CORRELATION_ID, required = false)
                                                         String userCorrelationId) {
        String correlationId = Strings.isNotEmpty(userCorrelationId) ? userCorrelationId : UUID.randomUUID().toString();
        ThreadContext.put(CORRELATION_ID, correlationId);
        try {
            logger.info("redirectTinyurl() shortUrl: {}", shortUrl);
            String longUrl = resolveToLongUrl(shortUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(longUrl));
            headers.add(X_CORRELATION_ID, correlationId);
            logger.info("redirecting to: resolvedUrl: {}", longUrl);
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).headers(headers).build();

        } catch (NotFoundException e) {
            logger.error("No mapping found for shortUrl: '{}'", shortUrl, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).header(X_CORRELATION_ID, correlationId)
                .body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected exception handling shortUrl: '{}'", shortUrl, e);
            return ResponseEntity.internalServerError().header(X_CORRELATION_ID, correlationId).body(
                INTERNAL_SERVER_ERROR_WITH_CORRELATION_ID +
                    correlationId);
        }
    }

    private String resolveToLongUrl(String shortUrl) throws NotFoundException {
        long id = NumToStrBijectiveConverter.strToNum(shortUrl);
        UrlMapping resolvedUrlMapping = repository.findById(id).orElse(null);
        if (null == resolvedUrlMapping) {
            // FIXME: We could distinguish between a key that's not found and one that's not valid (id is -ve),
            //        but letting end user know should be considered helping them if they're trying to exploit.
            logger.warn("no mapping found for shortUrl: {}, id: {} in DB.", shortUrl, id);
            String errMsg = String.format("<br/>No mapping found for shortUrl: %s. Did you <a target=\"_blank\" " +
                "href=\"http://localhost:8080/\" rel=\"noopener noreferrer\" " +
                "onmouseover=\"window.status='http://localhost:8080/';\" onmouseout=\"window.status='';\">" +
                "create a mapping?</a><br/>", shortUrl);

            throw new NotFoundException(errMsg);
        }

        logger.info("resolved '{}' to: resolvedUrlMapping: '{}'", shortUrl, resolvedUrlMapping);

        return resolvedUrlMapping.getLongUrl();
    }

    // ------------- front-end -------------

    public byte[] getFileContents(String fileName) throws IOException {
        // FIXME: Due to our hack of serving both front and back end from same micro-service, need to
        //        ensure that browser does not request anything that matches the pattern '/{shortUrl}'
        //        and call resolveAndRedirect() instead. E.g. /my.css
        //        We could've used some framework like ThemeLeaf or something, but it does the same
        //        thing (serve file contents) under the covers.
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("static/index/" + fileName);
        return StreamUtils.copyToByteArray(in);
    }

    @CrossOrigin
    @GetMapping(value = "/")
    public String root() throws IOException {
        return new String(getFileContents("index.html"));
    }

    @CrossOrigin
    @GetMapping(value = "/index.html")
    public String index() throws IOException {
        return root();
    }

    @CrossOrigin
    @GetMapping(value = "/favicon.ico", produces = "image/x-icon")
    public byte[] favicon() throws IOException {
        byte[] ret = getFileContents("favicon.ico");
        logger.debug("return ret.length: {}", ret.length);
        return ret;
    }

}
