/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;

public class CoreApiImportResult {
    public String kind;
    public String mode;
    public ProjectModel model;
    public List<Object> changes = new ArrayList<Object>();
    public List<Object> warnings = new ArrayList<Object>();
}
