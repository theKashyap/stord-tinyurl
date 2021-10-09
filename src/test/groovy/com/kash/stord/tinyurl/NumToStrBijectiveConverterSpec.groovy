package com.kash.stord.tinyurl


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import spock.lang.Specification
import spock.lang.Unroll

class NumToStrBijectiveConverterSpec extends Specification {
    private static final Logger logger = LogManager.getLogger();

    @Unroll
    def "bijective functions should convert num #num to/from str #str bijectively"() {
        given:
        def actualNum = NumToStrBijectiveConverter.strToNum(str)
        def actualStr = NumToStrBijectiveConverter.numToStr(num)

        expect:
        actualNum == num
        actualStr == str

        where:
        num         | str
        1234567890L | "gykQdf"
        123456789L  | "CBx5v"
        12345678L   | "dj64N"
        1234567L    | "pGnr"
        123456L     | "cdBH"
        12345L      | "hj7"
        1234L       | "Hn"
    }
}
