/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class OutlineCodeModel {
    public String fieldID;
    public String fieldName;
    public String alias;
    public Boolean onlyTableValues;
    public Boolean enterprise;
    public Boolean resourceSubstitutionEnabled;
    public Boolean leafOnly;
    public Boolean allLevelsRequired;
    public List<OutlineCodeMaskModel> masks = new ArrayList<OutlineCodeMaskModel>();
    public List<OutlineCodeValueModel> values = new ArrayList<OutlineCodeValueModel>();
}
