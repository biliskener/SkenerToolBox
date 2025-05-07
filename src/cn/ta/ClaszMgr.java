package cn.ta;

import java.rmi.UnexpectedException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class ClaszMgr {
    public TreeMap<String, ClaszInfo> allClaszs = new TreeMap<>();
    public TreeMap<String, ModuleInfo> allModules = new TreeMap<>();
    public ClaszInfo infoDataClaszInfo = null;

    public ClaszMgr() {
    }

    public void addInfoBaseClasz() throws UnexpectedException {
        this.infoDataClaszInfo = addClasz("InfoDataBase", "", "SC", new LinkedList<>());
    }

    public ClaszInfo addClasz(String claszName, String baseClaszName, String flags, List<FieldInfo> fields) throws UnexpectedException {
        ClaszInfo baseClasz = null;
        if(baseClaszName.isEmpty() == false) {
            baseClasz = allClaszs.get(baseClaszName);
            if(baseClasz == null) {
                throw new UnexpectedException("parent class not found: " + baseClaszName);
            }
        }
        ClaszInfo clasz = allClaszs.get(claszName);
        if(clasz == null) {
            clasz = new ClaszInfo(claszName, baseClasz, flags, fields);
            this.allClaszs.put(clasz.name, clasz);
            return clasz;
        }
        else if(clasz.isIdentical(claszName, baseClaszName, flags, fields)) {
            return clasz;
        }
        else {
            throw new UnexpectedException("Duplicated clasz name " + claszName);
        }
    }

    public ModuleInfo addModule(String moduleName) throws UnexpectedException {
        ModuleInfo module = new ModuleInfo(moduleName);
        if(allModules.get(moduleName) != null) {
            throw new UnexpectedException("Duplicated module: " + moduleName);
        }
        allModules.put(module.name, module);
        return module;
    }

    public void updateOwnerObjClaszes() {
        for(ClaszInfo clasz: this.allClaszs.values()) {
            clasz.unionOwnerObjClaszForAllSubClaszes();
        }
    }
}
