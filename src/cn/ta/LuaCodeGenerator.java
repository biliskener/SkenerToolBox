package cn.ta;

import cn.ta.config.LuaConfig;
import com.strobel.core.StringUtilities;
import org.apache.commons.collections4.map.LinkedMap;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.stream.Collectors;

public class LuaCodeGenerator extends BaseCodeGenerator {
    String inputDir;
    String clientOutputDir;
    String clientJsonOutputDir;
    String clientCodeOutputDir;
    boolean enableLazyMode = true;

    ClaszMgr serverClaszMgr;
    SheetMgr serverSheetMgr;
    LinkedList<Set<String>> mImportContextStacks = new LinkedList<>();

    Map<String, String> allLazyableClaszs = new LinkedMap<>();

    public LuaCodeGenerator(ClaszMgr serverClaszMgr, SheetMgr serverSheetMgr) {
        this.serverClaszMgr = serverClaszMgr;
        this.serverSheetMgr = serverSheetMgr;
        allLazyableClaszs.put("BonusInfo", "bonuses");
        allLazyableClaszs.put("RingProto", "rings");
        allLazyableClaszs.put("ItemProto", "items");
        allLazyableClaszs.put("ArmProto", "arms");
        allLazyableClaszs.put("CargoProto", "cargoes");
        allLazyableClaszs.put("SkillProto", "skills");
        allLazyableClaszs.put("MonsterInfo", "monsters");
        allLazyableClaszs.put("GuardProto", "guards");
        allLazyableClaszs.put("TitleProto", "titles");
    }

    public void initDirs(String inputDir, LuaConfig luaConfig) throws IOException {
        this.inputDir = inputDir;
        this.clientOutputDir = luaConfig.clientOutputDir;
        this.clientJsonOutputDir = luaConfig.clientJsonOutputDir;
        this.clientCodeOutputDir = luaConfig.clientCodeOutputDir;
    }

    @Override
    public void cleanDirs() throws IOException {
        System.out.println("=== cleaning client files ");
        for(String jsonFile: SUtils.listAllFiles(clientJsonOutputDir, true, null, new String[]{"\\.lua$"})) {
            System.out.println("  deleting file: " + jsonFile);
            SUtils.deleteFile(jsonFile);
        }
    }

    @Override
    public void saveAllFiles() throws IOException {
        saveAllConstLuaFiles();
        saveAllLuaClassFiles();
        saveAllLuaDataFiles();
    }

    void saveAllConstLuaFiles() throws IOException {
        List<String> lines = new LinkedList<>();

        lines.add("-- Tools generated, do not MODIFY!!!");
        lines.add("");

        for(ModuleInfo module: serverClaszMgr.allModules.values()) {
            for(EnumSheetInfo sheet: module.enumSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    saveEnumDeclCode(lines, sheet);
                }
            }
        }

