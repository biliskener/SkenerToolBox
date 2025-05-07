package cn.ta;

import java.util.Map;
import java.util.TreeMap;

public class UnrealTypes {
	static final Map<String, String[]> UE_TYPES = new TreeMap<String, String[]>(String::compareToIgnoreCase){{
		this.put("UStaticMesh",     new String[]{"UStaticMesh", "XUtil::LoadStaticMesh"});
		this.put("USkeletalMesh",   new String[]{"USkeletalMesh", "XUtil::LoadSkeletalMesh"});
		this.put("UBlueprint",      new String[]{"UBlueprint", "XUtil::LoadBlueprint"});
		this.put("UClass",          new String[]{"UClass", "XUtil::LoadClass"});
		this.put("UTexture2D",      new String[]{"UTexture2D", "XUtil::LoadTexture2D"});
		this.put("USoundCue",       new String[]{"USoundCue", "XUtil::LoadSoundCue"});
		this.put("USoundWave",      new String[]{"USoundWave", "XUtil::LoadSoundWave"});
		this.put("USoundBase",      new String[]{"USoundBase", "XUtil::LoadSoundBase"});
		this.put("UParticleSystem", new String[]{"UParticleSystem", "XUtil::LoadParticleSystem"});
		this.put("UMaterial",       new String[]{"UMaterial", "XUtil::LoadMaterial"});
		this.put("UAnimMontage",    new String[]{"UAnimMontage", "XUtil::LoadAnimMontage"});
		this.put("UAnimSequence",   new String[]{"UAnimSequence", "XUtil::LoadAnimSequence"});
	}};

	public static String getUETypeName(String typeName) {
		return UE_TYPES.get(typeName)[0];
	}

	public static String getLoadingCode(String typeName) {
		return UE_TYPES.get(typeName)[1];
	}

	public static boolean isUEType(String typeName) {
		return UE_TYPES.containsKey(typeName);
	}

	public static boolean isSubclassOf(String typeName) {
		return typeName.matches("^TSubclassOf<\\w+>$");
	}

	public static boolean isUETypeOrSubclassOf(String typeName) {
		return isUEType(typeName) || isSubclassOf(typeName);
	}

	public static boolean isUEType1D(String typeName) {
		return typeName.endsWith("[]") && isUEType(typeName.substring(0, typeName.length() - 2));
	}
	public static boolean isSubclassOf1D(String typeName) {
		return typeName.endsWith("[]") && isSubclassOf(typeName.substring(0, typeName.length() - 2));
	}

	public static boolean isUETypeOrSubclassOf1D(String typeName) {
		return typeName.endsWith("[]") && isUETypeOrSubclassOf(typeName.substring(0, typeName.length() - 2));
	}

	public static String trim1D(String typeName) {
		return typeName.substring(0, typeName.length()-2);
	}
}
