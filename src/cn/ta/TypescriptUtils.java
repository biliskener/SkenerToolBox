package cn.ta;

import java.rmi.UnexpectedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class TypescriptUtils {
	private static class TypeInfo {
		String typeName;
		String scriptedTypeName;
		String defaultValue;
		boolean isEnum;
		String enumValueTypeName;
		Map<String, Integer> enumStrToIntValues = new LinkedHashMap<>();
		Map<Integer, String> enumIntToStrValues = new LinkedHashMap<>();
		public TypeInfo(String typeName, String scriptedTypeName, String defaultValue, boolean isEnum, String enumValueTypeName) {
			this.typeName = typeName;
			this.scriptedTypeName = scriptedTypeName;
			this.defaultValue = defaultValue;
			this.enumValueTypeName = enumValueTypeName;
			this.isEnum = isEnum;
		}
	}

	private static Map<String, TypeInfo> scriptedTypeMap = new TreeMap<>();

	static {
		addType("int", "number", "0");
		addType("long", "number", "0");
		addType("float", "number", "0");
		addType("double", "number", "0");
		addType("bool", "boolean", "false");
		addType("string", "string", "''");
		addType("json", "object", "null");
		addType("int[]", "number[]", "null");
		addType("int[][]", "number[][]", "null");
		addType("int[][][]", "number[][][]", "null");
		addType("long[]", "number[]", "null");
		addType("long[][]", "number[][]", "null");
		addType("long[][][]", "number[][][]", "null");
		addType("float[]", "number[]", "null");
		addType("float[][]", "number[][]", "null");
		addType("float[][][]", "number[][][]", "null");
		addType("double[]", "number[]", "null");
		addType("double[][]", "number[][]", "null");
		addType("double[][][]", "number[][][]", "null");
		addType("bool[]", "boolean[]", "null");
		addType("bool[][]", "boolean[][]", "null");
		addType("bool[][][]", "boolean[][][]", "null");
		addType("string[]", "string[]", "null");
		addType("string[][]", "string[][]", "null");
		addType("string[][][]", "string[][][]", "null");
	}

	private static void addType(String typeName, String scriptedTypeName, String defaultValue) {
		TypeInfo typeInfo = new TypeInfo(typeName, scriptedTypeName, defaultValue, false, "");
		scriptedTypeMap.put(typeInfo.typeName, typeInfo);
	}

	public static void addEnumType(String typeName, String valueTypeName) {
		assert valueTypeName.equals("int");
		TypeInfo typeInfo = new TypeInfo(typeName, typeName, "null", true, valueTypeName);
		scriptedTypeMap.put(typeInfo.typeName, typeInfo);
	}

	public static void addEnumValue(String typeName, String valueName, int valueInt, boolean addToI2S) {
		TypeInfo typeInfo = scriptedTypeMap.get(typeName);
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
			return scriptedTypeMap.get(typeName).enumStrToIntValues.get(valueName);
		}
	}

	public static String getEnumStrValue(String typeName, int valueInt) {
		return scriptedTypeMap.get(typeName).enumIntToStrValues.get(valueInt);
	}

	public static boolean isEnumType(String typeName) {
		TypeInfo config = scriptedTypeMap.get(typeName);
		return config != null && config.isEnum;
	}

	public static String getScriptedEnumClassName(String typeName) throws UnexpectedException {
		TypeInfo config = scriptedTypeMap.get(typeName);
		if(config == null) {
			throw new UnexpectedException("Unknown type name: " + typeName);
		}
		if(config.isEnum) {
			return addPrefixForEnumType(config.scriptedTypeName);
		}
		else {
			throw new UnexpectedException("need enum type");
		}
	}

	public static String getScriptedTypeName(String typeName) throws UnexpectedException {
		if(UnrealTypes.isUETypeOrSubclassOf(typeName)) {
			typeName = "string";
		}
		if(UnrealTypes.isUETypeOrSubclassOf1D(typeName)) {
			typeName = "string[]";
		}
		if(typeName.startsWith("jsonArray|")) {
			return typeName.substring("jsonArray|".length());
		}
		else {
			TypeInfo config = scriptedTypeMap.get(typeName);
			if(config == null) {
				throw new UnexpectedException("Unknown type name: " + typeName);
			}
			if(config.isEnum) {
				if(Config.TYPESCRIPT_USE_ENUM) {
					return addPrefixForEnumType(config.scriptedTypeName);
				}
				else {
					return getMapKeyTypeName(config.enumValueTypeName);
				}
			}
			else {
				return config.scriptedTypeName;
			}
		}
	}

	private static Map<String, String> scriptedMapKeyMap = new TreeMap<>();
	static {
		scriptedMapKeyMap.put("int", "number");
		scriptedMapKeyMap.put("long", "number");
		scriptedMapKeyMap.put("float", "number");
		scriptedMapKeyMap.put("double", "number");
		scriptedMapKeyMap.put("string", "string");
		scriptedMapKeyMap.put("boolean", "boolean");
	}

	public static String getMapKeyTypeName(String typeName) throws UnexpectedException {
		TypeInfo config = scriptedTypeMap.get(typeName);
		if(config != null && config.isEnum) {
			if(Config.TYPESCRIPT_USE_ENUM) {
				return addPrefixForEnumType(typeName);
			}
			else {
				return getMapKeyTypeName(config.enumValueTypeName);
			}
		}
		else {
			String ret = scriptedMapKeyMap.get(typeName);
			if(ret == null || ret.isEmpty()) {
				throw new UnexpectedException("Unknown type name: " + typeName);
			}
			return ret;
		}
	}

	public static String getMapValueTypeName(String typeName) throws UnexpectedException {
		TypeInfo config = scriptedTypeMap.get(typeName);
		if(config != null && config.isEnum) {
			if(Config.TYPESCRIPT_USE_ENUM) {
				return addPrefixForEnumType(typeName);
			}
			else {
				return getMapValueTypeName(config.enumValueTypeName);
			}
		}
		else {
			String ret = scriptedMapKeyMap.get(typeName);
			if(ret == null || ret.isEmpty()) {
				throw new UnexpectedException("Unknown type name: " + typeName);
			}
			return ret;
		}
	}

	private static String addPrefixForEnumType(String typeName) {
		if(typeName.matches("^X[A-Z].*$")) {
			return typeName;
		}
		else {
			return "E" + typeName;
		}
	}
}
