package com.goldengate.couchbase.table;

import java.util.HashSet;

public class DataTypes {

	public static final int DT_NUMBER 		= 134;
	public static final int DT_VARCHAR2 	= 64;
	public static final int DT_CHAR 		= 0;
	public static final int DT_DATE			= 192;
	
	private static HashSet<Integer> hashTypes;
	
	static{
		hashTypes = new HashSet<Integer>();
		hashTypes.add(DT_NUMBER);
		hashTypes.add(DT_VARCHAR2);
		hashTypes.add(DT_CHAR);
		hashTypes.add(DT_DATE);
	}
	
	public static boolean checkType(int iType){
		return hashTypes.contains(iType);
	}
	
}
