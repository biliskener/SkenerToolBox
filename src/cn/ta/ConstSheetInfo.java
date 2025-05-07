package cn.ta;

import org.json.JSONObject;

public class ConstSheetInfo extends BaseSheetInfo {
    public JSONObject targetValue;

    ConstSheetInfo(ClaszInfo clasz, JSONObject targetValue) {
        super(clasz.name, clasz);
        this.targetValue = targetValue;
    }
}
