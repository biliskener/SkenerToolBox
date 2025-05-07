package cn.ta;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.rmi.UnexpectedException;
import java.util.*;

public class ClaszInfo {
    public ClaszInfo baseClasz;
    public ClaszInfo ownerObjClasz; // 持有者类
    public Map<String, ClaszInfo> subClaszes = new TreeMap();

    public String name;
    public String flags = "";
    public List<FieldInfo> fields;
    public TreeMap<String, FieldInfo> fieldsByName;

    protected List<FieldInfo> indexedFields;
    protected Map<String, FieldInfo> indexedFieldsMap;

    public String extFilterName = "";
    public Multimap<String, FieldInfo> extFilterFieldsMap = ArrayListMultimap.create();

    public boolean genColForServer = true;
    public boolean genColForClient = true;

    public static ClaszInfo objectClasz;

    static {
        try {
            objectClasz = new ClaszInfo("Object", null, "SC", new LinkedList<>());
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }
    }

    public ClaszInfo(String claszName, ClaszInfo baseClasz, String flags, List<FieldInfo> fields) throws UnexpectedException {
        this.baseClasz = baseClasz;
        if(baseClasz != null) {
            if(baseClasz.subClaszes.get(claszName) == null) {
                baseClasz.subClaszes.put(claszName, this);
            }
            else {
                throw new UnexpectedException("Duplicated sub clasz: " + claszName);
            }
        }
        this.name = claszName;
        this.flags = flags;

        this.genColForServer = flags.contains("S");
        this.genColForClient = flags.contains("C");
        if(this.baseClasz != null) {
            assert(this.genColForServer == this.baseClasz.genColForServer);
            assert(this.genColForClient == this.baseClasz.genColForClient);
        }

        this.fields = new ArrayList<>();
        this.fieldsByName = new TreeMap<>();
        this.indexedFields = new ArrayList<>();
        this.indexedFieldsMap = new TreeMap<>();

        this.deepCheckFieldsExistenceOfBaseClasz(fields);

        for(FieldInfo field: fields) {
            // 在基类中查找对应字段
            FieldInfo fieldInBaseClasz = null;
            if(baseClasz != null) {
                fieldInBaseClasz = baseClasz.deepFindFieldUpwardBaseClasz(field.name);
            }

            if(fieldInBaseClasz != null) {
                // 找到了就忽略
                if(!fieldInBaseClasz.isIdentical(field)) {
                    throw new UnexpectedException("field " + field.name + " is not identical between " + this.name + " and " + baseClasz.name);
                }
            }
            else {
                this.addFieldEx(field);
            }
        }
    }

    // 判断类信息结构是否一致
    public boolean isIdentical(String claszName, String baseClaszName, String flags, List<FieldInfo> fields) {
        if(this.name.equals(claszName) == false) {
            return false;
        }

        if(baseClaszName.equals(this.baseClasz != null ? this.baseClasz.name : "") == false) {
            return false;
        }

        if(this.flags.equals(flags) == false) {
            return false;
        }

        LinkedList<FieldInfo> fields2 = new LinkedList<>();
        for(ClaszInfo c = this; c != null; c = c.baseClasz) {
            for(int j = c.fields.size() - 1; j >= 0; --j) {
                fields2.addFirst(c.fields.get(j));
            }
        }

        if(fields2.size() != fields.size()) {
            return false;
        }

        for(int i = 0; i < fields2.size(); ++i) {
            FieldInfo field = fields.get(i);
            FieldInfo field2 = fields2.get(i);
            if(!field.isIdentical(field2)) {
                return false;
            }
        }

        return true;
    }

    // 确保所有字段是否包含了基类的所有字段，创建子类时，字段必须完整包含基类的所有字段
    public void deepCheckFieldsExistenceOfBaseClasz(List<FieldInfo> fields) throws UnexpectedException {
        Map<String, FieldInfo> fieldsMap = new TreeMap();
        for(FieldInfo field: fields) {
            fieldsMap.put(field.name, field);
        }
        for(ClaszInfo clasz = this.baseClasz; clasz != null; clasz = clasz.baseClasz) {
            for(FieldInfo field: clasz.fields) {
                if(fieldsMap.get(field.name) == null) {
                    throw new UnexpectedException("field " + field.name + " not found in derived class " + this.name);
                }
            }
        }
    }

    // 往基类方向查找对应的field
    public FieldInfo deepFindFieldUpwardBaseClasz(String name) {
        for(ClaszInfo clasz = this; clasz != null; clasz = clasz.baseClasz) {
            FieldInfo field = clasz.fieldsByName.get(name);
            if(field != null) {
                return field;
            }
        }
        return null;
    }

