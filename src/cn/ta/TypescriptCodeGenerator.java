package cn.ta;

import com.strobel.core.StringUtilities;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;

public class TypescriptCodeGenerator extends BaseCodeGenerator {
	String serverJsonOutputDir;
	String serverCodeOutputDir;
	String serverCodeInfoBaseOutputDir;
	String serverCodeInfoSubOutputDir;
	String serverCodeConstOutputDir;

	ClaszMgr serverClaszMgr;
	SheetMgr serverSheetMgr;

	static final boolean CHANGE_PROTOTYPE_MODE = true;

	public TypescriptCodeGenerator(ClaszMgr serverClaszMgr, SheetMgr serverSheetMgr) {
		this.serverClaszMgr = serverClaszMgr;
		this.serverSheetMgr = serverSheetMgr;
	}

	public void initDirs(String inputDir, TypescriptConfig config) throws IOException {
		this.inputDir = inputDir;
		this.serverJsonOutputDir = config.serverJsonOutputDir;
		this.serverCodeOutputDir = config.serverCodeOutputDir;
		this.serverCodeInfoBaseOutputDir = this.serverCodeOutputDir + "info/";
		this.serverCodeInfoSubOutputDir = this.serverCodeOutputDir + "info/";
		this.serverCodeConstOutputDir = this.serverCodeOutputDir + "info/";
	}

	@Override
	public void cleanDirs() throws IOException {
		System.out.println("=== cleaning server files ");
		for(String jsonFile: SUtils.listAllFiles(serverJsonOutputDir, false, null, new String[]{"\\.json$"})) {
			System.out.println("  deleting file: " + jsonFile);
			SUtils.deleteFile(jsonFile);
		}
	}

	@Override
	public void saveAllFiles() throws IOException {
		List<String> constTypeNames = saveAllConstFiles();

		// 生成JSON数据
		System.out.println("saving Typescript Json");
		String jsonOutputDir = serverJsonOutputDir;
		JSONObject json = new JSONObject();
		for(BookInfo bookInfo: serverSheetMgr.allBooks.values()) {
			SheetInfo sheet = bookInfo.primarySheet;
			if(!sheet.clasz.genColForClient) continue;
			if(isDynamicActivitySheet(sheet)) {
				continue;
			}
			json.put(sheet.name, bookInfo.getJsonValue(false));
		}
		String outputFile = jsonOutputDir + "InfoData.json";
		SUtils.makeDirAll(outputFile);
		SUtils.saveFile(outputFile, SUtils.encodeJson(json, 1));

		saveAllClassFiles(constTypeNames);
	}

	private List<String> saveAllConstFiles() throws IOException {
		List<String> typeNames = new LinkedList<>();

		List<String> lines = new LinkedList<>();

		lines.add("// Tools generated, do not MODIFY!!!");
		lines.add("");

		for(ModuleInfo module: serverClaszMgr.allModules.values()) {
			for(EnumSheetInfo sheet: module.enumSheets.values()) {
				if(sheet.clasz.genColForClient) {
					typeNames.add(saveEnumSheetScriptCode(lines, sheet));
				}
			}
		}

		for(ModuleInfo module: serverClaszMgr.allModules.values()) {
			for(ConstSheetInfo sheet: module.constSheets.values()) {
				if(sheet.clasz.genColForClient) {
					typeNames.add(saveConstSheetScriptCode(lines, sheet));
				}
			}

			for(HashSheetInfo sheet: module.hashSheets.values()) {
				if(sheet.clasz.genColForClient) {
					typeNames.add(saveHashSheetScriptCode(lines, sheet));
				}
			}
		}

		String outputDir = serverCodeConstOutputDir;
		String outputFile = outputDir + "AllConsts.ts";
		SUtils.makeDirAll(outputFile);
		SUtils.saveFile(outputFile, lines);

		return typeNames;
	}

	String saveEnumSheetScriptCode(List<String> lines, EnumSheetInfo sheet) throws IOException {
		FieldInfo nameField = sheet.clasz.fields.get(0);
		FieldInfo valueField = sheet.clasz.fields.get(1);
		FieldInfo commentField = sheet.clasz.fields.get(sheet.clasz.fields.size() - 1);

		String enumClassName = TypescriptUtils.getScriptedEnumClassName(sheet.name);

		if(nameField.name.equals("ename") && valueField.name.equals("value") && commentField.name.equals("comment")) {
			lines.add("export enum " + enumClassName + " {");
			for (int i = 0; i < sheet.targetValue.length(); ++i) {
				JSONObject json = sheet.targetValue.getJSONObject(i);
				String keyName = json.getString(nameField.name);
				String scriptedValue = castJsonValueToScript(json.get(valueField.name), "    ", valueField);
				String comment = json.getString(commentField.name);
				lines.add("    " + keyName + " = " + scriptedValue + ", // " + comment);
			}
			lines.add("}");
			lines.add("");
		}
		else {
			throw new UnexpectedException("impossible");
		}

		if(sheet.clasz.fields.size() > 3) {
			lines.add("export module " + enumClassName + " {");
			lines.add("    export type ExtInfo = {");

			// 成员声明
			for (int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
				FieldInfo field = sheet.clasz.fields.get(j);
				if(field.genColForClient) {
					String line = field.name + ": " + TypescriptUtils.getScriptedTypeName(field.typeName);
					if (field.desc.length() > 0) {
						line += " // " + field.desc;
					}
					line += ",";
					lines.add("        " + line);
				}
			}
			lines.add("    }");
			lines.add("");

			String leftScriptedTypeName = getMapKeyTypeName(valueField);
			lines.add("    export const extInfos: {[key: " + leftScriptedTypeName + "]: ExtInfo} = {");
			for (int i = 0; i < sheet.targetValue.length(); ++i) {
				JSONObject json = sheet.targetValue.getJSONObject(i);
				boolean needSkip = json.getBoolean("__needSkip");
				if(!needSkip) {
					String scriptedString = castJsonValueToScript(json.get(valueField.name), "    ", valueField);
					lines.add("        " + scriptedString + ": {");
					for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
						FieldInfo field = sheet.clasz.fields.get(j);
						if(field.genColForClient) {
							String scriptedValueString = castJsonValueToScript(json.get(field.name), "    ", field);
							lines.add("            " + field.name + ": " + scriptedValueString + ",");
						}
					}
					lines.add("        },");
				}
			}
			lines.add("    }");
			lines.add("}");
			lines.add("");
		}

