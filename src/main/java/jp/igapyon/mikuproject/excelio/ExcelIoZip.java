/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ExcelIoZip {
    public byte[] packZip(List<ZipEntryData> entries) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(buffer);
            for (ZipEntryData entry : entries) {
                ZipEntry zipEntry = new ZipEntry(entry.name);
                zip.putNextEntry(zipEntry);
                zip.write(entry.data);
                zip.closeEntry();
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("zip の生成に失敗しました", ex);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    public Map<String, byte[]> unpackZip(byte[] bytes) {
        Map<String, byte[]> result = new LinkedHashMap<String, byte[]>();
        ZipInputStream input = null;
        try {
            input = new ZipInputStream(new ByteArrayInputStream(bytes));
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = input.getNextEntry()) != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    stream.write(buffer, 0, read);
                }
                result.put(entry.getName(), stream.toByteArray());
                input.closeEntry();
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalArgumentException("zip の展開に失敗しました", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    public List<String> listEntries(byte[] bytes) {
        return new ArrayList<String>(unpackZip(bytes).keySet());
    }

    public static class ZipEntryData {
        public final String name;
        public final byte[] data;

        public ZipEntryData(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }
    }
}
