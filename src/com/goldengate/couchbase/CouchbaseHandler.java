package com.goldengate.couchbase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.goldengate.atg.datasource.*;
import com.goldengate.atg.datasource.GGDataSource.Status;
import com.goldengate.couchbase.table.ColumnDefinition;
import com.goldengate.couchbase.table.DataTypes;
import com.goldengate.couchbase.table.TableDefinition;

public class CouchbaseHandler extends AbstractHandler {

	private static final Logger log = Logger.getLogger( CouchbaseHandler.class.getName() );
	
	private Bucket bucket;
	private String bucketName;
	private String bucketPassword;
	// clusterAddress format: ip1,ip2,ip3,...
	private String clusterAddress;
	private String tableDefinitionFileName;
	
	public static final String KEY_SEPARATOR = "::";

	private long numTxs = 0;
	private long numOps = 0;

	public void setBucketName(String s){
		bucketName = s;
		log.log(Level.INFO, "bucketName: " + bucketName);
	}
	
	public void setBucketPassword(String s){
		bucketPassword = s;
		log.log(Level.INFO, "bucketPassword: " + bucketPassword);
	}
	
	public void setClusterAddress(String s){
		clusterAddress = s;
		log.log(Level.INFO, "clusterAddress: " + clusterAddress);
	}
	
	public void setTableDefinitionFileName(String s){
		tableDefinitionFileName = s;
		log.log(Level.INFO, "tableDefinitionFileName: " + tableDefinitionFileName + ". Loading table definitions");
		TableDefinition.loadFromFile(new File(tableDefinitionFileName));
	}
	
	public Bucket getBucket(){
		if(bucket == null){
			CouchbaseCluster cluster = CouchbaseCluster.create(Arrays.asList(clusterAddress.split(",")));
			if(bucketPassword != null)
				bucket = cluster.openBucket(bucketName, bucketPassword);
			else
				bucket = cluster.openBucket(bucketName);
		}
		return bucket;
	}
	
	public String getKey(DsEvent e, DsOperation operation){
		TableDefinition tdef = TableDefinition.getTableDefinition(operation.getTableName().getFullName());
		String tname = getTName(operation, tdef);
		StringBuffer sbkey = new StringBuffer(tname);
		int index = 0;
		ColumnDefinition colDef[] = tdef.getColumns();
		List<DsColumn> cols = operation.getColumns();
	    for(DsColumn col : cols){
	    	if(colDef[index].isPk()){
	    		sbkey.append(KEY_SEPARATOR);
	    		String value = "DELETE".equals(operation.getSqlType()) ? 
	    				getJsonTypeObject(col.getBeforeValue(), colDef[index].getType()).toString() : 
	    				getJsonTypeObject(col.getAfterValue() , colDef[index].getType()).toString();
	    		sbkey.append(value);
	    	}
	    	index++;
	    }
	    return sbkey.toString();		
	}
	
	
	
	private String getTName(DsOperation operation, TableDefinition tdef) {
		String tname = tdef.getName().toLowerCase();
		int dotIndex = tname.indexOf('.');
		if(dotIndex != -1)
			tname = tname.substring(dotIndex + 1);
		return tname;
	}

	public JsonObject getJson(DsEvent e, DsTransaction tx, DsOperation operation){
		
		if("DELETE".equals(operation.getSqlType()))
				return null;
		
		TableDefinition tdef = TableDefinition.getTableDefinition(operation.getTableName().getFullName());
		String tname = getTName(operation, tdef);
		int index = 0;
		ColumnDefinition colDef[] = tdef.getColumns();
		List<DsColumn> cols = operation.getColumns();
		JsonObject jsonContent = JsonObject.create();
		
		if("UPDATE".equals(operation.getSqlType()))
			jsonContent = getBucket().get(getKey(e, operation)).content();
		//INSERT
		else{
			jsonContent = JsonObject.create();
			jsonContent.put("type", tname);
		}
		
	    for(DsColumn col : cols){
	    	String val = col.getAfterValue();
	    	if(val != null && !val.isEmpty()){
	    		Object jsonVal = getJsonTypeObject(val, colDef[index].getType());
	    		jsonContent.put(colDef[index].getName().toLowerCase(), jsonVal);
	    	}
	    	index++;
	    }
	    return jsonContent;
	    
	    //return JsonObject.create().put(tname, jsonContent);
	}
	
	
	@Override
	public Status operationAdded(DsEvent e, DsTransaction tx, DsOperation operation) {

		super.operationAdded(e, tx, operation);
		numOps++;
		
		try {
			log.log(Level.INFO, "Operation on " + operation.getTableName().getFullName() + ". Type " + operation.getSqlType());
			log.log(Level.INFO, "Columns");
			List<DsColumn> cols = operation.getColumns();
			for(DsColumn col : cols){
				log.log(Level.INFO, col.getBeforeValue() + ":" + col.getAfterValue());
			}
			
			String key = this.getKey(e, operation);
			JsonObject json = getJson(e, tx, operation);
			JsonDocument doc = JsonDocument.create(key, json);
			
			switch(operation.getSqlType()){
			case "INSERT":
				getBucket().insert(doc);
				break;
			case "UPDATE":
				getBucket().replace(doc);
				break;
			case "DELETE":
				getBucket().remove(doc);
				break;
			}
		} catch (Exception e1) {
			String msg = "Error processing operation: " + e1.getMessage();
			log.log(Level.INFO, msg, e1);
			e1.printStackTrace();
		}

		return Status.OK;
	}

	@Override
	public Status transactionCommit(DsEvent e, DsTransaction tx) {
		super.transactionCommit(e, tx);
		numTxs++;
		return Status.OK;
	}

	@Override
	public String reportStatus() {
		return "Processed (mode='" + getMode() + "')" + " transactions=" + numTxs + ", operations=" + numOps;
	}

	private static Object getJsonTypeObject(String value, int type){
		Object res = null;
		try {
			switch(type) {
			case DataTypes.DT_CHAR:
			case DataTypes.DT_VARCHAR2:
				res = value;
				break;
			case DataTypes.DT_NUMBER:
				if(value.indexOf('.') != -1){
					res = Double.parseDouble(value);
				}
				else{
					res = Long.parseLong(value);
				}
				break;
			case DataTypes.DT_DATE:
				SimpleDateFormat parserSDF=new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
				res = parserSDF.parse(value).getTime();
				break;
			default:	
				res = value;
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
}
