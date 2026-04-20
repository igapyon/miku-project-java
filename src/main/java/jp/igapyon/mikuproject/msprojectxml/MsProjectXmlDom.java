/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.AssignmentBaselineModel;
import jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel;
import jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel;
import jp.igapyon.mikuproject.model.OutlineCodeMaskModel;
import jp.igapyon.mikuproject.model.OutlineCodeModel;
import jp.igapyon.mikuproject.model.OutlineCodeValueModel;
import jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel;
import jp.igapyon.mikuproject.model.ResourceBaselineModel;
import jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel;
import jp.igapyon.mikuproject.model.ResourceTimephasedDataModel;
import jp.igapyon.mikuproject.model.TaskBaselineModel;
import jp.igapyon.mikuproject.model.TaskExtendedAttributeModel;
import jp.igapyon.mikuproject.model.TaskTimephasedDataModel;
import jp.igapyon.mikuproject.model.WeekDayModel;
import jp.igapyon.mikuproject.model.WbsMaskModel;
import jp.igapyon.mikuproject.model.WorkWeekModel;
import jp.igapyon.mikuproject.model.WorkingTimeModel;

public class MsProjectXmlDom {
    public String textContent(Element parent, String tagName) {
        if (parent == null) {
            return "";
        }
        NodeList elements = parent.getElementsByTagName(tagName);
        if (elements == null || elements.getLength() == 0 || elements.item(0) == null) {
            return "";
        }
        String text = elements.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    public boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public int parseNumber(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public Element firstChildElement(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            if (tagName.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    public NodeList childElements(Element parent, String tagName) {
        Element child = firstChildElement(parent, tagName);
        if (child == null) {
            return emptyNodeList();
        }
        return child.getChildNodes();
    }

    public java.util.List<WeekDayModel> parseWeekDays(Element parent) {
        java.util.List<WeekDayModel> result = new java.util.ArrayList<WeekDayModel>();
        Element weekDaysElement = firstChildElement(parent, "WeekDays");
        if (weekDaysElement == null) {
            return result;
        }
        NodeList nodes = weekDaysElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"WeekDay".equals(((Element) node).getTagName())) {
                continue;
            }
            Element weekDayElement = (Element) node;
            WeekDayModel weekDay = new WeekDayModel();
            String dayType = textContent(weekDayElement, "DayType");
            if (!dayType.isEmpty()) {
                weekDay.dayType = Integer.valueOf(parseNumber(dayType, 0));
            }
            String dayWorking = textContent(weekDayElement, "DayWorking");
            if (!dayWorking.isEmpty()) {
                weekDay.dayWorking = parseBoolean(dayWorking);
            }
            weekDay.workingTimes.addAll(parseWorkingTimes(weekDayElement));
            result.add(weekDay);
        }
        return result;
    }

    public java.util.List<WorkingTimeModel> parseWorkingTimes(Element parent) {
        java.util.List<WorkingTimeModel> result = new java.util.ArrayList<WorkingTimeModel>();
        Element workingTimesElement = firstChildElement(parent, "WorkingTimes");
        if (workingTimesElement == null) {
            return result;
        }
        NodeList nodes = workingTimesElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"WorkingTime".equals(((Element) node).getTagName())) {
                continue;
            }
            Element workingTimeElement = (Element) node;
            WorkingTimeModel workingTime = new WorkingTimeModel();
            workingTime.fromTime = textContent(workingTimeElement, "FromTime");
            workingTime.toTime = textContent(workingTimeElement, "ToTime");
            result.add(workingTime);
        }
        return result;
    }

