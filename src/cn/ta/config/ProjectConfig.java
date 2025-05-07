package cn.ta.config;

import cn.ta.SUtils;
import cn.ta.TypescriptConfig;

public class ProjectConfig {
	public boolean isLongyinProject = false;
	public String inputDir;
	public JavaConfig javaConfig;
	public LuaConfig luaConfig;
	public AngelscriptConfig angelscriptConfig;
	public AggregateConfig aggregateConfig;
	public TypescriptConfig typescriptConfig;

	public ProjectConfig(AggregateConfig aggregateConfig, boolean isLongyinProject, String inputDir, JavaConfig javaConfig, LuaConfig luaConfig, AngelscriptConfig angelscriptConfig, TypescriptConfig typescriptConfig) {
		this.aggregateConfig = aggregateConfig;
		this.isLongyinProject = isLongyinProject;
		this.inputDir = inputDir;
		this.javaConfig = javaConfig;
		this.luaConfig = luaConfig;
		this.angelscriptConfig = angelscriptConfig;
		this.typescriptConfig = typescriptConfig;
	}

	public static ProjectConfig CAT = new ProjectConfig(
		new AggregateConfig(){{
			aggregateItems.put("baseProtos", ">Proto");
			aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
			aggregateItems.put("routineActivities", ">RoutineActivityInfo");
		}},
		false,
		SUtils.AD("../CatPublic/Xlsx/"),
		new JavaConfig(){{
			serverJsonOutputDir = new String[]{
					SUtils.AD("../CatServer/WorldServer/json"),
					SUtils.AD("../CatServer/CrossServer/json"),
					SUtils.AD("../CatServer/GameServer/json"),
					SUtils.AD("../CatServer/SimpleClient/json")
			};
			serverCodeOutputDir = SUtils.AD("../CatServer/GameServer/src");
			serverCodeInfoNamespace = "hk.moby.cat.info";
			serverCodeUtilsNamespace = "hk.moby.cat.util";
			serverCodeInfoBaseNamespace = "hk.moby.cat.info.base";
			serverCodeConstNamespace = "hk.moby.cat.consts";
		}},
		new LuaConfig(){{
			clientOutputDir = SUtils.AD("../CatClient/assets/resources");
			clientJsonOutputDir = SUtils.AD("../CatClient/Content/Script/Json");
			clientCodeOutputDir = SUtils.AD("../CatClient/Content/Script/Info");
		}},
		null,
		null
	);

	public static ProjectConfig LONGYIN = new ProjectConfig(
		new AggregateConfig(){{
			aggregateItems.put("skills", "=SkillProto");
			aggregateItems.put("cargoes", ">CargoProto");
			aggregateItems.put("items", ">ItemProto");
			aggregateItems.put("protos", ">Proto");

			aggregateItems.put("tasks", "=TaskInfo");

			aggregateItems.put("sceneElements", "=SceneElementInfo");
			aggregateItems.put("npcServices", "=NpcServiceInfo");

			aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
			aggregateItems.put("activities", ">ActivityInfo");
		}},
		true,
		SUtils.AD("E:/MyProjects/Longyin/LongyinPublic/Xlsx/"),
		new JavaConfig(){{
			serverJsonOutputDir = new String[]{
					SUtils.AD("../LongyinServer/GameServer/json"),
					SUtils.AD("../LongyinServer/SimpleClient/json")
			};
			serverCodeOutputDir = SUtils.AD("../LongyinServer/GameServer/src");
			serverCodeInfoNamespace = "hk.moby.longyin.info";
			serverCodeUtilsNamespace = "hk.moby.longyin.util";
			serverCodeInfoBaseNamespace = "hk.moby.longyin.info.base";
			serverCodeConstNamespace = "hk.moby.longyin.consts";
		}},
		new LuaConfig(){{
			clientOutputDir = SUtils.AD("../LongyinClient/assets/resources");
			clientJsonOutputDir = SUtils.AD("../LongyinClient/Content/Script/Json");
			clientCodeOutputDir = SUtils.AD("../LongyinClient/Content/Script/Info");
		}},
		null,
		null
	);

	public static ProjectConfig ROUGELIKE = new ProjectConfig(
		new AggregateConfig(),
		false,
		"F:/UELearn/Roguelike/Xlsx/",
		new JavaConfig(){{
			serverJsonOutputDir = new String[]{
					SUtils.AD("F:/UELearn/Roguelike/Content/_Rougelike/Json")
			};
			serverCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/src");
			serverCodeInfoNamespace = "hk.moby.cat.info";
			serverCodeUtilsNamespace = "hk.moby.cat.util";
			serverCodeInfoBaseNamespace = "hk.moby.cat.info.base";
			serverCodeConstNamespace = "hk.moby.cat.consts";
		}},
		null,
		new AngelscriptConfig(){{
			clientOutputDir = SUtils.AD("F:/UELearn/Roguelike/resources");
			clientJsonOutputDir = SUtils.AD("F:/UELearn/Roguelike/Content/_Rougelike/Json");
			clientCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/Script/Info");
		}},
		null
	);

