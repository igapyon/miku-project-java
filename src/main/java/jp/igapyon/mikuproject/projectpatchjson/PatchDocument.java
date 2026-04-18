/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.ArrayList;
import java.util.List;

public class PatchDocument {
    public List<PatchOperation> operations = new ArrayList<PatchOperation>();
}
