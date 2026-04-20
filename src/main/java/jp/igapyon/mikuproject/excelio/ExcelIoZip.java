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
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExcelIoZip {
    public static final int ZERO_MOD_TIME = 0;
    public static final int ZERO_MOD_DATE = 0;
    public static final int FIXED_2025_01_01_MOD_TIME = 0;
    public static final int FIXED_2025_01_01_MOD_DATE = ((2025 - 1980) << 9) | (1 << 5) | 1;

    public byte[] packZip(List<ZipEntryData> entries) {
        return packZip(entries, ZERO_MOD_TIME, ZERO_MOD_DATE);
    }

    public byte[] packZip(List<ZipEntryData> entries, int fixedModTime, int fixedModDate) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            List<byte[]> centralParts = new ArrayList<byte[]>();
            int offset = 0;
            for (ZipEntryData entry : entries) {
                byte[] nameBytes = ExcelIoUtil.encodeUtf8(entry.name);
                long crc32 = computeCrc32(entry.data);
                byte[] localHeader = buildLocalHeader(nameBytes, entry.data.length, crc32, fixedModTime, fixedModDate);
                byte[] centralHeader = buildCentralHeader(nameBytes, entry.data.length, crc32, fixedModTime, fixedModDate, offset);
                buffer.write(localHeader);
                buffer.write(entry.data);
                centralParts.add(centralHeader);
                offset += localHeader.length + entry.data.length;
            }
            int centralDirectoryOffset = offset;
            int centralDirectorySize = 0;
            for (byte[] centralPart : centralParts) {
                buffer.write(centralPart);
                centralDirectorySize += centralPart.length;
            }
            buffer.write(buildEndOfCentralDirectory(centralParts.size(), centralDirectorySize, centralDirectoryOffset));
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("zip の生成に失敗しました", ex);
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

    private byte[] buildLocalHeader(byte[] nameBytes, int dataLength, long crc32, int fixedModTime, int fixedModDate) {
        byte[] header = new byte[30 + nameBytes.length];
        putIntLE(header, 0, 0x04034b50L);
        putShortLE(header, 4, 20);
        putShortLE(header, 6, 0);
        putShortLE(header, 8, 0);
        putShortLE(header, 10, fixedModTime);
        putShortLE(header, 12, fixedModDate);
        putIntLE(header, 14, crc32);
        putIntLE(header, 18, dataLength);
        putIntLE(header, 22, dataLength);
        putShortLE(header, 26, nameBytes.length);
        putShortLE(header, 28, 0);
        System.arraycopy(nameBytes, 0, header, 30, nameBytes.length);
        return header;
    }

    private byte[] buildCentralHeader(byte[] nameBytes, int dataLength, long crc32, int fixedModTime, int fixedModDate,
            int offset) {
        byte[] header = new byte[46 + nameBytes.length];
        putIntLE(header, 0, 0x02014b50L);
        putShortLE(header, 4, 20);
        putShortLE(header, 6, 20);
        putShortLE(header, 8, 0);
        putShortLE(header, 10, 0);
        putShortLE(header, 12, fixedModTime);
        putShortLE(header, 14, fixedModDate);
        putIntLE(header, 16, crc32);
        putIntLE(header, 20, dataLength);
        putIntLE(header, 24, dataLength);
        putShortLE(header, 28, nameBytes.length);
        putShortLE(header, 30, 0);
        putShortLE(header, 32, 0);
        putShortLE(header, 34, 0);
        putShortLE(header, 36, 0);
        putIntLE(header, 38, 0);
        putIntLE(header, 42, offset);
        System.arraycopy(nameBytes, 0, header, 46, nameBytes.length);
        return header;
    }

    private byte[] buildEndOfCentralDirectory(int entryCount, int centralDirectorySize, int centralDirectoryOffset) {
        byte[] header = new byte[22];
        putIntLE(header, 0, 0x06054b50L);
        putShortLE(header, 4, 0);
        putShortLE(header, 6, 0);
        putShortLE(header, 8, entryCount);
        putShortLE(header, 10, entryCount);
        putIntLE(header, 12, centralDirectorySize);
        putIntLE(header, 16, centralDirectoryOffset);
        putShortLE(header, 20, 0);
        return header;
    }

    private long computeCrc32(byte[] bytes) {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return crc32.getValue();
    }

    private void putShortLE(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xff);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xff);
    }

    private void putIntLE(byte[] bytes, int offset, long value) {
        bytes[offset] = (byte) (value & 0xff);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xff);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xff);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xff);
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
