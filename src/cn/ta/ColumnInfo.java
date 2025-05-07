package cn.ta;

public class ColumnInfo implements Cloneable {
    public SheetInfo parentSheet;
    public SheetInfo childSheet;
    public FieldInfo field;
    public int index;   // 数据的列
    public String name; // 注意，名称可能带.

    public ColumnInfo(FieldInfo field, String name, int index) {
        this.parentSheet = null;
        this.childSheet = null;
        this.field = field;
        this.name = name;
        this.index = index;
    }

    public ColumnInfo getParentColumn() {
        return this.parentSheet.parentColumn;
    }
}