	public static ProjectConfig LONGYIN_TEST = new ProjectConfig(
			new AggregateConfig(){{
				aggregateItems.put("skills", "=SkillProto");
				aggregateItems.put("cargoes", ">CargoProto");
				aggregateItems.put("items", ">ItemProto");
				aggregateItems.put("protos", ">Proto");

				aggregateItems.put("tasks", "=TaskInfo");

				aggregateItems.put("sceneElements", "=SceneElementInfo");
				aggregateItems.put("npcServices", "=NpcServiceInfo");

				aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
				aggregateItems.put("activities", ">ActivityInfo");
			}},
			true,
			SUtils.AD("E:/MyProjects/Longyin/LongyinPublic/Xlsx/"),
			new JavaConfig(){{
				serverJsonOutputDir = new String[]{
						SUtils.AD("F:/UELearn/Roguelike/LongyinServer/GameServer/json")
				};
				serverCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/LongyinServer/GameServer/src");
				serverCodeInfoNamespace = "hk.moby.longyin.info";
				serverCodeUtilsNamespace = "hk.moby.longyin.util";
				serverCodeInfoBaseNamespace = "hk.moby.longyin.info.base";
				serverCodeConstNamespace = "hk.moby.longyin.consts";
			}},
			new LuaConfig(){{
				clientOutputDir = SUtils.AD("F:/UELearn/Roguelike/LongyinClient/assets/resources");
				clientJsonOutputDir = SUtils.AD("F:/UELearn/Roguelike/LongyinClient/Content/Script/Json");
				clientCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/LongyinClient/Content/Script/Info");
			}},
			new AngelscriptConfig(){{
				clientOutputDir = SUtils.AD("F:/UELearn/Roguelike/resources");
				clientJsonOutputDir = SUtils.AD("F:/UELearn/Roguelike/Content/_Rougelike/Json");
				clientCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/Script/Info");
			}},
			null
	);

	public static ProjectConfig CAT_TEST = new ProjectConfig(
			new AggregateConfig(){{
				aggregateItems.put("baseProtos", ">Proto");
				aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
				aggregateItems.put("routineActivities", ">RoutineActivityInfo");
			}},
			false,
			SUtils.AD("../CatPublic/Xlsx/"),
			new JavaConfig(){{
				serverJsonOutputDir = new String[]{
						SUtils.AD("F:/UELearn/Roguelike/CatServer/GameServer/json")
				};
				serverCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/CatServer/GameServer/src");
				serverCodeInfoNamespace = "hk.moby.cat.info";
				serverCodeUtilsNamespace = "hk.moby.cat.util";
				serverCodeInfoBaseNamespace = "hk.moby.cat.info.base";
				serverCodeConstNamespace = "hk.moby.cat.consts";
			}},
			new LuaConfig(){{
				clientOutputDir = SUtils.AD("F:/UELearn/Roguelike/CatClient/assets/resources");
				clientJsonOutputDir = SUtils.AD("F:/UELearn/Roguelike/CatClient/Content/Script/Json");
				clientCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/CatClient/Content/Script/Info");
			}},
			null,
			null
	);

	public static ProjectConfig MINI_CAT_TEST = new ProjectConfig(
			new AggregateConfig(){{
				aggregateItems.put("baseProtos", ">Proto");
				aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
				aggregateItems.put("routineActivities", ">RoutineActivityInfo");
			}},
			false,
			SUtils.AD("../CatPublic/Xlsx/"),
			new JavaConfig(){{
				serverJsonOutputDir = new String[]{
						SUtils.AD("F:/UELearn/Roguelike/CatServer/GameServer/json")
				};
				serverCodeOutputDir = SUtils.AD("F:/UELearn/Roguelike/CatServer/GameServer/src");
				serverCodeInfoNamespace = "hk.moby.cat.info";
				serverCodeUtilsNamespace = "hk.moby.cat.util";
				serverCodeInfoBaseNamespace = "hk.moby.cat.info.base";
				serverCodeConstNamespace = "hk.moby.cat.consts";
			}},
			null,
			null,
			new TypescriptConfig(){{
				serverJsonOutputDir = SUtils.AD("E:/MyProjects/Cat/MiniCatClient/assets/resources/json/");
				serverCodeOutputDir = SUtils.AD("E:/MyProjects/Cat/MiniCatClient/assets/src/game/");
			}}
	);

