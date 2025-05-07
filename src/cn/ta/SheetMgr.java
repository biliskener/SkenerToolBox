package cn.ta;

import com.strobel.core.StringUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

import java.rmi.UnexpectedException;
import java.util.*;

public class SheetMgr {
    public ClaszMgr claszMgr;
    public TreeMap<String, BookInfo> allBooks = new TreeMap<>();
    public TreeMap<String, SheetInfo> allPrimarySheetsMap = new TreeMap<>();
    public List<SheetInfo> allPrimarySheetList = new LinkedList<>();
    public BookInfo rootBook;
    public SheetInfo rootSheet;

    public SheetMgr(ClaszMgr claszMgr) {
        this.claszMgr = claszMgr;
    }

    public void addBook(BookInfo book) throws UnexpectedException {
        if (this.allBooks.get(book.name) == null) {
            this.allBooks.put(book.name, book);
        } else {
            throw new UnexpectedException("Duplicated book: " + book.name);
        }
    }

    public SheetInfo addSheet(BookInfo book, String sheetName, String typeName, String flags, ColumnInfo parentColumn, List<ColumnInfo> columns, ClaszInfo clasz) throws UnexpectedException {
        SheetInfo sheet = book.addSheet(sheetName, typeName, flags, parentColumn, columns, clasz);
        if(book.primarySheet == sheet) {
            if(this.allPrimarySheetsMap.get(sheet.name) != null) {
                throw new UnexpectedException("fatal error: " + sheet.name);
            }
            this.allPrimarySheetsMap.put(sheet.name, sheet);
            this.allPrimarySheetList.add(sheet);
        }

        for(String aggregateTargetSheetName: Config.aggregateItems.keySet()) {
            String aggregateClassName = Config.aggregateItems.get(aggregateTargetSheetName);
            if(aggregateClassName.startsWith("=")) {
                aggregateClassName = aggregateClassName.substring(1);
                if(sheet.clasz.name.equals(aggregateClassName)) {
                    SheetInfo aggregateParentSheet = this.allPrimarySheetsMap.get(aggregateTargetSheetName);
                    if(sheet != aggregateParentSheet) {
                        sheet.aggregateParentSheet = aggregateParentSheet;
                        aggregateParentSheet.aggregateChildSheets.add(sheet);
                    }
                }
            }
            else if(aggregateClassName.startsWith(">")) {
                aggregateClassName = aggregateClassName.substring(1);
                if(sheet.clasz.baseClasz != null && sheet.clasz.baseClasz.name.equals(aggregateClassName)) {
                    SheetInfo aggregateParentSheet = this.allPrimarySheetsMap.get(aggregateTargetSheetName);
                    if(sheet != aggregateParentSheet) {
                        sheet.aggregateParentSheet = aggregateParentSheet;
                        aggregateParentSheet.aggregateChildSheets.add(sheet);
                    }
                }
            }
            else {
                throw new UnexpectedException("fatal error");
            }
        }

        return sheet;
    }

    // 创建根sheet
    public void createRootSheet() throws UnexpectedException {
        ClaszInfo rootClasz = this.claszMgr.infoDataClaszInfo;

        TreeMap<Integer, ColumnInfo> columns = new TreeMap<>();
        for(BookInfo book: allBooks.values()) {
            SheetInfo sheet = book.primarySheet;
            FieldInfo field = rootClasz.fieldsByName.get(sheet.name);
            ColumnInfo column = new ColumnInfo(field, field.name, field.index);
            if(columns.containsKey(column.index)) {
                throw new UnexpectedException("fatal error");
            }
            else {
                columns.put(column.index, column);
            }
        }

        BookInfo rootBook = new BookInfo("info_data");
        SheetInfo rootSheet = rootBook.addSheet("infoData", rootClasz.name, "", null, new LinkedList<>(columns.values()), rootClasz);

        this.rootBook = rootBook;
        this.rootSheet = rootSheet;
    }

    public ColumnInfo findReferencedColumn(String typeName) throws UnexpectedException {
        typeName = typeName.replaceAll("\\[\\]$", "");
        typeName = typeName.replaceAll("\\?\\d+$", "");

        List<String> parts = StringUtilities.split(typeName, '.');
        SheetInfo sheet = null;
        for(int i = 0; i < parts.size() - 1; ++i) {
            String name = parts.get(i);
            if(sheet == null) {
                sheet = this.allPrimarySheetsMap.get(name);
                if(sheet == null) {
                    throw new UnexpectedException("fatal error: " + typeName);
                }
            }
            else {
                ColumnInfo column = sheet.columnsByName.get(name);
                if(column != null) {
                    sheet = column.childSheet;
                    if(sheet == null) {
                        throw new UnexpectedException("fatal error: " + typeName);
                    }
                }
                else {
                    throw new UnexpectedException("fatal error: " + typeName);
                }
            }
        }

        String columnName = parts.get(parts.size() - 1);
        ColumnInfo column = sheet.columnsByName.get(columnName);
        if(column != null) {
            return column;
        }
        else {
            throw new UnexpectedException("fatal error: " + typeName);
        }
    }

    void generateDynamicServices(boolean isServer) throws UnexpectedException {
        DYNAMICSERVICEID = 10000000;
        for(BookInfo book: this.allBooks.values()) {
            if(book.primarySheet.clasz.getTopBaseClasz().name.equals("TaskInfo")) {
                SheetInfo tasksSheet = book.primarySheet;
                JSONArray tasks = (JSONArray) book.getJsonValue(isServer);
                for(int i = 0; i < tasks.length(); ++i) {
                    JSONObject task = tasks.getJSONObject(i);
                    processTaskAcceptOrSubmitCourse(isServer, task, "accept", 1);
                    processTaskAcceptOrSubmitCourse(isServer, task, "submit", 2);
                    JSONArray taskTargets = task.getJSONArray("targets");
                    for(int k = 0; k < taskTargets.length(); ++k) {
                        int taskTargetId = taskTargets.getInt(k);
                        JSONObject taskTarget = findTaskTargetById(isServer, taskTargetId);
                        processTaskTargetCourse(isServer, task, taskTarget);
                    }
                }
            }
        }
    }

