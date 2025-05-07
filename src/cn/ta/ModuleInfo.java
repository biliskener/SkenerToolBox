package cn.ta;

import java.rmi.UnexpectedException;
import java.util.TreeMap;

public class ModuleInfo {
    public String name;
    public TreeMap<String, ConstSheetInfo> constSheets = new TreeMap<>();
    public TreeMap<String, HashSheetInfo> hashSheets = new TreeMap<>();
    public TreeMap<String, EnumSheetInfo> enumSheets = new TreeMap<>();

    public ModuleInfo(String name) {
        this.name = name;
    }

    void addConstSheet(ConstSheetInfo sheet) throws UnexpectedException {
        if(constSheets.get(sheet.getName()) != null) {
            throw new UnexpectedException("duplicated sheet: " + sheet.getName());
        }

        constSheets.put(sheet.getName(), sheet);
    }

    void addHashShet(HashSheetInfo sheet) throws UnexpectedException {
        if(hashSheets.get(sheet.getName()) != null) {
            throw new UnexpectedException("duplicated sheet: " + sheet.getName());
        }

        hashSheets.put(sheet.getName(), sheet);
    }

    void addEnumSheet(EnumSheetInfo sheet) throws UnexpectedException {
        if(enumSheets.get(sheet.getName()) != null) {
            throw new UnexpectedException("duplicated sheet: " + sheet.getName());
        }
        enumSheets.put(sheet.getName(), sheet);
    }
}
