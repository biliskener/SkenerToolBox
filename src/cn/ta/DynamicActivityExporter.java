package cn.ta;

import cn.ta.config.ProjectConfig;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.List;

public class DynamicActivityExporter {
    public static void exportAll(JSONObject json, String jsonOutputDir) throws IOException {
        jsonOutputDir = SUtils.D(jsonOutputDir + "/dynamic_activities");
        for(Object _key: json.keySet()) {
            String key = (String)_key;
            JSONArray jsonArray = json.getJSONArray(key);
            for(int i = 0; i < jsonArray.length(); ++i) {
                exportOne(jsonArray.getJSONObject(i), jsonOutputDir);
            }
        }
    }

    private static void exportOne(JSONObject json, String jsonOutputDir) throws IOException {
        JSONObject newJson = new JSONObject();
        JSONObject newData = new JSONObject();
        newJson.put("data", newData);
        if(ProjectConfig.CURRENT.isLongyinProject) {
            for(Object _key: json.keySet()) {
                String key = (String)_key;
                if(key.matches("^(activityId|name|desc|detail|needLevel|sortIndex|clasz)$")) {
                    newJson.put(key, json.get(key));
                }
                else if(key.matches("^(startTime|endTime)$")) {
                    String value = json.getString(key);
                    if(!value.matches("^\\d+-\\d+-\\d+ \\d+:\\d+:\\d+$")) {
                        throw new UnexpectedException(key + "格式" + "错误");
                    }
                    newJson.put(key, value);
                }
                else if(key.equals("segmentsPerWeek")) {
                    String value = json.getString(key);
                    newJson.put(key, value);
                }
                else if(key.equals("segmentsPerDay")) {
                    String value = json.getString(key);
                    newJson.put(key, value);
                }
                else {
                    newData.put(key, json.get(key));
                }
            }
        }
        else {
            for(Object _key: json.keySet()) {
                String key = (String)_key;
                if(key.matches("^(activityId|name|desc|detail|needLevel|sortIndex|clasz|dataMode|show)$")) {
                    newJson.put(key, json.get(key));
                }
                else if(key.matches("^(startTime|endTime)$")) {
                    String value = json.getString(key);
                    if(!value.matches("^\\d+-\\d+-\\d+ \\d+:\\d+:\\d+$")) {
                        throw new UnexpectedException(key + "格式" + "不合法");
                    }
                    newJson.put(key, value);
                }
                else {
                    newData.put(key, json.get(key));
                }
            }
        }

        int id = newJson.getInt("activityId");

        String pathName = jsonOutputDir + id + ".json";
        SUtils.makeDirAll(pathName);
        SUtils.saveFile(pathName, SUtils.encodeJson(newJson, 1));
    }
}
