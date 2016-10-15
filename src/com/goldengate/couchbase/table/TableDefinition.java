package com.goldengate.couchbase.table;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TableDefinition {
	
	private static final Logger log = Logger.getLogger( TableDefinition.class.getName() );
	
	private String name;
	private ColumnDefinition[] columns;
	
	private static String DEFTAB_INIT_TOKEN = "Definition for table ";
	private static String DEFTAB_END_TOKEN = "End of definition";
	
	private static HashMap<String,TableDefinition> tables = new HashMap<String,TableDefinition>();

	public static void main(String arg[]){
		try {
			loadFromFile(new File(arg[0]));
			for(String key  : tables.keySet()){
				log.log(Level.INFO, tables.get(key).toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadFromFile(File tableDefFile){
		try {
			log.log(Level.INFO, "Loading table definitions from file: " + tableDefFile.getAbsolutePath());
			Scanner scanner = new Scanner(tableDefFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if(line.startsWith(DEFTAB_INIT_TOKEN)){
					loadTableDefintion(line, scanner);
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "Unable to load table definitions from file: " + tableDefFile.getAbsolutePath());
			e.printStackTrace();
		} 
	}
	
	private static void loadTableDefintion(String initLine, Scanner scanner) {
		String tableName = initLine.substring(DEFTAB_INIT_TOKEN.length());
		log.log(Level.INFO, "Loading table: " + tableName);
		scanner.nextLine();
		scanner.nextLine();
		scanner.nextLine();
		String line = null;
		int index = 0;
		List<ColumnDefinition> columnList = new ArrayList<ColumnDefinition>();
		while(!(line = scanner.nextLine()).startsWith(DEFTAB_END_TOKEN)){
			String fields[] = line.split("\\s+");
			columnList.add(new ColumnDefinition(index ++, fields[0], Integer.parseInt(fields[1]), !"0".equals(fields[17])));
		}
		TableDefinition tdef = new TableDefinition(tableName, columnList.toArray(new ColumnDefinition[0]));
		log.log(Level.INFO, "Table: " + tableName + " loaded: " + tdef.toString());
		tables.put(tableName, tdef);		
	}
	
	
	public static TableDefinition getTableDefinition(String tableName){
		return tables.get(tableName);
	}

	public TableDefinition(String name, ColumnDefinition[] columns) {
		super();
		this.name = name;
		this.columns = columns;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ColumnDefinition[] getColumns() {
		return columns;
	}
	public void setColumns(ColumnDefinition[] columns) {
		this.columns = columns;
	}
	
	@Override
	public String toString() {
		return "TableDefinition [name=" + name + ", columns=" + Arrays.toString(columns) + "]";
	}

}

/*

GoldenGate def sample file:

 /u01/app/oracle/ggadapter/dirdef$ more hr.def 
*+- Defgen version 2.0, Encoding UTF-8
*
* Definitions created/modified  2016-01-15 00:30
*
*  Field descriptions for each column entry:
*
*     1    Name
*     2    Data Type
*     3    External Length
*     4    Fetch Offset
*     5    Scale
*     6    Level
*     7    Null
*     8    Bump if Odd
*     9    Internal Length
*    10    Binary Length
*    11    Table Length
*    12    Most Significant DT
*    13    Least Significant DT
*    14    High Precision
*    15    Low Precision
*    16    Elementary Item
*    17    Occurs
*    18    Key Column
*    19    Sub Data Type
*
Database type: ORACLE
Character set ID: UTF-8
National character set ID: UTF-16
Locale: neutral
Case sensitivity: 14 14 14 14 14 14 14 14 14 14 14 14 11 14 14 14
*
Definition for table HR.COUNTRIES
Record length: 108
Syskey: 0
Columns: 3
COUNTRY_ID      0      2        0  0  0 1 0      2      2      0 0 0 0 0 1    0 1   0
COUNTRY_NAME   64     40        6  0  0 1 0     40     40      0 0 0 0 0 1    0 0   0
REGION_ID      64     50       52  0  0 1 0     50     50     50 0 0 0 0 1    0 0   2
End of definition
*
Definition for table HR.DEPARTMENTS
Record length: 72
Syskey: 0
Columns: 4
DEPARTMENT_ID    134      8        0  0  0 1 0      8      8      8 0 0 0 0 1    0 1   3
DEPARTMENT_NAME   64     30       12  0  0 1 0     30     30      0 0 0 0 0 1    0 0   0
MANAGER_ID       134      8       48  0  0 1 0      8      8      8 0 0 0 0 1    0 0   3
....
....
 

*/