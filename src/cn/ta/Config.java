package cn.ta;

import cn.ta.config.AggregateConfig;

import java.util.*;

public class Config {
    static final boolean JAVA_USE_ENUM = false;
    static final boolean TYPESCRIPT_USE_ENUM = true;

    public static Map<String, String> aggregateItems = new LinkedHashMap<>();  // 需要聚集的目标
    public static Set<String> ignoreOwnerObjItems = new TreeSet<>();           // 不添加ownerObj的类
    static {
//        aggregateItems.put("skills", "=SkillProto");
//        aggregateItems.put("cargoes", ">CargoProto");
//        aggregateItems.put("items", ">ItemProto");
        aggregateItems.put("baseProtos", ">Proto");
//
//        aggregateItems.put("tasks", "=TaskInfo");
//
//        aggregateItems.put("sceneElements", "=SceneElementInfo");
//        aggregateItems.put("npcServices", "=NpcServiceInfo");
//
        aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
        aggregateItems.put("routineActivities", ">RoutineActivityInfo");

        //ignoreOwnerObjItems.add("ItemDropInfo");
        //ignoreOwnerObjItems.add("QualityDropInfo");
    }

    public static void init(AggregateConfig aggregateConfig) {
        aggregateItems = aggregateConfig.aggregateItems;
        ignoreOwnerObjItems = aggregateConfig.ignoreOwnerObjItems;
    }
}
