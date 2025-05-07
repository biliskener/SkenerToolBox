package cn.ta;

import cn.ta.config.ProjectConfig;
import com.strobel.core.StringUtilities;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;

public class Main {
    public static String inputDir;
    public static ClaszMgr serverClaszMgr = new ClaszMgr();
    public static SheetMgr serverSheetMgr = new SheetMgr(serverClaszMgr);
    public static boolean ALWAYS_EXPORT_EMPTY_FIELDS = ProjectConfig.CURRENT.angelscriptConfig != null || ProjectConfig.CURRENT.typescriptConfig != null;
    public static LinkedList<BaseCodeGenerator> codeGenerators = new LinkedList<>();

    //public static String svnRevision;

    public static void main(String[] args) throws IOException {
        SUtils.checkAssertion();

        serverClaszMgr.addInfoBaseClasz();

        //checkEnv();

        initCodeGenerators();

        cleanDir();

        scanFiles();

        for(BaseCodeGenerator codeGenerator: codeGenerators) {
            codeGenerator.saveAllFiles();
        }
    }

    private static void checkEnv() throws UnexpectedException {
        System.out.println("=== checking integer and double ");
        for(int i = 0; i < 2000000000; ++i) {
            int d = i;
            if((int)d != i) {
                throw new UnexpectedException("fatal error: " + i);
            }
        }
    }

    private static void initCodeGenerators() throws IOException {
        inputDir = ProjectConfig.CURRENT.inputDir;

        Config.init(ProjectConfig.CURRENT.aggregateConfig);

        if(ProjectConfig.CURRENT.javaConfig != null) {
            JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(serverClaszMgr, serverSheetMgr);
            javaCodeGenerator.initDirs(inputDir, ProjectConfig.CURRENT.javaConfig);
            codeGenerators.add(javaCodeGenerator);
        }
        if(ProjectConfig.CURRENT.luaConfig != null) {
            LuaCodeGenerator luaCodeGenerator = new LuaCodeGenerator(serverClaszMgr, serverSheetMgr);
            luaCodeGenerator.initDirs(inputDir, ProjectConfig.CURRENT.luaConfig);
            codeGenerators.add(luaCodeGenerator);
        }
        if(ProjectConfig.CURRENT.angelscriptConfig != null) {
            AngelscriptCodeGenerator angelscriptCodeGenerator = new AngelscriptCodeGenerator(serverClaszMgr, serverSheetMgr);
            angelscriptCodeGenerator.initDirs(inputDir, ProjectConfig.CURRENT.angelscriptConfig);
            codeGenerators.add(angelscriptCodeGenerator);
        }
        if(ProjectConfig.CURRENT.typescriptConfig != null) {
            TypescriptCodeGenerator typescriptCodeGenerator = new TypescriptCodeGenerator(serverClaszMgr, serverSheetMgr);
            typescriptCodeGenerator.initDirs(inputDir, ProjectConfig.CURRENT.typescriptConfig);
            codeGenerators.add(typescriptCodeGenerator);
        }

        //svnRevision = SUtils.getSVNRevision(inputDir);
    }

    private static void cleanDir() throws IOException {
        System.out.println("=== cleaning files ");
        for(BaseCodeGenerator codeGenerator: codeGenerators) {
            codeGenerator.cleanDirs();
        }
    }

