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
import jp.igapyon.mikuproject.model.WeekDayModel;
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
