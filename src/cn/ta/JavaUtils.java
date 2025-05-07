package cn.ta;

import java.rmi.UnexpectedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class JavaUtils {
    private static class TypeInfo {
        String typeName;
        String javaTypeName;
        String defaultValue;
        boolean isEnum;
        String enumValueTypeName;
        Map<String, Integer> enumStrToIntValues = new LinkedHashMap<>();
        Map<Integer, String> enumIntToStrValues = new LinkedHashMap<>();
        public TypeInfo(String typeName, String javaTypeName, String defaultValue, boolean isEnum, String enumValueTypeName) {
            this.typeName = typeName;
            this.javaTypeName = javaTypeName;
            this.defaultValue = defaultValue;
            this.enumValueTypeName = enumValueTypeName;
            this.isEnum = isEnum;
        }
    }

    private static Map<String, TypeInfo> javaTypeMap = new TreeMap<>();

    static {
        addType("int", "int", "0");
        addType("long", "long", "0");
        addType("float", "double", "0");
        addType("double", "double", "0");
        addType("bool", "boolean", "false");
        addType("string", "String", "''");
        addType("json", "JSONObject", "null");
        addType("int[]", "int[]", "null");
        addType("int[][]", "int[][]", "null");
        addType("int[][][]", "int[][][]", "null");
        addType("long[]", "long[]", "null");
        addType("long[][]", "long[][]", "null");
        addType("long[][][]", "long[][][]", "null");
        addType("float[]", "double[]", "null");
        addType("float[][]", "double[][]", "null");
        addType("float[][][]", "double[][][]", "null");
        addType("double[]", "double[]", "null");
        addType("double[][]", "double[][]", "null");
        addType("double[][][]", "double[][][]", "null");
        addType("bool[]", "boolean[]", "null");
        addType("bool[][]", "boolean[][]", "null");
        addType("bool[][][]", "boolean[][][]", "null");
        addType("string[]", "String[]", "null");
        addType("string[][]", "String[][]", "null");
        addType("string[][][]", "String[][][]", "null");
    }

    private static void addType(String typeName, String javaTypeName, String defaultValue) {
        TypeInfo typeInfo = new TypeInfo(typeName, javaTypeName, defaultValue, false, "");
        javaTypeMap.put(typeInfo.typeName, typeInfo);
    }

    public static void addEnumType(String typeName, String valueTypeName) {
        assert valueTypeName.equals("int");
        TypeInfo typeInfo = new TypeInfo(typeName, typeName, "null", true, valueTypeName);
        javaTypeMap.put(typeInfo.typeName, typeInfo);
    }

    public static void addEnumValue(String typeName, String valueName, int valueInt, boolean addToI2S) {
        TypeInfo typeInfo = javaTypeMap.get(typeName);
        assert typeInfo.isEnum: typeName + "." + valueName + " is not enum";
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
            return javaTypeMap.get(typeName).enumStrToIntValues.get(valueName);
        }
    }

    public static String getEnumStrValue(String typeName, int valueInt) {
        return javaTypeMap.get(typeName).enumIntToStrValues.get(valueInt);
    }

    public static boolean isEnumType(String typeName) {
        TypeInfo config = javaTypeMap.get(typeName);
        return config != null && config.isEnum;
    }

    public static String getJavaEnumClassName(String typeName) throws UnexpectedException {
        TypeInfo config = javaTypeMap.get(typeName);
        if(config == null) {
            throw new UnexpectedException("Unknown type name: " + typeName);
        }
        if(config.isEnum) {
            return "E" + config.javaTypeName;
        }
        else {
            throw new UnexpectedException("need enum type");
        }
    }

    public static String getJavaTypeName(String typeName) throws UnexpectedException {
        if(UnrealTypes.isUETypeOrSubclassOf(typeName)) {
            typeName = "string";
        }
        if(UnrealTypes.isUETypeOrSubclassOf1D(typeName)) {
            typeName = "string[]";
        }
        TypeInfo config = javaTypeMap.get(typeName);
        if(config == null) {
            throw new UnexpectedException("Unknown type name: " + typeName);
        }
        if(config.isEnum) {
            if(Config.JAVA_USE_ENUM) {
                return "E" + config.javaTypeName;
            }
            else {
                return getMapKeyTypeName(config.enumValueTypeName);
            }
        }
        else {
            return config.javaTypeName;
        }
    }

    private static Map<String, String> javaMapKeyMap = new TreeMap<>();
    static {
        javaMapKeyMap.put("int", "Integer");
        javaMapKeyMap.put("long", "Long");
        javaMapKeyMap.put("float", "Float");
        javaMapKeyMap.put("double", "Double");
        javaMapKeyMap.put("string", "String");
        javaMapKeyMap.put("boolean", "Boolean");
    }

    public static String getMapKeyTypeName(String typeName) throws UnexpectedException {
        TypeInfo config = javaTypeMap.get(typeName);
        if(config != null && config.isEnum) {
            if(Config.JAVA_USE_ENUM) {
                return "E" + typeName;
            }
            else {
                return getMapKeyTypeName(config.enumValueTypeName);
            }
        }
        else {
            String ret = javaMapKeyMap.get(typeName);
            if(ret == null || ret.isEmpty()) {
                throw new UnexpectedException("Unknown type name: " + typeName);
            }
            return ret;
        }
    }

    public static String getMapValueTypeName(String typeName) throws UnexpectedException {
        TypeInfo config = javaTypeMap.get(typeName);
        if(config != null && config.isEnum) {
            if(Config.JAVA_USE_ENUM) {
                return "E" + typeName;
            }
            else {
                return getMapValueTypeName(config.enumValueTypeName);
            }
        }
        else {
            String ret = javaMapKeyMap.get(typeName);
            if(ret == null || ret.isEmpty()) {
                throw new UnexpectedException("Unknown type name: " + typeName);
            }
            return ret;
        }
    }

    private static Map<String, String> javaJsonGetMethodMap = new TreeMap<>();
    static {
        javaJsonGetMethodMap.put("int", "getInt");
        javaJsonGetMethodMap.put("long", "getLong");
        javaJsonGetMethodMap.put("string", "getString");
        javaJsonGetMethodMap.put("float", "getDouble");
        javaJsonGetMethodMap.put("double", "getDouble");
        javaJsonGetMethodMap.put("bool", "getBoolean");
        javaJsonGetMethodMap.put("json", "'getJSONObject'");
    }

    public static String getJsonGetMethodName(String typeName) throws UnexpectedException {
        String methodName = javaJsonGetMethodMap.get(typeName);
        if(methodName == null || methodName.isEmpty()) {
            throw new UnexpectedException("Unknown type name: " + typeName);
        }
        return methodName;
    }
}