    private static void scanFiles() throws IOException {
        System.out.println("=== searching " + inputDir);

        List<String> fileList = SUtils.listAllFiles(inputDir, true, null, new String[]{"\\.xlsx?$"});
        System.out.println("=== parsing all files");

        fileList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                // 子目录应该往后排，让基类先解析
                o1 = o1.substring(inputDir.length());
                o2 = o2.substring(inputDir.length());
                int part1Count = o1.split("[\\\\/]").length;
                int part2Count = o2.split("[\\\\/]").length;
                if(part1Count != part2Count) {
                    return part1Count - part2Count;
                }
                return o1.compareTo(o2);
            }
        });

        for (String inputFile : fileList) {
            String relFileName = inputFile.substring(inputDir.length());

            // 临时文件跳过
            if (relFileName.contains("~")) {
                continue;
            }

            if (relFileName.contains("consts/")) {
                System.out.println("  scanning " + inputFile);
                scanConstFile(inputFile);
            }
        }

        for (String inputFile : fileList) {
            String relFileName = inputFile.substring(inputDir.length());

            // 临时文件跳过
            if (relFileName.contains("~")) {
                continue;
            }

            if (relFileName.contains("consts/")) {
                System.out.println("  parsing " + inputFile);
                processConstFile(inputFile);
            }
        }

        for (String inputFile : fileList) {
            String relFileName = inputFile.substring(inputDir.length());

            // 临时文件跳过
            if (relFileName.contains("~")) {
                continue;
            }

            if (relFileName.contains("consts/")) {
                continue;
            }

            processTableFile(inputFile);
        }

        serverClaszMgr.updateOwnerObjClaszes();
        serverSheetMgr.createRootSheet();
        serverSheetMgr.generateDynamicServices(true);
        serverSheetMgr.generateDynamicServices(false);
    }

    private static void scanConstFile(String inputFile) throws UnexpectedException {
        Workbook workbook = SUtils.readExcel(inputFile);
        for(int i = 0; i < workbook.getNumberOfSheets(); ++i) {
            Sheet worksheet = workbook.getSheetAt(i);
            String sheetName = worksheet.getSheetName();
            if(sheetName.startsWith("#")) {
                System.out.println("    ignoring sheet: " + sheetName);
            }
            else {
                System.out.println("    parsing sheet: " + sheetName);
                List<List<String>> tableData = XlsxUtil.loadTableData(worksheet);

                String sheetTypeName = tableData.get(0).get(0);
                String sheetFlags = tableData.get(0).get(1);
                String sheetComment = tableData.get(0).size() > 2 ? tableData.get(0).get(2) : "";

                if(!sheetTypeName.matches("^\\w+\\s+\\w+$")) {
                    throw new UnexpectedException("invalid sheet type name: " + sheetTypeName);
                }

                String typeName = sheetTypeName.split("\\s+")[0];
                String varName = sheetTypeName.split("\\s+")[1];
                if(typeName.equals("enum")) {
                    if(tableData.get(2).get(0).equals("ename") == false) {
                        throw new UnexpectedException("fatal error");
                    }
                    if(tableData.get(2).get(1).equals("value") == false) {
                        throw new UnexpectedException("fatal error");
                    }
                    String valueTypeName = tableData.get(3).get(1);
                    JavaUtils.addEnumType(varName, valueTypeName);
                    LuaUtils.addEnumType(varName);
                    AsUtils.addEnumType(varName, valueTypeName);
                    TypescriptUtils.addEnumType(varName, valueTypeName);
                    for(int row = 5; row < tableData.size(); ++row) {
                        String strValue = tableData.get(row).get(0);
                        String intValue = tableData.get(row).get(1);
                        if(intValue.matches("^-?\\d+$")) {
                            JavaUtils.addEnumValue(varName, strValue.replace("-", ""), Integer.parseInt(intValue), !strValue.startsWith("-"));
                            LuaUtils.addEnumValue(varName, strValue.replace("-", ""), Integer.parseInt(intValue), !strValue.startsWith("-"));
                            AsUtils.addEnumValue(varName, strValue.replace("-", ""), Integer.parseInt(intValue), !strValue.startsWith("-"));
                            TypescriptUtils.addEnumValue(varName, strValue.replace("-", ""), Integer.parseInt(intValue), !strValue.startsWith("-"));
                        }
                    }
                }
            }
        }
    }

    private static void processConstFile(String inputFile) throws UnexpectedException {
        String moduleName = SUtils.getBaseNameWithoutExtName(inputFile);
        ModuleInfo module = serverClaszMgr.addModule(moduleName);

        Workbook workbook = SUtils.readExcel(inputFile);
        for(int i = 0; i < workbook.getNumberOfSheets(); ++i) {
            Sheet worksheet = workbook.getSheetAt(i);
            String sheetName = worksheet.getSheetName();
            if(sheetName.startsWith("#")) {
                System.out.println("    ignoring sheet: " + sheetName);
            }
            else {
                System.out.println("    parsing sheet: " + sheetName);
                List<List<String>> tableData = XlsxUtil.loadTableData(worksheet);
                tableData = XlsxUtil.revertTableData(tableData);
                processConstData(module, tableData, inputFile, sheetName);
            }
        }
    }

    private static void processTableFile(String inputFile) throws IOException {
        System.out.println("  parsing " + inputFile);

        String bookName = SUtils.getBaseNameWithoutExtName(inputFile);
        if(bookName.matches("^\\d+\\w+$")) {
            bookName = bookName.replaceFirst("^\\d+", "");
        }

        BookInfo serverBook = new BookInfo(bookName);
        Workbook workbook = SUtils.readExcel(inputFile);
        for (int i = 0; i < workbook.getNumberOfSheets(); ++i) {
            Sheet worksheet = workbook.getSheetAt(i);
            String sheetName = worksheet.getSheetName();
            if(sheetName.startsWith("#")) {
                System.out.println("    ignoring sheet: " + sheetName);
            }
            else {
                System.out.println("    parsing sheet: " + sheetName);
                List<List<String>> tableData = XlsxUtil.loadTableData(worksheet);
                tableData = XlsxUtil.revertTableData(tableData);
                processTableData(workbook, serverBook, tableData, inputFile, sheetName);
            }
        }
        serverSheetMgr.addBook(serverBook);
    }

    private static void processConstData(ModuleInfo module, List<List<String>> tableData, String inputFile, String sheetName) throws UnexpectedException {
        // 获得行数与列数
        int maxRow = tableData.size();
        int maxCol = tableData.get(1).size();
        if (maxRow < XlsxUtil.DATA_FIRST_ROW_INDEX - 1) {
            throw new UnexpectedException(inputFile + " [" + sheetName + "]: maxRow " + maxRow + " invalid");
        }
        if (maxCol < 1) {
            throw new UnexpectedException(inputFile + " [" + sheetName + "]: maxCol " + maxCol + " invalid");
        }

        String sheetTypeName = tableData.get(0).get(0);
        String sheetFlags = tableData.get(0).get(1);
        String sheetComment = tableData.get(0).size() > 2 ? tableData.get(0).get(2) : "";

        if(!sheetTypeName.matches("^\\w+\\s+\\w+$")) {
            throw new UnexpectedException("invalid sheet type name: " + sheetTypeName);
        }

        String typeName = sheetTypeName.split("\\s+")[0];
        String varName = sheetTypeName.split("\\s+")[1];

        ArrayList<FieldInfo> fields = new ArrayList<>();
        for (int colIndex = 0; colIndex < maxCol; ++colIndex) {
            FieldInfo field = parseFieldInfo(tableData, colIndex, typeName);
            if(field == null) {
                break;
            }
            field.setTargetMaybeConst();
            fields.add(field);
        }
        ClaszInfo clasz = new ClaszInfo(varName, null, sheetFlags, fields);

        if(typeName.equals("const")) {
            JSONObject json = null;
            for(int row = 5; row < 6; ++row) {
                JSONObject itemJson = new JSONObject();
                for(FieldInfo field: clasz.fields) {
                    String cellData = tableData.get(5).get(field.index);
                    insertCellValueToRecordJson(true, itemJson, field.name, field, cellData);
                    insertCellValueToRecordJson(false, itemJson, field.name, field, cellData);
                }
                json = itemJson;
            }

            ConstSheetInfo constSheet = new ConstSheetInfo(clasz, json);
            module.addConstSheet(constSheet);
        }
        else if(typeName.equals("map")) {
            JSONArray json = new JSONArray();
            for(int row = 5; row < tableData.size(); ++row) {
                JSONObject itemJson = new JSONObject();
                for(FieldInfo field: clasz.fields) {
                    String cellData = tableData.get(row).get(field.index);
                    insertCellValueToRecordJson(true, itemJson, field.name, field, cellData);
                    insertCellValueToRecordJson(false, itemJson, field.name, field, cellData);
                }
                json.put(itemJson);
            }

            HashSheetInfo sheet = new HashSheetInfo(clasz, json);
            module.addHashShet(sheet);
        }
        else if(typeName.equals("enum")) {
            JSONArray json = new JSONArray();
            for(int row = 5; row < tableData.size(); ++row) {
                JSONObject itemJson = new JSONObject();
                for(FieldInfo field: clasz.fields) {
                    String cellData = tableData.get(row).get(field.index);
                    if(field.index == 0 && !field.name.equals("ename")) {
                        throw new UnexpectedException("fatal error: " + field.name);
                    }
                    if(field.index == 1 && !field.name.equals("value")) {
                        throw new UnexpectedException("fatal error: " + field.name);
                    }
                    if(field.index == 0) {
                        if(cellData.startsWith("-")) {
                            cellData = cellData.substring(1);
                            itemJson.put("__needSkip", true);
                        }
                        else {
                            itemJson.put("__needSkip", false);
                        }
                        insertCellValueToRecordJson(true, itemJson, field.name, field, cellData);
                        insertCellValueToRecordJson(false, itemJson, field.name, field, cellData);
                    }
                    else if(field.index == 1 || field.name.equals("comment")) {
                        insertCellValueToRecordJson(true, itemJson, field.name, field, cellData);
                        insertCellValueToRecordJson(false, itemJson, field.name, field, cellData);
                    }
                    else if(itemJson.getBoolean("__needSkip")) {
                    }
                    else {
                        insertCellValueToRecordJson(true, itemJson, field.name, field, cellData);
                        insertCellValueToRecordJson(false, itemJson, field.name, field, cellData);
                    }
                }
                json.put(itemJson);
            }

            EnumSheetInfo sheet = new EnumSheetInfo(clasz, json);
            module.addEnumSheet(sheet);
        }
    }

    private static void processTableData(Workbook workbook, BookInfo serverBook, List<List<String>> tableData, String inputFile, String sheetName) throws IOException {
        // 获得行数与列数
        int maxRow = tableData.size();
        int maxCol = tableData.get(1).size();
        if (maxRow < XlsxUtil.DATA_FIRST_ROW_INDEX - 1) {
            throw new UnexpectedException(inputFile + " [" + sheetName + "]: maxRow " + maxRow + " invalid");
        }
        if (maxCol < 1) {
            throw new UnexpectedException(inputFile + " [" + sheetName + "]: maxCol " + maxCol + " invalid");
        }

        TreeMap<Integer, FieldInfo> serverFieldsByIndex = new TreeMap<>();
        TreeMap<String, FieldInfo> serverFieldsByName = new TreeMap<>();

        String sheetTypeName = tableData.get(0).get(0);
        String sheetFlags = tableData.get(0).get(1);
        String sheetComment = tableData.get(0).size() > 2 ? tableData.get(0).get(2) : "";

        String baseClaszName = "";
        if(sheetTypeName.contains(":")) {
            String[] parts = sheetTypeName.split(":");
            if(parts.length != 2) {
                throw new UnexpectedException("Invalid sheet type name: " + sheetTypeName);
            }
            baseClaszName = parts[1];
            sheetTypeName = parts[0];
        }

        if(!sheetTypeName.matches("^\\w+(?:\\[\\]|\\{\\})?$")) {
            throw new UnexpectedException("Invalid sheet type name: " + sheetTypeName);
        }

        for (int colIndex = 0; colIndex < maxCol; ++colIndex) {
            FieldInfo field = parseFieldInfo(tableData, colIndex, "class");
            if(field == null) {
                maxCol = colIndex;
                break;
            }

            if(workbook.getSheet(field.name) != null) {
                field.setTargetClasz();
            }

            // 添加服务器项
            if (serverFieldsByName.get(field.name) != null) {
                throw new UnexpectedException("Duplicated field name: " + field.name);
            }
            serverFieldsByName.put(field.name, field);
            serverFieldsByIndex.put(field.index, field);
            //print "   adding for server $colInfo->{name}\n";
        }

        // 添加类和表格信息
        SheetInfo serverSheet = createTableAndSheet(serverBook, sheetName, sheetTypeName, baseClaszName, sheetFlags, serverFieldsByIndex);

        // 生成数据
        genTableData(true, inputFile, serverBook, serverSheet, tableData, maxRow, maxCol);
        genTableData(false, inputFile, serverBook, serverSheet, tableData, maxRow, maxCol);
    }

    public static FieldInfo searchFieldInColumnUpward(ColumnInfo column, String fullName) throws UnexpectedException {
        String[] parts = fullName.split("\\.");
        if(parts.length > 2) {
            throw new UnexpectedException("fatal error: " + fullName);
        }
        if(parts.length == 2) {
            String targetSheetName = parts[0];
            String targetColumnName = parts[1];
            for(; column != null; column = column.parentSheet.parentColumn) {
                if(column.parentSheet.name.equals(targetSheetName)) {
                    ColumnInfo targetColumn = column.parentSheet.columnsByName.get(targetColumnName);
                    if(targetColumn != null) {
                        return targetColumn.field;
                    }
                }
            }
            throw new UnexpectedException("sheet not found: " + fullName);
        }
        return null;
    }

    public static FieldInfo parseFieldInfo(List<List<String>> tableData, int index, String claszType) throws UnexpectedException {
        // 描述信息
        String desc = tableData.get(XlsxUtil.DESC_ROW_INDEX).get(index);

        String name = tableData.get(XlsxUtil.NAME_ROW_INDEX).get(index);

        // 名称，碰到无名项就要跳过, 剩下的就给策划用了
        if (name.equals("")) {
            return null;
        }

        IndexType indexType = IndexType.NotIndex;
        if(name.endsWith("?")) {
            indexType = IndexType.SingleIndex;
            name = name.substring(0, name.length() - 1);
        }
        else if(name.endsWith("*")) {
            indexType = IndexType.MultiIndex;
            name = name.substring(0, name.length() - 1);
        }

        if(claszType.equals("const")) {
            if(!name.matches("^(?:[A-Z]\\w*_)?[A-Z]\\w*$")) {
                throw new UnexpectedException("Invalid column name: " + name);
            }
        }
        else {
            if(!name.matches("^(?:[a-z]\\w*\\.)?[a-z]\\w*$")) {
                throw new UnexpectedException("Invalid column name: " + name);
            }
        }

        // 类型
        String typeName = tableData.get(XlsxUtil.TYPE_ROW_INDEX).get(index);

        String flags = "";
        String extName = "";
        String[] extValues = new String[]{};
        String extString = "";

        try {
            flags = tableData.get(XlsxUtil.EXT_ROW_INDEX).get(index);
        }
        catch (IndexOutOfBoundsException e) {
        }
        if(flags.contains("|")) {
            extString = flags.substring(flags.indexOf('|') + 1);
            flags = flags.substring(0, flags.indexOf('|'));
        }

        if(extString.matches("^\\w+=\\w+(?:,\\w+)*$")) {
            String[] parts = extString.split("=");
            extName = parts[0];
            extValues = parts[1].split(",");
        }
        else if(extString.length() > 0) {
            throw new UnexpectedException("Invalid extension: " + extString);
        }

        // 服务器和客户端是否生成数据
        if (!desc.startsWith("#")) {
            if (!SUtils.isRegMatch("^[SC]+$", flags)) {
                throw new UnexpectedException("Invlaid flags: " + flags);
            }
        }

        FieldInfo field = new FieldInfo(index, name, desc, typeName,  flags, indexType,extName, extValues);
        return field;
    }

    public static Integer castCellToInteger(String cellType, String cellData) throws UnexpectedException {
        // 转数值
        return Integer.valueOf(cellData);
    }

    public static Long castCellToLong(String cellType, String cellData) throws UnexpectedException {
        // 转数值
        return Long.valueOf(cellData);
    }

    public static Float castCellToFloat(String cellType, String cellData) throws UnexpectedException {
        // 转数值
        return Float.valueOf(cellData);
    }

    public static Double castCellToDouble(String cellType, String cellData) throws UnexpectedException {
        return Double.valueOf(cellData);
    }

    public static Boolean castCellToBool(String cellType, String cellData) throws UnexpectedException {
        if(cellData.equals("1")) {
            return Boolean.valueOf(true);
        }
        else if(cellData.equals("0")) {
            return Boolean.valueOf(false);
        }
        else {
            throw new UnexpectedException("invalid boolean value: " + cellData);
        }
    }


    public static String castCellToString(String cellType, String cellData) throws UnexpectedException {
        return cellData;
    }

    public static JSONArray castCellToJsonArray(String cellType, String cellData) throws UnexpectedException {
        return new JSONArray(cellData);
    }

    public static SheetInfo createTableAndSheet(BookInfo serverBook, String sheetName, String sheetTypeName, String baseClaszName, String sheetFlags, TreeMap<Integer, FieldInfo> serverFieldsByIndex) throws UnexpectedException {
        ColumnInfo parentColumn = serverBook.searchParentColumnForSheet(sheetName);
        String claszName = sheetTypeName.replaceAll("\\[\\]", "").replaceAll("\\{\\}", "");
        // 要找出类的字段和类持有者的字段(owner字段)
        List<FieldInfo> fieldsForOwner = new ArrayList<>();
        List<FieldInfo> fieldsForClasz = new ArrayList<>();
        for(FieldInfo field: serverFieldsByIndex.values()) {
            FieldInfo oldField = searchFieldInColumnUpward(parentColumn, field.name);
            if(oldField != null) {
                fieldsForOwner.add(oldField);
            }
            else {
                fieldsForClasz.add(field);
            }
        }
        ClaszInfo clasz = serverClaszMgr.addClasz(claszName, baseClaszName, sheetFlags, fieldsForClasz);
        if(parentColumn != null) {
            clasz.setOwnerObjClasz(parentColumn.parentSheet.clasz);
        }

        // 必须重新查找field, 因为之前的可能会失效
        List<ColumnInfo> columns = new ArrayList<>();
        for(FieldInfo field: serverFieldsByIndex.values()) {
            FieldInfo field2 = searchFieldInColumnUpward(parentColumn, field.name);
            if(field2 == null) {
                field2 = clasz.deepFindFieldUpwardBaseClasz(field.name);
            }
            if(field2 == null) {
                throw new UnexpectedException("fatal error");
            }
            columns.add(new ColumnInfo(field2, field.name, columns.size())); // 注意必须是field.name，名称中可能带sheet前缀
        }
        SheetInfo serverSheet = serverSheetMgr.addSheet(serverBook, sheetName, sheetTypeName, sheetFlags, parentColumn, columns, clasz);

        if(serverSheet == serverBook.primarySheet) {
            FieldInfo infoDataField = new FieldInfo(
                    serverClaszMgr.infoDataClaszInfo.fields.size(),
                    sheetName,
                    "",
                    sheetTypeName,
                    sheetFlags
            );
            infoDataField.setTargetClasz();
            serverClaszMgr.infoDataClaszInfo.addField(infoDataField);
        }

        return serverSheet;
    }

    public static void genTableData(boolean isServer, String inputFile, BookInfo book, SheetInfo sheet, List<List<String>> tableData, int maxRow, int maxCol) throws UnexpectedException {
        if(sheet == book.primarySheet && sheet.isMulti) {
            book.setJsonValue(isServer, new JSONArray());
        }

        // 生成数据
        for(int rowIndex = XlsxUtil.DATA_FIRST_ROW_INDEX; rowIndex < maxRow; ++rowIndex) {
            List<String> rowData = tableData.get(rowIndex);
            if(SUtils.isRegMatch("^\\s*$", rowData.get(0))) {
                continue;
            }

            JSONObject recordJson = new JSONObject();
            for(int colIndex = 0; colIndex < maxCol; ++colIndex) {
                ColumnInfo column = sheet.columnsByIndex.get(colIndex);
                if (column == null) {
                    continue;
                }
                String cellData = rowData.get(colIndex);
                insertCellValueToRecordJson(isServer, recordJson, column.name, column.field, cellData);
            }
            insertRecordJson(isServer, book, sheet, recordJson);
        }
    }

    public static void insertCellValueToRecordJson(boolean isServer, JSONObject itemJson, String name, FieldInfo field, String cellData) throws UnexpectedException {
        String cellType = field.typeName;
        String cellName = name;
        Object cellValue = null;

        if(!cellData.equals("N/A")) {
            if(cellType.equals("int")) {
                if(field.isTargetMaybeConst && cellData.matches("^$(?:[A-Z]\\w*_)*[A-Z]\\w*")) {
                    cellValue = cellData;
                }
                else {
                    cellValue = castCellToInteger(cellType, cellData);
                }
            }
            else if(cellType.equals("long")) {
                if(field.isTargetMaybeConst && cellData.matches("^$(?:[A-Z]\\w*_)*[A-Z]\\w*")) {
                    cellValue = cellData;
                }
                else {
                    cellValue = castCellToLong(cellType, cellData);
                }
            }
            else if(cellType.equals("float")) {
                cellValue = castCellToFloat(cellType, cellData);
            }
            else if(cellType.equals("double")) {
                cellValue = castCellToDouble(cellType, cellData);
            }
            else if(cellType.equals("string") || UnrealTypes.isUETypeOrSubclassOf(cellType)) {
                cellValue = castCellToString(cellType, cellData);
            }
            else if(cellType.startsWith("jsonArray|")) {
                cellValue = castCellToJsonArray(cellType, cellData);
            }
            else if(cellType.equals("bool")) {
                cellValue = castCellToBool(cellType, cellData);
            }
            else if(JavaUtils.isEnumType(cellType)) {
                cellValue = JavaUtils.getEnumIntValue(cellType, cellData);
//                cellValue = cellData;
            }
            else if(cellType.equals("int[]")) {
                JSONArray jsonArray = new JSONArray();
                List<String> intStrArray = StringUtilities.split(cellData, new char[]{' ', ';', ','});
                for(String intStr: intStrArray) {
                    jsonArray.put(castCellToInteger(cellType, intStr));
                }
                cellValue = jsonArray;
            }
            else if(cellType.equals("long[]")) {
                JSONArray jsonArray = new JSONArray();
                List<String> intStrArray = StringUtilities.split(cellData, new char[]{' ', ';', ','});
                for(String intStr: intStrArray) {
                    jsonArray.put(castCellToLong(cellType, intStr));
                }
                cellValue = jsonArray;
            }
            else if(cellType.equals("string[]") || UnrealTypes.isUETypeOrSubclassOf1D(cellType)) {
                JSONArray jsonArray = new JSONArray();
                List<String> strStrArray = StringUtilities.split(cellData.replaceAll("\r+\n", "\n"), new char[]{'\n'});
                for(String strStr: strStrArray) {
                    jsonArray.put(castCellToString(cellType, strStr));
                }
                cellValue = jsonArray;
            }
            else if(field.isTargetClasz) {
                if(field.isMultiObject()) {
                    cellValue = new JSONArray();
                }
                else {
                }
                // 在另外的表中
            }
            else if(field.isTargetMaybeConst) {
                if(cellData.matches("^(?:[A-Z]\\w*_)*[A-Z]\\w*$")) {
                    cellValue = castCellToString(cellType, cellData);
                }
                else {
                    throw new UnexpectedException("fatal error: " + cellData);
                }
            }
            else if(field.isRefObject()) {
                //ColumnInfo refColumn = serverSheetMgr.findReferencedField(cellType);
                if(field.isArray()) {
                    if(cellType.contains("bonuses.id")) {
                        JSONArray jsonArray = new JSONArray();
                        List<String> strStrArray = StringUtilities.split(cellData, new char[]{' '});
                        for(String strStr: strStrArray) {
                            jsonArray.put(castCellToString(cellType, strStr));
                        }
                        cellValue = jsonArray;
                    }
                    else {
                        JSONArray jsonArray = new JSONArray();
                        List<String> intStrArray = StringUtilities.split(cellData, new char[]{' ', ';', ','});
                        for(String intStr: intStrArray) {
                            jsonArray.put(castCellToInteger(cellType, intStr));
                        }
                        cellValue = jsonArray;
                    }
                }
                else {
                    if(cellType.contains("bonuses.id")) {
                        cellValue = castCellToString(cellType, cellData);
                    }
                    else {
                        cellValue = castCellToInteger(cellType, cellData);
                    }
                }
            }
            else {
                throw new UnexpectedException("Unexpected itemType " + cellType + " cellData: " + cellData);
            }

            if(cellValue != null) {
                itemJson.put(cellName, cellValue);
            }
            else if(field.isTargetClasz) {
                //
            }
            else {
                throw new UnexpectedException("Cell value is null");
            }
        }
        else if(!isServer && !ALWAYS_EXPORT_EMPTY_FIELDS) {
        }
        // 暂时不做有效判断，全部生成
        else if(cellType.contains("[]")) {
            itemJson.put(cellName, new JSONArray());
        }
        else if(field.isTargetClasz) {
            itemJson.put(cellName, (Object)null);
        }
        else if(cellType.equals("string")) {
            itemJson.put(cellName, "");
        }
        else if(cellType.equals("bool")) {
            itemJson.put(cellName, false);
        }
        else {
            itemJson.put(cellName, 0);
        }
    }

    public static void insertRecordJson(boolean isServer, BookInfo book, SheetInfo sheet, JSONObject recordJson) throws UnexpectedException {
        if(sheet == book.primarySheet) {
            if(sheet.isMulti) {
                ((JSONArray)book.getJsonValue(isServer)).put(recordJson);
            }
            else if(book.getJsonValue(isServer) == null) {
                book.setJsonValue(isServer, recordJson);
            }
            else {
                throw new UnexpectedException("value already exists");
            }
        }
        else {
            // 找到所有的父列，并且排序
            LinkedList<ColumnInfo> parentColumnStack = new LinkedList<>();
            for(ColumnInfo parentColumn = sheet.parentColumn; parentColumn != null; parentColumn = parentColumn.getParentColumn()) {
                parentColumnStack.addFirst(parentColumn);
            }

            Object targetJsonValue = book.getJsonValue(isServer);
            for(int i = 0; i < parentColumnStack.size(); ++i) {
                JSONObject targetObject = null;
                ColumnInfo parentColumn = parentColumnStack.get(i);
                if(parentColumn.parentSheet.isMulti) {
                    List<JSONObject> targetObjects = new LinkedList<>();
                    for(int j = 0; j < ((JSONArray)targetJsonValue).length(); ++j) {
                        JSONObject obj = ((JSONArray)targetJsonValue).getJSONObject(j);
                        if(checkTargetObj(obj, recordJson, parentColumn)) {
                            targetObjects.add(obj);
                        }
                    }
                    if(targetObjects.size() != 1) {
                        throw new UnexpectedException("fatal error");
                    }
                    targetObject = targetObjects.get(0);
                }
                else {
                    targetObject = (JSONObject)targetJsonValue;
                }

                // 最后一个父时就加入对象
                if(i == parentColumnStack.size() - 1) {
                    stripJsonValue(recordJson);
                    if(parentColumn.field.isMultiObject()) {
                        targetObject.getJSONArray(parentColumn.name).put(recordJson);
                    }
                    else if(targetObject.has(parentColumn.name) == false) {
                        targetObject.put(parentColumn.name, recordJson);
                    }
                    else {
                        throw new UnexpectedException("value is exists");
                    }
                }
                else {
                    targetJsonValue = targetObject.get(parentColumn.name);
                }
            }
        }
    }

    public static boolean checkTargetObj(JSONObject targetObj, JSONObject itemJson, ColumnInfo parentColumn) throws UnexpectedException {
        SheetInfo parentSheet = parentColumn.parentSheet;
        boolean matched = true;
        for(Object key: itemJson.keySet()) {
            String keyName = (String)key;
            String[] parts = keyName.split("\\.");
            if(parts.length == 2) {
                String targetSheetName = parts[0];
                String targetFieldName = parts[1];
                if(targetSheetName.equals(parentSheet.name)) {
                    FieldInfo targetField = parentSheet.clasz.deepFindFieldUpwardBaseClasz(targetFieldName);
                    if(targetField == null) {
                        throw new UnexpectedException("field " + targetFieldName + "not found in class " + parentSheet.clasz.name);
                    }
                    else if(targetField.typeName.equals("int")) {
                        int value1 = itemJson.getInt(keyName);
                        int value2 = targetObj.getInt(targetFieldName);
                        if(value1 != value2) {
                            matched = false;
                            break;
                        }
                    }
                    else if(targetField.typeName.equals("long")) {
                        long value1 = itemJson.getLong(keyName);
                        long value2 = targetObj.getLong(targetFieldName);
                        if(value1 != value2) {
                            matched = false;
                            break;
                        }
                    }
                    else if(targetField.typeName.equals("string")) {
                        String value1 = itemJson.getString(keyName);
                        String value2 = targetObj.getString(targetFieldName);
                        if(value1.equals(value2) == false) {
                            matched = false;
                            break;
                        }
                    }
                    else if(JavaUtils.isEnumType(targetField.typeName)) {
                        int value1 = itemJson.getInt(keyName);
                        int value2 = targetObj.getInt(targetFieldName);
                        if(value1 != value2) {
                            matched = false;
                            break;
                        }
                    }
                    else {
                        throw new UnexpectedException("unsupported type name: " + targetField.typeName);
                    }
                }
            }
        }
        return matched;
    }

    // 移除所有带前缀的值
    public static void stripJsonValue(JSONObject itemJson) {
        List<String> keysForStripping = new LinkedList<>();
        for(Object key: itemJson.keySet()) {
            String keyName = (String) key;
            if(keyName.contains(".")) {
                keysForStripping.add(keyName);
            }
        }
        for(String key: keysForStripping) {
            itemJson.remove(key);
        }
    }
}
