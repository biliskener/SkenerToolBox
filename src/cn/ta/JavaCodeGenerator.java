package cn.ta;

import cn.ta.config.JavaConfig;
import com.strobel.core.StringUtilities;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;

public class JavaCodeGenerator extends BaseCodeGenerator {
    String[] serverJsonOutputDir;
    String serverCodeOutputDir;
    String serverCodeInfoBaseOutputDir;
    String serverCodeInfoSubOutputDir;
    String serverCodeConstOutputDir;
    String serverCodeConstNamespace;
    String serverCodeInfoBaseNamespace;
    String serverCodeInfoNamespace;
    String serverCodeUtilsNamespace;

    ClaszMgr serverClaszMgr;
    SheetMgr serverSheetMgr;

    public JavaCodeGenerator(ClaszMgr serverClaszMgr, SheetMgr serverSheetMgr) {
        this.serverClaszMgr = serverClaszMgr;
        this.serverSheetMgr = serverSheetMgr;
    }

    public void initDirs(String inputDir, JavaConfig javaConfig) throws IOException {
        this.inputDir = inputDir;
        this.serverJsonOutputDir = javaConfig.serverJsonOutputDir;
        this.serverCodeOutputDir = javaConfig.serverCodeOutputDir;
        this.serverCodeInfoNamespace = javaConfig.serverCodeInfoNamespace;
        this.serverCodeUtilsNamespace = javaConfig.serverCodeUtilsNamespace;
        this.serverCodeInfoBaseNamespace = javaConfig.serverCodeInfoBaseNamespace;
        this.serverCodeConstNamespace = javaConfig.serverCodeConstNamespace;

        serverCodeInfoBaseOutputDir = serverCodeOutputDir + serverCodeInfoBaseNamespace.replaceAll("\\.", "/") + '/';
        serverCodeConstOutputDir = serverCodeOutputDir + serverCodeConstNamespace.replaceAll("\\.", "/") + "/";
        serverCodeInfoSubOutputDir = serverCodeInfoBaseOutputDir.replaceAll("base/$", "");
    }

    @Override
    public void cleanDirs() throws IOException {
        System.out.println("=== cleaning server files ");
        for(String outputDir: serverJsonOutputDir) {
            for(String jsonFile: SUtils.listAllFiles(outputDir, false, null, new String[]{"\\.json$"})) {
                System.out.println("  deleting file: " + jsonFile);
                SUtils.deleteFile(jsonFile);
            }
        }

        for(String javaFile: SUtils.listAllFiles(serverCodeConstOutputDir, false, null, new String[]{"\\.java$"})) {
            System.out.println("  deleting file: " + javaFile);
            SUtils.deleteFile(javaFile);
        }

        for(String javaFile: SUtils.listAllFiles(serverCodeInfoBaseOutputDir, false, null, new String[]{"\\.java$"})) {
            System.out.println("  deleting file: " + javaFile);
            SUtils.deleteFile(javaFile);
        }
    }

    @Override
    public void saveAllFiles() throws IOException {
        // 保存常量表
        saveAllConstJavaFiles();

        // 生成服务器数据
        System.out.println("saving Server Json");
        for (String jsonOutputDir : serverJsonOutputDir) {
            for(BookInfo bookInfo: serverSheetMgr.allBooks.values()) {
                JSONObject bookJson = new JSONObject();
                bookJson.put(bookInfo.primarySheet.name, bookInfo.getJsonValue(true));
                String outputFile = jsonOutputDir + bookInfo.name + ".json";
                SUtils.makeDirAll(outputFile);
                SUtils.saveFile(outputFile, SUtils.encodeJson(bookJson, 1));
                ClaszInfo baseClasz = bookInfo.primarySheet.clasz.baseClasz;
                if(baseClasz != null && baseClasz.name.equals("DynamicActivityInfo")) {
                    DynamicActivityExporter.exportAll(bookJson, jsonOutputDir);
                }
            }
        }

        System.out.println("saving Server Code");
        saveAllBaseJavaClassFiles();
        saveAllSubJavaClassFiles();
        saveInfoDataBaseFile();
    }

    public void saveAllConstJavaFiles() throws IOException {
        for(ModuleInfo module: serverClaszMgr.allModules.values()) {
            for(ConstSheetInfo sheet: module.constSheets.values()) {
                if(sheet.clasz.genColForServer) {
                    saveConstSheetJavaCode(sheet);
                }
            }

            for(HashSheetInfo sheet: module.hashSheets.values()) {
                if(sheet.clasz.genColForServer) {
                    saveHashSheetJavaCode(sheet);
                }
            }

            for(EnumSheetInfo sheet: module.enumSheets.values()) {
                if(sheet.clasz.genColForServer) {
                    saveEnumSheetJavaCode(sheet);
                }
            }
        }
    }

    private void saveConstSheetJavaCode(ConstSheetInfo sheet) throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");
        lines.add("package " + serverCodeConstNamespace + ";");
        lines.add("");
        lines.add("public class " + sheet.name + " {");
        for(FieldInfo field: sheet.clasz.fields) {
            String value;
            if(field.typeName.equals("int")) {
                value = sheet.targetValue.getInt(field.name) + "";
            }
            else if(field.typeName.equals("long")) {
                value = sheet.targetValue.getLong(field.name) + "L";
            }
            else if(field.typeName.equals("float") || field.typeName.equals("double")) {
                value = sheet.targetValue.getDouble(field.name) + "";
            }
            else if(field.typeName.equals("bool")) {
                value = sheet.targetValue.getBoolean(field.name) + "";
            }
            else if(field.typeName.equals("string")) {
                value = "\"" + escapeStringForJava(sheet.targetValue.getString(field.name)) + "\"";
            }
            else {
                throw new UnexpectedException("fatal error: " + field.typeName);
            }
            lines.add("    public static final " + JavaUtils.getJavaTypeName(field.typeName) + " " + field.name + " = " + value + "; // " + field.desc);
        }
        lines.add("}");
        lines.add("");