	public static ProjectConfig MINI_LONGYIN_TEST = new ProjectConfig(
			new AggregateConfig(){{
				aggregateItems.put("skills", "=SkillProto");
				aggregateItems.put("cargoes", ">CargoProto");
				aggregateItems.put("items", ">ItemProto");
				aggregateItems.put("protos", ">Proto");

				aggregateItems.put("tasks", "=TaskInfo");

				aggregateItems.put("sceneElements", "=SceneElementInfo");
				aggregateItems.put("npcServices", "=NpcServiceInfo");

				aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
				aggregateItems.put("activities", ">ActivityInfo");
			}},
			true,
			SUtils.AD("E:/MyProjects/Longyin/LongyinPublic/Xlsx/"),
			null,
			null,
			null,
			new TypescriptConfig(){{
				serverJsonOutputDir = SUtils.AD("E:/MyProjects/Cat/MiniCatClient/assets/resources/json/");
				serverCodeOutputDir = SUtils.AD("E:/MyProjects/Cat/MiniCatClient/assets/src/game/");
			}}
	);

	public static ProjectConfig SKENER = new ProjectConfig(
			new AggregateConfig(){{
				aggregateItems.put("baseProtos", ">Proto");
				aggregateItems.put("dynamicActivities", ">DynamicActivityInfo");
				aggregateItems.put("routineActivities", ">RoutineActivityInfo");
			}},
			false,
			SUtils.AD("../SkenerPublic/Xlsx/"),
			new JavaConfig(){{
				serverJsonOutputDir = new String[]{
						SUtils.AD("../SkenerJavaServer/WorldServer/json"),
						SUtils.AD("../SkenerJavaServer/CrossServer/json"),
						SUtils.AD("../SkenerJavaServer/GameServer/json"),
						SUtils.AD("../SkenerJavaServer/SimpleClient/json")
				};
				serverCodeOutputDir = SUtils.AD("../SkenerJavaServer/GameServer/src");
				serverCodeInfoNamespace = "hk.moby.cat.info";
				serverCodeUtilsNamespace = "hk.moby.cat.util";
				serverCodeInfoBaseNamespace = "hk.moby.cat.info.base";
				serverCodeConstNamespace = "hk.moby.cat.consts";
			}},
			new LuaConfig(){{
				clientOutputDir = SUtils.AD("../SkenerUnrealClient/assets/resources");
				clientJsonOutputDir = SUtils.AD("../SkenerUnrealClient/Content/Script/Json");
				clientCodeOutputDir = SUtils.AD("../SkenerUnrealClient/Content/Script/Info");
			}},
			null,
			null
	);


	public static ProjectConfig MiniSurvivors = new ProjectConfig(
			new AggregateConfig(){{
			}},
			false,
			//SUtils.AD("E:/MyProjects/MiniSurvivors/xlsx/"),
			SUtils.AD("../UnrealSurvivors/_Xlsx/"),
			null,
			null,
			null,
			new TypescriptConfig(){{
				serverJsonOutputDir = SUtils.AD("E:/MyProjects/MiniSurvivors/assets/resources/json/");
				serverCodeOutputDir = SUtils.AD("E:/MyProjects/MiniSurvivors/assets/src/game/");
			}}
	);

	public static ProjectConfig UnrealSurvivors = new ProjectConfig(
			new AggregateConfig(),
			false,
			SUtils.AD("../UnrealSurvivors/_Xlsx/"),
			null/*new JavaConfig(){{
				serverJsonOutputDir = new String[]{
						SUtils.AD("../UnrealSurvivors/Content/_Game/Json")
				};
				serverCodeOutputDir = SUtils.AD("../UnrealSurvivors/src");
				serverCodeInfoNamespace = "hk.moby.unreal_survivors.info";
				serverCodeUtilsNamespace = "hk.moby.unreal_survivors.util";
				serverCodeInfoBaseNamespace = "hk.moby.unreal_survivors.info.base";
				serverCodeConstNamespace = "hk.moby.unreal_survivors.consts";
			}}*/,
			null,
			new AngelscriptConfig(){{
				clientOutputDir = SUtils.AD("../UnrealSurvivors/resources");
				clientJsonOutputDir = SUtils.AD("../UnrealSurvivors/Content/_Game/Json");
				clientCodeOutputDir = SUtils.AD("../UnrealSurvivors/Script/Info");
				useLowerCaseStyle = true;
			}},
			null
	);


	public static ProjectConfig UnrealBrotato = new ProjectConfig(
			new AggregateConfig(),
			false,
			SUtils.AD("../UnrealBrotato/_Xlsx/"),
			null,
			null,
			new AngelscriptConfig(){{
				clientOutputDir = SUtils.AD("../UnrealBrotato/resources");
				clientJsonOutputDir = SUtils.AD("../UnrealBrotato/Content/_Game/Json");
				clientCodeOutputDir = SUtils.AD("../UnrealBrotato/Script/Info");
				useLowerCaseStyle = true;
			}},
			null
	);

	public static ProjectConfig CURRENT = ProjectConfig.UnrealBrotato;
}