		return enumClassName;
	}

	private String saveConstSheetScriptCode(List<String> lines, ConstSheetInfo sheet) throws IOException {
		lines.add("export const " + sheet.name + " = {");
		for(FieldInfo field: sheet.clasz.fields) {
			String value;
			if(field.typeName.equals("int")) {
				value = sheet.targetValue.getInt(field.name) + "";
			}
			else if(field.typeName.equals("long")) {
				value = sheet.targetValue.getLong(field.name) + "";
			}
			else if(field.typeName.equals("float") || field.typeName.equals("double")) {
				value = sheet.targetValue.getDouble(field.name) + "";
			}
			else if(field.typeName.equals("bool")) {
				value = sheet.targetValue.getBoolean(field.name) + "";
			}
			else if(field.typeName.equals("string")) {
				value = "\"" + escapeStringForScript(sheet.targetValue.getString(field.name)) + "\"";
			}
			else {
				throw new UnexpectedException("fatal error: " + field.typeName);
			}
			lines.add("    " + field.name + ": " + value + ", // " + field.desc);
		}
		lines.add("};");
		lines.add("");

		return sheet.name;
	}

	private String saveHashSheetScriptCode(List<String> lines, HashSheetInfo sheet) throws IOException {
		FieldInfo leftField = sheet.clasz.fields.get(0);
		FieldInfo rightField = sheet.clasz.fields.get(1);
		FieldInfo commentField = sheet.clasz.fields.get(2);
		String leftTypeName = getMapKeyTypeName(leftField);
		if(leftTypeName.equals("string") == false) leftTypeName = "number";
		String rightTypeName = getMapValueTypeName(rightField);
		lines.add("export const " + sheet.name + ": {[key: " + leftTypeName + "]: " + rightTypeName + "} = {");
		for(int i = 0; i < sheet.targetValue.length(); ++i) {
			JSONObject value = sheet.targetValue.getJSONObject(i);
			String leftValue;
			if(leftField.typeName.equals("int")) {
				leftValue = value.getInt(leftField.name) + "";
			}
			else if(leftField.typeName.equals("long")) {
				leftValue = value.getLong(leftField.name) + "";
			}
			else if(leftField.typeName.equals("bool")) {
				leftValue = value.getBoolean(leftField.name) + "";
			}
			else if(leftField.typeName.equals("string")) {
				leftValue = "\"" + value.getString(leftField.name) + "\"";
			}
			else if(TypescriptUtils.isEnumType(leftField.typeName)) {
				leftValue = TypescriptUtils.getScriptedEnumClassName(leftField.typeName) + "." + TypescriptUtils.getEnumStrValue(leftField.typeName, value.getInt(leftField.name));
			}
			else {
				throw new UnexpectedException("fatal error");
			}

			String rightValue;
			if(rightField.typeName.equals("int")) {
				rightValue = value.getInt(rightField.name) + "";
			}
			else if(rightField.typeName.equals("long")) {
				rightValue = value.getLong(rightField.name) + "";
			}
			else if(rightField.typeName.equals("bool")) {
				rightValue = value.getBoolean(rightField.name) + "";
			}
			else if(rightField.typeName.equals("string")) {
				rightValue = "\"" + value.getString(rightField.name) + "\"";
			}
			else if(TypescriptUtils.isEnumType(rightField.typeName)) {
				rightValue = TypescriptUtils.getScriptedEnumClassName(rightField.typeName) + "." + TypescriptUtils.getEnumStrValue(rightField.typeName, value.getInt(rightField.name));
			}
			else {
				throw new UnexpectedException("fatal error");
			}

			lines.add("    [" + leftValue + "]: " + rightValue + ",");
		}

		lines.add("};");
		lines.add("");

		return sheet.name;
	}

	private Set<ClaszInfo> claszSaved = new HashSet<>();

	public void saveAllClassFiles(List<String> constTypeNames) throws IOException {
		List<String> lines = new LinkedList<>();
		lines.add("// Tools generated, do not MODIFY!!!");
		lines.add("");
		lines.add("import { assert } from \"cc\";");
		lines.add("import { " + StringUtils.join(constTypeNames,", ") + " } from \"./AllConsts\";");

		Set<ClaszInfo> allClaszs = new LinkedHashSet<>();
		for(BookInfo book: serverSheetMgr.allBooks.values()) {
			for (SheetInfo sheet : book.sheetsList) {
				LinkedList<ClaszInfo> stack = new LinkedList<>();
				for(ClaszInfo c = sheet.clasz; c != null; c = c.baseClasz) {
					stack.addFirst(c);
				}
				for(ClaszInfo c: stack) {
					if(!allClaszs.contains(c)) {
						allClaszs.add(c);
					}
				}
			}
		}
		for(ClaszInfo clasz: allClaszs) {
			saveScriptClassFile(lines, clasz);
		}

		saveInfoDataScriptClassFile(lines);

		String outputDir = serverCodeInfoBaseOutputDir;
		String outputFile = outputDir + "InfoDataBase.ts";
		SUtils.makeDirAll(outputFile);
		SUtils.saveFile(outputFile, lines);
	}

	private void saveScriptClassFile(List<String> lines, ClaszInfo clasz) throws UnexpectedException {
		if(!clasz.genColForClient) {
			System.out.println("    skipping base class " + clasz.name);
			return;
		}

		/*
		if(claszSaved.contains(clasz)) {
			return;
		}
		else {
			claszSaved.add(clasz);
		}
		 */

		System.out.println("    processing class " + clasz.name);
		String claszName = clasz.name;

		boolean isAbstract = !clasz.name.equals("InfoDataBase");
		String baseClaszName = "";
		if (clasz.baseClasz != null) {
			baseClaszName = clasz.baseClasz.name;
		}

		String classWord = isAbstract ? "class" : "class";

		if (clasz.baseClasz != null) {
			lines.add("export " + classWord + " " + claszName + " extends " + baseClaszName + " {");
		} else {
			lines.add("export " + classWord + " " + claszName + " {");
		}

		// 成员变量
		ClaszInfo ownerObjClasz = clasz.getOwnerObjClasz(false);
		if(ownerObjClasz != null) {
			lines.add("    public ownerObj: " + ownerObjClasz.name + " = null!;");
		}
		for(FieldInfo field: clasz.fields) {
			if(field.onwingClasz != clasz) {
				if(field.onwingClasz.name.equals(clasz.name)) {
					throw new UnexpectedException("fatal error");
				}
				continue;
			}

			if(!field.genColForClient) continue;

			String colName = field.name;
			String colType = field.typeName;

			if(field.desc.isEmpty() == false) {
				lines.add("    // " + field.desc.trim().replaceAll("[\r\n]+", "; "));
			}
			if (field.isTargetClasz) {
				ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
				String childSheetName = field.name; // childSheet的名称应该和field.name一样
				if(field.isMultiObject()) {
					addArrayMemberDeclCodeForScript(lines, childSheetName, targetClasz);
				}
				else {
					addObjectMemberDeclCodeForScript(lines, childSheetName, targetClasz);
				}
			}
			else if(field.isRefObject()) {
				ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
				String scriptedTypeName = TypescriptUtils.getScriptedTypeName(refColumn.field.typeName);
				String objTypeName = refColumn.parentSheet.clasz.name;
				if(field.isArray()) {
					scriptedTypeName = scriptedTypeName + "[]";
					objTypeName = "" + objTypeName + "[]";
					lines.add("    public " + "__" + colName + ": " + scriptedTypeName + " = [];");
					lines.add("    public " + colName + ": " + objTypeName + " = null!;");
				}
				else {
					lines.add("    public " + "__" + colName + ": " + scriptedTypeName + " = null!;");
					lines.add("    public " + colName + ": " + objTypeName + " = null!;");
				}
			}
			else {
				String scriptedTypeName = TypescriptUtils.getScriptedTypeName(colType);
				lines.add("    public " + colName + ": " + scriptedTypeName + " = null!;");
			}
		}
		lines.add("");

		// 从JSON解析
		if (clasz.baseClasz != null) {
			lines.add("    public override decodeJson(xJson: Record<string, any>): void {");
			lines.add("        super.decodeJson(xJson);");
		}
		else {
			lines.add("    public decodeJson(xJson: Record<string, any>): void {");
		}
		for (FieldInfo field : clasz.fields) {
			if(!field.genColForClient) continue;
			String fieldName = field.name;
			String fieldType = field.typeName;
			String jsonFieldName = field.name;
			if(field.isRefObject()) {
				if(!field.genColForClient) continue;
				ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(fieldType);
				if(field.isArray()) {
					if(refColumn.field.typeName.equals("int")) {
						if(!CHANGE_PROTOTYPE_MODE) {
							array1Parse(lines, "__" + fieldName, jsonFieldName, "int");
						}
						else {
							lines.add("        this.__" + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
							lines.add("        delete (this as any)." + fieldName + ";");
						}
					}
					else if(refColumn.field.typeName.equals("long")) {
						if(!CHANGE_PROTOTYPE_MODE) {
							array1Parse(lines, "__" + fieldName, jsonFieldName, "long");
						}
						else {
							lines.add("        this.__" + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
							lines.add("        delete (this as any)." + fieldName + ";");
						}
					}
					else if(refColumn.field.typeName.equals("string")) {
						if(!CHANGE_PROTOTYPE_MODE) {
							array1Parse(lines, "__" + fieldName, jsonFieldName, "string");
						}
						else {
							lines.add("        this.__" + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
							lines.add("        delete (this as any)." + fieldName + ";");
						}
					}
					else {
						throw new UnexpectedException(fieldType + " not support");
					}
				}
				else {
					if(refColumn.field.typeName.equals("int") || refColumn.field.typeName.equals("long") || refColumn.field.typeName.equals("string")) {
						if(!CHANGE_PROTOTYPE_MODE) {
							lines.add("        this.__" + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
						}
						else {
							lines.add("        this.__" + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
							lines.add("        delete (this as any)." + fieldName + ";");
						}
					}
					else {
						throw new UnexpectedException(fieldType + " not support");
					}
				}
			}
			else if (fieldType.equals("int")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("long")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("float")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("double")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("bool")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("string") || UnrealTypes.isUETypeOrSubclassOf(fieldType)) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("json")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.startsWith("jsonArray|")) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (TypescriptUtils.isEnumType(fieldType)) {
				if(!CHANGE_PROTOTYPE_MODE) lines.add("        this." + fieldName + " = xJson[\"" + jsonFieldName + "\"];");
			} else if (fieldType.equals("int[]")) {
				if(!CHANGE_PROTOTYPE_MODE) array1Parse(lines, fieldName, jsonFieldName, "int");
			} else if (fieldType.equals("int[][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array2Parse(lines, fieldName, jsonFieldName, "int");
			} else if (fieldType.equals("int[][][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array3Parse(lines, fieldName, jsonFieldName, "int");
			} else if (fieldType.equals("long[]")) {
				if(!CHANGE_PROTOTYPE_MODE) array1Parse(lines, fieldName, jsonFieldName, "long");
			} else if (fieldType.equals("long[][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array2Parse(lines, fieldName, jsonFieldName, "long");
			} else if (fieldType.equals("long[][][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array3Parse(lines, fieldName, jsonFieldName, "long");
			} else if (fieldType.equals("float[]")) {
				if(!CHANGE_PROTOTYPE_MODE) array1Parse(lines, fieldName, jsonFieldName, "float");
			} else if (fieldType.equals("float[][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array2Parse(lines, fieldName, jsonFieldName, "float");
			} else if (fieldType.equals("float[][][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array3Parse(lines, fieldName, jsonFieldName, "float");
			} else if (fieldType.equals("double[]")) {
				if(!CHANGE_PROTOTYPE_MODE) array1Parse(lines, fieldName, jsonFieldName, "double");
			} else if (fieldType.equals("double[][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array2Parse(lines, fieldName, jsonFieldName, "double");
			} else if (fieldType.equals("double[][][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array3Parse(lines, fieldName, jsonFieldName, "double");
			} else if (fieldType.equals("bool[]")) {
				if(!CHANGE_PROTOTYPE_MODE) array1Parse(lines, fieldName, jsonFieldName, "bool");
			} else if (fieldType.equals("bool[][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array2Parse(lines, fieldName, jsonFieldName, "bool");
			} else if (fieldType.equals("bool[][][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array3Parse(lines, fieldName, jsonFieldName, "bool");
			} else if (fieldType.equals("string[]") || UnrealTypes.isUETypeOrSubclassOf1D(fieldType)) {
				if(!CHANGE_PROTOTYPE_MODE) array1Parse(lines, fieldName, jsonFieldName, "string");
			} else if (fieldType.equals("string[][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array2Parse(lines, fieldName, jsonFieldName, "string");
			} else if (fieldType.equals("string[][][]")) {
				if(!CHANGE_PROTOTYPE_MODE) array3Parse(lines, fieldName, jsonFieldName, "string");
			} else if(field.isTargetClasz) {
				ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
				if(field.isMultiObject()) {
					addArrayMemberInitCodeForScript(lines, field.name, targetClasz, jsonFieldName, clasz);
				}
				else {
					addObjectMemberInitCodeForScript(lines, field.name, targetClasz, jsonFieldName, clasz);
				}
			}
			else {
				throw new UnexpectedException(fieldType + " not support");
			}
		}
		lines.add("    }");
		lines.add("");


		lines.add("    public bindRefObjects(infoData: InfoDataBase): void {");
		if(clasz.baseClasz != null) {
			lines.add("        super.bindRefObjects(infoData);");
		}
		for (FieldInfo field : clasz.fields) {
			if(!field.genColForClient) continue;
			if(field.isRefObject()) {
				ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
				String typeName = field.typeName;
				int dimCount = field.getArrayDimCount();
				if(dimCount >= 2) {
					throw new UnexpectedException("not supported");
				}
				typeName = typeName.replaceAll("\\[\\]", "");
				boolean canNull = false;
				String canNullValue = "";
				if(typeName.contains("?")) {
					canNull = true;
					canNullValue = StringUtilities.split(typeName, '?').get(1);
					typeName = typeName.replaceAll("\\?\\d+$", "");
				}
				List<String> parts = StringUtilities.split(typeName, '.');
				canNull = true;
				if(dimCount == 0) {
					String getCode = "infoData." + typeName.replaceAll("\\.\\w+$", "");
					getCode = getCode + "Map[this.__" + field.name + "]";

					if(canNull) {
						lines.add("        if (this.__" + field.name + " && this.__" + field.name + " + '' !== '') {");
					}
					else {
						lines.add("        if (true) {");
					}
					lines.add("            this." + field.name + " = " + getCode + ";");
					lines.add("            assert(this." + field.name + " != null, \"Ref obj not found\");");
					lines.add("        }");
				}
				else if(dimCount == 1) {
					String getCode = "infoData." + typeName.replaceAll("\\.\\w+$", "");
					getCode = getCode + "Map[this.__" + field.name + "[i]]";
					lines.add("        if(this." + field.name + " == null) {");
					lines.add("            this." + field.name + " = [];");
					lines.add("            for (let i = 0; i < this.__" + field.name + ".length; ++i) {");
					lines.add("                if(!this.__" + field.name + "[i] || this.__" + field.name + "[i] + '' === '') {");
					lines.add("                    this." + field.name + ".push(null!);");
					lines.add("                }");
					lines.add("                else {");
					lines.add("                    let obj = " + getCode + ";");
					lines.add("                    assert(obj != null, \"Ref obj not found\");");
					lines.add("                    this." + field.name + ".push(obj);");
					lines.add("                }");
					lines.add("            }");
					lines.add("        }");
				}
				else {
					throw new UnexpectedException("not supported");
				}
			}

			else if(field.isTargetClasz) {
				if(field.isMulti()) {
					lines.add("        for(let obj of this." + field.name + ") {");
					lines.add("            obj.bindRefObjects(infoData);");
					lines.add("        }");
				}
				else {
					lines.add("        this." + field.name + ".bindRefObjects(infoData);");
				}
			}
		}
		lines.add("    }");
		lines.add("");

		lines.add("}");
		lines.add("");
	}

	public void saveInfoDataScriptClassFile(List<String> lines) throws IOException {
		lines.add("export class InfoDataBase {");
		lines.add("    public static readonly CHANGE_PROTOTYPE_MODE = " + CHANGE_PROTOTYPE_MODE + ";");

		// 成员变量
		for(BookInfo book: serverSheetMgr.allBooks.values()) {
			SheetInfo sheet = book.primarySheet;
			if(!sheet.clasz.genColForClient) continue;
			if(sheet.isMulti) {
				addArrayMemberDeclCodeForScript(lines, sheet.name, sheet.clasz);
			}
			else if(sheet.isObject) {
				addObjectMemberDeclCodeForScript(lines, sheet.name, sheet.clasz);
			}
			else {
				throw new UnexpectedException("not supported: " + sheet.typeName);
			}
		}
		lines.add("");

		// get函数
		for(BookInfo book: serverSheetMgr.allBooks.values()) {
			SheetInfo sheet = book.primarySheet;
			if(!sheet.clasz.genColForClient) continue;
			if(sheet.isMulti && sheet.clasz != null) {
				addArrayMemberGetCodeForScript(lines, sheet.name, sheet.clasz);
			}
			else if(sheet.clasz != null) {
				addObjectMemberGetCodeForScript(lines, sheet.name, sheet.clasz);
			}
			else {
				throw new UnexpectedException("not supported: " + sheet.typeName);
			}
		}
		lines.add("");

		// 解析函数
		for(BookInfo book: serverSheetMgr.allBooks.values()) {
			SheetInfo sheet = book.primarySheet;
			if(!sheet.clasz.genColForClient) continue;
			if(isDynamicActivitySheet(book.primarySheet)) continue;
			String loadMethodName = "load" + SUtils.ucfirst(book.primarySheet.name);
			lines.add("    protected " + loadMethodName + "(xJson: Record<string, any>): void {");
			//lines.add("        let obj: object;");
			//String jsonRootVarName = "json";
			//String jsonFileName = "./json/" + book.name + ".json";
			//lines.add("        JSONObject " + jsonRootVarName + " = Utils.loadJson(\"" + jsonFileName + "\");");
			if(sheet.isMulti && sheet.clasz != null) {
				addArrayMemberInitCodeForScript(lines, sheet.name, sheet.clasz, sheet.name, null);
			}
			else if(sheet.clasz != null) {
				addObjectMemberInitCodeForScript(lines, sheet.name, sheet.clasz, sheet.name, null);
			}
			else {
				throw new UnexpectedException("not supported: " + sheet.typeName);
			}
			lines.add("    }");
			lines.add("");
		}

		lines.add("    protected bindRefObjects(infoData: InfoDataBase): void {");
		for(BookInfo book: serverSheetMgr.allBooks.values()) {
			SheetInfo sheet = book.primarySheet;
			if(!sheet.clasz.genColForClient) continue;
			if(isDynamicActivitySheet(book.primarySheet)) continue;
			if(sheet.isMulti) {
				lines.add("        for(let obj of this." + sheet.name + ") {");
				lines.add("            obj.bindRefObjects(infoData);");
				lines.add("        }");
			}
			else if(sheet.isObject) {
				lines.add("        this." + sheet.name + ".bindRefObjects(infoData);");
			}
		}
		lines.add("    }");
		lines.add("");

		// 解析函数
		lines.add("    public loadAll(xJson: Record<string, any>): void {");

		for(BookInfo book: serverSheetMgr.allBooks.values()) {
			if(!book.primarySheet.clasz.genColForClient) continue;
			if(isDynamicActivitySheet(book.primarySheet)) continue;
			String loadMethodName = "load" + SUtils.ucfirst(book.primarySheet.name);
			lines.add("        this." + loadMethodName + "(xJson);");
			//lines.add("        this." + loadMethodName + "(xJson['" + book.primarySheet.name + "']);");
		}
		lines.add("");

		for(String sheetName: Config.aggregateItems.keySet()) {
			SheetInfo sheet = serverSheetMgr.allPrimarySheetsMap.get(sheetName);
			if(sheet.clasz.genColForClient) {
				if(isDynamicActivitySheet(sheet)) {
					continue;
				}
				for(SheetInfo aggregateChildSheet: sheet.aggregateChildSheets) {
					if(isDynamicActivitySheet(aggregateChildSheet)) {
						continue;
					}
					if(aggregateChildSheet.clasz.genColForClient) {
						addAggregateCode(lines, sheet, aggregateChildSheet);
					}
				}
			}
		}

		lines.add("        this.bindRefObjects(this);");

		lines.add("    }");
		lines.add("");


		lines.add("}");
		lines.add("");
	}

	private boolean isDynamicActivitySheet(SheetInfo sheet) {
		if(sheet.clasz.name.equals("DynamicActivityInfo")) {
			return true;
		}
		if(sheet.clasz.baseClasz != null && sheet.clasz.baseClasz.name.equals("DynamicActivityInfo")) {
			return true;
		}
		return false;
	}

	private void addAggregateCode(List<String> lines, SheetInfo aggregateParentSheet, SheetInfo aggregateChildSheet) throws UnexpectedException {
		LinkedList<SheetInfo> aggregateChildSheetStack = new LinkedList<>();
		for(SheetInfo parentSheet = aggregateChildSheet; parentSheet != null; ) {
			aggregateChildSheetStack.addFirst(parentSheet);
			if(parentSheet.parentColumn != null) {
				parentSheet = parentSheet.parentColumn.parentSheet;
			}
			else {
				parentSheet = null;
			}
		}

		String lastVarName = "this";
		String indent = "        ";
		addAggregateCodeEx(lines, indent, lastVarName, aggregateParentSheet, aggregateChildSheetStack, 0);
		lines.add("");
	}

	private void addAggregateCodeEx(List<String> lines, String indent, String lastVarName, SheetInfo aggregateParentSheet, LinkedList<SheetInfo> aggregateChildSheetStack, int stackIndex) throws UnexpectedException {
		if(stackIndex < aggregateChildSheetStack.size()) {
			SheetInfo parentSheet = aggregateChildSheetStack.get(stackIndex);
			if(parentSheet.isMulti) {
				String typeName = parentSheet.clasz.name;
				String varName = SUtils.lcfirst(typeName);
				String listName = lastVarName + "." + this.getArrayMemberVarNameOfColumnForScript(parentSheet.name, parentSheet.clasz);
				lines.add(indent + "for(let " + varName + " of " + listName + ") {");
				addAggregateCodeEx(lines, indent + "    ", varName, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
				lines.add(indent + "}");
			}
			else {
				addAggregateCodeEx(lines, indent, lastVarName + "." + parentSheet.name, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
			}
		}
		else {
			String memberName = getArrayMemberVarNameOfColumnForScript(aggregateParentSheet.name, aggregateParentSheet.clasz);
			List<FieldInfo> indexedFields = aggregateParentSheet.clasz.getIndexedFields();
			if(indexedFields.size() == 1) {
				FieldInfo field = indexedFields.get(0);
				String mapMemberName = getMapMemberVarNameOfColumnForScript(aggregateParentSheet.name, aggregateParentSheet.clasz);
				String keyGetCode = lastVarName + "." + (field.isRefObject() ? "__" : "") + field.name;
				lines.add(indent + "if(this." + mapMemberName + "[" + keyGetCode + "] == null) {");
				lines.add(indent + "    this." + mapMemberName + "[" + keyGetCode + "] = " +  lastVarName +";");
				lines.add(indent + "    this." + memberName + ".push(" + lastVarName + ");");
				lines.add(indent + "} else if(this." + mapMemberName + "[" + keyGetCode + "] != " + lastVarName + ") {");
				lines.add(indent + "    assert(false, \"Duplicated element\");");
				lines.add(indent + "}");
			}
			else {
				throw new UnexpectedException("fatal error");
			}
		}
	}

	private static String getObjectMemberTypeNameOfColumnForScript(String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		return targetClasz.name;
	}

	public static String getObjectMemberVarNameOfColumnForScript(String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		return name;
	}

	private static String getArrayMemberTypeNameOfColumnForScript(String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		return targetClasz.name + "[]";
	}

	public static String getArrayMemberVarNameOfColumnForScript(String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		return name;
	}

	private String getMapMemberTypeNameOfColumnForScript(String name, ClaszInfo targetClasz) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		return getMapMemberTypeNameOfColumnForScriptEx(name, targetClasz, 0, false);
	}

	private String getMapMemberTypeNameOfColumnForScriptEx(String name, ClaszInfo targetClasz, int firstIndex, boolean subClass) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		subClass = true;
		if(targetClasz == null) {
			throw new UnexpectedException("impossible");
		}
		else if(targetClasz.getIndexedFields().size() > 0) {
			String result = "";
			List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
			for(int i = firstIndex; i < indexedFields.size(); ++i) {
				FieldInfo indexField = indexedFields.get(i);
				if(i == firstIndex && subClass) {
					result += "{[key: ";
				}
				else {
					result += "{[key: ";
				}
				result += getMapKeyTypeName(indexField);
				result += "]: ";
				if(i == indexedFields.size() - 1) {
					if(indexField.indexType == IndexType.MultiIndex) {
						result += targetClasz.name + "[]";
					}
					else {
						result += targetClasz.name;
					}
				}
			}
			for(int i = firstIndex; i < indexedFields.size(); ++i) {
				result += "}";
			}
			return result;
		}
		else {
			throw new UnexpectedException("impossible");
		}
	}

	public static String getMapMemberVarNameOfColumnForScript(String name, ClaszInfo targetClasz) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		if(targetClasz == null) {
			throw new UnexpectedException("impossible");
		}
		else if(targetClasz.getIndexedFields().size() > 0) {
			return name + "Map";
		}
		else {
			throw new UnexpectedException("impossible");
		}
	}

	private static String getObjectMemberGetMethodNameForScript(String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		return "get" + SUtils.ucfirst(name);
	}

	private static String getArrayMemberGetMethodNameForScript(String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		return "get" + SUtils.ucfirst(name);
	}

	private static String getMapMemberGetMethodNameForScript(String name, ClaszInfo targetClasz) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		if(targetClasz == null) {
			throw new UnexpectedException("impossible");
		}
		else if(targetClasz.getIndexedFields().size() > 0) {
			return "get" + SUtils.ucfirst(name) + "Map";
		}
		else {
			throw new UnexpectedException("impossible");
		}
	}

	public static void addObjectMemberDeclCodeForScript(List<String> lines, String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		String className = getObjectMemberTypeNameOfColumnForScript(name, targetClasz);
		String memberName = getObjectMemberVarNameOfColumnForScript(name, targetClasz);
		lines.add("    public " + memberName + ": " + className + " = null!;");
	}

	public void addArrayMemberDeclCodeForScript(List<String> lines, String name, ClaszInfo targetClasz) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		if(targetClasz == null) {
			throw new UnexpectedException("impossible");
		}
		else  {
			String className = getArrayMemberTypeNameOfColumnForScript(name, targetClasz);
			String memberName = getArrayMemberVarNameOfColumnForScript(name, targetClasz);
			lines.add("    public " + memberName + ": " + className + " = [];");
			if(targetClasz.getIndexedFields().size() > 0) {
				String mapClassName = getMapMemberTypeNameOfColumnForScript(name, targetClasz);
				String mapMemberName = getMapMemberVarNameOfColumnForScript(name, targetClasz);
				lines.add("    public " + mapMemberName + ": " + mapClassName + " = {};");
			}
		}
	}

	public static void addObjectMemberInitCodeForScript(List<String> lines, String name, ClaszInfo targetClasz, String jsonFieldName, ClaszInfo ownerObjClasz) {
		assert(targetClasz.genColForClient);
		String className = getObjectMemberTypeNameOfColumnForScript(name, targetClasz);
		String memberName = getObjectMemberVarNameOfColumnForScript(name, targetClasz);
		if(!CHANGE_PROTOTYPE_MODE) {
			lines.add("        this." + memberName + " = null;");
		}
		lines.add("        if(true) {");
		lines.add("            let jsonObj = xJson[\"" + jsonFieldName + "\"];");
		if(CHANGE_PROTOTYPE_MODE) {
			lines.add("            Object.setPrototypeOf(jsonObj, " + className + ".prototype);");
		}
		else {
			lines.add("            this." + memberName + " = new " + className + "();");
		}
		lines.add("            this." + memberName + ".decodeJson(jsonObj);");
		if(ownerObjClasz != null && targetClasz.getOwnerObjClasz(true) != null) {
			assert(ownerObjClasz.genColForClient);
			lines.add("            this." + memberName + ".ownerObj = this instanceof " + ownerObjClasz.name +  " ? (this as " + ownerObjClasz.name + ") : null!;");
			lines.add("            if(this." + memberName + ".ownerObj == null) {");
			lines.add("                assert(false, \"fatal error\");");
			lines.add("            }");
		}
		lines.add("        }");
	}

	public void addArrayMemberInitCodeForScript(List<String> lines, String name, ClaszInfo targetClasz, String jsonFieldName, ClaszInfo ownerObjClasz) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		if(targetClasz == null) {
			throw new UnexpectedException("impossible");
		}
		else  {
			String className = getArrayMemberTypeNameOfColumnForScript(name, targetClasz);
			String memberName = getArrayMemberVarNameOfColumnForScript(name, targetClasz);
			if(!CHANGE_PROTOTYPE_MODE) {
				lines.add("        this." + memberName + " = [];");
			}
			String mapClassName;
			String mapMemberName;
			if(targetClasz.getIndexedFields().size() > 0) {
				mapClassName = getMapMemberTypeNameOfColumnForScript(name, targetClasz);
				mapMemberName = getMapMemberVarNameOfColumnForScript(name, targetClasz);
				lines.add("        this." + mapMemberName + " = {};");
			}
			lines.add("        if(true) {");
			lines.add("            let jsonArray = xJson[\"" + jsonFieldName + "\"];");
			lines.add("            for(let i = 0; i < jsonArray.length; ++i) {");
			lines.add("                let jsonObj = jsonArray[i];");
			if(CHANGE_PROTOTYPE_MODE) {
				lines.add("                Object.setPrototypeOf(jsonObj, " + targetClasz.name + ".prototype);");
				lines.add("                let obj: " + targetClasz.name + " = jsonObj;");
			}
			else {
				lines.add("                let obj: " + targetClasz.name + " = new " + targetClasz.name + "();");
			}
			lines.add("                obj.decodeJson(jsonObj);");
			if(!CHANGE_PROTOTYPE_MODE) {
				lines.add("                this." + memberName + ".push(obj);");
			}
			if(ownerObjClasz != null && targetClasz.getOwnerObjClasz(true) != null) {
				assert(ownerObjClasz.genColForClient);
				lines.add("                obj.ownerObj = this instanceof " + ownerObjClasz.name + " ? (this as " + ownerObjClasz.name + ") : null!;");
				lines.add("                if(obj.ownerObj == null) {");
				lines.add("                    assert(false, \"fatal error\");");
				lines.add("                }");
			}
			if(targetClasz.getIndexedFields().size() > 0) {
				addMapMemberAddElementCodeForScript(lines, name, targetClasz, "this");
			}
			lines.add("            }");
			lines.add("        }");
		}
	}

	public void addMapMemberAddElementCodeForScript(List<String> lines, String name, ClaszInfo targetClasz, String targetObj) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		String memberName = getMapMemberVarNameOfColumnForScript(name, targetClasz);
		List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
		int lastIndex = indexedFields.size() - 1;
		FieldInfo lastField = indexedFields.get(lastIndex);
		String lastValueCode = "obj." + (lastField.isRefObject() ? "__" : "") + lastField.name;
		String lastVarName = targetObj + "." + memberName;
		for(int i = 0; i < indexedFields.size() - 1; ++i) {
			FieldInfo indexedField = indexedFields.get(i);
			String indexValue = "obj." + (indexedField.isRefObject() ? "__" : "") + indexedField.name;
			String clsNameL = getMapMemberTypeNameOfColumnForScriptEx(name, targetClasz, i + 1, false);
			String clsNameR = getMapMemberTypeNameOfColumnForScriptEx(name, targetClasz, i + 1, true);
			String varNameL = "map" + (i + 1);
			String varNameR = i == 0 ? targetObj + "." + memberName : "map" + i;
			lines.add("                let " + varNameL + " = " + varNameR + "[" + indexValue + "];");
			lines.add("                if(" + varNameL + " == null) {");
			lines.add("                    " + varNameL + " = {};");
			lines.add("                    " + varNameR + "[" + indexValue + "] = " +  varNameL + ";");
			lines.add("                }");
			lastVarName = varNameL;
		}
		if(lastField.indexType == IndexType.MultiIndex) {
			String listClsNameL = "ArrayList<" + targetClasz.name + ">";
			String listClsNameR = "ArrayList<" + targetClasz.name + ">";
			lines.add("                let arr = " + lastVarName + "[" + lastValueCode + "];");
			lines.add("                if(arr == null) {");
			lines.add("                    arr = [];");
			lines.add("                    " + lastVarName + "[" + lastValueCode + "] = " +  "arr;");
			lines.add("                }");
			lines.add("                arr.push(obj);");
		}
		else if(lastField.indexType == IndexType.SingleIndex) {
//            lines.add("                if(" + lastVarName + ".get(" + lastValueCode + ") == null) {");
			lines.add("                " + lastVarName + "[" + lastValueCode + "] = " +  "obj;");
//            lines.add("                } else {");
//            lines.add("                    throw new UnexpectedException(\"Duplicated object in " + memberName + "\");");
//            lines.add("                }");
		}
		else {
			throw new UnexpectedException("impossible");
		}
	}

	public static void addObjectMemberGetCodeForScript(List<String> lines, String name, ClaszInfo targetClasz) {
		assert(targetClasz.genColForClient);
		String className = getObjectMemberTypeNameOfColumnForScript(name, targetClasz);
		String memberName = getObjectMemberVarNameOfColumnForScript(name, targetClasz);
		String getMethodName = getObjectMemberGetMethodNameForScript(name, targetClasz);
		lines.add("    public " + getMethodName + "(): " + className + " {");
		lines.add("        return this." + memberName + ";");
		lines.add("    }");
	}

	public void addArrayMemberGetCodeForScript(List<String> lines, String name, ClaszInfo targetClasz) throws UnexpectedException {
		assert(targetClasz.genColForClient);
		if(targetClasz == null) {
			throw new UnexpectedException("impossible");
		}
		else {
			String className = getArrayMemberTypeNameOfColumnForScript(name, targetClasz);
			String memberName = getArrayMemberVarNameOfColumnForScript(name, targetClasz);
			String getMethodName = getArrayMemberGetMethodNameForScript(name, targetClasz);
			lines.add("    public " + getMethodName + "(): " + className + " {");
			lines.add("        return this." + memberName + ";");
			lines.add("    }");
			if(targetClasz.getIndexedFields().size() > 0)  {
				String mapClassName = getMapMemberTypeNameOfColumnForScript(name, targetClasz);
				String mapMemberName = getMapMemberVarNameOfColumnForScript(name, targetClasz);
				String mapGetMethodName = getMapMemberGetMethodNameForScript(name, targetClasz);
				lines.add("    public " + mapGetMethodName + "(): " + mapClassName + " {");
				lines.add("        return this." + mapMemberName + ";");
				lines.add("    }");
			}
		}
	}

	public static void array1Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
		String type = TypescriptUtils.getScriptedTypeName(typeName);
		lines.add("        let " + varName + "_ = xJson[\"" + colName + "\"] as " + type +"[];");
		lines.add("        this." + varName + " = [];");
		lines.add("        for(let i = 0; i < " + varName + "_.length; ++i) {");
		lines.add("            this." + varName + ".push(" + varName + "_[i]);");
		lines.add("        }");
	}

	public static void array2Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
		String type = TypescriptUtils.getScriptedTypeName(typeName);
		lines.add("        let " + varName + "_ = xJson[\"" + colName + "\"] as " + type +"[][];");
		lines.add("        this." + varName + " = [];");
		lines.add("        for(let i = 0; i < " + varName + "_.length(); ++i) {");
		lines.add("            let " + varName + "__ = " + varName + "_[i];");
		lines.add("            this." + varName + ".push([]);");
		lines.add("            for(let j = 0; j < " + varName + "__.length; ++j) {");
		lines.add("                this." + varName + "[i].push(" + varName + "__.[j];");
		lines.add("            }");
		lines.add("        }");
	}

	public static void array3Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
		String type = TypescriptUtils.getScriptedTypeName(typeName);
		lines.add("        let " + varName + "_ = xJson[\"" + colName + "\"] as" + type +"[][][];");
		lines.add("        this." + varName + " = [];");
		lines.add("        for(let i = 0; i < " + varName + "_.length(); ++i) {");
		lines.add("            let " + varName + "__ = " + varName + "_[i];");
		lines.add("            this." + varName + ".push([]);");
		lines.add("            for(let j = 0; j < " + varName + "__.length; ++j) {");
		lines.add("                let " + varName + "___ = " + varName + "__[j];");
		lines.add("                this." + varName + "[i].push([]);");
		lines.add("                for(let k = 0; k < " + varName + "___.length; ++k) {");
		lines.add("                    this." + varName + "[i][j].push(" + varName + "___.[k];");
		lines.add("                }");
		lines.add("            }");
		lines.add("        }");
	}

	private static String escapeStringForScript(String in) {
		in = in.replaceAll("\\\\", "\\\\\\\\");
		in = in.replaceAll("\"", "\\\\\\\"");
		in = in.replaceAll("\'", "\\\\\\\'");
		in = in.replaceAll("\r?\n", "\\\\n");
		return in;
	}

	private String castJsonValueToScript(Object jsonValue, String indent, FieldInfo field) throws IOException {
		assert(field.genColForClient);
		if(field.isMultiObject()) {
			throw new UnexpectedException("fatal error");
		}
		else if(field.isTargetClasz) {
			throw new UnexpectedException("fatal error");
		}
		else {
			return castBaseValueToScript(jsonValue, indent, field.typeName);
		}
	}

	private String castBaseValueToScript(Object jsonValue, String indent, String typeName) throws UnexpectedException {
		if(typeName.endsWith("[]")) {
			throw new UnexpectedException("impossible");
		}
		else if(typeName.equals("int")) {
			if(!(jsonValue instanceof Integer)) throw new UnexpectedException("fatal error");
			String luaValue = jsonValue.toString();
			return luaValue;
		}
		else if(typeName.equals("long")) {
			if(!(jsonValue instanceof Long)) throw new UnexpectedException("fatal error");
			String luaValue = jsonValue.toString();
			return luaValue;
		}
		else if(typeName.equals("float")) {
			if(!(jsonValue instanceof Float) && !(jsonValue instanceof Integer)) {
				throw new UnexpectedException("fatal error");
			}
			String luaValue = jsonValue.toString();
			return luaValue;
		}
		else if(typeName.equals("double")) {
			if(!(jsonValue instanceof Double) && !(jsonValue instanceof Integer)) {
				throw new UnexpectedException("fatal error");
			}
			String luaValue = jsonValue.toString();
			return luaValue;
		}
		else if(typeName.equals("bool")) {
			if(!(jsonValue instanceof Boolean)) throw new UnexpectedException("fatal error");
			String luaValue = jsonValue.toString();
			return luaValue;
		}
		else if(typeName.equals("string")) {
			if(!(jsonValue instanceof String)) throw new UnexpectedException("fatal error");
			String luaValue = Q((String)jsonValue);
			return luaValue;
		}
		else if(TypescriptUtils.isEnumType(typeName)) {
			if(!(jsonValue instanceof Integer)) throw new UnexpectedException("fatal error");
			String luaValue = TypescriptUtils.getScriptedEnumClassName(typeName) + "." + TypescriptUtils.getEnumStrValue(typeName, (int)jsonValue);
			return luaValue;
		}
		else {
			throw new UnexpectedException("impossible");
		}
	}

	public String getMapKeyTypeName(FieldInfo field) throws UnexpectedException {
		assert(field.genColForClient);
		if(field.isRefObject()) {
			ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
			return getMapKeyTypeName(refColumn.field);
		}
		else {
			return TypescriptUtils.getMapKeyTypeName(field.typeName);
		}
	}

	public String getMapValueTypeName(FieldInfo field) throws UnexpectedException {
		assert(field.genColForClient);
		if(field.isRefObject()) {
			ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
			return getMapValueTypeName(refColumn.field);
		}
		else {
			return TypescriptUtils.getMapValueTypeName(field.typeName);
		}
	}
}