        String outputDir = serverCodeConstOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + sheet.name + ".java";
        SUtils.saveFile(outputFile, lines);
    }

    private void saveHashSheetJavaCode(HashSheetInfo sheet) throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");
        lines.add("package " + serverCodeConstNamespace + ";");
        lines.add("");
        lines.add("import java.util.*;");
        lines.add("");

        lines.add("public class " + sheet.name + " {");

        FieldInfo leftField = sheet.clasz.fields.get(0);
        FieldInfo rightField = sheet.clasz.fields.get(1);
        FieldInfo commentField = sheet.clasz.fields.get(2);
        String leftTypeName = getMapKeyTypeName(leftField);
        String rightTypeName = getMapValueTypeName(rightField);
        lines.add("    public static Map<" + leftTypeName + ", " + rightTypeName + "> VALUES = new TreeMap<>();");
        lines.add("    static {");
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
            else if(JavaUtils.isEnumType(leftField.typeName)) {
                leftValue = JavaUtils.getJavaEnumClassName(leftField.typeName) + "." + JavaUtils.getEnumStrValue(leftField.typeName, value.getInt(leftField.name));
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
            else if(JavaUtils.isEnumType(rightField.typeName)) {
                rightValue = JavaUtils.getJavaEnumClassName(rightField.typeName) + "." + JavaUtils.getEnumStrValue(rightField.typeName, value.getInt(rightField.name));
            }
            else {
                throw new UnexpectedException("fatal error");
            }

            lines.add("        VALUES.put(" + leftValue + ", " + rightValue + ");");
        }
        lines.add("    }");

        lines.add("}");
        lines.add("");

        String outputDir = serverCodeConstOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + sheet.name + ".java";
        SUtils.saveFile(outputFile, lines);
    }

    private void saveEnumSheetJavaCode(EnumSheetInfo sheet) throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");
        lines.add("package " + serverCodeConstNamespace + ";");
        lines.add("");
        lines.add("import java.util.Map;");
        lines.add("import java.util.TreeMap;");
        lines.add("");

        FieldInfo nameField = sheet.clasz.fields.get(0);
        FieldInfo valueField = sheet.clasz.fields.get(1);
        FieldInfo commentField = sheet.clasz.fields.get(sheet.clasz.fields.size() - 1);

        String enumClassName = JavaUtils.getJavaEnumClassName(sheet.name);

        String indent = "";

        lines.add(indent + "public class " + enumClassName + " {");

        if(Config.JAVA_USE_ENUM) {
            String javaValueKeyType = JavaUtils.getMapKeyTypeName(valueField.typeName);
            lines.add(indent + "    static Map<" + javaValueKeyType + ", " + enumClassName + "> MAP = new TreeMap<>();");
            lines.add("");

            String javaValueType = JavaUtils.getJavaTypeName(valueField.typeName);
            lines.add(indent + "    public static " + enumClassName + " of(" + javaValueType + " value) {");
            lines.add(indent + "        return MAP.get(value);");
            lines.add(indent + "    }");
            lines.add("");
        }

        // 写出所有的枚举值
        for (int i = 0; i < sheet.targetValue.length(); ++i) {
            JSONObject json = sheet.targetValue.getJSONObject(i);
            String keyName = json.getString(nameField.name);
            String javaType = JavaUtils.getJavaTypeName(valueField.typeName);
            String javaValue = castJsonValueToJava(json.get(valueField.name), "    ", valueField);
            String comment = json.getString(commentField.name);
            if(Config.JAVA_USE_ENUM) {
                lines.add(indent + "    public static final " + enumClassName + " " + keyName + " = MAP.computeIfAbsent(" + javaValue + ", k -> new " + enumClassName + "(" + javaValue + ")); // " + comment);
            }
            else {
                lines.add(indent + "    public static final " + javaType + " " + keyName + " = " + javaValue + "; // " + comment);
            }
        }
        lines.add("");

        if(Config.JAVA_USE_ENUM) {
            // 成员声明
            for (int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                FieldInfo field = sheet.clasz.fields.get(j);
                if(field == valueField) {
                    String line = "    public ";
                    line += JavaUtils.getJavaTypeName(field.typeName);
                    line += " " + field.name + ";";
                    if (field.desc.length() > 0) {
                        line += " // " + field.desc;
                    }
                    lines.add(indent + line);
                }
            }
            lines.add("");

            // 写入构造函数
            {
                String line = "    " + enumClassName;
                List<String> args = new LinkedList<>();
                for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                    FieldInfo field = sheet.clasz.fields.get(j);
                    if(field == valueField) {
                        String arg = JavaUtils.getJavaTypeName(field.typeName) + " " + field.name;
                        args.add(arg);
                    }
                }
                line += "(" + StringUtilities.join(", ", args) + ") {";
                lines.add(indent + line);
                for(int j = 1; j < sheet.clasz.fields.size() - 1; ++j) {
                    FieldInfo field = sheet.clasz.fields.get(j);
                    if(field == valueField) {
                        lines.add(indent + "        this." + field.name + " = " + field.name + ";");
                    }
                }
                lines.add(indent + "    }");
                lines.add("");
            }
        }

        if(sheet.clasz.fields.size() > 3) {
            indent = "    ";

            lines.add(indent + "public static class ExtInfo {");

            // 成员声明
            for (int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                FieldInfo field = sheet.clasz.fields.get(j);
                if(field.genColForServer) {
                    String line = "    public ";
                    line += JavaUtils.getJavaTypeName(field.typeName);
                    line += " " + field.name + ";";
                    if (field.desc.length() > 0) {
                        line += " // " + field.desc;
                    }
                    lines.add(indent + line);
                }
            }
            lines.add("");

            // 写入构造函数
            {
                String line = "    ExtInfo";
                List<String> args = new LinkedList<>();
                for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                    FieldInfo field = sheet.clasz.fields.get(j);
                    if(field.genColForServer) {
                        String arg = JavaUtils.getJavaTypeName(field.typeName) + " " + field.name;
                        args.add(arg);
                    }
                }
                line += "(" + StringUtilities.join(", ", args) + ") {";
                lines.add(indent + line);
                for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                    FieldInfo field = sheet.clasz.fields.get(j);
                    if(field.genColForServer) {
                        lines.add(indent + "        this." + field.name + " = " + field.name + ";");
                    }
                }
                lines.add(indent + "    }");
            }

            lines.add(indent + "}");
            lines.add("");

            indent = "";
        }

        // 写出所有的扩展信息
        if(sheet.clasz.fields.size() > 3) {
            String leftJavaTypeName = getMapKeyTypeName(valueField);
            String rightJavaTypeName = "ExtInfo";
            String baseMapTypeName = "Map<" + leftJavaTypeName + ", " + rightJavaTypeName + ">";
            String mapTypeName = "TreeMap<" + leftJavaTypeName + ", " + rightJavaTypeName + ">";
            lines.add(indent + "    public static " + baseMapTypeName + " extInfos = null;");

            lines.add(indent + "    static {");
                lines.add(indent + "        extInfos = new " + mapTypeName + "();");
                for (int i = 0; i < sheet.targetValue.length(); ++i) {
                    JSONObject json = sheet.targetValue.getJSONObject(i);
                    boolean needSkip = json.getBoolean("__needSkip");
                    if(!needSkip) {
                        String javaKeyString = castJsonValueToJava(json.get(valueField.name), "    ", valueField);
                        List<String> javaValueStrings = new LinkedList<>();
                        for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                            FieldInfo field = sheet.clasz.fields.get(j);
                            if(field.genColForServer) {
                                String javaValueString = castJsonValueToJava(json.get(field.name), "    ", field);
                                javaValueStrings.add(javaValueString);
                            }
                        }
                        String javaValueString = StringUtilities.join(", ", javaValueStrings);
                        lines.add(indent + "        extInfos.put(" + javaKeyString + ", new ExtInfo(" + javaValueString + "));");
                    }
                }
            lines.add(indent + "    }");
            lines.add("");
        }

        lines.add(indent + "}");
        lines.add("");

        String outputDir = serverCodeConstOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + enumClassName + ".java";
        SUtils.saveFile(outputFile, lines);
    }

    public void saveAllBaseJavaClassFiles() throws IOException {
        for(ClaszInfo clasz: serverClaszMgr.allClaszs.values()) {
            if(!clasz.genColForServer) {
                System.out.println("    skipping base class " + clasz.name);
                continue;
            }

            System.out.println("    processing base class " + clasz.name);
            List<String> lines = new LinkedList<>();
            String claszName = clasz.name;

            boolean isAbstract = !clasz.name.equals("InfoDataBase");
            if(isAbstract) {
                claszName = claszName + "Base";
            }
            String baseClaszName = "";
            if (clasz.baseClasz != null) {
                baseClaszName = clasz.baseClasz.name;
            }

            lines.add("// Tools generated, do not MODIFY!!!");
            lines.add("");
            lines.add("package " + serverCodeInfoBaseNamespace + ";");
            lines.add("");
            lines.add("import java.io.Serializable;");
            lines.add("import java.util.*;");
            lines.add("import java.rmi.UnexpectedException;");
            lines.add("import java.lang.String;");
            lines.add("import org.json.JSONObject;");
            lines.add("import org.json.JSONArray;");
            lines.add("import org.json.JSONException;");
            lines.add("");

            lines.add("import " + serverCodeInfoNamespace + ".*;");
            lines.add("");

            String classWord = isAbstract ? "abstract class" : "class";

            if (clasz.baseClasz != null) {
                lines.add("public " + classWord + " " + claszName + " extends " + baseClaszName + " {");
            } else {
                lines.add("public " + classWord + " " + claszName + " {");
            }

            // 成员变量
            ClaszInfo ownerObjClasz = clasz.getOwnerObjClasz(false);
            if(ownerObjClasz != null) {
                lines.add("    public " + ownerObjClasz.name + " ownerObj;");
            }
            for(FieldInfo field: clasz.fields) {
                if(field.onwingClasz != clasz) {
                    if(field.onwingClasz.name.equals(clasz.name)) {
                        throw new UnexpectedException("fatal error");
                    }
                    continue;
                }

                if(!field.genColForServer) continue;

                String colName = field.name;
                String colType = field.typeName;

                if(field.desc.isEmpty() == false) {
                    lines.add("    // " + field.desc.trim().replaceAll("[\r\n]+", "; "));
                }
                if (field.isTargetClasz) {
                    ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
                    String childSheetName = field.name; // childSheet的名称应该和field.name一样
                    if(field.isMultiObject()) {
                        addArrayMemberDeclCodeForJava(lines, childSheetName, targetClasz);
                    }
                    else {
                        addObjectMemberDeclCodeForJava(lines, childSheetName, targetClasz);
                    }
                }
                else if(field.isRefObject()) {
                    ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
                    String javaTypeName = JavaUtils.getJavaTypeName(refColumn.field.typeName);
                    String objTypeName = refColumn.parentSheet.clasz.name;
                    if(field.isArray()) {
                        javaTypeName = javaTypeName + "[]";
                        objTypeName = "List<" + objTypeName + ">";
                    }
                    lines.add("    public " + javaTypeName + " __" + colName + ";");
                    lines.add("    public " + objTypeName + " " + colName + ";");
                }
                else {
                    String javaTypeName = JavaUtils.getJavaTypeName(colType);
                    lines.add("    public " + javaTypeName + " " + colName + ";");
                }
            }
            lines.add("");

            // 构造函数
            lines.add("    public " + claszName + "() {");
            lines.add("    }");
            lines.add("");

            // 从JSON解析
            if (clasz.baseClasz != null) {
                lines.add("    @Override");
            }
            lines.add("    public void decodeJson(JSONObject json) throws JSONException, UnexpectedException {");
            if (clasz.baseClasz != null) {
                lines.add("        super.decodeJson(json);");
            }
            for (FieldInfo field : clasz.fields) {
                if(!field.genColForServer) continue;
                String fieldName = field.name;
                String fieldType = field.typeName;
                String jsonFieldName = field.name;
                if(field.isRefObject()) {
                    if(!field.genColForServer) continue;
                    ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(fieldType);
                    if(field.isArray()) {
                        if(refColumn.field.typeName.equals("int")) {
                            array1Parse(lines, "__" + fieldName, jsonFieldName, "int");
                        }
                        else if(refColumn.field.typeName.equals("long")) {
                            array1Parse(lines, "__" + fieldName, jsonFieldName, "long");
                        }
                        else if(refColumn.field.typeName.equals("string")) {
                            array1Parse(lines, "__" + fieldName, jsonFieldName, "string");
                        }
                        else {
                            throw new UnexpectedException(fieldType + " not support");
                        }
                    }
                    else {
                        if(refColumn.field.typeName.equals("int")) {
                            lines.add("        this.__" + fieldName + " = json.getInt(\"" + jsonFieldName + "\");");
                        }
                        else if(refColumn.field.typeName.equals("long")) {
                            lines.add("        this.__" + fieldName + " = json.getLong(\"" + jsonFieldName + "\");");
                        }
                        else if(refColumn.field.typeName.equals("string")) {
                            lines.add("        this.__" + fieldName + " = json.getString(\"" + jsonFieldName + "\");");
                        }
                        else {
                            throw new UnexpectedException(fieldType + " not support");
                        }
                    }
                }
                else if (fieldType.equals("int")) {
                    lines.add("        this." + fieldName + " = json.getInt(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("long")) {
                    lines.add("        this." + fieldName + " = json.getLong(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("float")) {
                    lines.add("        this." + fieldName + " = json.getDouble(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("double")) {
                    lines.add("        this." + fieldName + " = json.getDouble(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("bool")) {
                    lines.add("        this." + fieldName + " = json.getBoolean(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("string") || UnrealTypes.isUETypeOrSubclassOf(fieldType)) {
                    lines.add("        this." + fieldName + " = json.getString(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("json")) {
                    lines.add("        this." + fieldName + " = json.getJSONObject(\"" + jsonFieldName + "\");");
                } else if (JavaUtils.isEnumType(fieldType)) {
                    lines.add("        this." + fieldName + " = json.getInt(\"" + jsonFieldName + "\");");
                } else if (fieldType.equals("int[]")) {
                    array1Parse(lines, fieldName, jsonFieldName, "int");
                } else if (fieldType.equals("int[][]")) {
                    array2Parse(lines, fieldName, jsonFieldName, "int");
                } else if (fieldType.equals("int[][][]")) {
                    array3Parse(lines, fieldName, jsonFieldName, "int");
                } else if (fieldType.equals("long[]")) {
                    array1Parse(lines, fieldName, jsonFieldName, "long");
                } else if (fieldType.equals("long[][]")) {
                    array2Parse(lines, fieldName, jsonFieldName, "long");
                } else if (fieldType.equals("long[][][]")) {
                    array3Parse(lines, fieldName, jsonFieldName, "long");
                } else if (fieldType.equals("float[]")) {
                    array1Parse(lines, fieldName, jsonFieldName, "float");
                } else if (fieldType.equals("float[][]")) {
                    array2Parse(lines, fieldName, jsonFieldName, "float");
                } else if (fieldType.equals("float[][][]")) {
                    array3Parse(lines, fieldName, jsonFieldName, "float");
                } else if (fieldType.equals("double[]")) {
                    array1Parse(lines, fieldName, jsonFieldName, "double");
                } else if (fieldType.equals("double[][]")) {
                    array2Parse(lines, fieldName, jsonFieldName, "double");
                } else if (fieldType.equals("double[][][]")) {
                    array3Parse(lines, fieldName, jsonFieldName, "double");
                } else if (fieldType.equals("bool[]")) {
                    array1Parse(lines, fieldName, jsonFieldName, "bool");
                } else if (fieldType.equals("bool[][]")) {
                    array2Parse(lines, fieldName, jsonFieldName, "bool");
                } else if (fieldType.equals("bool[][][]")) {
                    array3Parse(lines, fieldName, jsonFieldName, "bool");
                } else if (fieldType.equals("string[]") || UnrealTypes.isUETypeOrSubclassOf1D(fieldType)) {
                    array1Parse(lines, fieldName, jsonFieldName, "string");
                } else if (fieldType.equals("string[][]")) {
                    array2Parse(lines, fieldName, jsonFieldName, "string");
                } else if (fieldType.equals("string[][][]")) {
                    array3Parse(lines, fieldName, jsonFieldName, "string");
                } else if(field.isTargetClasz) {
                    ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
                    if(field.isMultiObject()) {
                        addArrayMemberInitCodeForJava(lines, field.name, targetClasz, jsonFieldName, clasz);
                    }
                    else {
                        addObjectMemberInitCodeForJava(lines, field.name, targetClasz, jsonFieldName, clasz);
                    }
                }
                else {
                    throw new UnexpectedException(fieldType + " not support");
                }
            }
            lines.add("    }");
            lines.add("");

            lines.add("    public void bindRefObjects(InfoDataBase infoData) throws JSONException, UnexpectedException {");
            if(clasz.baseClasz != null) {
                lines.add("        super.bindRefObjects(infoData);");
            }
            for (FieldInfo field : clasz.fields) {
                if(!field.genColForServer) continue;
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
                    canNullValue = "0";
                    if(dimCount == 0) {
                        String getCode = "infoData." + typeName.replaceAll("\\.\\w+$", "");
                        getCode = getCode + "Map.get(this.__" + field.name + ")";

                        if(canNull) {
                            lines.add("        if (!(this.__" + field.name + "+\"\").equals(" + canNullValue + "+\"\")) {");
                        }
                        else {
                            lines.add("        if (true) {");
                        }
                        lines.add("            this." + field.name + " = " + getCode + ";");
                        lines.add("            if(this." + field.name + " == null) {");
                        lines.add("                throw new UnexpectedException(\"Ref obj not found\");");
                        lines.add("            }");
                        lines.add("        }");
                    }
                    else if(dimCount == 1) {
                        String getCode = "infoData." + typeName.replaceAll("\\.\\w+$", "");
                        getCode = getCode + "Map.get(this.__" + field.name + "[i])";
                        lines.add("        if(this." + field.name + " == null) {");
                        lines.add("            this." + field.name + " = new LinkedList<>();");
                        lines.add("            for (int i = 0; i < this.__" + field.name + ".length; ++i) {");
                        lines.add("                if((this.__" + field.name + "[i] + \"\").equals(" + canNullValue + "+\"\")) {");
                        lines.add("                    this." + field.name + ".add(null);");
                        lines.add("                }");
                        lines.add("                else {");
                        lines.add("                    " + refColumn.parentSheet.clasz.name + " obj = " + getCode + ";");
                        lines.add("                    if(obj == null) {");
                        lines.add("                        throw new UnexpectedException(\"Ref obj not found\");");
                        lines.add("                    }");
                        lines.add("                    this." + field.name + ".add(obj);");
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
                        lines.add("        for(" + field.targetClaszName + " obj: this." + field.name + ") {");
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

            lines.add("    public JSONObject encodeJson() {");
            lines.add("        return new JSONObject();");
            lines.add("    }");
            lines.add("");

            lines.add("    @Override");
            lines.add("    public String toString() {");
            lines.add("        return this.encodeJson().toString();");
            lines.add("    }");
            lines.add("");

            lines.add("}");
            lines.add("");

            String outputDir = serverCodeInfoBaseOutputDir;
            SUtils.makeDirAll(outputDir);
            String outputFile = outputDir + claszName + ".java";
            SUtils.saveFile(outputFile, lines);
        }
    }

    public void saveAllSubJavaClassFiles() throws IOException {
        for(ClaszInfo clasz: serverClaszMgr.allClaszs.values()) {
            if(!clasz.genColForServer) continue;
            boolean isAbstract = !clasz.name.equals("InfoDataBase");
            if(isAbstract) {
                System.out.println("    processing sub class " + clasz.name);
                String baseClaszName = clasz.name + "Base";
                String claszName = clasz.name;

                List<String> lines = new LinkedList<>();

                lines.add("// Tools generated, do not MODIFY!!!");
                lines.add("");
                lines.add("package " + serverCodeInfoBaseNamespace + ";");
                lines.add("");
                lines.add("import java.io.Serializable;");
                lines.add("import java.util.*;");
                lines.add("import java.rmi.UnexpectedException;");
                lines.add("import java.lang.String;");
                lines.add("import org.json.JSONObject;");
                lines.add("import org.json.JSONArray;");
                lines.add("import org.json.JSONException;");
                lines.add("");
                lines.add("import " + serverCodeInfoBaseNamespace + "." + baseClaszName + ";");
                lines.add("");

                lines.add("public class " + claszName + " extends " + baseClaszName + " {");
                /*
                // 构造函数
                lines.add("    public " + claszName + "() {");
                lines.add("        super();");
                lines.add("    }");
                lines.add("");
                 */

                lines.add("}");
                lines.add("");

                String outputDir = serverCodeInfoBaseOutputDir;
                SUtils.makeDirAll(outputDir);
                String outputFile = outputDir + claszName + ".java";
                if(SUtils.fileExists(serverCodeInfoSubOutputDir + clasz.name + ".java")) {
                    System.out.println("        skipping write file " + outputFile);
                }
                else {
                    SUtils.saveFile(outputFile, lines);
                }
            }
            else {
                System.out.println("    skipping sub class " + clasz.name);
            }
        }
    }

    public void saveInfoDataBaseFile() throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");
        lines.add("package " + serverCodeInfoBaseNamespace + ";");
        lines.add("");
        lines.add("import java.io.Serializable;");
        lines.add("import java.util.*;");
        lines.add("import org.json.JSONObject;");
        lines.add("import org.json.JSONArray;");
        lines.add("import org.json.JSONException;");
        lines.add("import java.lang.Integer;");
        lines.add("import java.rmi.UnexpectedException;");
        lines.add("import " + serverCodeUtilsNamespace + ".Utils;");
        lines.add("");

        /*
        TreeSet<String> importClaszNames = new TreeSet<>();
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            ClaszInfo targetClasz = sheet.clasz;
            if(targetClasz != null && targetClasz.subClaszes.size() == 0) {
                importClaszNames.add(targetClasz.name);
            }
        }
        if(importClaszNames.size() > 0) {
            for(String targetClaszName: importClaszNames) {
                lines.add("import " + serverCodeInfoNamespace + "." + targetClaszName + ";");
            }
            lines.add("");
        }
         */
        lines.add("import " + serverCodeInfoNamespace + ".*;");
        lines.add("");

        lines.add("public class InfoDataBase {");

        // 成员变量
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForServer) continue;
            if(sheet.isMulti) {
                addArrayMemberDeclCodeForJava(lines, sheet.name, sheet.clasz);
            }
            else if(sheet.isObject) {
                addObjectMemberDeclCodeForJava(lines, sheet.name, sheet.clasz);
            }
            else {
                throw new UnexpectedException("not supported: " + sheet.typeName);
            }
        }
        lines.add("");

        // 构造函数
        lines.add("    protected InfoDataBase() {");
        lines.add("    }");
        lines.add("");

        // get函数
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForServer) continue;
            if(sheet.isMulti && sheet.clasz != null) {
                addArrayMemberGetCodeForJava(lines, sheet.name, sheet.clasz);
            }
            else if(sheet.clasz != null) {
                addObjectMemberGetCodeForJava(lines, sheet.name, sheet.clasz);
            }
            else {
                throw new UnexpectedException("not supported: " + sheet.typeName);
            }
        }
        lines.add("");

        // 解析函数
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForServer) continue;
            String loadMethodName = "load" + SUtils.ucfirst(book.primarySheet.name);
            lines.add("    protected void " + loadMethodName + "() throws JSONException, UnexpectedException {");
            String jsonRootVarName = "json";
            String jsonFileName = "./json/" + book.name + ".json";
            lines.add("        JSONObject " + jsonRootVarName + " = Utils.loadJson(\"" + jsonFileName + "\");");
            if(sheet.isMulti && sheet.clasz != null) {
                addArrayMemberInitCodeForJava(lines, sheet.name, sheet.clasz, sheet.name, null);
            }
            else if(sheet.clasz != null) {
                addObjectMemberInitCodeForJava(lines, sheet.name, sheet.clasz, sheet.name, null);
            }
            else {
                throw new UnexpectedException("not supported: " + sheet.typeName);
            }
            lines.add("    }");
            lines.add("");
        }

        lines.add("    protected void bindRefObjects(InfoDataBase infoData) throws JSONException, UnexpectedException {");
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForServer) continue;
            if(sheet.isMulti) {
                lines.add("        for(" + sheet.clasz.name + " obj: this." + sheet.name + ") {");
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
        lines.add("    public void loadAll() throws JSONException, UnexpectedException {");

        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            if(!book.primarySheet.clasz.genColForServer) continue;
            String loadMethodName = "load" + SUtils.ucfirst(book.primarySheet.name);
            lines.add("        " + loadMethodName + "();");
        }
        lines.add("");

        for(String sheetName: Config.aggregateItems.keySet()) {
            SheetInfo sheet = serverSheetMgr.allPrimarySheetsMap.get(sheetName);
            if(!sheet.clasz.genColForServer) continue;
            for(SheetInfo aggregateChildSheet: sheet.aggregateChildSheets) {
                if(!aggregateChildSheet.clasz.genColForServer) continue;
                addAggregateCode(lines, sheet, aggregateChildSheet);
            }
        }

        lines.add("        this.bindRefObjects(this);");

        lines.add("    }");
        lines.add("");


        lines.add("}");
        lines.add("");

        String outputDir = serverCodeInfoBaseOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + "InfoDataBase.java";
        SUtils.saveFile(outputFile, lines);
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
                String listName = lastVarName + "." + this.getArrayMemberVarNameOfColumnForJava(parentSheet.name, parentSheet.clasz);
                lines.add(indent + "for(" + typeName + " " + varName + ": " + listName + ") {");
                addAggregateCodeEx(lines, indent + "    ", varName, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
                lines.add(indent + "}");
            }
            else {
                addAggregateCodeEx(lines, indent, lastVarName + "." + parentSheet.name, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
            }
        }
        else {
            String memberName = getArrayMemberVarNameOfColumnForJava(aggregateParentSheet.name, aggregateParentSheet.clasz);
            List<FieldInfo> indexedFields = aggregateParentSheet.clasz.getIndexedFields();
            if(indexedFields.size() == 1) {
                FieldInfo field = indexedFields.get(0);
                String mapMemberName = getMapMemberVarNameOfColumnForJava(aggregateParentSheet.name, aggregateParentSheet.clasz);
                String keyGetCode = lastVarName + "." + (field.isRefObject() ? "__" : "") + field.name;
                lines.add(indent + "if(this." + mapMemberName + ".get(" + keyGetCode + ") == null) {");
                lines.add(indent + "    this." + mapMemberName + ".put(" + keyGetCode + ", " +  lastVarName +");");
                lines.add(indent + "    this." + memberName + ".add(" + lastVarName + ");");
                lines.add(indent + "} else if(this." + mapMemberName + ".get(" + keyGetCode + ") != " + lastVarName + ") {");
                lines.add(indent + "    throw new UnexpectedException(\"Duplicated element\");");
                lines.add(indent + "}");
            }
            else {
                throw new UnexpectedException("fatal error");
            }
        }
    }

    private static String getObjectMemberTypeNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        return targetClasz.name;
    }

    private static String getArrayMemberTypeNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        return "ArrayList<" + targetClasz.name + ">";
    }

    private String getMapMemberTypeNameOfColumnForJava(String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForServer);
        return getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, 0, false);
    }

    private String getMapMemberTypeNameOfColumnForJavaEx(String name, ClaszInfo targetClasz, int firstIndex, boolean subClass) throws UnexpectedException {
        assert(targetClasz.genColForServer);
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
                    result += "TreeMap<";
                }
                else {
                    result += "TreeMap<";
                }
                result += getMapKeyTypeName(indexField);
                result += ", ";
                if(i == indexedFields.size() - 1) {
                    if(indexField.indexType == IndexType.MultiIndex) {
                        result += "ArrayList<" + targetClasz.name + ">";
                    }
                    else {
                        result += targetClasz.name;
                    }
                }
            }
            for(int i = firstIndex; i < indexedFields.size(); ++i) {
                result += ">";
            }
            return result;
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }


    public static String getObjectMemberVarNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        return name;
    }

    public static String getArrayMemberVarNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        return name;
    }

    public static String getMapMemberVarNameOfColumnForJava(String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForServer);
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

    private static String getObjectMemberGetMethodNameForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        return "get" + SUtils.ucfirst(name);
    }

    private static String getArrayMemberGetMethodNameForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        return "get" + SUtils.ucfirst(name);
    }

    private static String getMapMemberGetMethodNameForJava(String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForServer);
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


    public static void addObjectMemberDeclCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        String className = getObjectMemberTypeNameOfColumnForJava(name, targetClasz);
        String memberName = getObjectMemberVarNameOfColumnForJava(name, targetClasz);
        lines.add("    public " + className + " " + memberName + " = null;");
    }

    public void addArrayMemberDeclCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForServer);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else  {
            String className = getArrayMemberTypeNameOfColumnForJava(name, targetClasz);
            String memberName = getArrayMemberVarNameOfColumnForJava(name, targetClasz);
            lines.add("    public " + className + " " + memberName + " = new ArrayList<>();");
            if(targetClasz.getIndexedFields().size() > 0) {
                String mapClassName = getMapMemberTypeNameOfColumnForJava(name, targetClasz);
                String mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                lines.add("    public " + mapClassName + " " + mapMemberName + " = new TreeMap<>();");
            }
        }
    }

    public static void addObjectMemberInitCodeForJava(List<String> lines, String name, ClaszInfo targetClasz, String jsonFieldName, ClaszInfo ownerObjClasz) {
        assert(targetClasz.genColForServer);
        String className = getObjectMemberTypeNameOfColumnForJava(name, targetClasz);
        String memberName = getObjectMemberVarNameOfColumnForJava(name, targetClasz);
        lines.add("        this." + memberName + " = null;");
        lines.add("        if(true) {");
        lines.add("            JSONObject jsonObj = json.getJSONObject(\"" + jsonFieldName + "\");");
        lines.add("            this." + memberName + " = new " + className + "();");
        lines.add("            this." + memberName + ".decodeJson(jsonObj);");
        if(ownerObjClasz != null && targetClasz.getOwnerObjClasz(true) != null) {
            assert(ownerObjClasz.genColForServer);
            lines.add("            this." + memberName + ".ownerObj = (" + (ownerObjClasz.name) + ")this;");
            lines.add("            if(this." + memberName + ".ownerObj == null) {");
            lines.add("                throw new UnexpectedException(\"fatal error\");");
            lines.add("            }");
        }
        lines.add("        }");
    }

    public void addArrayMemberInitCodeForJava(List<String> lines, String name, ClaszInfo targetClasz, String jsonFieldName, ClaszInfo ownerObjClasz) throws UnexpectedException {
        assert(targetClasz.genColForServer);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else  {
            String className = getArrayMemberTypeNameOfColumnForJava(name, targetClasz);
            String memberName = getArrayMemberVarNameOfColumnForJava(name, targetClasz);
            lines.add("        this." + memberName + " = new " + className + "();");
            String mapClassName;
            String mapMemberName;
            if(targetClasz.getIndexedFields().size() > 0) {
                mapClassName = getMapMemberTypeNameOfColumnForJava(name, targetClasz);
                mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                lines.add("        this." + mapMemberName + " = new " + mapClassName + "();");
            }
            lines.add("        if(true) {");
            lines.add("            JSONArray jsonArray = json.getJSONArray(\"" + jsonFieldName + "\");");
            lines.add("            for(int i = 0; i < jsonArray.length(); ++i) {");
            lines.add("                JSONObject jsonObj = jsonArray.getJSONObject(i);");
            lines.add("                " + targetClasz.name + " obj = new " + targetClasz.name + "();");
            lines.add("                obj.decodeJson(jsonObj);");
            lines.add("                this." + memberName + ".add(obj);");
            if(ownerObjClasz != null && targetClasz.getOwnerObjClasz(true) != null) {
                assert(ownerObjClasz.genColForServer);
                lines.add("                obj.ownerObj = (" + ownerObjClasz.name + ")this;");
                lines.add("                if(obj.ownerObj == null) {");
                lines.add("                    throw new UnexpectedException(\"fatal error\");");
                lines.add("                }");
            }
            if(targetClasz.getIndexedFields().size() > 0) {
                addMapMemberAddElementCodeForJava(lines, name, targetClasz, "this");
            }
            lines.add("            }");
            lines.add("        }");
        }
    }

    public void addMapMemberAddElementCodeForJava(List<String> lines, String name, ClaszInfo targetClasz, String targetObj) throws UnexpectedException {
        assert(targetClasz.genColForServer);
        String memberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
        List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
        int lastIndex = indexedFields.size() - 1;
        FieldInfo lastField = indexedFields.get(lastIndex);
        String lastValueCode = "obj." + (lastField.isRefObject() ? "__" : "") + lastField.name;
        String lastVarName = targetObj + "." + memberName;
        for(int i = 0; i < indexedFields.size() - 1; ++i) {
            FieldInfo indexedField = indexedFields.get(i);
            String indexValue = "obj." + (indexedField.isRefObject() ? "__" : "") + indexedField.name;
            String clsNameL = getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, i + 1, false);
            String clsNameR = getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, i + 1, true);
            String varNameL = "map" + (i + 1);
            String varNameR = i == 0 ? targetObj + "." + memberName : "map" + i;
            lines.add("                " + clsNameL + " " + varNameL + " = " + varNameR + ".get(" + indexValue + ");");
            lines.add("                if(" + varNameL + " == null) {");
            lines.add("                    " + varNameL + " = new " + clsNameR + "();");
            lines.add("                    " + varNameR + ".put(" + indexValue + ", " +  varNameL + ");");
            lines.add("                }");
            lastVarName = varNameL;
        }
        if(lastField.indexType == IndexType.MultiIndex) {
            String listClsNameL = "ArrayList<" + targetClasz.name + ">";
            String listClsNameR = "ArrayList<" + targetClasz.name + ">";
            lines.add("                " + listClsNameL + " arr = " + lastVarName + ".get(" + lastValueCode + ");");
            lines.add("                if(arr == null) {");
            lines.add("                    arr = new " + listClsNameR + "();");
            lines.add("                    " + lastVarName + ".put(" + lastValueCode + ", " +  "arr);");
            lines.add("                }");
            lines.add("                arr.add(obj);");
        }
        else if(lastField.indexType == IndexType.SingleIndex) {
//            lines.add("                if(" + lastVarName + ".get(" + lastValueCode + ") == null) {");
            lines.add("                " + lastVarName + ".put(" + lastValueCode + ", " +  "obj);");
//            lines.add("                } else {");
//            lines.add("                    throw new UnexpectedException(\"Duplicated object in " + memberName + "\");");
//            lines.add("                }");
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }


    public static void addObjectMemberGetCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForServer);
        String className = getObjectMemberTypeNameOfColumnForJava(name, targetClasz);
        String memberName = getObjectMemberVarNameOfColumnForJava(name, targetClasz);
        String getMethodName = getObjectMemberGetMethodNameForJava(name, targetClasz);
        lines.add("    public " + className + " " + getMethodName + "() {");
        lines.add("        return this." + memberName + ";");
        lines.add("    }");
    }

    public void addArrayMemberGetCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForServer);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else {
            String className = getArrayMemberTypeNameOfColumnForJava(name, targetClasz);
            String memberName = getArrayMemberVarNameOfColumnForJava(name, targetClasz);
            String getMethodName = getArrayMemberGetMethodNameForJava(name, targetClasz);
            lines.add("    public " + className + " " + getMethodName + "() {");
            lines.add("        return this." + memberName + ";");
            lines.add("    }");
            if(targetClasz.getIndexedFields().size() > 0)  {
                String mapClassName = getMapMemberTypeNameOfColumnForJava(name, targetClasz);
                String mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                String mapGetMethodName = getMapMemberGetMethodNameForJava(name, targetClasz);
                lines.add("    public " + mapClassName + " " + mapGetMethodName + "() {");
                lines.add("        return this." + mapMemberName + ";");
                lines.add("    }");
            }
        }
    }

    public static void array1Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        String get = JavaUtils.getJsonGetMethodName(typeName);
        String type = JavaUtils.getJavaTypeName(typeName);
        lines.add("        JSONArray " + varName + "_ = json.getJSONArray(\"" + colName + "\");");
        lines.add("        this." + varName + " = new " + type + "[" + varName + "_.length()];");
        lines.add("        for(int i = 0; i < " + varName + "_.length(); ++i) {");
        lines.add("            this." + varName + "[i] = " + varName + "_." + get + "(i);");
        lines.add("        }");
    }

    public static void array2Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        String get = JavaUtils.getJsonGetMethodName(typeName);
        String type = JavaUtils.getJavaTypeName(typeName);
        lines.add("        JSONArray " + varName + "_ = json.getJSONArray(\"" + colName + "\");");
        lines.add("        this." + varName + " = new " + type + "[" + varName + "_.length()][];");
        lines.add("        for(int i = 0; i < " + varName + "_.length(); ++i) {");
        lines.add("            JSONArray " + varName + "__ = " + varName + "_.getJSONArray(i);");
        lines.add("            this." + varName + "[i] = new " + type + "[" + varName + "__.length()];");
        lines.add("            for(int j = 0; j < " + varName + "__.length(); ++j) {");
        lines.add("                this." + varName + "[i][j] = " + varName + "__." + get + "(j);");
        lines.add("            }");
        lines.add("        }");
    }

    public static void array3Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        String get = JavaUtils.getJsonGetMethodName(typeName);
        String type = JavaUtils.getJavaTypeName(typeName);
        lines.add("        JSONArray " + varName + "_ = json.getJSONArray(\"" + colName + "\");");
        lines.add("        this." + varName + " = new " + type + "[" + varName + "_.length()][][];");
        lines.add("        for(int i = 0; i < " + varName + "_.length(); ++i) {");
        lines.add("            JSONArray " + varName + "__ = " + varName + "_.getJSONArray(i);");
        lines.add("            this." + varName + "[i] = new " + type + "[" + varName + "__.length()][];");
        lines.add("            for(int j = 0; j < " + varName + "__.length(); ++j) {");
        lines.add("                JSONArray " + varName + "___ = " + varName + "__.getJSONArray(j);");
        lines.add("                this." + varName + "[i][j] = new " + type + "[" + varName + "___.length()];");
        lines.add("                for(int k = 0; k < " + varName + "___.length(); ++k) {");
        lines.add("                    this." + varName + "[i][j][k] = " + varName + "___." + get + "(k);");
        lines.add("                }");
        lines.add("            }");
        lines.add("        }");
    }

    private static String escapeStringForJava(String in) {
        in = in.replaceAll("\\\\", "\\\\\\\\");
        in = in.replaceAll("\"", "\\\\\\\"");
        in = in.replaceAll("\'", "\\\\\\\'");
        in = in.replaceAll("\r?\n", "\\\\n");
        return in;
    }

    private String castJsonValueToJava(Object jsonValue, String indent, FieldInfo field) throws IOException {
        assert(field.genColForServer);
        if(field.isMultiObject()) {
            throw new UnexpectedException("fatal error");
        }
        else if(field.isTargetClasz) {
            throw new UnexpectedException("fatal error");
        }
        else {
            return castBaseValueToJava(jsonValue, indent, field.typeName);
        }
    }

    private String castBaseValueToJava(Object jsonValue, String indent, String typeName) throws UnexpectedException {
        if(typeName.endsWith("[]")) {
            /*
            String baseTypeName = typeName.substring(0, typeName.length() - 2);
            JSONArray jsonArray = (JSONArray)jsonValue;
            LinkedList<String> values = new LinkedList<>();
            for(int i = 0; i < jsonArray.length(); ++i) {
                values.add(castBaseValueToJava(jsonArray.get(i), indent, baseTypeName));
            }
            return "{" + StringUtilities.join(", ") + "}";
             */
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
        else if(JavaUtils.isEnumType(typeName)) {
            if(!(jsonValue instanceof Integer)) throw new UnexpectedException("fatal error");
            String luaValue = JavaUtils.getJavaEnumClassName(typeName) + "." + JavaUtils.getEnumStrValue(typeName, (int)jsonValue);
            return luaValue;
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    public String getMapKeyTypeName(FieldInfo field) throws UnexpectedException {
        assert(field.genColForServer);
        if(field.isRefObject()) {
            ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
            return getMapKeyTypeName(refColumn.field);
        }
        else {
            return JavaUtils.getMapKeyTypeName(field.typeName);
        }
    }

    public String getMapValueTypeName(FieldInfo field) throws UnexpectedException {
        assert(field.genColForServer);
        if(field.isRefObject()) {
            ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
            return getMapValueTypeName(refColumn.field);
        }
        else {
            return JavaUtils.getMapValueTypeName(field.typeName);
        }
    }
}
