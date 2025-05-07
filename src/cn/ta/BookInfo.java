package cn.ta;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BookInfo {
    public String name;
    private Object serverJsonValue;// 单一值时为JSONObject, 多重值时为JSONArray
    private Object clientJsonValue;// 单一值时为JSONObject, 多重值时为JSONArray
    public List<SheetInfo> sheetsList = new ArrayList<>();
    public Map<String, SheetInfo> sheetsHash = new TreeMap<>();
    public SheetInfo primarySheet;
    public BookInfo(String name) {
        this.name = name;
    }

    public SheetInfo addSheet(String name, String typeName, String flags, ColumnInfo parentColumn, List<ColumnInfo> columns, ClaszInfo clasz) throws UnexpectedException {
        if(sheetsHash.get(name) == null) {
            SheetInfo sheet = new SheetInfo(name, typeName, flags, parentColumn, columns, clasz);

            if(this.primarySheet == null) {
                this.primarySheet = sheet;
                if(parentColumn != null) {
                    throw new UnexpectedException("fatal error");
                }
            }
            else {
                if(parentColumn == null) {
                    throw new UnexpectedException("Parent sheet not found: " + sheet.name);
                }
            }

            this.sheetsList.add(sheet);
            this.sheetsHash.put(sheet.name, sheet);
            sheet.setBook(this);
            return sheet;
        }
        else {
            throw new UnexpectedException("Duplicated sub table key " + name + " in " + this.name);
        }
    }

    // 查找sheet的父column
    public ColumnInfo searchParentColumnForSheet(String sheetName) throws UnexpectedException {
        ColumnInfo result = null;
        for(SheetInfo sheet: this.sheetsList) {
            for(ColumnInfo column : sheet.columnsByIndex.values()) {
                if(column.field.name.equals(sheetName)) {
                    if(result == null) {
                        result = column;
                    }
                    else {
                        throw new UnexpectedException("Duplicated column name: " + sheetName);
                    }
                }
            }
        }
        return result;
    }

    public Object getJsonValue(boolean isServer) {
        return isServer ? serverJsonValue : clientJsonValue;
    }

    public void setJsonValue(boolean isServer, Object jsonValue) {
        if(isServer) {
            serverJsonValue = jsonValue;
        }
        else {
            clientJsonValue = jsonValue;
        }
    }
}

