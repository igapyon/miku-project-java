/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

public class CoreApiReportPublic {
    private final CoreApiReportAdapters adapters = new CoreApiReportAdapters();

    public final CoreApiReportAdapters.ReportApi report = adapters.report;
}
