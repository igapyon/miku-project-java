/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ExcelIoUtil {
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    private ExcelIoUtil() {
    }

    public static byte[] encodeUtf8(String value) {
        return value.getBytes(UTF_8);
    }

    public static String decodeUtf8(byte[] bytes) {
        return new String(bytes, UTF_8);
    }

    public static byte[] concatBytes(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
