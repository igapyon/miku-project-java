package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class CalendarExceptionModel {
    public String name;
    public String fromDate;
    public String toDate;
    public Boolean dayWorking;
    public List<WorkingTimeModel> workingTimes = new ArrayList<WorkingTimeModel>();
}
