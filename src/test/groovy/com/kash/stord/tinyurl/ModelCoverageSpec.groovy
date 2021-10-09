package com.kash.stord.tinyurl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.orm.jpa.JpaSystemException
import spock.lang.Specification
import spock.lang.Unroll

class ModelCoverageSpec extends Specification {
    private static final Logger logger = LogManager.getLogger();

    @Unroll
    def "UrlMapping.equals() should return #retVal when comparing to #other"() {
        given:
        def thisMapping = new UrlMapping(id: 1, longUrl: "longUrlInDB")

        expect:
        retVal == (thisMapping.equals(other))
        thisMapping.equals(thisMapping)

        where:
        retVal | other
        true   | new UrlMapping(id: 1, longUrl: "longUrlInDB")
        false  | new Object()
        false  | new UrlMapping(id: null, longUrl: "different longUrlInDB")
        false  | new UrlMapping(id: 1, longUrl: "different longUrlInDB")
        false  | new UrlMapping(id: 2, longUrl: "longUrlInDB")
        false  | new UrlMapping(id: 2, longUrl: "different longUrlInDB")
    }

    @Unroll
    def "UrlMappingPojo.toString() should not throw exception when ObjectMapper does"() {
        when:
        def mapper = Mock(ObjectMapper)
        mapper.writeValueAsString(_) >> { throw new JsonMappingException(null) }
        def thisMapping = new UrlMappingPojo(shortUrl: "url1", longUrl: "longUrlInDB", objectMapper: mapper)
        thisMapping.toString()

        then:
        noExceptionThrown()
    }
}
