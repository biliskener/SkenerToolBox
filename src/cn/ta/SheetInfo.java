package cn.ta;

import org.json.JSONArray;

import java.rmi.UnexpectedException;
import java.util.*;

public class SheetInfo {
    public String name;
    public String typeName;
    public boolean isList;
    public boolean isHash;
    public boolean isMulti;
    public boolean isObject;
    public List<SheetInfo> aggregateChildSheets = new LinkedList<>();
    public SheetInfo aggregateParentSheet = null;
    public ClaszInfo clasz;
    public TreeMap<Integer, ColumnInfo> columnsByIndex;
    public TreeMap<String, ColumnInfo> columnsByName;
    public BookInfo bookInfo;
    public ColumnInfo parentColumn;

    public Map<Integer, ColumnInfo> indexedColumnsByIndex;
    public Map<String, ColumnInfo> indexedColumnsByName;

    public SheetInfo(String name, String typeName, String flags, ColumnInfo parentColumn, List<ColumnInfo> columns, ClaszInfo clasz) throws UnexpectedException {
        this.name = name;
        this.typeName = typeName;
        this.isList = this.typeName.contains("[]");
        this.isHash = this.typeName.contains("{}");
        this.isMulti = this.isList || this.isHash;
        this.isObject = clasz != null;
        this.clasz = clasz;

        if(parentColumn != null) {
            this.setParentColumn(parentColumn);
        }

        this.columnsByName = new TreeMap<>();
        this.columnsByIndex = new TreeMap<>();
        this.indexedColumnsByIndex = new TreeMap<>();
        this.indexedColumnsByName = new TreeMap<>();

        for(ColumnInfo column: columns) {
            this.addColumn(column);
        }

        /*
        for(FieldInfo field: ownerIndexedFields) {
            ColumnInfo column = new ColumnInfo(field);
            this.addColumn(column);
        }

        LinkedList<ClaszInfo> baseClaszStack = new LinkedList<>();
        for(ClaszInfo baseClasz = clasz; baseClasz != null; baseClasz = baseClasz.baseClasz) {
            baseClaszStack.addFirst(baseClasz);
        }

        for(ClaszInfo baseClasz: baseClaszStack) {
            for(FieldInfo field: baseClasz.fields) {
                ColumnInfo column = new ColumnInfo(field);
                this.addColumn(column);
            }
        }
         */

        this.checkOwnerIndexedFields();
    }

    public void setBook(BookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }

    public void setParentColumn(ColumnInfo column) throws UnexpectedException {
        this.parentColumn = column;
        if(parentColumn.childSheet == null) {
            parentColumn.childSheet = this;
        }
        else {
            throw new UnexpectedException("column already has child sheet");
        }
    }

    /*
    private boolean isParentIndexedColumn(ColumnInfo column) {
        for(ColumnInfo parentColumn = this.parentColumn; parentColumn != null; parentColumn = parentColumn.getParentColumn()) {
            if(parentColumn.parentSheet.indexedColumnsMap.get(column.field.name) != null) {
                return true;
            }
        }
        return false;
    }
     */

    // 检查Sheet的索引结构与类的索引结构要匹配
    private void checkOwnerIndexedFields() throws UnexpectedException {
        /*
        LinkedList<FieldInfo> fields = new LinkedList<>();

        // 往owner方向找到所有的索引字段, 注意排序规则
        for(ColumnInfo column = this.parentColumn; column != null; column = column.parentSheet.parentColumn) {
            ClaszInfo clasz = column.parentSheet.clasz;
            List<FieldInfo> indexedFields = clasz.getIndexedFields();
            for(int i = indexedFields.size() - 1; i >= 0; --i) {
                fields.addFirst(indexedFields.get(i));
            }
        }

        // 检查一致性
        if(ownerIndexedFields.size() != fields.size()) {
            throw new UnexpectedException("prefix fields dismatch");
        }
        for(int i = 0; i < ownerIndexedFields.size(); ++i) {
            FieldInfo field1 = ownerIndexedFields.get(i);
            FieldInfo field2 = fields.get(i);
            if(!field1.isIdentical(field2)) {
                throw new UnexpectedException("prefix fields dismatch");
            }
        }
         */

        LinkedList<FieldInfo> allIndexedFields = new LinkedList<>();

        // 往owner方向找到所有的索引字段, 注意排序规则
        for(SheetInfo sheet = this; sheet != null; sheet = sheet.parentColumn != null ? sheet.parentColumn.parentSheet : null) {
            // 如果Sheet本身是单一的，即使对应的clasz有索引，也不要在sheet的索引中添加
            if(sheet.isMulti) {
                ClaszInfo clasz = sheet.clasz;
                List<FieldInfo> indexedFields = clasz.getIndexedFields();
                for(int i = indexedFields.size() - 1; i >= 0; --i) {
                    allIndexedFields.addFirst(indexedFields.get(i));
                }
            }
        }

        List<ColumnInfo> indexedColumns = new ArrayList<>();
        for(ColumnInfo column: this.indexedColumnsByIndex.values()) {
            indexedColumns.add(column);
        }

        // 检查一致性
        if(indexedColumns.size() != allIndexedFields.size()) {
            throw new UnexpectedException("indexed columns and fields dismatch");
        }
        for(int i = 0; i < indexedColumns.size(); ++i) {
            ColumnInfo column = indexedColumns.get(i);
            FieldInfo field = allIndexedFields.get(i);
            if(column.field != field) {
                throw new UnexpectedException("indexed columns and fields dismatch");
            }
        }
    }

    private void addColumn(ColumnInfo column) throws UnexpectedException {
        if(this.columnsByName.get(column.name) != null) {
            throw new UnexpectedException("impossible");
        }
        this.columnsByName.put(column.name, column);
        this.columnsByIndex.put(column.index, column);
        column.parentSheet = this;
        // 只有sheet自身是多重时，才有索引列
        if(this.isMulti && column.field.indexType != IndexType.NotIndex) {
            this.indexedColumnsByIndex.put(column.index, column);
            this.indexedColumnsByName.put(column.name, column);
        }
    }
}
