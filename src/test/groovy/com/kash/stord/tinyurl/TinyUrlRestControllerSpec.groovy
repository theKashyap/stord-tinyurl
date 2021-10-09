package com.kash.stord.tinyurl

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.orm.jpa.JpaSystemException
import spock.lang.Specification
import spock.lang.Unroll

class TinyUrlRestControllerSpec extends Specification {
    private static final Logger logger = LogManager.getLogger();

    @Unroll
    def "createTinyurl should succeed for #tcName with valid url #longUrl"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        def hardcodedId = 1234567890L
        def hardcodedShortUrl = NumToStrBijectiveConverter.numToStr(hardcodedId)
        1 * repo.save(_) >> new UrlMapping(id: hardcodedId, longUrl: longUrl)
        def resp = rc.createTinyurl(new UrlMappingPojo().withLongUrl(longUrl), null)

        expect:
        logger.debug(resp)
        resp.getStatusCode() == HttpStatus.OK
        resp.getBody().longUrl == longUrl
        resp.getBody().shortUrl.endsWith(hardcodedShortUrl)

        where:
        tcName             | longUrl
        "top level"        | "https://www.wikipedia.org/"
        "littleTree"       | "https://www.wikipedia.org/wiki/TinyURL"
        "with params"      | "https://duckduckgo.com/?t=ffab&q=stord&ia=web"
        "with URL encoded" | "https://www.google.com/maps/place/817+W.+Peachtree+St.+NW+Suite+200+Atlanta%2C+GA+30308"
    }

    @Unroll
    def "createTinyurl should reject invalid url with #problem"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        def resp = rc.createTinyurl(new UrlMappingPojo().withLongUrl(longUrl), UUID.randomUUID().toString())

        expect:
        logger.debug(resp)
        resp.getStatusCode() == HttpStatus.BAD_REQUEST

        where:
        problem           | longUrl
        "null URL"        | null
        "empty URL"       | ""
        "invalid host"    | "http://.com"
        "invalid host2"   | "http://com."
        "space"           | "http:// "
        "space2"          | "http://www.wikipedia.org/ /wiki"
        "space3"          | "http://www.wikipedia.org/wiki?q=a b"
        "invalid schema"  | "unknown:// "
        "wrong authority" | "ftp://::::@example.com"
    }

    def "createTinyurl should return 500 with error message when DB throws exception"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        1 * repo.save(_) >> { throw new JpaSystemException(null) }
        def resp = rc.createTinyurl(new UrlMappingPojo().withLongUrl("https://www.wikipedia.org"), "")

        expect:
        logger.debug(resp)
        resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        resp.getBody().message != null
    }

    def "resolveTinyurl should return 500 with error message when DB throws exception"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        1 * repo.findById(_) >> { throw new JpaSystemException(null) }
        def resp = rc.resolveTinyurl("hardcodedShortUrl", "")

        expect:
        logger.debug(resp)
        resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        resp.getBody().message != null
    }

    def "resolveTinyurl should return 404 when mapping is not found in DB"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        def hardcodedId = 1234567890L
        def hardcodedShortUrl = NumToStrBijectiveConverter.numToStr(hardcodedId)
        1 * repo.findById(hardcodedId) >> Optional.ofNullable(null)
        def resp = rc.resolveTinyurl(hardcodedShortUrl, UUID.randomUUID().toString())

        expect:
        logger.debug(resp)
        resp.getStatusCode() == HttpStatus.NOT_FOUND
        resp.getBody().longUrl == null
        resp.getBody().message != null
    }

    def "resolveTinyurl should succeed when mapping exists in db"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        def longUrlInDB = "https://www.wikipedia.org/"
        1 * repo.findById(id) >> Optional.ofNullable(new UrlMapping(id: id, longUrl: longUrlInDB))
        def resp = rc.resolveTinyurl(inputShortUrl, UUID.randomUUID().toString())

        expect:
        logger.debug(resp)
        resp.getStatusCode() == HttpStatus.OK
        resp.getBody().longUrl == longUrlInDB

        where:
        id          | inputShortUrl | expectedHttpStatus
        1234567890L | "gykQdf"      | HttpStatus.OK
        123456789L  | "CBx5v"       | HttpStatus.OK
        12345678L   | "dj64N"       | HttpStatus.OK
        1234567L    | "pGnr"        | HttpStatus.OK
        123456L     | "cdBH"        | HttpStatus.OK
        12345L      | "hj7"         | HttpStatus.OK
        1234L       | "Hn"          | HttpStatus.OK
    }

    @Unroll
    def "resolveAndRedirect should redirect when mapping exists #id"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        def longUrlInDB = "https://www.wikipedia.org/"
        1 * repo.findById(id) >> Optional.ofNullable(new UrlMapping(id: id, longUrl: longUrlInDB))
        def myCorrelationId = UUID.randomUUID().toString()
        def resp = rc.resolveAndRedirect(inputShortUrl, myCorrelationId)

        expect:
        logger.debug(resp)
        resp.statusCode == HttpStatus.MOVED_PERMANENTLY
        resp.getHeaders().get("Location") == [longUrlInDB]
        resp.getHeaders().get(TinyUrlRestController.X_CORRELATION_ID) == [myCorrelationId]

        where:
        id          | inputShortUrl | expectedHttpStatus
        1234567890L | "gykQdf"      | HttpStatus.OK
        12345L      | "hj7"         | HttpStatus.OK
        1234L       | "Hn"          | HttpStatus.OK
    }

    def "resolveTinyurl should return 404 when mapping is missing"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        def hardcodedId = 1234567890L
        def hardcodedShortUrl = NumToStrBijectiveConverter.numToStr(hardcodedId)
        1 * repo.findById(hardcodedId) >> Optional.ofNullable(null)
        def resp = rc.resolveAndRedirect(hardcodedShortUrl, null)

        expect:
        logger.debug(resp)
        resp.statusCode == HttpStatus.NOT_FOUND
        resp.getHeaders().get(TinyUrlRestController.X_CORRELATION_ID) != null
    }

    def "resolveAndRedirect should return 500 with correlation id when DB throws exception"() {
        given:
        def repo = Mock(TinyUrlRepository)
        def rc = new TinyUrlRestController(repo)
        1 * repo.findById(_) >> { throw new JpaSystemException(null) }
        def resp = rc.resolveAndRedirect("ignored", "")

        expect:
        logger.debug(resp)
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        resp.getHeaders().get(TinyUrlRestController.X_CORRELATION_ID) != null
    }

    def "index/root should not throw any exceptions"() {
        when:
        def resp = new TinyUrlRestController(Mock(TinyUrlRepository)).index()
        logger.debug(resp)

        then:
        // Not much we can verify here. Front end should be seperated and tested using appropriate tech.
        noExceptionThrown()
    }
}
