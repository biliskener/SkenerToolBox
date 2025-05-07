package cn.ta.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AggregateConfig {
	public Map<String, String> aggregateItems = new LinkedHashMap<>();  // 需要聚集的目标
	public Set<String> ignoreOwnerObjItems = new TreeSet<>();           // 不添加ownerObj的类
}
