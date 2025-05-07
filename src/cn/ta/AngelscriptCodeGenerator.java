package cn.ta;

import cn.ta.config.AngelscriptConfig;
import com.strobel.core.StringUtilities;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class AngelscriptCodeGenerator extends BaseCodeGenerator {
    String clientOutputDir;
    String clientJsonOutputDir;
    String clientCodeOutputDir;
    String clientCodeInfoBaseOutputDir;
    String clientCodeInfoSubOutputDir;
    String clientCodeConstOutputDir;

    ClaszMgr serverClaszMgr;
    SheetMgr serverSheetMgr;

    public AngelscriptCodeGenerator(ClaszMgr serverClaszMgr, SheetMgr serverSheetMgr) {
        this.serverClaszMgr = serverClaszMgr;
        this.serverSheetMgr = serverSheetMgr;
    }

    public void initDirs(String inputDir, AngelscriptConfig angelscriptConfig) throws IOException {
        AsUtils.USE_LOWER_CASE_STYLE = angelscriptConfig.useLowerCaseStyle;

        this.inputDir = inputDir;
        this.clientOutputDir = angelscriptConfig.clientOutputDir;
        this.clientJsonOutputDir = angelscriptConfig.clientJsonOutputDir;
        this.clientCodeOutputDir = angelscriptConfig.clientCodeOutputDir;

        clientCodeInfoBaseOutputDir = clientCodeOutputDir + "base/";
        clientCodeConstOutputDir = clientCodeOutputDir + "const/";
        clientCodeInfoSubOutputDir = clientCodeInfoBaseOutputDir.replaceAll("base/$", "");
    }

    @Override
    public void cleanDirs() throws IOException {
        System.out.println("=== cleaning server files ");
        for(String jsonFile: SUtils.listAllFiles(clientJsonOutputDir, false, null, new String[]{"\\.json$"})) {
            System.out.println("  deleting file: " + jsonFile);
            SUtils.deleteFile(jsonFile);
        }

        for(String javaFile: SUtils.listAllFiles(clientCodeConstOutputDir, false, null, new String[]{"\\.java$"})) {
            System.out.println("  deleting file: " + javaFile);
            SUtils.deleteFile(javaFile);
        }

        for(String javaFile: SUtils.listAllFiles(clientCodeInfoBaseOutputDir, false, null, new String[]{"\\.java$"})) {
            System.out.println("  deleting file: " + javaFile);
            SUtils.deleteFile(javaFile);
        }
    }

    @Override
    public void saveAllFiles() throws IOException {
        // 保存常量表
        saveAllConstCodeFiles();

        // 生成JSON数据
        System.out.println("saving Angelscript Json");
        for(BookInfo bookInfo: serverSheetMgr.allBooks.values()) {
            JSONObject bookJson = new JSONObject();
            bookJson.put(bookInfo.primarySheet.name, bookInfo.getJsonValue(false));
            String outputFile = clientJsonOutputDir + bookInfo.name + ".json";
            SUtils.makeDirAll(outputFile);
            SUtils.saveFile(outputFile, SUtils.encodeJson(bookJson, 1));
            ClaszInfo baseClasz = bookInfo.primarySheet.clasz.baseClasz;
            if(baseClasz != null && baseClasz.name.equals("DynamicActivityInfo")) {
                DynamicActivityExporter.exportAll(bookJson, clientJsonOutputDir);
            }
        }


        System.out.println("saving Server Code");
        saveAllBaseJavaClassFiles();
        saveAllSubJavaClassFiles();
        saveInfoDataBaseFile();
    }

    public void saveAllConstCodeFiles() throws IOException {
        for(ModuleInfo module: serverClaszMgr.allModules.values()) {
            for(ConstSheetInfo sheet: module.constSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    saveConstSheetJavaCode(sheet);
                }
            }

            for(HashSheetInfo sheet: module.hashSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    //saveHashSheetJavaCode(sheet); //!! 暂时不支持生成
                }
            }

            for(EnumSheetInfo sheet: module.enumSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    saveEnumSheetJavaCode(sheet);
                }
            }
        }
    }

    private void saveConstSheetJavaCode(ConstSheetInfo sheet) throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");
        lines.add("namespace " + AsUtils.toNamespaceName(sheet.name) + " {");
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
                value = "\"" + escapeStringForJava(sheet.targetValue.getString(field.name)) + "\"";
            }
            else {
                throw new UnexpectedException("fatal error: " + field.typeName);
            }
            lines.add("    const " + AsUtils.getScriptTypeName(field.typeName) + " " + AsUtils.toConstMemberName(field.typeName, field.name) + " = " + value + "; // " + field.desc);
        }
        lines.add("}");
        lines.add("");

        String outputDir = clientCodeConstOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + AsUtils.PREFIX + sheet.name + ".as";
        SUtils.saveFile(outputFile, lines);
    }

    private void saveHashSheetJavaCode(HashSheetInfo sheet) throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");

        lines.add("class " + sheet.name + " {");

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
            else if(AsUtils.isEnumType(leftField.typeName)) {
                leftValue = AsUtils.getScriptEnumClassName(leftField.typeName) + "." + value.getString(leftField.name);
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
            else if(AsUtils.isEnumType(rightField.typeName)) {
                rightValue = AsUtils.getScriptEnumClassName(rightField.typeName) + "." + value.getString(rightField.name);
            }
            else {
                throw new UnexpectedException("fatal error");
            }

            lines.add("        VALUES.put(" + leftValue + ", " + rightValue + ");");
        }
        lines.add("    }");

        lines.add("}");
        lines.add("");

        String outputDir = clientCodeConstOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + AsUtils.PREFIX + sheet.name + ".as";
        SUtils.saveFile(outputFile, lines);
    }

    private void saveEnumSheetJavaCode(EnumSheetInfo sheet) throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("// Tools generated, do not MODIFY!!!");
        lines.add("");

        FieldInfo nameField = sheet.clasz.fields.get(0);
        FieldInfo valueField = sheet.clasz.fields.get(1);
        FieldInfo commentField = sheet.clasz.fields.get(sheet.clasz.fields.size() - 1);

        String enumClassName = AsUtils.getScriptEnumClassName(sheet.name);

        String indent = "";

        lines.add(indent + "enum " + enumClassName + " {");

        // 写出所有的枚举值
        for(boolean needSkip: new boolean[]{false, true}) {
            for (int i = 0; i < sheet.targetValue.length(); ++i) {
                JSONObject json = sheet.targetValue.getJSONObject(i);
                boolean __needSkip = json.getBoolean("__needSkip");
                if(__needSkip == needSkip) {
                    String keyName = json.getString(nameField.name);
                    String javaType = AsUtils.getScriptTypeName(valueField.typeName);
                    String javaValue = castJsonValueToJava(json.get(valueField.name), "    ", valueField);
                    String comment = json.getString(commentField.name);
                    lines.add(indent + "    " + keyName + " = " + javaValue + ", // " + comment);
                }
            }
        }

        lines.add(indent + "}");
        lines.add("");

        if(sheet.clasz.fields.size() > 3) { //!! 暂不支持
            String extInfoClassName = AsUtils.F_PREFIX + AsUtils.toNamespaceName(sheet.name) + "ExtInfo";
            if(true) {
                //indent = "    ";
                lines.add(indent + "USTRUCT()");
                lines.add(indent + String.format("struct %s {", extInfoClassName));

                // 成员声明
                for (int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                    FieldInfo field = sheet.clasz.fields.get(j);
                    if(field.genColForClient) {
                        String line = "    ";
                        line += AsUtils.getScriptTypeName(field.typeName);
                        line += " " + AsUtils.toConstMemberName(field.typeName, field.name) + ";";
                        if (field.desc.length() > 0) {
                            line += " // " + field.desc;
                        }
                        lines.add(indent + line);
                    }
                }
                lines.add("");

                {
                    String line = "    " + extInfoClassName;
                    List<String> args = new LinkedList<>();
                    for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                        FieldInfo field = sheet.clasz.fields.get(j);
                        if(field.genColForClient) {
                            String arg = AsUtils.getScriptTypeName(field.typeName) + " " + AsUtils.toConstMemberName(field.typeName, field.name) + "_";
                            args.add(arg);
                        }
                    }
                    line += "(" + StringUtilities.join(", ", args) + ") {";
                    lines.add(indent + line);
                    for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                        FieldInfo field = sheet.clasz.fields.get(j);
                        if(field.genColForClient) {
                            lines.add(indent + "        this." + AsUtils.toConstMemberName(field.typeName, field.name) + " = " + AsUtils.toConstMemberName(field.typeName, field.name) + "_" + ";");
                        }
                    }
                    lines.add(indent + "    }");
                }

                lines.add(indent + "}");
                lines.add("");

                //indent = "";
            }

            //lines.add("namespace " + AsUtils.toNamespaceName(sheet.name) + " {");
            lines.add("namespace " + extInfoClassName + " {");

            // 写出所有的扩展信息
            if(true) {
                final boolean forceKeyToNumber = false;
                String leftJavaTypeName = forceKeyToNumber ? getMapKeyTypeName(valueField) : enumClassName;
                String rightJavaTypeName = extInfoClassName;
                String baseMapTypeName = "TMap<" + leftJavaTypeName + ", " + rightJavaTypeName + ">";
                String mapTypeName = "TMap<" + leftJavaTypeName + ", " + rightJavaTypeName + ">";

                lines.add(indent + String.format("    %s GetMap() {", mapTypeName));
                lines.add(indent + String.format("        %s ExtInfos;", mapTypeName));
                for (int i = 0; i < sheet.targetValue.length(); ++i) {
                    JSONObject json = sheet.targetValue.getJSONObject(i);
                    boolean needSkip = json.getBoolean("__needSkip");
                    if(!needSkip) {
                        String javaKeyString = forceKeyToNumber ? castJsonValueToJava(json.get(valueField.name), "    ", valueField) : enumClassName + "::" + json.getString(nameField.name);
                        List<String> javaValueStrings = new LinkedList<>();
                        for(int j = 0; j < sheet.clasz.fields.size() - 1; ++j) {
                            FieldInfo field = sheet.clasz.fields.get(j);
                            if(field.genColForClient) {
                                String javaValueString = castJsonValueToJava(json.get(field.name), "    ", field);
                                javaValueStrings.add(javaValueString);
                            }
                        }
                        String javaValueString = StringUtilities.join(", ", javaValueStrings);
                        lines.add(indent + String.format("        ExtInfos.Add(%s, %s(%s));", javaKeyString, extInfoClassName, javaValueString));
                    }
                }
                lines.add(indent + "        return ExtInfos;");
                lines.add(indent + "    }");
                lines.add("");

                //lines.add(indent + "    const " + baseMapTypeName + " _ExtInfos = __CreateExtInfoMap();");
            }

            lines.add(indent + "}");
            lines.add("");
        }

        String outputDir = clientCodeConstOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + enumClassName + ".as";
        SUtils.saveFile(outputFile, lines);
    }

    public void saveAllBaseJavaClassFiles() throws IOException {
        for(ClaszInfo clasz: serverClaszMgr.allClaszs.values()) {
            if(!clasz.genColForClient) {
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

            lines.add("UCLASS()");
            String classWord = isAbstract ? "class" : "class";

            if (clasz.baseClasz != null) {
                lines.add(classWord + " " + AsUtils.toUClassName(claszName) + ": " +  AsUtils.toUClassName(baseClaszName) + " {");
            } else {
                lines.add(classWord + " " + AsUtils.toUClassName(claszName) + " {");
            }

            // 成员变量
            ClaszInfo ownerObjClasz = clasz.getOwnerObjClasz(false);
            if(ownerObjClasz != null) {
                lines.add("    " + AsUtils.toUClassName(ownerObjClasz.name) + " OwnerObj;");
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
                        addArrayMemberDeclCodeForJava(lines, childSheetName, targetClasz);
                    }
                    else {
                        addObjectMemberDeclCodeForJava(lines, childSheetName, targetClasz);
                    }
                }
                else if(field.isRefObject()) {
                    ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
                    String javaTypeName = AsUtils.getScriptTypeName(refColumn.field.typeName);
                    String objTypeName = refColumn.parentSheet.clasz.name;
                    if(field.isArray()) {
                        javaTypeName =  "TArray<" + javaTypeName + ">";
                        objTypeName = "TArray<" + AsUtils.toUClassName(objTypeName) + ">";
                    }
                    else {
                        objTypeName = AsUtils.toUClassName(objTypeName);
                    }
                    lines.add("    " + javaTypeName + " __" + AsUtils.toClassMemberName(colType, colName) + ";");
                    lines.add("    " + objTypeName + " " + AsUtils.toClassMemberName(colType, colName) + ";");
                }
                else {
                    String javaTypeName = AsUtils.getScriptTypeName(colType);
                    lines.add("    " + javaTypeName + " " + AsUtils.toClassMemberName(colType, colName) + ";");
                }
            }
            lines.add("");

            // 从JSON解析
            lines.add(String.format("    void DecodeJson(FJsonObject& Json)%s {", clasz.baseClasz != null ? " override" : ""));
            if (clasz.baseClasz != null) {
                lines.add("        Super::DecodeJson(Json);");
            }
            for (FieldInfo field : clasz.fields) {
                if(!field.genColForClient) continue;
                String fieldName = field.name;
                String fieldType = field.typeName;
                String jsonFieldName = field.name;
                String scriptMemberName = AsUtils.toClassMemberName(fieldType, fieldName);
                if(field.isRefObject()) {
                    ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(fieldType);
                    if(field.isArray()) {
                        if(refColumn.field.typeName.equals("int")) {
                            array1Parse(lines, "__" + scriptMemberName, jsonFieldName, "int");
                        }
                        else if(refColumn.field.typeName.equals("long")) {
                            array1Parse(lines, "__" + scriptMemberName, jsonFieldName, "long");
                        }
                        else if(refColumn.field.typeName.equals("string")) {
                            array1Parse(lines, "__" + scriptMemberName, jsonFieldName, "string");
                        }
                        else {
                            throw new UnexpectedException(fieldType + " not support");
                        }
                    }
                    else {
                        if(refColumn.field.typeName.equals("int")) {
                            lines.add(String.format("        this.__%s = int(Json.GetNumberField(\"%s\"));", scriptMemberName, jsonFieldName));
                        }
                        else if(refColumn.field.typeName.equals("long")) {
                            lines.add(String.format("        this.__%s = long(Json.GetNumberField(\"%s\"));", scriptMemberName, jsonFieldName));
                        }
                        else if(refColumn.field.typeName.equals("string")) {
                            lines.add(String.format("        this.__%s = Json.GetStringField(\"%s\");", scriptMemberName, jsonFieldName));
                        }
                        else {
                            throw new UnexpectedException(fieldType + " not support");
                        }
                    }
                }
                else if (fieldType.equals("int")) {
                    lines.add(String.format("        this.%s = int(Json.GetNumberField(\"%s\"));", scriptMemberName, jsonFieldName));
                } else if (fieldType.equals("long")) {
                    lines.add(String.format("        this.%s = long(Json.GetNumberField(\"%s\"));", scriptMemberName, jsonFieldName));
                } else if (fieldType.equals("float")) {
                    lines.add(String.format("        this.%s = Json.GetNumberField(\"%s\");", scriptMemberName, jsonFieldName));
                } else if (fieldType.equals("double")) {
                    lines.add(String.format("        this.%s = Json.GetNumberField(\"%s\");", scriptMemberName, jsonFieldName));
                } else if (fieldType.equals("bool")) {
                    lines.add(String.format("        this.%s = Json.GetBoolField(\"%s\");", scriptMemberName, jsonFieldName));
                } else if (fieldType.equals("string")) {
                    lines.add(String.format("        this.%s = Json.GetStringField(\"%s\");", scriptMemberName, jsonFieldName));
                } else if (UnrealTypes.isUEType(fieldType)) {
                    String loadingCode = UnrealTypes.getLoadingCode(fieldType);
                    lines.add(String.format("        this.%s = %s(Json.GetStringField(\"%s\"));", scriptMemberName, loadingCode, jsonFieldName));
                } else if (UnrealTypes.isSubclassOf(fieldType)) {
                    String loadingCode = UnrealTypes.getLoadingCode("UClass");
                    lines.add(String.format("        this.%s = %s(Json.GetStringField(\"%s\"));", scriptMemberName, loadingCode, jsonFieldName));
                } else if (AsUtils.isEnumType(fieldType)) {
                    lines.add(String.format("        this.%s = %s(Json.GetNumberField(\"%s\"));", scriptMemberName, AsUtils.getScriptTypeName(fieldType), jsonFieldName));
                } else if (fieldType.equals("json")) {
                    lines.add(String.format("        this.%s = Json.getJSONObject(\"%s\");", scriptMemberName, jsonFieldName));
                } else if (fieldType.equals("int[]")) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, "int");
                } else if (fieldType.equals("int[][]")) {
                    array2Parse(lines, scriptMemberName, jsonFieldName, "int");
                } else if (fieldType.equals("int[][][]")) {
                    array3Parse(lines, scriptMemberName, jsonFieldName, "int");
                } else if (fieldType.equals("long[]")) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, "long");
                } else if (fieldType.equals("long[][]")) {
                    array2Parse(lines, scriptMemberName, jsonFieldName, "long");
                } else if (fieldType.equals("long[][][]")) {
                    array3Parse(lines, scriptMemberName, jsonFieldName, "long");
                } else if (fieldType.equals("float[]")) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, "float");
                } else if (fieldType.equals("float[][]")) {
                    array2Parse(lines, scriptMemberName, jsonFieldName, "float");
                } else if (fieldType.equals("float[][][]")) {
                    array3Parse(lines, scriptMemberName, jsonFieldName, "float");
                } else if (fieldType.equals("double[]")) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, "double");
                } else if (fieldType.equals("double[][]")) {
                    array2Parse(lines, scriptMemberName, jsonFieldName, "double");
                } else if (fieldType.equals("double[][][]")) {
                    array3Parse(lines, scriptMemberName, jsonFieldName, "double");
                } else if (fieldType.equals("bool[]")) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, "bool");
                } else if (fieldType.equals("bool[][]")) {
                    array2Parse(lines, scriptMemberName, jsonFieldName, "bool");
                } else if (fieldType.equals("bool[][][]")) {
                    array3Parse(lines, scriptMemberName, jsonFieldName, "bool");
                } else if (fieldType.equals("string[]")) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, "string");
                } else if (fieldType.equals("string[][]")) {
                    array2Parse(lines, scriptMemberName, jsonFieldName, "string");
                } else if (fieldType.equals("string[][][]")) {
                    array3Parse(lines, scriptMemberName, jsonFieldName, "string");
                } else if (UnrealTypes.isUEType1D(fieldType)) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, UnrealTypes.trim1D(fieldType));
                } else if (UnrealTypes.isSubclassOf1D(fieldType)) {
                    array1Parse(lines, scriptMemberName, jsonFieldName, UnrealTypes.trim1D(fieldType));
                } else if (fieldType.equals("jsonArray|string[]")) {
                    plainArray1Parse(lines, scriptMemberName, jsonFieldName, "string");
                } else if (fieldType.equals("jsonArray|int[]")) {
                    plainArray1Parse(lines, scriptMemberName, jsonFieldName, "int");
                } else if (fieldType.equals("jsonArray|float[]")) {
                    plainArray1Parse(lines, scriptMemberName, jsonFieldName, "float");
                } else if (fieldType.equals("jsonArray|int[][]")) {
                    plainArray2Parse(lines, scriptMemberName, jsonFieldName, "int");
                } else if (fieldType.equals("jsonArray|[int, int][]")) {
                    plainArray2Parse(lines, scriptMemberName, jsonFieldName, "int");
                } else if (fieldType.equals("jsonArray|boolean[]")) {
                    plainArray1Parse(lines, scriptMemberName, jsonFieldName, "bool");
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

            lines.add(String.format("    void BindRefObjects(%s InfoData)%s {", AsUtils.toUClassName("InfoDataBase") , clasz.baseClasz != null ? " override" : ""));
            if(clasz.baseClasz != null) {
                lines.add("        Super::BindRefObjects(InfoData);");
            }
            for (FieldInfo field : clasz.fields) {
                if(!field.genColForClient) continue;
                String scriptMemberName = AsUtils.toClassMemberName(field.typeName, field.name);
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

                    canNull = true; //!! 原来的代码这个地方是打开的
                    canNullValue = refColumn.field.typeName.equals("string") ? "\"\"" : "0";
                    if(dimCount == 0) {
                        String getCode = "InfoData";
                        for(int i = 0; i < parts.size() - 1; ++i) {
                            getCode += ".";
                            getCode += AsUtils.toClassMemberName("", parts.get(i)); //!! 未知类型，但一定不是bool
                        }
                        getCode = getCode + String.format("Map%s.Find(this.__%s, this.%s)", (AsUtils.FORCE_U_CONTAINER ? ".Values" : ""), scriptMemberName, scriptMemberName);
                        if(canNull) {
                            lines.add("        if (this.__" + scriptMemberName + " != " + canNullValue + ") {");
                        }
                        else {
                            lines.add("        if (true) {");
                        }
                        lines.add("            if(" + getCode + " == false) {");
                        if(!canNull) {
                            lines.add("                check(false, \"Ref obj not found\");");
                        }
                        lines.add("            }");
                        lines.add("        }");
                    }
                    else if(dimCount == 1) {
                        String getCode = "InfoData";
                        for(int i = 0; i < parts.size() - 1; ++i) {
                            getCode += ".";
                            getCode += AsUtils.toClassMemberName("", parts.get(i)); //!! 未知类型，但一定不是bool
                        }
                        getCode = getCode + String.format("Map%s.Find(this.__%s[i], Obj)", (AsUtils.FORCE_U_CONTAINER ? ".Values" : ""), scriptMemberName);
                        lines.add("        if(this." + scriptMemberName + ".Num() == 0) {");
                        lines.add(String.format("            for (int i = 0, j = this.__%s.Num(); i < j; ++i) {", scriptMemberName));
                        if(canNull) {
                            lines.add("                if (this.__" + scriptMemberName + "[i] != " + canNullValue + ") {");
                        }
                        else {
                            lines.add("                if (true) {");
                        }
                        lines.add("                    " + AsUtils.toUClassName(refColumn.parentSheet.clasz.name) + " Obj;");
                        lines.add("                    if(" + getCode + " == false) {");
                        lines.add("                        check(false, \"Ref obj not found\");");
                        lines.add("                    }");
                        lines.add("                    this." + scriptMemberName + ".Add(Obj);");
                        lines.add("                }");
                        lines.add("                else {");
                        lines.add("                    this." + scriptMemberName + ".Add(nullptr);");
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
                        lines.add("        for(" + AsUtils.toUClassName(field.targetClaszName) + " Obj: this." + scriptMemberName + ") {");
                        lines.add("            Obj.BindRefObjects(InfoData);");
                        lines.add("        }");
                    }
                    else {
                        lines.add("        this." + scriptMemberName + ".BindRefObjects(InfoData);");
                    }
                }
            }
            lines.add("    }");
            lines.add("");

            //!! 暂时用不到JSON编码与字符串编码
            /*
            lines.add("    public JSONObject encodeJson() {");
            lines.add("        return new JSONObject();");
            lines.add("    }");
            lines.add("");

            lines.add("    @Override");
            lines.add("    public String toString() {");
            lines.add("        return this.encodeJson().toString();");
            lines.add("    }");
            lines.add("");
            */

            lines.add("}");
            lines.add("");

            if(clasz.getIndexedFields().size() > 0) {
                if(!AsUtils.FORCE_U_CONTAINER && clasz.getIndexedFields().size() == 1 && clasz.getIndexedFields().get(0).indexType != IndexType.MultiIndex) {
                }
                else {
                    addMapClassDeclCodeForJava(lines, clasz);
                }
            }

            String outputDir = clientCodeInfoBaseOutputDir;
            SUtils.makeDirAll(outputDir);
            String outputFile = outputDir + AsUtils.PREFIX + claszName + ".as";
            SUtils.saveFile(outputFile, lines);
        }
    }

    public void saveAllSubJavaClassFiles() throws IOException {
        for(ClaszInfo clasz: serverClaszMgr.allClaszs.values()) {
            if(!clasz.genColForClient) continue;
            boolean isAbstract = !clasz.name.equals("InfoDataBase");
            if(isAbstract) {
                System.out.println("    processing sub class " + clasz.name);
                String baseClaszName = clasz.name + "Base";
                String claszName = clasz.name;

                List<String> lines = new LinkedList<>();

                lines.add("// Tools generated, do not MODIFY!!!");
                lines.add("");

                lines.add("UCLASS()");
                lines.add("class " + AsUtils.toUClassName(claszName) + ": " + AsUtils.toUClassName(baseClaszName) + " {");
                lines.add("}");
                lines.add("");

                String outputDir = clientCodeInfoSubOutputDir;
                SUtils.makeDirAll(outputDir);
                String outputFile = outputDir + AsUtils.PREFIX + claszName + ".as";
                if(SUtils.fileExists(outputFile)) {
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

        lines.add("UCLASS()");
        lines.add(String.format("class %s {", AsUtils.toUClassName("InfoDataBase")));

        // 成员变量
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForClient) continue;
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

        // get函数
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForClient) continue;
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
            if(!sheet.clasz.genColForClient) continue;
            String loadMethodName = "Load" + SUtils.ucfirst(book.primarySheet.name);
            lines.add("    void " + loadMethodName + "() {");
            String jsonRootVarName = "Json";
            String jsonOutputDir = this.clientJsonOutputDir.substring(this.clientJsonOutputDir.indexOf("Content/") + "Content/".length());
            String jsonFileName = jsonOutputDir + book.name + ".json";
            lines.add(String.format("        FJsonObject %s = Json::ParseString(XUtil::LoadTextFromContentDir(\"%s\"));",
                    jsonRootVarName,
                    jsonFileName
            ));
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

        lines.add(String.format("    void BindRefObjects(%s InfoData) {", AsUtils.toUClassName("InfoDataBase")));
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForClient) continue;
            if(sheet.isMulti) {
                lines.add("        for(" + AsUtils.toUClassName(sheet.clasz.name) + " Obj: this." + AsUtils.toClassMemberName(sheet.typeName, sheet.name) + ") {");
                lines.add("            Obj.BindRefObjects(InfoData);");
                lines.add("        }");
            }
            else if(sheet.isObject) {
                lines.add("        this." + AsUtils.toClassMemberName(sheet.typeName, sheet.name) + ".BindRefObjects(InfoData);");
            }
        }
        lines.add("    }");
        lines.add("");

        // 解析函数
        lines.add("    void LoadAll() {");

        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            if(!book.primarySheet.clasz.genColForClient) continue;
            String loadMethodName = "Load" + SUtils.ucfirst(book.primarySheet.name);
            lines.add("        " + loadMethodName + "();");
        }
        lines.add("");

        for(String sheetName: Config.aggregateItems.keySet()) {
            SheetInfo sheet = serverSheetMgr.allPrimarySheetsMap.get(sheetName);
            if(!sheet.clasz.genColForClient) continue;
            for(SheetInfo aggregateChildSheet: sheet.aggregateChildSheets) {
                if(!aggregateChildSheet.clasz.genColForClient) continue;
                addAggregateCode(lines, sheet, aggregateChildSheet);
            }
        }

        lines.add("        this.BindRefObjects(this);");
        lines.add("    }");
        lines.add("");

        lines.add("}");
        lines.add("");

        String outputDir = clientCodeInfoBaseOutputDir;
        SUtils.makeDirAll(outputDir);
        String outputFile = outputDir + AsUtils.PREFIX + "InfoDataBase.as";
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
                String varName = SUtils.ucfirst(typeName);
                String listName = lastVarName + "." + this.getArrayMemberVarNameOfColumnForJava(parentSheet.name, parentSheet.clasz);
                lines.add(indent + "for(" + AsUtils.toUClassName(typeName) + " " + varName + ": " + listName + ") {");
                addAggregateCodeEx(lines, indent + "    ", varName, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
                lines.add(indent + "}");
            }
            else {
                addAggregateCodeEx(lines, indent, lastVarName + "." + AsUtils.toClassMemberName(parentSheet.typeName, parentSheet.name), aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
            }
        }
        else {
            String memberName = getArrayMemberVarNameOfColumnForJava(aggregateParentSheet.name, aggregateParentSheet.clasz);
            List<FieldInfo> indexedFields = aggregateParentSheet.clasz.getIndexedFields();
            if(indexedFields.size() == 1) {
                FieldInfo field = indexedFields.get(0);
                String mapMemberName = getMapMemberVarNameOfColumnForJava(aggregateParentSheet.name, aggregateParentSheet.clasz);
                String keyGetCode = lastVarName + "." + (field.isRefObject() ? "__" : "") + AsUtils.toClassMemberName(field.typeName, field.name);
                String values = AsUtils.FORCE_U_CONTAINER ? ".Values" : "";
                lines.add(indent + "{");
                lines.add(indent + "    " + AsUtils.toUClassName(aggregateParentSheet.clasz.name) + " Obj;");
                lines.add(indent + String.format("    if(this.%s%s.Find(%s, Obj) == false) {", mapMemberName, values, keyGetCode));
                lines.add(indent + String.format("        this.%s%s.Add(%s, %s);", mapMemberName, values, keyGetCode, lastVarName));
                lines.add(indent + "        this." + memberName + ".Add(" + lastVarName + ");");
                lines.add(indent + "    } else if(Obj != " + lastVarName + ") {");
                lines.add(indent + "        check(false, \"Duplicated element\");");
                lines.add(indent + "    }");
                lines.add(indent + "}");
            }
            else {
                throw new UnexpectedException("fatal error");
            }
        }
    }

    private static String getObjectMemberTypeNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        return AsUtils.toUClassName(targetClasz.name);
    }

    private static String getArrayMemberTypeNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        return "TArray<" + AsUtils.toUClassName(targetClasz.name) + ">";
    }

    private String getMapMemberTypeNameOfColumnForJava(String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        return getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, 0, false);
    }

    private String getMapMemberTypeNameOfColumnForJavaEx(String name, ClaszInfo targetClasz, int firstIndex, boolean subClass) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        subClass = true;
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else if(targetClasz.getIndexedFields().size() > 0) {
            if(!AsUtils.FORCE_U_CONTAINER && targetClasz.getIndexedFields().size() == 1 && targetClasz.getIndexedFields().get(0).indexType != IndexType.MultiIndex) {
                List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
                int i = firstIndex;

                FieldInfo indexField = indexedFields.get(i);
                String result = "TMap<";
                result += getMapKeyTypeName(indexField);
                result += ", ";

                String valueTypeName;
                if(i == indexedFields.size() - 1) {
                    valueTypeName = AsUtils.toUClassName(targetClasz.name);
                }
                else {
                    valueTypeName = getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, i + 1, subClass);
                }

                if(i == indexedFields.size() - 1 && indexField.indexType == IndexType.MultiIndex) {
                    result += "TArray<" + valueTypeName + ">";
                }
                else {
                    result += valueTypeName;
                }

                result += ">";

                return result;
            }
            else {
                return getMapClassName(firstIndex, targetClasz);
            }
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    public static String getObjectMemberVarNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        return AsUtils.toClassMemberName(targetClasz.name, name);
    }

    public static String getArrayMemberVarNameOfColumnForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        return AsUtils.toClassMemberName(targetClasz.name, name);
    }

    public static String getMapMemberVarNameOfColumnForJava(String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else if(targetClasz.getIndexedFields().size() > 0) {
            return AsUtils.toClassMemberName(targetClasz.name, name) + "Map";
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    private static String getObjectMemberGetMethodNameForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        return "Get" + SUtils.ucfirst(name);
    }

    private static String getArrayMemberGetMethodNameForJava(String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        return "Get" + SUtils.ucfirst(name);
    }

    private static String getMapMemberGetMethodNameForJava(String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else if(targetClasz.getIndexedFields().size() > 0) {
            return "Get" + SUtils.ucfirst(name) + "Map";
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    public HashSet<String> CreatedMapOrArrayClasses = new HashSet<>();

    public String getMapClassName(int firstIndex, ClaszInfo targetClasz) throws UnexpectedException {
        return getMapClassNameOrDeclCode(firstIndex, targetClasz, null);
    }

    public void addMapClassDeclCodeForJava(List<String> lines, ClaszInfo targetClasz) throws UnexpectedException {
        getMapClassNameOrDeclCode(0, targetClasz, lines);
    }

    public String getMapClassNameOrDeclCode(int firstIndex, ClaszInfo targetClasz, List<String> lines) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else  {
            List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
            String suffix = "_";
            String lastMapClassName = "";
            for(int i = indexedFields.size() - 1; i >= firstIndex; --i) {
                FieldInfo indexField = indexedFields.get(i);
                String keyTypeName = getMapKeyTypeName(indexField);
                if(keyTypeName.matches("[A-Z][A-Z].*")) {
                    suffix += keyTypeName.substring(1);
                }
                else {
                    suffix += SUtils.ucfirst(keyTypeName);
                }
                String mapClassName = AsUtils.toUClassName(targetClasz.name) + suffix;
                String mapTypeName = "TMap<" + keyTypeName + ", ";
                String valueTypeName;
                if(i == indexedFields.size() - 1) {
                    valueTypeName = AsUtils.toUClassName(targetClasz.name);
                }
                else {
                    valueTypeName = lastMapClassName;
                }

                if(i == indexedFields.size() - 1 && indexField.indexType == IndexType.MultiIndex) {
                    String arrayClassName = valueTypeName + "_Array";
                    String arrayTypeName = "TArray<" + valueTypeName + ">";
                    if(lines != null && CreatedMapOrArrayClasses.contains(arrayClassName) == false) {
                        CreatedMapOrArrayClasses.add(arrayClassName);
                        lines.add("UCLASS()");
                        lines.add(String.format("class %s {", arrayClassName));
                        lines.add(String.format("    %s Values;", arrayTypeName));
                        lines.add("}");
                        lines.add("");
                    }
                    mapClassName += "MultiMap";
                    mapTypeName += arrayClassName;
                }
                else {
                    mapClassName += "Map";
                    mapTypeName += valueTypeName;
                }

                mapTypeName += ">";

                if(lines != null && CreatedMapOrArrayClasses.contains(mapClassName) == false) {
                    CreatedMapOrArrayClasses.add(mapClassName);
                    lines.add("UCLASS()");
                    lines.add(String.format("class %s {", mapClassName));
                    lines.add(String.format("    %s Values;", mapTypeName));
                    lines.add("}");
                    lines.add("");
                }

                lastMapClassName = mapClassName;
            }
            return lastMapClassName;
        }
    }

    public static void addObjectMemberDeclCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        String className = getObjectMemberTypeNameOfColumnForJava(name, targetClasz);
        String memberName = getObjectMemberVarNameOfColumnForJava(name, targetClasz);
        lines.add("    " + className + " " + memberName + ";");
    }

    public void addArrayMemberDeclCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else  {
            String className = getArrayMemberTypeNameOfColumnForJava(name, targetClasz);
            String memberName = getArrayMemberVarNameOfColumnForJava(name, targetClasz);
            lines.add("    " + className + " " + memberName + ";");
            if(targetClasz.getIndexedFields().size() > 0) {
                if(AsUtils.USE_U_CONTAINER || !AsUtils.FORCE_U_CONTAINER && targetClasz.getIndexedFields().size() == 1 && targetClasz.getIndexedFields().get(0).indexType != IndexType.MultiIndex) {
                    String mapClassName = getMapMemberTypeNameOfColumnForJava(name, targetClasz);
                    String mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                    lines.add("    " + mapClassName + " " + mapMemberName + ";");
                }
            }
        }
    }

    public static void addObjectMemberInitCodeForJava(List<String> lines, String name, ClaszInfo targetClasz, String jsonFieldName, ClaszInfo ownerObjClasz) {
        assert(targetClasz.genColForClient);
        String className = getObjectMemberTypeNameOfColumnForJava(name, targetClasz);
        String memberName = getObjectMemberVarNameOfColumnForJava(name, targetClasz);
        lines.add("        this." + memberName + " = nullptr;");
        lines.add("        if(true) {");
        lines.add("            FJsonObject JsonObj = Json.GetObjectField(\"" + jsonFieldName + "\");");
        lines.add(String.format("            this.%s = %s;", memberName, AsUtils.newObjectCode(className)));
        lines.add("            this." + memberName + ".DecodeJson(JsonObj);");
        if(ownerObjClasz != null && targetClasz.getOwnerObjClasz(true) != null && ownerObjClasz == targetClasz.getOwnerObjClasz(true)) {
            assert(ownerObjClasz.genColForClient);
            lines.add("            this." + memberName + ".OwnerObj = Cast<" + AsUtils.toUClassName(ownerObjClasz.name) + ">(this);");
            lines.add("            if(this." + memberName + ".OwnerObj == nullptr) {");
            lines.add("                check(false, \"fatal error\");");
            lines.add("            }");
        }
        lines.add("        }");
    }

    public void addArrayMemberInitCodeForJava(List<String> lines, String name, ClaszInfo targetClasz, String jsonFieldName, ClaszInfo ownerObjClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else  {
            String className = getArrayMemberTypeNameOfColumnForJava(name, targetClasz);
            String memberName = getArrayMemberVarNameOfColumnForJava(name, targetClasz);
            lines.add("        this." + memberName + ".Reset();");
            String mapClassName;
            String mapMemberName;
            if(targetClasz.getIndexedFields().size() > 0) {
                if(!AsUtils.FORCE_U_CONTAINER && targetClasz.getIndexedFields().size() == 1 && targetClasz.getIndexedFields().get(0).indexType != IndexType.MultiIndex) {
                    mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                    lines.add("        this." + mapMemberName + ".Reset();");
                }
                else if(AsUtils.USE_U_CONTAINER) {
                    mapClassName = getMapMemberTypeNameOfColumnForJava(name, targetClasz);
                    mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                    lines.add(String.format("        this.%s = %s;", mapMemberName, AsUtils.newObjectCode(mapClassName)));
                }
            }
            lines.add("        if(true) {");
            lines.add("            FJsonArray JsonArray = Json.GetArrayField(\"" + jsonFieldName + "\");");
            lines.add("            for(int i = 0, j = JsonArray.Num(); i < j; ++i) {");
            lines.add("                FJsonValue JsonValue = JsonArray.GetValueAt(i);");
            lines.add("                FJsonObject JsonObj;");
            lines.add("                if(!JsonValue.TryGetObject(JsonObj)) {");
            lines.add("                    check(false);");
            lines.add("                }");
            lines.add(String.format("                %s Obj = %s;", AsUtils.toUClassName(targetClasz.name), AsUtils.newObjectCode(targetClasz)));
            lines.add("                Obj.DecodeJson(JsonObj);");
            lines.add("                this." + memberName + ".Add(Obj);");
            if(ownerObjClasz != null && targetClasz.getOwnerObjClasz(true) != null && ownerObjClasz == targetClasz.getOwnerObjClasz(true)) {
                assert(ownerObjClasz.genColForClient);
                lines.add("                Obj.OwnerObj = Cast<" + AsUtils.toUClassName(ownerObjClasz.name) + ">(this);");
                lines.add("                if(Obj.OwnerObj == nullptr) {");
                lines.add("                    check(false, \"fatal error\");");
                lines.add("                }");
            }
            if(targetClasz.getIndexedFields().size() > 0) {
                if(AsUtils.USE_U_CONTAINER || !AsUtils.FORCE_U_CONTAINER && targetClasz.getIndexedFields().size() == 1 && targetClasz.getIndexedFields().get(0).indexType != IndexType.MultiIndex) {
                    addMapMemberAddElementCodeForJava(lines, name, targetClasz, "this");
                }
            }
            lines.add("            }");
            lines.add("        }");
        }
    }

    public void addMapMemberAddElementCodeForJava(List<String> lines, String name, ClaszInfo targetClasz, String targetObj) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        String memberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
        List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
        int lastIndex = indexedFields.size() - 1;
        FieldInfo lastField = indexedFields.get(lastIndex);
        String lastValueCode = "Obj." + (lastField.isRefObject() ? "__" : "") + AsUtils.toClassMemberName(lastField.typeName, lastField.name);
        String lastVarName = targetObj + "." + memberName;
        for(int i = 0; i < indexedFields.size() - 1; ++i) {
            FieldInfo indexedField = indexedFields.get(i);
            String indexValue = "Obj." + (indexedField.isRefObject() ? "__" : "") + AsUtils.toClassMemberName(indexedField.typeName, indexedField.name);
            String clsNameL = getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, i + 1, false);
            String clsNameR = getMapMemberTypeNameOfColumnForJavaEx(name, targetClasz, i + 1, true);
            String varNameL = "Map" + (i + 1);
            String varNameR = i == 0 ? targetObj + "." + memberName : "Map" + i;
            lines.add(String.format("                %s %s;", clsNameL, varNameL));
            lines.add(String.format("                if(%s.Values.Find(%s, %s) == false) {", varNameR, indexValue, varNameL));
            lines.add(String.format("                    %s = %s;", varNameL, AsUtils.newObjectCode(clsNameR)));
            lines.add(String.format("                    %s.Values.Add(%s, %s);", varNameR, indexValue, varNameL));
            lines.add("                }");
            lastVarName = varNameL;
        }
        if(lastField.indexType == IndexType.MultiIndex) {
            String listClsNameL = AsUtils.toUClassName(targetClasz.name) + "_Array";
            String listClsNameR = AsUtils.toUClassName(targetClasz.name) + "_Array";
            lines.add(String.format("                %s Arr;", listClsNameL));
            lines.add(String.format("                if(%s.Values.Find(%s, Arr) == false) {", lastVarName, lastValueCode));
            lines.add(String.format("                    Arr = %s;", AsUtils.newObjectCode(listClsNameR)));
            lines.add(String.format("                    %s.Values.Add(%s, Arr);", lastVarName, lastValueCode));
            lines.add("                }");
            lines.add("                Arr.Values.Add(Obj);");
        }
        else if(lastField.indexType == IndexType.SingleIndex) {
            String values = (!AsUtils.FORCE_U_CONTAINER && indexedFields.size() == 1 ? "" : ".Values");
            lines.add(String.format("                %s%s.Add(%s, Obj);", lastVarName, values, lastValueCode));
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    public static void addObjectMemberGetCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) {
        assert(targetClasz.genColForClient);
        String className = getObjectMemberTypeNameOfColumnForJava(name, targetClasz);
        String memberName = getObjectMemberVarNameOfColumnForJava(name, targetClasz);
        String getMethodName = getObjectMemberGetMethodNameForJava(name, targetClasz);
        lines.add("    " + className + " " + getMethodName + "() {");
        lines.add("        return this." + memberName + ";");
        lines.add("    }");
    }

    public void addArrayMemberGetCodeForJava(List<String> lines, String name, ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        if(targetClasz == null) {
            throw new UnexpectedException("impossible");
        }
        else {
            String className = getArrayMemberTypeNameOfColumnForJava(name, targetClasz);
            String memberName = getArrayMemberVarNameOfColumnForJava(name, targetClasz);
            String getMethodName = getArrayMemberGetMethodNameForJava(name, targetClasz);
            lines.add("    " + className + "& " + getMethodName + "() {");
            lines.add("        return this." + memberName + ";");
            lines.add("    }");
            if(targetClasz.getIndexedFields().size() > 0) {
                if(AsUtils.USE_U_CONTAINER || !AsUtils.FORCE_U_CONTAINER && targetClasz.getIndexedFields().size() == 1 && targetClasz.getIndexedFields().get(0).indexType != IndexType.MultiIndex) {
                    String mapClassName = getMapMemberTypeNameOfColumnForJava(name, targetClasz);
                    String mapMemberName = getMapMemberVarNameOfColumnForJava(name, targetClasz);
                    String mapGetMethodName = getMapMemberGetMethodNameForJava(name, targetClasz);
                    lines.add("    " + mapClassName + "& " + mapGetMethodName + "() {");
                    lines.add("        return this." + mapMemberName + ";");
                    lines.add("    }");
                }
            }
        }
    }

    public static void array1Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        String type = AsUtils.getScriptTypeName(typeName);
        lines.add(String.format("        FJsonArray %s_ = Json.GetArrayField(\"%s\");", varName, colName));
        lines.add(String.format("        this.%s.Empty();", varName));
        lines.add(String.format("        for(int i = 0, j = %s_.Num(); i < j; ++i) {", varName));
        if(UnrealTypes.isUETypeOrSubclassOf(typeName)) {
            String get = AsUtils.getJsonGetMethodName("string");
            String loadingCode = UnrealTypes.getLoadingCode(UnrealTypes.isSubclassOf(typeName) ? "UClass" : typeName);
            lines.add(String.format("            this.%s.Add(%s(%s(%s_, i)));", varName, loadingCode, get, varName));
        }
        else {
            String get = AsUtils.getJsonGetMethodName(typeName);
            lines.add(String.format("            this.%s.Add(%s(%s_, i));", varName, get, varName));
        }
        lines.add(String.format("        }"));
    }

    public static void array2Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        String get = AsUtils.getJsonGetMethodName(typeName);
        String type = AsUtils.getScriptTypeName(typeName);
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
        String get = AsUtils.getJsonGetMethodName(typeName);
        String type = AsUtils.getScriptTypeName(typeName);
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

    public static void plainArray1Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        lines.add(String.format("        FJsonArray %s_ = Json.GetArrayField(\"%s\");", varName, colName));
        lines.add(String.format("        this.%s.Empty();", varName));
        lines.add(String.format("        for(int I0 = 0, I0m = %s_.Num(); I0 < I0m; ++I0) {", varName));
        if(UnrealTypes.isUETypeOrSubclassOf(typeName)) {
            String get = AsUtils.getJsonGetMethodName("string");
            String loadingCode = UnrealTypes.getLoadingCode(UnrealTypes.isSubclassOf(typeName) ? "UClass" : typeName);
            lines.add(String.format("            this.%s.Add(%s(%s(%s_, I0)));", varName, loadingCode, get, varName));
        }
        else {
            String get = AsUtils.getJsonGetMethodName(typeName);
            lines.add(String.format("            this.%s.Add(%s(%s_, I0));", varName, get, varName));
        }
        lines.add(String.format("        }"));
    }

    public static void plainArray2Parse(List<String> lines, String varName, String colName, String typeName) throws UnexpectedException {
        lines.add(String.format("        FJsonArray %s_ = Json.GetArrayField(\"%s\");", varName, colName));
        lines.add(String.format("        this.%s.SetNum(%s_.Num());", varName, varName));
        lines.add(String.format("        for(int I0 = 0, I0m = %s_.Num(); I0 < I0m; ++I0) {", varName));
        lines.add(String.format("            FJsonArray %s__;", varName));
        lines.add(String.format("            bool bRet = %s_.GetValueAt(I0).TryGetArray(%s__);", varName, varName));
        lines.add(String.format("            check(bRet);"));
        lines.add(String.format("            for(int I1 = 0, I1m = %s__.Num(); I1 < I1m; ++I1) {", varName));
        if(UnrealTypes.isUETypeOrSubclassOf(typeName)) {
            String get = AsUtils.getJsonGetMethodName("string");
            String loadingCode = UnrealTypes.getLoadingCode(UnrealTypes.isSubclassOf(typeName) ? "UClass" : typeName);
            lines.add(String.format("                this.%s[I0].Values.Add(%s(%s(%s__, I1)));", varName, loadingCode, get, varName));
        }
        else {
            String get = AsUtils.getJsonGetMethodName(typeName);
            lines.add(String.format("                this.%s[I0].Values.Add(%s(%s__, I1));", varName, get, varName));
        }
        lines.add(String.format("            }"));
        lines.add(String.format("        }"));
    }

    private static String escapeStringForJava(String in) {
        in = in.replaceAll("\\\\", "\\\\\\\\");
        in = in.replaceAll("\"", "\\\\\\\"");
        in = in.replaceAll("\'", "\\\\\\\'");
        in = in.replaceAll("\r?\n", "\\\\n");
        return in;
    }

    private String castJsonValueToJava(Object jsonValue, String indent, FieldInfo field) throws IOException {
        assert(field.genColForClient);
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
            String luaValue = (String)jsonValue;
            luaValue = Q(luaValue);
            return luaValue;
        }
        else if(AsUtils.isEnumType(typeName)) {
            if(!(jsonValue instanceof String)) throw new UnexpectedException("fatal error");
            String luaValue = (String)jsonValue;

            luaValue = AsUtils.getScriptEnumClassName(typeName) + "::" + jsonValue.toString();
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
            return AsUtils.getMapKeyTypeName(field.typeName);
        }
    }

    public String getMapValueTypeName(FieldInfo field) throws UnexpectedException {
        assert(field.genColForClient);
        if(field.isRefObject()) {
            ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
            return getMapValueTypeName(refColumn.field);
        }
        else {
            return AsUtils.getMapValueTypeName(field.typeName);
        }
    }
}
