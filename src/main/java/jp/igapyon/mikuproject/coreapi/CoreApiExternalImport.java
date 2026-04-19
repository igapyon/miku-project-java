/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.Arrays;
import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;

public class CoreApiExternalImport {
    private final CoreApiExternalBinary externalBinary = new CoreApiExternalBinary();
    private final CoreApiExternalDocument externalDocument = new CoreApiExternalDocument();

    public CoreApiImportResult importExternal(ExternalImportInput input) {
        ExternalImportSource source = input.source;
        String mode = safe(input.mode);
        ProjectModel baseModel = input.baseModel;

        if ("ms_project_xml".equals(source.format)) {
            assertImportMode(source.format, mode, Arrays.asList("replace"));
            return externalBinary.importMsProjectXml(source.text, mode);
        }

        if ("xlsx".equals(source.format)) {
            assertImportMode(source.format, mode, Arrays.asList("replace", "merge"));
            if ("merge".equals(mode)) {
                assertBaseModelRequired(source.format, mode, baseModel);
            }
            return externalBinary.importXlsx(source.bytes, mode, baseModel);
        }

        if ("workbook_json".equals(source.format) || "project_draft_view".equals(source.format)
                || "patch_json".equals(source.format)) {
            if ("workbook_json".equals(source.format)) {
                assertImportMode(source.format, mode, Arrays.asList("replace", "merge"));
                if ("merge".equals(mode)) {
                    assertBaseModelRequired(source.format, mode, baseModel);
                }
            }
            if ("project_draft_view".equals(source.format)) {
                assertImportMode(source.format, mode, Arrays.asList("replace"));
            }
            if ("patch_json".equals(source.format)) {
                assertImportMode(source.format, mode, Arrays.asList("patch"));
                assertBaseModelRequired(source.format, mode, baseModel);
            }
            return externalDocument.importDocument(source.format, source.document, mode, baseModel);
        }

        throw new IllegalArgumentException("未対応の import format です: " + safe(source.format));
    }

    private void assertImportMode(String sourceFormat, String mode, List<String> allowedModes) {
        if (allowedModes.contains(mode)) {
            return;
        }
        throw new IllegalArgumentException("importExternal: format=" + sourceFormat + " は "
                + formatAllowedModes(allowedModes) + " のみ対応です (received: mode=" + mode + ")");
    }

    private void assertBaseModelRequired(String sourceFormat, String mode, ProjectModel baseModel) {
        if (baseModel != null) {
            return;
        }
        throw new IllegalArgumentException("importExternal: format=" + sourceFormat + " mode=" + mode + " には baseModel が必要です");
    }

    private String formatAllowedModes(List<String> modes) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < modes.size(); index++) {
            if (index > 0) {
                builder.append(" / ");
            }
            builder.append("mode=").append(modes.get(index));
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class ExternalImportInput {
        public ExternalImportSource source;
        public String mode;
        public ProjectModel baseModel;
    }

    public static class ExternalImportSource {
        public String format;
        public String text;
        public byte[] bytes;
        public Object document;
    }
}