    public java.util.List<CalendarExceptionModel> parseCalendarExceptions(Element parent) {
        java.util.List<CalendarExceptionModel> result = new java.util.ArrayList<CalendarExceptionModel>();
        Element exceptionsElement = firstChildElement(parent, "Exceptions");
        if (exceptionsElement == null) {
            return result;
        }
        NodeList nodes = exceptionsElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Exception".equals(((Element) node).getTagName())) {
                continue;
            }
            Element exceptionElement = (Element) node;
            CalendarExceptionModel exception = new CalendarExceptionModel();
            exception.name = textContent(exceptionElement, "Name");
            exception.fromDate = textContent(exceptionElement, "FromDate");
            exception.toDate = textContent(exceptionElement, "ToDate");
            String dayWorking = textContent(exceptionElement, "DayWorking");
            if (!dayWorking.isEmpty()) {
                exception.dayWorking = Boolean.valueOf(parseBoolean(dayWorking));
            }
            exception.workingTimes.addAll(parseWorkingTimes(exceptionElement));
            result.add(exception);
        }
        return result;
    }

    public java.util.List<WorkWeekModel> parseWorkWeeks(Element parent) {
        java.util.List<WorkWeekModel> result = new java.util.ArrayList<WorkWeekModel>();
        Element workWeeksElement = firstChildElement(parent, "WorkWeeks");
        if (workWeeksElement == null) {
            return result;
        }
        NodeList nodes = workWeeksElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"WorkWeek".equals(((Element) node).getTagName())) {
                continue;
            }
            Element workWeekElement = (Element) node;
            WorkWeekModel workWeek = new WorkWeekModel();
            workWeek.name = textContent(workWeekElement, "Name");
            workWeek.fromDate = textContent(workWeekElement, "FromDate");
            workWeek.toDate = textContent(workWeekElement, "ToDate");
            workWeek.weekDays.addAll(parseWeekDays(workWeekElement));
            result.add(workWeek);
        }
        return result;
    }

    public java.util.List<OutlineCodeMaskModel> parseOutlineCodeMasks(Element parent) {
        java.util.List<OutlineCodeMaskModel> result = new java.util.ArrayList<OutlineCodeMaskModel>();
        Element masksElement = firstChildElement(parent, "Masks");
        if (masksElement == null) {
            return result;
        }
        NodeList nodes = masksElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Mask".equals(((Element) node).getTagName())) {
                continue;
            }
            Element maskElement = (Element) node;
            OutlineCodeMaskModel mask = new OutlineCodeMaskModel();
            String level = textContent(maskElement, "Level");
            if (!level.isEmpty()) {
                mask.level = Integer.valueOf(parseNumber(level, 0));
            }
            String length = textContent(maskElement, "Length");
            if (!length.isEmpty()) {
                mask.length = Integer.valueOf(parseNumber(length, 0));
            }
            String sequence = textContent(maskElement, "Sequence");
            if (!sequence.isEmpty()) {
                mask.sequence = Integer.valueOf(parseNumber(sequence, 0));
            }
            mask.mask = textContent(maskElement, "Mask");
            result.add(mask);
        }
        return result;
    }

    public java.util.List<OutlineCodeValueModel> parseOutlineCodeValues(Element parent) {
        java.util.List<OutlineCodeValueModel> result = new java.util.ArrayList<OutlineCodeValueModel>();
        Element valuesElement = firstChildElement(parent, "Values");
        if (valuesElement == null) {
            return result;
        }
        NodeList nodes = valuesElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Value".equals(((Element) node).getTagName())) {
                continue;
            }
            Element valueElement = (Element) node;
            OutlineCodeValueModel value = new OutlineCodeValueModel();
            value.value = textContent(valueElement, "Value");
            value.description = textContent(valueElement, "Description");
            result.add(value);
        }
        return result;
    }

    public java.util.List<OutlineCodeModel> parseOutlineCodes(Element parent) {
        java.util.List<OutlineCodeModel> result = new java.util.ArrayList<OutlineCodeModel>();
        Element outlineCodesElement = firstChildElement(parent, "OutlineCodes");
        if (outlineCodesElement == null) {
            return result;
        }
        NodeList nodes = outlineCodesElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"OutlineCode".equals(((Element) node).getTagName())) {
                continue;
            }
            Element outlineCodeElement = (Element) node;
            OutlineCodeModel outlineCode = new OutlineCodeModel();
            outlineCode.fieldID = textContent(outlineCodeElement, "FieldID");
            outlineCode.fieldName = textContent(outlineCodeElement, "FieldName");
            outlineCode.alias = textContent(outlineCodeElement, "Alias");
            String onlyTableValues = textContent(outlineCodeElement, "OnlyTableValues");
            if (!onlyTableValues.isEmpty()) {
                outlineCode.onlyTableValues = Boolean.valueOf(parseBoolean(onlyTableValues));
            }
            String enterprise = textContent(outlineCodeElement, "Enterprise");
            if (!enterprise.isEmpty()) {
                outlineCode.enterprise = Boolean.valueOf(parseBoolean(enterprise));
            }
            String resourceSubstitutionEnabled = textContent(outlineCodeElement, "ResourceSubstitutionEnabled");
            if (!resourceSubstitutionEnabled.isEmpty()) {
                outlineCode.resourceSubstitutionEnabled = Boolean.valueOf(parseBoolean(resourceSubstitutionEnabled));
            }
            String leafOnly = textContent(outlineCodeElement, "LeafOnly");
            if (!leafOnly.isEmpty()) {
                outlineCode.leafOnly = Boolean.valueOf(parseBoolean(leafOnly));
            }
            String allLevelsRequired = textContent(outlineCodeElement, "AllLevelsRequired");
            if (!allLevelsRequired.isEmpty()) {
                outlineCode.allLevelsRequired = Boolean.valueOf(parseBoolean(allLevelsRequired));
            }
            outlineCode.masks.addAll(parseOutlineCodeMasks(outlineCodeElement));
            outlineCode.values.addAll(parseOutlineCodeValues(outlineCodeElement));
            result.add(outlineCode);
        }
        return result;
    }

    public java.util.List<WbsMaskModel> parseWbsMasks(Element parent) {
        java.util.List<WbsMaskModel> result = new java.util.ArrayList<WbsMaskModel>();
        Element wbsMasksElement = firstChildElement(parent, "WBSMasks");
        if (wbsMasksElement == null) {
            return result;
        }
        NodeList nodes = wbsMasksElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"WBSMask".equals(((Element) node).getTagName())) {
                continue;
            }
            Element wbsMaskElement = (Element) node;
            WbsMaskModel wbsMask = new WbsMaskModel();
            String level = textContent(wbsMaskElement, "Level");
            if (!level.isEmpty()) {
                wbsMask.level = Integer.valueOf(parseNumber(level, 0));
            }
            String length = textContent(wbsMaskElement, "Length");
            if (!length.isEmpty()) {
                wbsMask.length = Integer.valueOf(parseNumber(length, 0));
            }
            String sequence = textContent(wbsMaskElement, "Sequence");
            if (!sequence.isEmpty()) {
                wbsMask.sequence = Integer.valueOf(parseNumber(sequence, 0));
            }
            wbsMask.mask = textContent(wbsMaskElement, "Mask");
            result.add(wbsMask);
        }
        return result;
    }

    public java.util.List<ProjectExtendedAttributeModel> parseProjectExtendedAttributes(Element parent) {
        java.util.List<ProjectExtendedAttributeModel> result = new java.util.ArrayList<ProjectExtendedAttributeModel>();
        Element extendedAttributesElement = firstChildElement(parent, "ExtendedAttributes");
        if (extendedAttributesElement == null) {
            return result;
        }
        NodeList nodes = extendedAttributesElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                continue;
            }
            Element attributeElement = (Element) node;
            ProjectExtendedAttributeModel attribute = new ProjectExtendedAttributeModel();
            attribute.fieldID = textContent(attributeElement, "FieldID");
            attribute.fieldName = textContent(attributeElement, "FieldName");
            attribute.alias = textContent(attributeElement, "Alias");
            String calculationType = textContent(attributeElement, "CalculationType");
            if (!calculationType.isEmpty()) {
                attribute.calculationType = Integer.valueOf(parseNumber(calculationType, 0));
            }
            String restrictValues = textContent(attributeElement, "RestrictValues");
            if (!restrictValues.isEmpty()) {
                attribute.restrictValues = Boolean.valueOf(parseBoolean(restrictValues));
            }
            String appendNewValues = textContent(attributeElement, "AppendNewValues");
            if (!appendNewValues.isEmpty()) {
                attribute.appendNewValues = Boolean.valueOf(parseBoolean(appendNewValues));
            }
            result.add(attribute);
        }
        return result;
    }

    public java.util.List<TaskExtendedAttributeModel> parseTaskExtendedAttributes(Element parent) {
        java.util.List<TaskExtendedAttributeModel> result = new java.util.ArrayList<TaskExtendedAttributeModel>();
        Element extendedAttributesElement = firstChildElement(parent, "ExtendedAttributes");
        if (extendedAttributesElement != null) {
            NodeList nodes = extendedAttributesElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element attributeElement = (Element) node;
                TaskExtendedAttributeModel attribute = new TaskExtendedAttributeModel();
                attribute.fieldID = textContent(attributeElement, "FieldID");
                attribute.value = textContent(attributeElement, "Value");
                result.add(attribute);
            }
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                continue;
            }
            Element attributeElement = (Element) node;
            TaskExtendedAttributeModel attribute = new TaskExtendedAttributeModel();
            attribute.fieldID = textContent(attributeElement, "FieldID");
            attribute.value = textContent(attributeElement, "Value");
            result.add(attribute);
        }
        return result;
    }

    public java.util.List<ResourceExtendedAttributeModel> parseResourceExtendedAttributes(Element parent) {
        java.util.List<ResourceExtendedAttributeModel> result = new java.util.ArrayList<ResourceExtendedAttributeModel>();
        Element extendedAttributesElement = firstChildElement(parent, "ExtendedAttributes");
        if (extendedAttributesElement != null) {
            NodeList nodes = extendedAttributesElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element attributeElement = (Element) node;
                ResourceExtendedAttributeModel attribute = new ResourceExtendedAttributeModel();
                attribute.fieldID = textContent(attributeElement, "FieldID");
                attribute.value = textContent(attributeElement, "Value");
                result.add(attribute);
            }
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                continue;
            }
            Element attributeElement = (Element) node;
            ResourceExtendedAttributeModel attribute = new ResourceExtendedAttributeModel();
            attribute.fieldID = textContent(attributeElement, "FieldID");
            attribute.value = textContent(attributeElement, "Value");
            result.add(attribute);
        }
        return result;
    }

    public java.util.List<AssignmentExtendedAttributeModel> parseAssignmentExtendedAttributes(Element parent) {
        java.util.List<AssignmentExtendedAttributeModel> result = new java.util.ArrayList<AssignmentExtendedAttributeModel>();
        Element extendedAttributesElement = firstChildElement(parent, "ExtendedAttributes");
        if (extendedAttributesElement != null) {
            NodeList nodes = extendedAttributesElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element attributeElement = (Element) node;
                AssignmentExtendedAttributeModel attribute = new AssignmentExtendedAttributeModel();
                attribute.fieldID = textContent(attributeElement, "FieldID");
                attribute.value = textContent(attributeElement, "Value");
                result.add(attribute);
            }
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"ExtendedAttribute".equals(((Element) node).getTagName())) {
                continue;
            }
            Element attributeElement = (Element) node;
            AssignmentExtendedAttributeModel attribute = new AssignmentExtendedAttributeModel();
            attribute.fieldID = textContent(attributeElement, "FieldID");
            attribute.value = textContent(attributeElement, "Value");
            result.add(attribute);
        }
        return result;
    }

    public java.util.List<TaskBaselineModel> parseTaskBaselines(Element parent) {
        java.util.List<TaskBaselineModel> result = new java.util.ArrayList<TaskBaselineModel>();
        Element baselinesElement = firstChildElement(parent, "Baselines");
        if (baselinesElement != null) {
            NodeList nodes = baselinesElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"Baseline".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element baselineElement = (Element) node;
                TaskBaselineModel baseline = new TaskBaselineModel();
                String number = textContent(baselineElement, "Number");
                if (!number.isEmpty()) {
                    baseline.number = Integer.valueOf(parseNumber(number, 0));
                }
                baseline.start = textContent(baselineElement, "Start");
                baseline.finish = textContent(baselineElement, "Finish");
                baseline.work = textContent(baselineElement, "Work");
                String cost = textContent(baselineElement, "Cost");
                if (!cost.isEmpty()) {
                    baseline.cost = Double.valueOf(parseDouble(cost, 0.0d));
                }
                result.add(baseline);
            }
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"Baseline".equals(((Element) node).getTagName())) {
                continue;
            }
            Element baselineElement = (Element) node;
            TaskBaselineModel baseline = new TaskBaselineModel();
            String number = textContent(baselineElement, "Number");
            if (!number.isEmpty()) {
                baseline.number = Integer.valueOf(parseNumber(number, 0));
            }
            baseline.start = textContent(baselineElement, "Start");
            baseline.finish = textContent(baselineElement, "Finish");
            baseline.work = textContent(baselineElement, "Work");
            String cost = textContent(baselineElement, "Cost");
            if (!cost.isEmpty()) {
                baseline.cost = Double.valueOf(parseDouble(cost, 0.0d));
            }
            result.add(baseline);
        }
        return result;
    }

    public java.util.List<ResourceBaselineModel> parseResourceBaselines(Element parent) {
        java.util.List<ResourceBaselineModel> result = new java.util.ArrayList<ResourceBaselineModel>();
        Element baselinesElement = firstChildElement(parent, "Baselines");
        if (baselinesElement != null) {
            NodeList nodes = baselinesElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"Baseline".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element baselineElement = (Element) node;
                ResourceBaselineModel baseline = new ResourceBaselineModel();
                String number = textContent(baselineElement, "Number");
                if (!number.isEmpty()) {
                    baseline.number = Integer.valueOf(parseNumber(number, 0));
                }
                baseline.start = textContent(baselineElement, "Start");
                baseline.finish = textContent(baselineElement, "Finish");
                baseline.work = textContent(baselineElement, "Work");
                String cost = textContent(baselineElement, "Cost");
                if (!cost.isEmpty()) {
                    baseline.cost = Double.valueOf(parseDouble(cost, 0.0d));
                }
                result.add(baseline);
            }
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"Baseline".equals(((Element) node).getTagName())) {
                continue;
            }
            Element baselineElement = (Element) node;
            ResourceBaselineModel baseline = new ResourceBaselineModel();
            String number = textContent(baselineElement, "Number");
            if (!number.isEmpty()) {
                baseline.number = Integer.valueOf(parseNumber(number, 0));
            }
            baseline.start = textContent(baselineElement, "Start");
            baseline.finish = textContent(baselineElement, "Finish");
            baseline.work = textContent(baselineElement, "Work");
            String cost = textContent(baselineElement, "Cost");
            if (!cost.isEmpty()) {
                baseline.cost = Double.valueOf(parseDouble(cost, 0.0d));
            }
            result.add(baseline);
        }
        return result;
    }

    public java.util.List<AssignmentBaselineModel> parseAssignmentBaselines(Element parent) {
        java.util.List<AssignmentBaselineModel> result = new java.util.ArrayList<AssignmentBaselineModel>();
        Element baselinesElement = firstChildElement(parent, "Baselines");
        if (baselinesElement != null) {
            NodeList nodes = baselinesElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"Baseline".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element baselineElement = (Element) node;
                AssignmentBaselineModel baseline = new AssignmentBaselineModel();
                String number = textContent(baselineElement, "Number");
                if (!number.isEmpty()) {
                    baseline.number = Integer.valueOf(parseNumber(number, 0));
                }
                baseline.start = textContent(baselineElement, "Start");
                baseline.finish = textContent(baselineElement, "Finish");
                baseline.work = textContent(baselineElement, "Work");
                String cost = textContent(baselineElement, "Cost");
                if (!cost.isEmpty()) {
                    baseline.cost = Double.valueOf(parseDouble(cost, 0.0d));
                }
                result.add(baseline);
            }
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"Baseline".equals(((Element) node).getTagName())) {
                continue;
            }
            Element baselineElement = (Element) node;
            AssignmentBaselineModel baseline = new AssignmentBaselineModel();
            String number = textContent(baselineElement, "Number");
            if (!number.isEmpty()) {
                baseline.number = Integer.valueOf(parseNumber(number, 0));
            }
            baseline.start = textContent(baselineElement, "Start");
            baseline.finish = textContent(baselineElement, "Finish");
            baseline.work = textContent(baselineElement, "Work");
            String cost = textContent(baselineElement, "Cost");
            if (!cost.isEmpty()) {
                baseline.cost = Double.valueOf(parseDouble(cost, 0.0d));
            }
            result.add(baseline);
        }
        return result;
    }

    public java.util.List<TaskTimephasedDataModel> parseTaskTimephasedData(Element parent) {
        java.util.List<TaskTimephasedDataModel> result = new java.util.ArrayList<TaskTimephasedDataModel>();
        Element timephasedDataElement = firstChildElement(parent, "TimephasedData");
        if (timephasedDataElement != null && hasDirectChildElement(timephasedDataElement, "TimephasedData")) {
            NodeList nodes = timephasedDataElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"TimephasedData".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element valueElement = (Element) node;
                TaskTimephasedDataModel value = new TaskTimephasedDataModel();
                fillTimephasedData(valueElement, value);
                result.add(value);
            }
            return result;
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"TimephasedData".equals(((Element) node).getTagName())) {
                continue;
            }
            Element valueElement = (Element) node;
            TaskTimephasedDataModel value = new TaskTimephasedDataModel();
            fillTimephasedData(valueElement, value);
            result.add(value);
        }
        return result;
    }

    public java.util.List<ResourceTimephasedDataModel> parseResourceTimephasedData(Element parent) {
        java.util.List<ResourceTimephasedDataModel> result = new java.util.ArrayList<ResourceTimephasedDataModel>();
        Element timephasedDataElement = firstChildElement(parent, "TimephasedData");
        if (timephasedDataElement != null && hasDirectChildElement(timephasedDataElement, "TimephasedData")) {
            NodeList nodes = timephasedDataElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"TimephasedData".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element valueElement = (Element) node;
                ResourceTimephasedDataModel value = new ResourceTimephasedDataModel();
                fillTimephasedData(valueElement, value);
                result.add(value);
            }
            return result;
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"TimephasedData".equals(((Element) node).getTagName())) {
                continue;
            }
            Element valueElement = (Element) node;
            ResourceTimephasedDataModel value = new ResourceTimephasedDataModel();
            fillTimephasedData(valueElement, value);
            result.add(value);
        }
        return result;
    }

    public java.util.List<AssignmentTimephasedDataModel> parseAssignmentTimephasedData(Element parent) {
        java.util.List<AssignmentTimephasedDataModel> result = new java.util.ArrayList<AssignmentTimephasedDataModel>();
        Element timephasedDataElement = firstChildElement(parent, "TimephasedData");
        if (timephasedDataElement != null && hasDirectChildElement(timephasedDataElement, "TimephasedData")) {
            NodeList nodes = timephasedDataElement.getChildNodes();
            for (int index = 0; index < nodes.getLength(); index += 1) {
                Node node = nodes.item(index);
                if (!(node instanceof Element) || !"TimephasedData".equals(((Element) node).getTagName())) {
                    continue;
                }
                Element valueElement = (Element) node;
                AssignmentTimephasedDataModel value = new AssignmentTimephasedDataModel();
                fillTimephasedData(valueElement, value);
                result.add(value);
            }
            return result;
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (!(node instanceof Element) || !"TimephasedData".equals(((Element) node).getTagName())) {
                continue;
            }
            Element valueElement = (Element) node;
            AssignmentTimephasedDataModel value = new AssignmentTimephasedDataModel();
            fillTimephasedData(valueElement, value);
            result.add(value);
        }
        return result;
    }

    private void fillTimephasedData(Element valueElement, TaskTimephasedDataModel value) {
        String type = textContent(valueElement, "Type");
        if (!type.isEmpty()) {
            value.type = Integer.valueOf(parseNumber(type, 0));
        }
        value.uid = textContent(valueElement, "UID");
        value.start = textContent(valueElement, "Start");
        value.finish = textContent(valueElement, "Finish");
        String unit = textContent(valueElement, "Unit");
        if (!unit.isEmpty()) {
            value.unit = Integer.valueOf(parseNumber(unit, 0));
        }
        value.value = textContent(valueElement, "Value");
    }

    private void fillTimephasedData(Element valueElement, ResourceTimephasedDataModel value) {
        String type = textContent(valueElement, "Type");
        if (!type.isEmpty()) {
            value.type = Integer.valueOf(parseNumber(type, 0));
        }
        value.uid = textContent(valueElement, "UID");
        value.start = textContent(valueElement, "Start");
        value.finish = textContent(valueElement, "Finish");
        String unit = textContent(valueElement, "Unit");
        if (!unit.isEmpty()) {
            value.unit = Integer.valueOf(parseNumber(unit, 0));
        }
        value.value = textContent(valueElement, "Value");
    }

    private void fillTimephasedData(Element valueElement, AssignmentTimephasedDataModel value) {
        String type = textContent(valueElement, "Type");
        if (!type.isEmpty()) {
            value.type = Integer.valueOf(parseNumber(type, 0));
        }
        value.uid = textContent(valueElement, "UID");
        value.start = textContent(valueElement, "Start");
        value.finish = textContent(valueElement, "Finish");
        String unit = textContent(valueElement, "Unit");
        if (!unit.isEmpty()) {
            value.unit = Integer.valueOf(parseNumber(unit, 0));
        }
        value.value = textContent(valueElement, "Value");
    }

    private NodeList emptyNodeList() {
        return new NodeList() {
            public Node item(int index) {
                return null;
            }

            public int getLength() {
                return 0;
            }
        };
    }

    private boolean hasDirectChildElement(Element parent, String tagName) {
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index += 1) {
            Node node = childNodes.item(index);
            if (node instanceof Element && tagName.equals(((Element) node).getTagName())) {
                return true;
            }
        }
        return false;
    }

    public Document parseXmlDocument(String xmlText) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlText)));
            document.getDocumentElement().normalize();
            return document;
        } catch (Exception ex) {
            throw new IllegalArgumentException("XML の解析に失敗しました", ex);
        }
    }
}
