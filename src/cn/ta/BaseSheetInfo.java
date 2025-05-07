package cn.ta;

public class BaseSheetInfo {
    public String name;
    public ClaszInfo clasz;

    protected BaseSheetInfo(String name, ClaszInfo clasz) {
        this.name = name;
        this.clasz = clasz;
    }

    public String getName() {
        return name;
    }
}
