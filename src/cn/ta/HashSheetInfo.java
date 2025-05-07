package cn.ta;

import org.json.JSONArray;

public class HashSheetInfo extends BaseSheetInfo {
    public JSONArray targetValue;

    public HashSheetInfo(ClaszInfo clasz, JSONArray targetValue) {
        super(clasz.name, clasz);
        this.targetValue = targetValue;
    }
}