    JSONObject findSceneElementById(boolean isServer, int id) {
        SheetInfo scenesSheet = this.allPrimarySheetsMap.get("scenes");
        JSONArray scenes = (JSONArray) scenesSheet.bookInfo.getJsonValue(isServer);
        for(int i = 0; i < scenes.length(); ++i) {
            JSONObject scene = scenes.getJSONObject(i);
            JSONArray elements = scene.getJSONArray("elements");
            for(int j = 0; j < elements.length(); ++j) {
                JSONObject element = elements.getJSONObject(j);
                int elementId = element.getInt("id");
                if(elementId == id) {
                    return element;
                }
            }
        }
        return null;
    }

    JSONObject findTaskTargetById(boolean isServer, int id) {
        SheetInfo taskTargetsSheet = this.allPrimarySheetsMap.get("taskTargets");
        JSONArray taskTargets = (JSONArray) taskTargetsSheet.bookInfo.getJsonValue(isServer);
        for(int i = 0; i < taskTargets.length(); ++i) {
            JSONObject taskTarget = taskTargets.getJSONObject(i);
            if(taskTarget.getInt("id") == id) {
                return taskTarget;
            }
        }
        return null;
    }

    void processTaskAcceptOrSubmitCourse(boolean isServer, JSONObject task, String prefix, int talkType) throws UnexpectedException {
        int taskId = task.getInt("id");
        int plotCourseId = task.getInt(prefix + "PlotCourse");
        assert(plotCourseId > 0);
        if(task.getInt(prefix + "Type") != 1) {
            JSONArray targetServices = new JSONArray();
            Object _elementId = task.get(prefix + "Elements");
            if(_elementId instanceof JSONArray) {
                JSONArray elementIds = (JSONArray)_elementId;
                for(int i = 0; i < elementIds.length(); ++i) {
                    int elementId = elementIds.getInt(i);
                    addServiceIntoSceneElement(isServer, task.getString("name"), elementId, talkType, plotCourseId, taskId, 0, targetServices);
                }
            }
            else {
                throw new UnexpectedException("fatal error");
                //int elementId = (int)_elementId;
                //addServiceIntoSceneElement(task.getString("name"), elementId, targetServices);
            }
            task.put(prefix + "Services", targetServices);
        }
    }

    void processTaskTargetCourse(boolean isServer, JSONObject task, JSONObject target) throws UnexpectedException {
        int taskId = task.getInt("id");
        int type = target.getInt("type");
        if(type == 14 || type == 13 || type == 24 || type == 38 || type == 43) {
            JSONArray targetServices = new JSONArray();
            Object _elementId = target.get("elements");
            if(_elementId instanceof JSONArray) {
                JSONArray elementIds = (JSONArray)_elementId;
                for(int i = 0; i < elementIds.length(); ++i) {
                    int elementId = elementIds.getInt(i);
                    int taskTargetId = target.getInt("id");
                    int plotCourseId = target.getInt("plotCourse");
                    assert(plotCourseId > 0);
                    addServiceIntoSceneElement(isServer, task.getString("name"), elementId, 3, plotCourseId, taskId, taskTargetId, targetServices);
                }
            }
            else {
                throw new UnexpectedException("fatal error");
//                int elementId = (int)_elementId;
//                addServiceIntoSceneElement(task.getString("name"), elementId, targetServices);
            }
            target.put("services", targetServices);
        }
        else if(type == 11 || type == 12) {
            JSONArray targets = target.getJSONArray("targets");
            for(int i = 0; i < targets.length(); ++i) {
                int taskTargetId = targets.getInt(i);
                JSONObject taskTarget = findTaskTargetById(isServer, taskTargetId);
                processTaskTargetCourse(isServer, task, taskTarget);
            }
        }
    }

    void addServiceIntoSceneElement(boolean isServer, String serviceName, int elementId, int talkType, int plotCourseId, int taskId, int taskTarget, JSONArray targetServices) throws UnexpectedException {
        JSONObject sceneElement = findSceneElementById(isServer, elementId);
        if(sceneElement != null) {
            JSONObject service = new JSONObject();
            service.put("type", 2); // 交谈
            service.put("id", DYNAMICSERVICEID++);
            service.put("name", serviceName);
            service.put("talkType", talkType);
            service.put("targetTask", taskId);
            service.put("targetTaskTarget", taskTarget);
            service.put("plotCourse", plotCourseId);
            service.put("initVisible", "0");

            ClaszInfo serviceClasz = claszMgr.allClaszs.get("NpcServiceInfo");
            for(FieldInfo field: serviceClasz.fields) {
                if(!service.has(field.name)) {
                    // 暂时不做有效判断，全部生成
                    if(field.typeName.contains("[]")) {
                        service.put(field.name, new JSONArray());
                    }
                    else if(field.isTargetClasz) {
                        service.put(field.name, (Object)null);
                    }
                    else if(field.typeName.equals("string")) {
                        service.put(field.name, "");
                    }
                    else if(field.typeName.equals("bool")) {
                        service.put(field.name, false);
                    }
                    else {
                        service.put(field.name, 0);
                    }
                }
            }

            sceneElement.getJSONArray("services").put(service);
            targetServices.put(service.getInt("id"));
        }
        else {
            throw new UnexpectedException("sceneElement not found: " + elementId);
        }
    }

    static int DYNAMICSERVICEID = 10000000;
}
