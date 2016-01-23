package com.goldengate.couchbase.table;

public class ColumnDefinition {
	
	private int index;
	private String name;
	private int type;
	private boolean isPk;
	
	public ColumnDefinition(int index, String name, int type, boolean isPk) {
		super();
		this.index = index;
		this.name = name;
		this.type = type;
		this.isPk = isPk;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public boolean isPk() {
		return isPk;
	}
	public void setPk(boolean isPk) {
		this.isPk = isPk;
	}
	@Override
	public String toString() {
		return "Column [index=" + index + ", name=" + name + ", type=" + type + ", isPk=" + isPk + "]";
	}
	
}
