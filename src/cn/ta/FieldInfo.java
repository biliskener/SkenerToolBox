package cn.ta;

import com.strobel.core.Triple;

import java.rmi.UnexpectedException;
import java.util.LinkedList;

public class FieldInfo implements Cloneable {
    public int index = -1;
    public String name = "";
    public String desc = "";
    public String typeName = "";
    public String flags = "";
    public IndexType indexType = IndexType.NotIndex;
    public String extName = "";
    public String[] extValues = new String[]{};

    public boolean isTargetClasz = false;
    public String targetClaszName = "";
    public boolean isTargetMaybeConst = false; // 可能为常量

    public ClaszInfo onwingClasz = null;

    public boolean genColForServer = true;
    public boolean genColForClient = true;

    public FieldInfo(int index, String name, String desc, String typeName, String flags, IndexType indexType, String extName, String[] extValues) throws UnexpectedException {
        this.index = index;
        this.name = name;
        this.desc = desc;
        this.typeName = typeName;
        this.flags = flags;
        this.indexType = indexType;

        this.extName = extName;
        this.extValues = extValues;

        this.genColForServer = flags.contains("S");
        this.genColForClient = flags.contains("C");
        if(this.indexType != IndexType.NotIndex) {
            assert(this.genColForServer);
            assert(this.genColForClient);
        }
    }

    public FieldInfo(int index, String name, String desc, String typeName, String flags) throws UnexpectedException {
        this(index, name, desc, typeName, flags, IndexType.NotIndex, "", new String[]{});
    }

    @Override
    public final FieldInfo clone() {
        FieldInfo field = null;
        try {
            field = new FieldInfo(
                    index,
                    name,
                    desc,
                    typeName,
                    flags,
                    indexType,
                    extName,
                    extValues
            );
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }

        field.isTargetClasz = isTargetClasz;
        field.targetClaszName = targetClaszName;
        field.isTargetMaybeConst = isTargetMaybeConst;

        field.onwingClasz = null;

        return field;
    }

    public void setTargetClasz() throws UnexpectedException {
        this.isTargetClasz = true;
        this.targetClaszName = this.typeName.replaceAll("\\{\\}", "").replaceAll("\\[\\]", "");
        if(!this.targetClaszName.matches("^[A-Z]\\w+$")) {
            throw new UnexpectedException("Invalid type name: " + this.typeName);
        }
    }

    public void setTargetMaybeConst() {
        this.isTargetMaybeConst = true;
    }

    // 获得数组维度数量
    public int getArrayDimCount() {
        int dimCount = 0;
        String typeName = this.typeName;
        while(typeName.endsWith("[]")) {
            ++dimCount;
            typeName = typeName.substring(0, typeName.length() - 2);
        }
        return dimCount;
    }

    public String getBaseTypeName() {
        int dimCount = 0;
        String typeName = this.typeName;
        while(typeName.endsWith("[]")) {
            ++dimCount;
            typeName = typeName.substring(0, typeName.length() - 2);
        }
        return typeName;
    }

    public boolean isArray() {
        return this.typeName.contains("[]");
    }

    public boolean isMap() {
        return this.typeName.contains("{}");
    }

    public boolean isMulti() {
        return isArray() || isMap();
    }

    public boolean isMultiObject() {
        return this.isTargetClasz && this.isMulti();
    }

    public boolean isRefObject() {
        return this.typeName.contains(".");
    }

    public boolean isIdentical(FieldInfo field) {
        if(field == null) {
            return false;
        }

        if(field.index != index) {
            //return false;
        }
        if(field.name.equals(name) == false) {
            return false;
        }
        if(field.desc.equals(desc) == false) {
//            return false;
        }
        if(field.typeName.equals(typeName) == false) {
            return false;
        }
        if(field.targetClaszName.equals(targetClaszName) == false) {
            return false;
        }
        if(field.extName.equals(extName) == false) {
            return false;
        }
        if(field.extValues.length != extValues.length) {
            return false;
        }
        for(int i = 0; i < field.extValues.length; ++i) {
            if(field.extValues[i].equals(extValues[i]) == false) {
                return false;
            }
        }
        if(field.isTargetClasz != isTargetClasz) {
            return false;
        }
        if(field.isTargetMaybeConst != isTargetMaybeConst) {
            return false;
        }
        if(field.indexType != indexType) {
            return false;
        }
        if(field.flags.equals(flags) == false) {
            return false;
        }
        if(field.genColForServer != genColForServer) {
            return false;
        }
        if(field.genColForClient != genColForClient) {
            return false;
        }
        return true;
    }
}
