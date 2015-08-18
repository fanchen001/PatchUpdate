package com.fanchen.update.jni;

public class PatchUpdate {

	static{
		System.loadLibrary("patch");
	}
	public static	native	int	update(String oldApkPath,	String	newApkPath,	String	patchPath);
}
