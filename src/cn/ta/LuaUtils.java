package cn.ta;

import java.rmi.UnexpectedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class LuaUtils {
    private static class TypeInfo {
        String typeName;
        String luaTypeName;
        String defaultValue;
        boolean isEnum;
        Map<String, Integer> enumStrToIntValues = new LinkedHashMap<>();
        Map<Integer, String> enumIntToStrValues = new LinkedHashMap<>();
        public TypeInfo(String typeName, String luaTypeName, String defaultValue, boolean isEnum) {
            this.typeName = typeName;
            this.luaTypeName = luaTypeName;
            this.defaultValue = defaultValue;
            this.isEnum = isEnum;
        }
    }

    private static Map<String, TypeInfo> luaTypeMap = new TreeMap<>();

    static {
        addType("int", "number", "0");
        addType("long", "number", "0");
        addType("float", "number", "0");
        addType("double", "number", "0");
        addType("bool", "boolean", "false");
        addType("string", "string", "''");
        addType("json", "json", "nil");
        addType("int[]", "number[]", "nil");
        addType("int[][]", "number[][]", "nil");
        addType("int[][][]", "number[][][]", "nil");
        addType("long[]", "number[]", "nil");
        addType("long[][]", "number[][]", "nil");
        addType("long[][][]", "number[][][]", "nil");
        addType("float[]", "number[]", "nil");
        addType("float[][]", "number[][]", "nil");
        addType("float[][][]", "number[][][]", "nil");
        addType("double[]", "number[]", "nil");
        addType("double[][]", "number[][]", "nil");
        addType("double[][][]", "number[][][]", "nil");
        addType("bool[]", "boolean[]", "nil");
        addType("bool[][]", "boolean[][]", "nil");
        addType("bool[][][]", "boolean[][][]", "nil");
        addType("string[]", "string[]", "nil");
        addType("string[][]", "string[][]", "nil");
        addType("string[][][]", "string[][][]", "nil");
    }

    private static void addType(String typeName, String javaTypeName, String defaultValue) {
        TypeInfo typeInfo = new TypeInfo(typeName, javaTypeName, defaultValue, false);
        luaTypeMap.put(typeInfo.typeName, typeInfo);
    }

    public static void addEnumType(String typeName) {
        TypeInfo typeInfo = new TypeInfo(typeName, typeName, "null", true);
        luaTypeMap.put(typeInfo.typeName, typeInfo);
    }

    public static void addEnumValue(String typeName, String valueName, int valueInt, boolean addToI2S) {
        TypeInfo typeInfo = luaTypeMap.get(typeName);
        assert typeInfo.isEnum;
        if(typeInfo.enumStrToIntValues.containsKey(valueName) && typeInfo.enumStrToIntValues.get(valueName) != valueInt) {
            assert typeInfo.enumStrToIntValues.containsKey(valueName) == false: typeName + "." + valueName + " is duplicated";
        }
        typeInfo.enumStrToIntValues.put(valueName, valueInt);
        if(addToI2S) {
            typeInfo.enumIntToStrValues.put(valueInt, valueName);
        }
    }

    public static int getEnumIntValue(String typeName, String valueName) {
        if(valueName.matches("^-?\\d+$")) {
            return Integer.parseInt(valueName);
        }
        else {
            return luaTypeMap.get(typeName).enumStrToIntValues.get(valueName);
        }
    }

    public static String getEnumStrValue(String typeName, int valueInt) {
        return luaTypeMap.get(typeName).enumIntToStrValues.get(valueInt);
    }

    public static boolean isEnumType(String typeName) {
        TypeInfo config = luaTypeMap.get(typeName);
        return config != null && config.isEnum;
    }

    public static String getLuaTypeName(String typeName) throws UnexpectedException {
        TypeInfo config = luaTypeMap.get(typeName);
        if(config == null) {
            throw new UnexpectedException("Unknown type name: " + typeName);
        }
        return config.luaTypeName;
    }

    private static Map<String, String> luaMapKeyMap = new TreeMap<>();
    static {
        luaMapKeyMap.put("int", "number");
        luaMapKeyMap.put("long", "number");
        luaMapKeyMap.put("float", "number");
        luaMapKeyMap.put("double", "number");
        luaMapKeyMap.put("string", "string");
        luaMapKeyMap.put("boolean", "boolean");
    }

    public static String getMapKeyTypeName(String typeName) throws UnexpectedException {
        String ret = luaMapKeyMap.get(typeName);
        if(ret == null || ret.isEmpty()) {
            throw new UnexpectedException("Unknown type name: " + typeName);
        }
        return ret;
    }
}

