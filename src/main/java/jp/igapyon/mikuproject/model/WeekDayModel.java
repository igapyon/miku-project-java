package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class WeekDayModel {
    public Integer dayType;
    public boolean dayWorking;
    public List<WorkingTimeModel> workingTimes = new ArrayList<WorkingTimeModel>();
}