        for(ModuleInfo module: serverClaszMgr.allModules.values()) {
            for(EnumSheetInfo sheet: module.enumSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    saveEnumInfosDataCode(lines, sheet);
                }
            }
        }

        for(ModuleInfo module: serverClaszMgr.allModules.values()) {
            for (ConstSheetInfo sheet : module.constSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    saveConstLuaCode(lines, sheet);
                }
            }

            for(HashSheetInfo sheet: module.hashSheets.values()) {
                if(sheet.clasz.genColForClient) {
                    saveHashLuaCode(lines, sheet);
                }
            }
        }

        String outputDir = clientCodeOutputDir;
        String outputFile = outputDir + "AllConsts.lua";
        SUtils.makeDirAll(outputFile);
        SUtils.saveFile(outputFile, lines);
    }

    void saveAllLuaClassFiles() throws IOException {
        List<String> lines = new LinkedList<>();
        lines.add("-- Tools generated, do not MODIFY!!!");
        lines.add("");

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
        allClaszs.add(serverClaszMgr.infoDataClaszInfo);

        for(ClaszInfo clasz: allClaszs) {
            if(!clasz.genColForClient) continue;
            String luaClassName = clasz.name;
            if(clasz.baseClasz != null) {
                lines.add("---@class " + clasz.name + " : " + clasz.baseClasz.name);
            }
            else {
                lines.add("---@class " + clasz.name);
            }

            ClaszInfo ownerObjClasz = clasz.getOwnerObjClasz(false);
            if(ownerObjClasz != null) {
                assert(ownerObjClasz.genColForClient);
                if(ownerObjClasz.name.equals("Object")) {
                    lines.add("---@field ownerObj " + "any");
                }
                else {
                    lines.add("---@field ownerObj " + ownerObjClasz.name);
                }
            }
            for(FieldInfo field: clasz.fields) {
                if(!field.genColForClient) continue;
                if(isDynamicActivityField(field)) {
                    continue;
                }
                if(field.isRefObject()) {
                    ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
                    String luaTypeName = getLuaTypeName(refColumn.field, field.isMap());
                    String objTypeName = refColumn.parentSheet.clasz.name;
                    if(field.isArray()) {
                        luaTypeName = luaTypeName + "[]";
                        objTypeName = objTypeName + "[]";
                    }
                    lines.add("---@field __" + field.name + " " + luaTypeName + " @ " + field.desc.trim().replaceAll("[\r\n]+", "; "));
                    lines.add("---@field " + field.name + " " + objTypeName + " @ " + field.desc.trim().replaceAll("[\r\n]+", "; "));
                }
                else {
                    lines.add("---@field " + field.name + " " + getLuaTypeName(field, field.isMap()) + " @ " + field.desc.trim().replaceAll("[\r\n]+", "; "));
                }
            }

            if(enableLazyMode && clasz == serverClaszMgr.infoDataClaszInfo) {
                String luaPropsName = "__props_" + luaClassName;
                lines.add("_G." + luaClassName + " = LuaClass()");
                lines.add("_G." + luaClassName + ".__props = {");
                for(FieldInfo field: clasz.fields) {
                    if(!field.genColForClient) continue;
                    if(isDynamicActivityField(field)) {
                        continue;
                    }
                    if(field.isTargetClasz) {
                        lines.add("    " + field.name + " = 1,");
                    }
                }
                lines.add("}");
                lines.add("_G." + luaClassName + ".__index = function(t, k)");
                lines.add("    if _G." + luaClassName + ".__props[k] then");
                lines.add("        local v = assert(require('Json/' .. k))");
                lines.add("        t[k] = v");
                lines.add("        return v");
                lines.add("    end");
                lines.add("end");
            }
            else {
                if(clasz.baseClasz != null) {
                    lines.add("_G." + luaClassName + " = LuaClass(" + clasz.baseClasz.name + ")");
                }
                else {
                    lines.add("_G." + luaClassName + " = LuaClass()");
                }

                lines.add("_G." + luaClassName + ".__index = function(t, k)");
                lines.add("    local c = _G." + luaClassName);
                if(enableLazyMode) {
                    lines.add("    if not rawget(t, '__br') then");
                    lines.add("        rawset(t, '__br', true)");
                    lines.add("        t:bindRefObjects(_G.InfoDataBase.ROOT)");
                    lines.add("        return t[k]");
                    lines.add("    end");
                }
                lines.add("    local v = rawget(c, k)");
                lines.add("    if v ~= nil then return v end");
                if(clasz.baseClasz != null) {
                    lines.add("    return c.Super.__index(t, k)");
                }
                lines.add("end");

                lines.add("function _G." + luaClassName + ":bindRefObjects(infoData)");
                if(clasz.baseClasz != null) {
                    lines.add("    " + luaClassName + ".Super.bindRefObjects(self, infoData)");
                }
                for (FieldInfo field : clasz.fields) {
                    if(!field.genColForClient) continue;
                    if(isDynamicActivityField(field)) {
                        continue;
                    }
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
                            getCode = getCode + "[self.__" + field.name + "]";
                            if(canNull) {
                                lines.add("    if self.__" + field.name + " and self.__" + field.name + " ~= 0 and self.__" + field.name + " ~= '' then");
                            }
                            else {
                                lines.add("    if self.__" + field.name + " then");
                            }
                            lines.add("        self." + field.name + " = " + getCode);
                            lines.add("        if not self." + field.name + " then");
                            lines.add("            assert(false, \"Ref obj not found\")");
                            lines.add("        end");
                            lines.add("    end");
                        }
                        else if(dimCount == 1) {
                            String getCode = "infoData." + typeName.replaceAll("\\.\\w+$", "");
                            getCode = getCode + "[_id]";
                            lines.add("    if self.__" + field.name + " and not rawget(self, '" + field.name + "') then");
                            lines.add("        self." + field.name + " = {}");
                            lines.add("        for _, _id in pairs(self.__" + field.name + ") do");
                            lines.add("            if _id == nil or _id == 0 or _id == '' then");
                            lines.add("                table.insert(self." + field.name + ", nil)");
                            lines.add("            else");
                            lines.add("                local obj = " + getCode);
                            lines.add("                if not obj then");
                            lines.add("                    assert(false, \"Ref obj not found\")");
                            lines.add("                end");
                            lines.add("                table.insert(self." + field.name + ", obj)");
                            lines.add("            end");
                            lines.add("        end");
                            lines.add("    end");
                        }
                        else {
                            throw new UnexpectedException("not supported");
                        }
                    }
                    else if(field.isTargetClasz) {
                        ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);

                        lines.add("    if self." + field.name + " then");

                        if(field.isMap()) {

                            String lastVarName = "self." + field.name;
                            String indent = "    ";
                            List<FieldInfo> indexedFields = targetClasz.getIndexedFields();

                            for(int i = 0; i < indexedFields.size(); ++i) {
                                FieldInfo fieldInfo = indexedFields.get(i);
                                String varName = "obj" + i;
                                lines.add(indent + "    for _, " + varName + " in pairs(" + lastVarName + ") do");
                                lastVarName = varName;
                                indent += "    ";
                            }

                            if(indexedFields.get(indexedFields.size() - 1).indexType == IndexType.MultiIndex) {
                                indent += "    ";
                                lines.add(indent + "    for _, obj in pairs(" + lastVarName + ") do");
                                lines.add(indent + "        obj:bindRefObjects(infoData)");
                                if(clasz != serverClaszMgr.infoDataClaszInfo && targetClasz.getOwnerObjClasz(true) != null) {
                                    lines.add(indent + "        obj.ownerObj = self");
                                }
                                lines.add(indent + "    end");
                                indent = indent.substring(0, indent.length() - 4);
                            }
                            else {
                                lines.add(indent + "    " + lastVarName + ":bindRefObjects(infoData)");
                                if(clasz != serverClaszMgr.infoDataClaszInfo && targetClasz.getOwnerObjClasz(true) != null) {
                                    lines.add(indent + "    " + lastVarName + ".ownerObj = self");
                                }
                            }

                            for(FieldInfo indexField: indexedFields) {
                                indent = indent.substring(0, indent.length() - 4);
                                lines.add(indent + "    end");
                            }
                        }
                        else if(field.isArray()) {
                            lines.add("        for _, obj in pairs(self." + field.name + ") do");
                            lines.add("            obj:bindRefObjects(infoData)");
                            if(clasz != serverClaszMgr.infoDataClaszInfo && targetClasz.getOwnerObjClasz(true) != null) {
                                lines.add("            obj.ownerObj = self");
                            }
                            lines.add("        end");
                        }
                        else {
                            lines.add("        self." + field.name + ":bindRefObjects(infoData)");
                            if(clasz != serverClaszMgr.infoDataClaszInfo && targetClasz.getOwnerObjClasz(true) != null) {
                                lines.add("        self." + field.name + ".ownerObj = self");
                            }
                        }
                        lines.add("    end");
                    }
                }
                lines.add("end");
            }
            lines.add("");
        }

        {
            ClaszInfo clasz = serverClaszMgr.infoDataClaszInfo;
            String luaClassName = clasz.name;
            lines.add("function _G." + luaClassName + ":LoadAll()");
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
            if(enableLazyMode) {
                lines.add("    --self:bindRefObjects(self)");
            }
            else {
                lines.add("    self:bindRefObjects(self)");
            }

            lines.add("end");
        }

        String outputDir = clientCodeOutputDir;
        String outputFile = outputDir + "InfoDataBase.lua";
        SUtils.makeDirAll(outputFile);
        SUtils.saveFile(outputFile, lines);
    }

    void saveAllLuaDataFiles() throws IOException {
        List<String> lines = new LinkedList<>();
        String luaClassName = "InfoDataBase";
        String outputFileName = "infoData.lua";

        lines.add("return setmetatable({");
        for(BookInfo book: serverSheetMgr.allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            if(!sheet.clasz.genColForClient) continue;
            if(isDynamicActivitySheet(sheet)) {
                continue;
            }
            if(sheet.isMulti) {
                saveMultiObjectJsonFile(sheet.name, sheet.clasz, book.getJsonValue(false), sheet.isHash);
            }
            else if(sheet.isObject) {
                saveSingleObjectJsonFile(sheet.name, sheet.clasz, book.getJsonValue(false));
            }
            else {
                throw new UnexpectedException("impossible");
            }
            if(!enableLazyMode) {
                lines.add("    " + sheet.name + " = require(\"Json/" + sheet.name + "\"),");
            }
        }
        lines.add("}, assert(" + luaClassName + "))");

        String outputDir = clientJsonOutputDir;
        String outputFile = outputDir + outputFileName;
        SUtils.makeDirAll(outputFile);
        SUtils.saveFile(outputFile, lines);

        if(mImportContextStacks.size() > 0) {
            throw new UnexpectedException("fatal error");
        }
    }

    private boolean isDynamicActivityField(FieldInfo field) {
        if(field.isTargetClasz) {
            ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
            if(targetClasz.name.equals("DynamicActivityInfo")) {
                return true;
            }
            if(targetClasz.baseClasz != null && targetClasz.baseClasz.name.equals("DynamicActivityInfo")) {
                return true;
            }
        }
        return false;
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

        String lastVarName = "self";
        String indent = "    ";
        addAggregateCodeEx(lines, indent, lastVarName, aggregateParentSheet, aggregateChildSheetStack, 0);
        lines.add("");
    }

    private void addAggregateCodeEx(List<String> lines, String indent, String lastVarName, SheetInfo aggregateParentSheet, LinkedList<SheetInfo> aggregateChildSheetStack, int stackIndex) throws UnexpectedException {
        if(stackIndex < aggregateChildSheetStack.size()) {
            SheetInfo parentSheet = aggregateChildSheetStack.get(stackIndex);
            if(parentSheet.isMulti) {
                String typeName = parentSheet.clasz.name;
                String varName = SUtils.lcfirst(typeName);
                String listName = lastVarName + "." + parentSheet.name;
                lines.add(indent + "for _, " + varName + " in pairs(" + listName + ") do");
                addAggregateCodeEx(lines, indent + "    ", varName, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
                lines.add(indent + "end");
            }
            else {
                addAggregateCodeEx(lines, indent, lastVarName + "." + parentSheet.name, aggregateParentSheet, aggregateChildSheetStack, stackIndex + 1);
            }
        }
        else {
            List<FieldInfo> indexedFields = aggregateParentSheet.clasz.getIndexedFields();
            if(indexedFields.size() == 1) {
                FieldInfo field = indexedFields.get(0);
                String mapMemberName = "" + aggregateParentSheet.name;
                String keyGetCode = lastVarName + "." + field.name;
                lines.add(indent + "if rawget(self." + mapMemberName + ", " + keyGetCode + ") == nil then");
                lines.add(indent + "    rawset(self." + mapMemberName + ", " + keyGetCode + ", " +  lastVarName +")");
                lines.add(indent + "elseif rawget(self." + mapMemberName + ", " + keyGetCode + ") ~= " + lastVarName + " then");
                lines.add(indent + "    assert(false)");
                lines.add(indent + "end");
            }
            else {
                throw new UnexpectedException("fatal error");
            }
        }
    }

    void saveMultiObjectJsonFile(String scopeName, ClaszInfo clasz, Object jsonValue, boolean isHash) throws IOException {
        assert(clasz.genColForClient);

        LinkedList<String> lines = new LinkedList<>();
        String outputFileName = scopeName + ".lua";

        mImportContextStacks.addLast(new LinkedHashSet<>());

        JSONArray json = (JSONArray)jsonValue;
        lines.add("return " + castObjectArrayValueToLua(json, "", clasz, isHash));
        lines.add("");

        Set<String> importContext = mImportContextStacks.removeLast();
        lines.addAll(0, importContext.stream().map(obj -> {
            return "local " + obj + " = _G." + obj + " or assert(false)";
        }).collect(Collectors.toSet()));

        String outputDir = clientJsonOutputDir;
        String outputFile = outputDir + outputFileName;
        SUtils.makeDirAll(outputFile);
        SUtils.saveFile(outputFile, lines);
    }

    void saveSingleObjectJsonFile(String scopeName, ClaszInfo clasz, Object jsonValue) throws IOException {
        assert(clasz.genColForClient);

        List<String> lines = new LinkedList<>();
        String outputFileName = scopeName + ".lua";

        mImportContextStacks.addLast(new LinkedHashSet<>());

        JSONObject json = (JSONObject)jsonValue;
        lines.add("return " + castObjectValueToLua(json, "", clasz));
        lines.add("");

        Set<String> importContext = mImportContextStacks.removeLast();
        lines.addAll(0, importContext.stream().map(obj -> {
            return "local " + obj + " = _G." + obj + " or assert(false)";
        }).collect(Collectors.toSet()));

        String outputDir = clientJsonOutputDir;
        String outputFile = outputDir + outputFileName;
        SUtils.makeDirAll(outputFile);
        SUtils.saveFile(outputFile, lines);
    }

    void saveEnumDeclCode(List<String> lines, EnumSheetInfo sheet) throws IOException {
        FieldInfo nameField = sheet.clasz.fields.get(0);
        FieldInfo valueField = sheet.clasz.fields.get(1);
        FieldInfo commentField = sheet.clasz.fields.get(sheet.clasz.fields.size() - 1);

        if(nameField.name.equals("ename") && valueField.name.equals("value") && commentField.name.equals("comment")) {
            lines.add("_G.E" + sheet.name + " = {");
            for (int i = 0; i < sheet.targetValue.length(); ++i) {
                JSONObject json = sheet.targetValue.getJSONObject(i);
                String keyName = json.getString(nameField.name);
                String luaValue = castJsonValueToLua(json.get(valueField.name), "    ", valueField);
                String comment = json.getString(commentField.name);
                lines.add("    " + keyName + " = " + luaValue + ", -- " + comment);
            }
            lines.add("}");
            lines.add("");
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    void saveEnumInfosDataCode(List<String> lines, EnumSheetInfo sheet) throws IOException {
        FieldInfo nameField = sheet.clasz.fields.get(0);
        FieldInfo valueField = sheet.clasz.fields.get(1);
        FieldInfo commentField = sheet.clasz.fields.get(sheet.clasz.fields.size() - 1);

        if(sheet.clasz.fields.size() > 3) {
            lines.add("_G.E" + sheet.name + "Infos = {");
            for (int i = 0; i < sheet.targetValue.length(); ++i) {
                JSONObject json = sheet.targetValue.getJSONObject(i);
                boolean needSkip = json.getBoolean("__needSkip");
                if (!needSkip) {
                    String keyName = json.getString(nameField.name);
                    String luaValue = castObjectValueToLuaEx(json, "    ", sheet.clasz, true);
                    lines.add("    [assert(E" + sheet.clasz.name + "." + keyName + ")] = " + luaValue + ",");
                }
            }
            lines.add("}");
            lines.add("");
        }
    }

    void saveConstLuaCode(List<String> lines, ConstSheetInfo sheet) throws IOException {
        lines.add("_G." + sheet.name + " = {");
        for(FieldInfo field: sheet.clasz.fields) {
            if(!field.genColForClient) continue;
            String value;
            if(field.typeName.equals("int") || field.typeName.equals("long")) {
                value = castJsonValueToLua(sheet.targetValue.get(field.name), "    ", field);
                lines.add("    " + field.name + " = " + value + ", -- " + field.desc);
            }
            else if(field.typeName.equals("float") || field.typeName.equals("double")) {
                value = castJsonValueToLua(sheet.targetValue.get(field.name), "    ", field);
                lines.add("    " + field.name + " = " + value + ", -- " + field.desc);
            }
            else if(field.typeName.equals("bool")) {
                value = castJsonValueToLua(sheet.targetValue.get(field.name), "    ", field);
                lines.add("    " + field.name + " = " + value + ", -- " + field.desc);
            }
            else if(field.typeName.equals("string")) {
                value = castJsonValueToLua(sheet.targetValue.get(field.name), "    ", field);
                lines.add("    " + field.name + " = " + value + ", -- " + field.desc);
            }
            else {
                throw new UnexpectedException("fatal error: " + field.typeName);
            }
        }
        lines.add("}");
        lines.add("");
    }

    void saveHashLuaCode(List<String> lines, HashSheetInfo sheet) throws IOException {
        FieldInfo leftField = sheet.clasz.fields.get(0);
        FieldInfo rightField = sheet.clasz.fields.get(1);
        FieldInfo commentField = sheet.clasz.fields.get(2);
        String leftTypeName = LuaUtils.getLuaTypeName(leftField.typeName);
        String rightTypeName = LuaUtils.getLuaTypeName(rightField.typeName);
        lines.add("_G." + sheet.name + " = {");
        for(int i = 0; i < sheet.targetValue.length(); ++i) {
            JSONObject value = sheet.targetValue.getJSONObject(i);
            String leftValue;
            if(leftField.typeName.equals("int") || leftField.typeName.equals("long") || leftField.typeName.equals("bool") || leftField.typeName.equals("string")) {
                leftValue = "[" + castJsonValueToLua(value.get(leftField.name), "    ", leftField) + "]";
            }
            else if(JavaUtils.isEnumType(leftTypeName)) {
                leftValue = "[" + castJsonValueToLua(value.get(leftField.name), "    ", leftField) + "]";
            }
            else {
                throw new UnexpectedException("fatal error");
            }

            String rightValue;
            if(rightField.typeName.equals("int") || rightField.typeName.equals("long") || rightField.typeName.equals("bool") || rightField.typeName.equals("string")) {
                rightValue = castJsonValueToLua(value.get(rightField.name), "    ", rightField);
            }
            else if(JavaUtils.isEnumType(rightTypeName)) {
                rightValue = castJsonValueToLua(value.get(rightField.name), "    ", rightField);
            }
            else {
                throw new UnexpectedException("fatal error");
            }
            lines.add("    " + leftValue + " = " + rightValue + ",");
        }
        lines.add("}");
        lines.add("");
    }

    private String castJsonValueToLua(Object jsonValue, String indent, FieldInfo field) throws IOException {
        assert(field.genColForClient);
        if(field.isMultiObject()) {
            ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
            return castObjectArrayValueToLua((JSONArray) jsonValue, indent, targetClasz, field.isMap());
        }
        else if(field.isTargetClasz) {
            ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
            return castObjectValueToLua((JSONObject) jsonValue, indent, targetClasz);
        }
        else if(field.isRefObject()) {
            if(field.isArray()) {
                ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
                return castBaseValueToLua(jsonValue, indent, refColumn.field.typeName + "[]");
            }
            else {
                ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
                return castBaseValueToLua(jsonValue, indent, refColumn.field.typeName);
            }
        }
        else {
            return castBaseValueToLua(jsonValue, indent, field.typeName);
        }
    }

    private JSONObject castArrayValueToMappedValue(JSONArray jsonArray, List<FieldInfo> indexedFields) throws UnexpectedException {
        JSONObject root = new JSONObject();
        for(int i = 0; i < jsonArray.length(); ++i) {
            JSONObject value = jsonArray.getJSONObject(i);
            JSONObject parent = root;
            for(int j = 0; j < indexedFields.size(); ++j) {
                FieldInfo field = indexedFields.get(j);
                Object _indexValue = value.get(field.name);
                if(_indexValue instanceof String || _indexValue instanceof Integer || _indexValue instanceof Long) {
                    String indexValue = _indexValue.toString();
                    if(j < indexedFields.size() - 1) {
                        JSONObject child = parent.has(indexValue) ? parent.getJSONObject(indexValue) : null;
                        if(child == null) {
                            child = new JSONObject();
                            parent.put(indexValue, child);
                        }
                        parent = child;
                    }
                    else if(field.indexType == IndexType.MultiIndex) {
                        JSONArray array = parent.has(indexValue) ? parent.getJSONArray(indexValue) : null;
                        if(array == null) {
                            array = new JSONArray();
                            parent.put(indexValue, array);
                        }
                        array.put(value);
                    }
                    else if(field.indexType == IndexType.SingleIndex) {
                        JSONObject child = parent.has(indexValue) ? parent.getJSONObject(indexValue) : null;
                        if(child == null) {
                            parent.put(indexValue, value);
                        }
                        else {
                            throw new UnexpectedException("impossible");
                        }
                    }
                    else {
                        throw new UnexpectedException("impossible");
                    }
                }
                else {
                    throw new UnexpectedException("impossible");
                }
            }
        }
        return root;
    }

    private String castMappedValueToLua(JSONObject json, String indent, ClaszInfo clasz, List<FieldInfo> indexedFields, int index) throws IOException {
        assert(clasz.genColForClient);

        LinkedList<String> lines = new LinkedList<>();

        FieldInfo field = indexedFields.get(index);
        if(index < indexedFields.size() - 1) {
            lines.add("{");
            for(Object _key: json.keySet()) {
                String key = (String)_key;
                JSONObject value = json.getJSONObject(key);
                String luaKey = "[" + (field.typeName.equals("string") ? Q(key) : key) + "]";
                String luaValue = castMappedValueToLua(value, indent + "    ", clasz, indexedFields, index + 1);
                lines.add(indent + "    " + luaKey + " = " + luaValue + ",");
            }
            lines.add(indent + "}");
        }
        else if(field.indexType == IndexType.MultiIndex) {
            lines.add("{");
            for(Object _key: json.keySet()) {
                String key = (String)_key;
                JSONArray array = json.getJSONArray(key);
                String luaKey = "[" + (field.typeName.equals("string") ? Q(key) : key) + "]";
                lines.add(indent + "    " + luaKey + " = {");
                for(int i = 0; i < array.length(); ++i) {
                    JSONObject value = array.getJSONObject(i);
                    String luaValue = castObjectValueToLua(value, indent + "    ", clasz);
                    lines.add(indent + "    " + luaValue + ",");
                }
                lines.add(indent + "    },");
            }
            lines.add(indent + "}");
        }
        else if(field.indexType == IndexType.SingleIndex) {
            lines.add("{");
            for(Object _key: json.keySet()) {
                String key = (String)_key;
                JSONObject value = json.getJSONObject(key);
                String luaKey = "[" + (field.typeName.equals("string") ? Q(key) : key) + "]";
                String luaValue = castObjectValueToLua(value, indent + "    ", clasz);
                lines.add(indent + "    " + luaKey + " = " + luaValue + ",");
            }
            lines.add(indent + "}");
        }
        else {
            throw new UnexpectedException("impossible");
        }
        return StringUtilities.join("\n", lines);
    }

    private String castObjectArrayValueToLua(JSONArray jsonArray, String indent, ClaszInfo clasz, boolean isHash) throws IOException {
        assert(clasz.genColForClient);
        List<FieldInfo> indexedFields = clasz.getIndexedFields();
        if(isHash) {
            if(indexedFields.size() == 0) {
                throw new UnexpectedException("impossible");
            }
            JSONObject mappedValue = castArrayValueToMappedValue(jsonArray, indexedFields);
            return castMappedValueToLua(mappedValue, indent, clasz, indexedFields, 0);
        }
        else {
            // 转成数组
            LinkedList<String> lines = new LinkedList<>();
            lines.add("{");
            for(int i = 0; i < jsonArray.length(); ++i) {
                String luaValue = castObjectValueToLua(jsonArray.getJSONObject(i), indent + "    ", clasz);
                lines.add(indent + "    " + luaValue + ",");
            }
            lines.add(indent + "}");
            return StringUtilities.join("\n", lines);
        }
    }

    private String castObjectValueToLua(JSONObject jsonObject, String indent, ClaszInfo targetClasz) throws IOException {
        assert(targetClasz.genColForClient);
        return castObjectValueToLuaEx(jsonObject, indent, targetClasz, false);
    }

    private String castObjectValueToLuaEx(JSONObject jsonObject, String indent, ClaszInfo targetClasz, boolean isEnumClasz) throws IOException {
        assert(targetClasz.genColForClient);
        LinkedList<String> lines = new LinkedList<>();
        if(isEnumClasz) {
            lines.add("{");
        }
        else {
            lines.add("setmetatable({");
        }
        for(Object _key: jsonObject.keySet()) {
            String keyName = (String)_key;
            if(!keyName.equals("__needSkip")) {
                Object _value = jsonObject.get(keyName);
                FieldInfo field = targetClasz.deepFindFieldUpwardBaseClasz(keyName);
                if(field.genColForClient) {
                    if(field.isRefObject()) {
                        String luaValue = castJsonValueToLua(_value, indent + "    ", field);
                        lines.add(indent + "    __" + keyName + " = " + luaValue + ",");
                    }
                    else {
                        String luaValue = castJsonValueToLua(_value, indent + "    ", field);
                        lines.add(indent + "    " + keyName + " = " + luaValue + ",");
                    }
                }
            }
        }
        if(isEnumClasz) {
            lines.add(indent + "}");
        }
        else {
            lines.add(indent + "}, assert(" + targetClasz.name + "))");
            Set<String> importContext = mImportContextStacks.getLast();
            importContext.add(targetClasz.name);
        }
        return StringUtilities.join("\n", lines);
    }

    private String castBaseValueToLua(Object jsonValue, String indent, String typeName) throws UnexpectedException {
        if(typeName.endsWith("[]")) {
            String baseTypeName = typeName.substring(0, typeName.length() - 2);
            JSONArray jsonArray = (JSONArray)jsonValue;
            LinkedList<String> values = new LinkedList<>();
            for(int i = 0; i < jsonArray.length(); ++i) {
                values.add(castBaseValueToLua(jsonArray.get(i), indent, baseTypeName));
            }
            return "{" + StringUtilities.join(", ", values) + "}";
        }
        else if(typeName.equals("int")) {
            if(!(jsonValue instanceof Integer)) {
                throw new UnexpectedException("fatal error");
            }
            String luaValue = jsonValue.toString();
            return luaValue;
        }
        else if(typeName.equals("long")) {
            if(!(jsonValue instanceof Long || jsonValue instanceof Integer)) {
                throw new UnexpectedException("fatal error");
            }
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
        else if(LuaUtils.isEnumType(typeName)) {
            if(!(jsonValue instanceof Integer)) throw new UnexpectedException("fatal error");
            String luaValue = "assert(E" + typeName + "." + LuaUtils.getEnumStrValue(typeName, (int)jsonValue) + ")";
            return luaValue;
        }
        else {
            throw new UnexpectedException("impossible");
        }
    }

    private String getLuaTypeNameForMultiObject(ClaszInfo targetClasz, boolean isHash) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        List<FieldInfo> indexedFields = targetClasz.getIndexedFields();
        if(isHash) {
            if(indexedFields.size() == 0) {
                throw new UnexpectedException("fatal error");
            }

            String typeName = "";
            for(int i = 0; i < indexedFields.size(); ++i) {
                FieldInfo indexedField = indexedFields.get(i);
                String keyName = getMapKeyTypeName(indexedField);
                typeName += "table<" + keyName + ", ";
            }
            typeName += targetClasz.name;
            if(indexedFields.get(indexedFields.size() - 1).indexType == IndexType.MultiIndex) {
                typeName += "[]";
            }
            for(int i = 0; i < indexedFields.size(); ++i) {
                typeName += ">";
            }
            return typeName;
        }
        else {
            return targetClasz.name + "[]";
        }
    }

    private String getLuaTypeNameForSingleObject(ClaszInfo targetClasz) throws UnexpectedException {
        assert(targetClasz.genColForClient);
        return targetClasz.name;
    }

    private String getLuaTypeName(FieldInfo field, boolean isHash) throws UnexpectedException {
        assert(field.genColForClient);
        if(field.isMultiObject()) {
            ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
            if(!targetClasz.genColForClient){
                System.out.println("die");
            }
            assert(targetClasz.genColForClient);
            return getLuaTypeNameForMultiObject(targetClasz, isHash);
        }
        else if(field.isTargetClasz) {
            ClaszInfo targetClasz = serverClaszMgr.allClaszs.get(field.targetClaszName);
            assert(targetClasz.genColForClient);
            return getLuaTypeNameForSingleObject(targetClasz);
        }
        else if(field.isRefObject()) {
            ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
            return getLuaTypeNameForSingleObject(refColumn.parentSheet.clasz);
        }
        else {
            return LuaUtils.getLuaTypeName(field.typeName);
        }
    }

    public String getMapKeyTypeName(FieldInfo field) throws UnexpectedException {
        assert(field.genColForClient);
        if(field.isRefObject()) {
            ColumnInfo refColumn = serverSheetMgr.findReferencedColumn(field.typeName);
            return getMapKeyTypeName(refColumn.field);
        }
        else {
            return LuaUtils.getMapKeyTypeName(field.typeName);
        }
    }
}