    // 获得索引，只有topBaseClasz才有索引
    public List<FieldInfo> getIndexedFields() {
        if(this.baseClasz != null) {
            return this.baseClasz.getIndexedFields();
        }
        else {
            return this.indexedFields;
        }
    }

    // 获得索引，只有topBaseClasz才有索引
    public Map<String, FieldInfo> getIndexedFieldsMap() {
        if(this.baseClasz != null) {
            return this.baseClasz.getIndexedFieldsMap();
        }
        else {
            return this.indexedFieldsMap;
        }
    }

    // 获得最上层的基类
    public ClaszInfo getTopBaseClasz() {
        ClaszInfo baseClasz = this;
        while(baseClasz.baseClasz != null) {
            baseClasz = baseClasz.baseClasz;
        }
        return baseClasz;
    }

    public void addField(FieldInfo field) throws UnexpectedException {
        // 在基类中查找对应字段
        FieldInfo fieldInBaseClasz = null;
        if(baseClasz != null) {
            fieldInBaseClasz = baseClasz.deepFindFieldUpwardBaseClasz(field.name);
        }

        if(fieldInBaseClasz != null) {
            // 找到了就忽略
            if(!fieldInBaseClasz.isIdentical(field)) {
                throw new UnexpectedException("field " + field.name + " is not identical between " + this.name + " and " + baseClasz.name);
            }
        }
        else {
            this.addFieldEx(field);
        }
    }

    private void addFieldEx(FieldInfo field) throws UnexpectedException {
        // 加入到列表
        this.fields.add(field);
        this.fieldsByName.put(field.name, field);
        field.onwingClasz = this;

        // 加入到索引列表
        if(field.indexType != IndexType.NotIndex) {
            this.indexedFields.add(field);
            this.indexedFieldsMap.put(field.name, field);
        }

        // 处理扩展
        if(!field.extName.isEmpty()) {
            if(!this.extFilterName.isEmpty() && !this.extFilterName.equals(field.extName)) {
                throw new UnexpectedException("ext name dismatch: " + this.extFilterName + " <=> " + field.extName);
            }
            else {
                this.extFilterName = field.extName;
                for(String extValue: field.extValues) {
                    this.extFilterFieldsMap.put(extValue, field);
                }
            }
        }
    }

    ClaszInfo getSameBaseClasz(ClaszInfo clasz2) {
        for(ClaszInfo baseClasz1 = this; baseClasz1 != null; baseClasz1 = baseClasz1.baseClasz) {
            for(ClaszInfo baseClasz2 = clasz2; baseClasz2 != null; baseClasz2 = baseClasz2.baseClasz) {
                if(baseClasz1 == objectClasz || baseClasz2 == objectClasz) {
                    return objectClasz;
                }
                if(baseClasz1 == baseClasz2) {
                    return baseClasz1;
                }
            }
        }
        return objectClasz;
    }

    void __setOwnerObjClasz(ClaszInfo ownerObjClasz) {
        if(this.ownerObjClasz == null) {
            this.ownerObjClasz = ownerObjClasz;
        }
        else {
            this.ownerObjClasz = this.ownerObjClasz.getSameBaseClasz(ownerObjClasz);
        }
    }

    void unionOwnerObjClaszForAllSubClaszes() {
        if(this.ownerObjClasz != null) {
            ArrayList<ClaszInfo> subClaszesQueue = new ArrayList<>();
            subClaszesQueue.addAll(this.subClaszes.values());
            for(int i = 0; i < subClaszesQueue.size(); ++i) {
                ClaszInfo subClasz = subClaszesQueue.get(i);
                if(subClasz.ownerObjClasz != null) {
                    this.ownerObjClasz = this.ownerObjClasz.getSameBaseClasz(subClasz.ownerObjClasz);
                    subClasz.ownerObjClasz = null;
                }
                subClaszesQueue.addAll(subClasz.subClaszes.values());
            }
        }
    }

    void setOwnerObjClasz(ClaszInfo ownerObjClasz) throws UnexpectedException {
        // 在父类往上搜索
        for(ClaszInfo baseClasz = this; baseClasz != null; baseClasz = baseClasz.baseClasz) {
            if(Config.ignoreOwnerObjItems.contains(baseClasz.name)) {
                return;
            }

            if(baseClasz.ownerObjClasz != null) {
                baseClasz.__setOwnerObjClasz(ownerObjClasz);
                return;
            }
        }

        this.__setOwnerObjClasz(ownerObjClasz);
    }

    ClaszInfo getOwnerObjClasz(boolean searchBase) {
        if(this.ownerObjClasz != null) {
            return this.ownerObjClasz;
        }
        else if(searchBase && this.baseClasz != null) {
            return this.baseClasz.getOwnerObjClasz(searchBase);
        }
        else {
            return null;
        }
    }
}
