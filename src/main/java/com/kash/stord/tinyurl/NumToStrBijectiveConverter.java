package com.kash.stord.tinyurl;

/*
 * Source: https://github.com/delight-im/ShortURL
 * Renamed and modified to support long instead of int.
 *
 * Licensed under the MIT License (https://opensource.org/licenses/MIT)
 */

/**
 * NumToStrBijectiveConverter: Bijective conversion between natural numbers (IDs) and short strings
 * <p>
 * NumToStrBijectiveConverter.numToStr() takes an ID and turns it into a short string
 * NumToStrBijectiveConverter.strToNum() takes a short string and turns it into an ID
 * <p>
 * Features:
 * + large alphabet (51 chars) and thus very short resulting strings
 * + proof against offensive words (removed 'a', 'e', 'i', 'o' and 'u')
 * + unambiguous (removed 'I', 'l', '1', 'O' and '0')
 * <p>
 * Example output:
 * 123456789 <=> pgK8p
 */
public class NumToStrBijectiveConverter {

    public static final String ALPHABET = "bcdfghjkmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ23456789";
    public static final int BASE = ALPHABET.length();

    public static String numToStr(long num) {
        StringBuilder str = new StringBuilder();
        while (num > 0) {
            str.insert(0, ALPHABET.charAt((int) num % BASE));
            num = num / BASE;
        }
        return str.toString();
    }

    public static long strToNum(String str) {
        long num = 0;
        for (int i = 0; i < str.length(); i++) {
            num = num * BASE + ALPHABET.indexOf(str.charAt(i));
        }
        return num;
    }

    private NumToStrBijectiveConverter() {
        // Use static methods. No instance needed.
    }
}