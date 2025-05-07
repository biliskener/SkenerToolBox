package cn.ta;

import org.json.JSONArray;

public class EnumSheetInfo extends BaseSheetInfo {
    public JSONArray targetValue;

    public EnumSheetInfo(ClaszInfo clasz, JSONArray targetValue) {
        super(clasz.name, clasz);
        this.targetValue = targetValue;
    }
}
