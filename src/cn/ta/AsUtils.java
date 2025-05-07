package cn.ta;

import java.rmi.UnexpectedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class AsUtils {
	public static boolean USE_U_CONTAINER = true;		// 启用U类容器
	public static boolean FORCE_U_CONTAINER = false;	// 强制1维也使用U类容器
	public static boolean USE_LOWER_CASE_STYLE = false;	// 是否使用小写风格

	public static final boolean USE_PREFIX = false;
	public static final String PREFIX = USE_PREFIX ? "As" : "";
	public static final String F_PREFIX = USE_PREFIX ? "F" : "";
	public static final String U_PREFIX = USE_PREFIX ? "U" : "";
	public static final String E_PREFIX = USE_PREFIX ? "E" : "";

	public static String toUClassName(String className) {
		return U_PREFIX + PREFIX + SUtils.ucfirst(className);
	}

	public static String toNamespaceName(String sheetName) {
		return PREFIX + sheetName;
	}

	public static String toConstMemberName(String typeName, String filedName) {
		if(USE_LOWER_CASE_STYLE) {
			return filedName;
		}
		else {
			if(typeName.equals("bool")) {
				return "b" + SUtils.ucfirst(filedName);
			}
			else {
				if(filedName.equals("Name") || filedName.equals("name")) {
					filedName = filedName + "X";
				}
				return SUtils.ucfirst(filedName);
			}
		}
	}

	public static String toClassMemberName(String typeName, String filedName) {
		if(USE_LOWER_CASE_STYLE) {
			return filedName;
		}
		else {
			if(typeName.equals("bool")) {
				return "b" + SUtils.ucfirst(filedName);
			}
			else {
				if(filedName.equals("Name") || filedName.equals("name")) {
					filedName = filedName + "X";
				}
				return SUtils.ucfirst(filedName);
			}
		}
	}

	public static String newObjectCode(ClaszInfo targetClasz) {
		return String.format("NewObject(this, %s)", AsUtils.toUClassName(targetClasz.name));
	}

	public static String newObjectCode(String UClassName) {
		return String.format("NewObject(this, %s)", UClassName);
	}

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

	private static Map<String, AsUtils.TypeInfo> javaTypeMap = new TreeMap<>();

	static {
		addType("int", "int", "0");
		addType("long", "int64", "0");
		//addType("float", "float32", "0");
		addType("float", "double", "0");
		addType("double", "double", "0");
		addType("bool", "bool", "false");
		addType("string", "FString", "''");
		//addType("UTexture2D", "UTexture2D", "nullptr");
		addType("json", "JSONObject", "null");
		addType("int[]", "TArray<int>", "null");
		addType("int[][]", "int[][]", "null");
		addType("int[][][]", "int[][][]", "null");
		addType("long[]", "TArray<int64>", "null");
		addType("long[][]", "int64[][]", "null");
		addType("long[][][]", "int64[][][]", "null");
		addType("float[]", "TArray<double>", "null");
		addType("float[][]", "double[][]", "null");
		addType("float[][][]", "double[][][]", "null");
		addType("double[]", "TArray<double>", "null");
		addType("double[][]", "double[][]", "null");
		addType("double[][][]", "double[][][]", "null");
		addType("bool[]", "TArray<bool>", "null");
		addType("bool[][]", "bool[][]", "null");
		addType("bool[][][]", "bool[][][]", "null");
		addType("string[]", "TArray<FString>", "null");
		addType("string[][]", "FString[][]", "null");
		addType("string[][][]", "FString[][][]", "null");
	}

	private static void addType(String typeName, String javaTypeName, String defaultValue) {
		AsUtils.TypeInfo typeInfo = new AsUtils.TypeInfo(typeName, javaTypeName, defaultValue, false, "");
		javaTypeMap.put(typeInfo.typeName, typeInfo);
	}

	public static void addEnumType(String typeName, String valueTypeName) {
		assert valueTypeName.equals("int");
		AsUtils.TypeInfo typeInfo = new AsUtils.TypeInfo(typeName, typeName, "null", true, valueTypeName);
		javaTypeMap.put(typeInfo.typeName, typeInfo);
	}

	public static void addEnumValue(String typeName, String valueName, int valueInt, boolean addToI2S) {
		TypeInfo typeInfo = javaTypeMap.get(typeName);
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

	public static String getScriptEnumClassName(String typeName) throws UnexpectedException {
		AsUtils.TypeInfo config = javaTypeMap.get(typeName);
		if(config == null) {
			throw new UnexpectedException("Unknown type name: " + typeName);
		}
		if(config.isEnum) {
			return E_PREFIX + PREFIX + config.javaTypeName;
		}
		else {
			throw new UnexpectedException("need enum type");
		}
	}

	public static String getScriptTypeName(String typeName) throws UnexpectedException {
		if(UnrealTypes.isUEType(typeName)) {
			return UnrealTypes.getUETypeName(typeName);
		}
		if(UnrealTypes.isSubclassOf(typeName)) {
			return typeName;
		}
		if(UnrealTypes.isUEType1D(typeName)) {
			return "TArray<" + UnrealTypes.getUETypeName(UnrealTypes.trim1D(typeName)) + ">";
		}
		if(UnrealTypes.isSubclassOf1D(typeName)) {
			return "TArray<" + UnrealTypes.trim1D(typeName) + ">";
		}
		if(typeName.equals("jsonArray|string[]")) {
			return "TArray<FString>";
		}
		if(typeName.equals("jsonArray|int[]")) {
			return "TArray<int>";
		}
		if(typeName.equals("jsonArray|float[]")) {
			return "TArray<float32>";
		}
		if(typeName.equals("jsonArray|int[][]")) {
			return "TArray<FIntArray>";
		}
		if(typeName.equals("jsonArray|[int, int][]")) {
			return "TArray<FIntArray>";
		}
		if(typeName.equals("jsonArray|boolean[]")) {
			return "TArray<bool>";
		}
		AsUtils.TypeInfo config = javaTypeMap.get(typeName);
		if(config == null) {
			throw new UnexpectedException("Unknown type name: " + typeName);
		}
		if(config.isEnum) {
			return E_PREFIX + PREFIX + config.javaTypeName;
		}
		else {
			return config.javaTypeName;
		}
	}

	private static Map<String, String> javaMapKeyMap = new TreeMap<>();
	static {
		javaMapKeyMap.put("int", "int");
		javaMapKeyMap.put("long", "int64");
		javaMapKeyMap.put("float", "float");
		javaMapKeyMap.put("double", "double");
		javaMapKeyMap.put("string", "FString");
		javaMapKeyMap.put("boolean", "bool");
	}

	public static String getMapKeyTypeName(String typeName) throws UnexpectedException {
		AsUtils.TypeInfo config = javaTypeMap.get(typeName);
		if(config != null && config.isEnum) {
			return E_PREFIX + PREFIX + typeName; //!! AS中整数转为枚举编译不过
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
		AsUtils.TypeInfo config = javaTypeMap.get(typeName);
		if(config != null && config.isEnum) {
			return E_PREFIX + PREFIX + typeName;
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
		javaJsonGetMethodMap.put("int", "XUtil::GetIntFieldValue");
		javaJsonGetMethodMap.put("long", "XUtil::GetLongFieldValue");
		javaJsonGetMethodMap.put("string", "XUtil::GetStringFieldValue");
		javaJsonGetMethodMap.put("float", "XUtil::GetFloatFieldValue");
		javaJsonGetMethodMap.put("double", "XUtil::GetDoubleFieldValue");
		javaJsonGetMethodMap.put("bool", "XUtil::GetBoolFieldValue");
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
