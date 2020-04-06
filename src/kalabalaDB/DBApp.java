package kalabalaDB;


import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import BPTree.BPTree;
import BPTree.GeneralReference;
import BPTree.Ref;

public class DBApp {
	// static Vector tables=new Vector();
//	public static Vector<String> tables = new Vector();
	int MaximumRowsCountinPage ;
	int nodeSize;
	public static void clear() {
		File metadata = new File("data/metadata.csv");
		if (metadata!=null) 
			metadata.delete();
		File data = new File("data/");
		String[] pages = data.list();
		if (pages==null) return;
		for (String p: pages) {
			File pageToDelete = new File("data/"+p);
			pageToDelete.delete();
		}
	}

	public void init() throws DBAppException{
		try {
			//Configuration 
			InputStream inStream = new FileInputStream("config/DBApp.properties");
			Properties bal = new Properties();
			bal.load(inStream);
			MaximumRowsCountinPage = Integer.parseInt(bal.getProperty("MaximumRowsCountinPage"));
			nodeSize=Integer.parseInt(bal.getProperty("NodeSize"));
			
			//Assuring data folder and metadata.csv exist
			File data = new File("data");
			data.mkdir();
			File metadata = new File("data/metadata.csv");
			metadata.createNewFile();
			File metaBPtree = new File("data/metaBPtree.csv");
			if(metaBPtree.createNewFile()) {
				FileWriter csvWriter = new FileWriter("data/metaBPtree.csv");
				csvWriter.append("0");
				csvWriter.flush();
				csvWriter.close();
			}
		}
		catch(IOException e) {
			System.out.println(e.getStackTrace());
			throw new DBAppException("IO Exception");
		}
		

	}
	
	public Vector<String> getTablesNames()throws DBAppException{
		Vector meta = readFile("data/metadata.csv");
		Vector res = new Vector<>();
		HashSet<String> hs = new HashSet<>();
		for (Object O : meta) {
			String[] curr = (String[]) O;
			//TODO: If added the headers row to metadata.csv ;;
			// will need to discard the first row
			if (hs.contains(curr[0]))
				continue;
			else {
				res.add(curr[0]);
				hs.add(curr[0]);
			}
		}
		return res;
	}
	public void printAllPagesInAllTables() throws DBAppException{
		printAllPagesInAllTables("AllData");
	}
	public void printAllPagesInAllTables(String fileName) throws DBAppException {
		try {
		File file = new File("data/"+fileName+".txt");
		
		FileWriter yy = new FileWriter(file);
		PrintWriter writeFile = new PrintWriter(yy);
		Vector<String> tables = getTablesNames();
		for (String tblName : tables) {
			writeFile.println("Table " + tblName + " has the following pages:\n\n");
			Table y = deserialize(tblName);
			for (String pageName : y.getPages()) {
				writeFile.println("Page " + pageName + " has the following tuples:\n");
				Page p = Table.deserialize(pageName);
				writeFile.println(p);
				p.serialize();
			}
			writeFile.println("\n\n\n");
			serialize(y);
		}
		writeFile.close();
		}
		catch (IOException e) {
			throw new DBAppException("IO Exception");
		}
	}
	public boolean exists(String strTableName) throws DBAppException{
		try {
			Vector meta = readFile("data/metadata.csv");
			for (Object O : meta) {
				String[] curr = (String[]) O;
				if (curr[0].equals(strTableName)) {
					return true;
				}
			}
			return false;
		}
		catch (Exception e) {
			return false;
		}
	
	}
	public void createTable(String strTableName, String strClusteringKey, Hashtable<String, String> htblColNameType)
			throws DBAppException {
		if (exists(strTableName)) {
			throw new DBAppException("A table with this name already exists in the database!");
		}
		Table table = new Table();
		table.setMaximumRowsCountinPage(MaximumRowsCountinPage);
		table.setTableName(strTableName);
		table.setStrClusteringKey(strClusteringKey);
		try (FileWriter writer = new FileWriter(new File("data/metadata.csv"), true)) {

			Set<String> keys = htblColNameType.keySet();
			for (String key : keys) {
//				System.out.println("Value of " + key + " is: " + htblColNameType.get(key));
				writer.append(strTableName + ",");
				writer.append(key + ",");
				String typ = htblColNameType.get(key);
				writer.append(htblColNameType.get(key) + ",");
				writer.write((strClusteringKey.equals(key)) ? "True," : "False,");
//				writer.write("False" + ",");
				writer.write("False");
				writer.write("\n");
			}

		}
		catch (IOException e) {
			throw new DBAppException("IO Exception");
		}
		serialize(table);
//		tables.add(strTableName);
	}
	
	public void insertIntoTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException {
		Table y = deserialize(strTableName);
		Object keyValue = null;
		Tuple newEntry = new Tuple();
		String keyType="";
		String keyColName="";
		ArrayList colNames=new ArrayList<String>();
		int i = 0;
		Vector meta = readFile("data/metadata.csv");
		for (Object O : meta) {
			String[] curr = (String[]) O;
			if (curr[0].equals(strTableName)) {
				String name = curr[1];
				String type = curr[2];
				colNames.add(name);
				if (!htblColNameValue.containsKey(name)) {
					throw new DBAppException("col name invalid");
				} else {
					// String strColType=(String) colTypes.get(i++);
					try {
						Class colType = Class.forName(type);
						Class parameterType = htblColNameValue.get(name).getClass();
						// System.out.println(colType+" "+parameterType);
						Class polyOriginal = Class.forName("java.awt.Polygon");
						if (colType == polyOriginal) {
							colType = Class.forName("kalabalaDB.Polygons");
						}
						if (!colType.equals(parameterType)) {
							throw new DBAppException("DATA types 8alat");
						} else {
							newEntry.addAttribute(htblColNameValue.get(name));
							if (Boolean.parseBoolean(curr[3])) {
								y.setPrimaryPos(i);
								keyValue = htblColNameValue.get(name);
								keyType=type;
								keyColName=name;
	
							}
						}
					} 
					catch(ClassNotFoundException e) {
						throw new DBAppException("Class Not Found Exception");
					}
					
				}
				i++;
			}
		}
		


		newEntry.addAttribute(new Date());
		y.insertSorted(newEntry, keyValue,keyType,keyColName,nodeSize,colNames); // TODO
		serialize(y);

	}

	public void updateTable(String strTableName, String strClusteringKey, Hashtable<String, Object> htblColNameValue)
			throws DBAppException, IOException
	{
		Table y = deserialize(strTableName);
		try {
			Vector meta = readFile("data/metadata.csv");
			Comparable key = null;
			boolean key_index = false;
			String key_column_name = "";
			// get the key from the metadata and type cast it
			for (Object O : meta) 
			{
				String[] curr = (String[]) O;
				if (curr[0].equals(strTableName) && curr[3].equals("True")) // search in metadata for the table name and the													// key
				{
					key_column_name = curr[1];
					if(curr[3].equals("True"))
						key_index = true;
					
					if (curr[2].equals("java.lang.Integer"))
						key = Integer.parseInt(strClusteringKey);
					else if (curr[2].equals("java.lang.Double"))
						key = Double.parseDouble(strClusteringKey);
					else if (curr[2].equals("java.util.Date"))
						key = parseDate(strClusteringKey);
					else if (curr[2].equals("java.lang.Boolean"))
						key = Boolean.parseBoolean(strClusteringKey);
					else if (curr[2].equals("java.awt.Polygon"))
						key = Polygons.parsePolygon(strClusteringKey);
					else
						throw new DBAppException("The key has an UNKNOWN TYPE");
						//TODO: Is the previous line good ? 
				}
			}

			// get the full information about the table
			ArrayList<String> types = new ArrayList<String>();		// types of the columns
			ArrayList<String> colnames = new ArrayList<String>(); 	// hold the names of the columns
			ArrayList<Boolean> indexed = new ArrayList<Boolean>();	// hold if the column is indexed or not
			
			// get the full information about the table
			for (Object O : meta) 
			{
				String[] curr = (String[]) O;
				if (curr[0].equals(strTableName)) // search in metadata for the table name and the key
				{
					String name = curr[1];
					String type = curr[2];
					boolean indx = Boolean.parseBoolean(curr[4]);
					types.add(type);
					colnames.add(name);
					indexed.add(indx);
				}
			}
			
			// check validity of the hashtable entries
			Set<String> hashtableKeys = htblColNameValue.keySet();
			for (String str : hashtableKeys) 
			{
				if (!colnames.contains(str)) 
				{
//					System.out.println("Invalid column types");
					throw new DBAppException("Invalid column types");
//					return;
				}
				
				int pos = colnames.indexOf(str);
				Class colType = Class.forName(types.get(pos));
				Class parameterType = htblColNameValue.get(str).getClass();
				Class polyOriginal = Class.forName("java.awt.Polygon");
				if (colType == polyOriginal) {
					colType = Class.forName("kalabalaDB.Polygons");
				}
				System.out.println(colType+" "+parameterType);
				
				if (!colType.equals(parameterType)) {
					throw new DBAppException("Data types do not match with those of the actual column of the table");	
				}
			}		
			
			
			// if the key is indexed use binary search
			if(key_index)
			{
				
				BPTree key_tree = y.getColNameBTreeIndex().get(key_column_name);
				GeneralReference GR = key_tree.search(key);  // the result of the search in the B+ tree
				ArrayList<Ref> references = GR.getALLRef();  // the entire references where the key exists
				HashSet<String> hs = new HashSet<String>();	 // the names of pages where there is a key	
				
				for(Ref r: references)
					hs.add(r.getPage());
				
				for(String p_name:hs)
				{
					Page p = Table.deserialize(p_name); 
					int i=0;
					while (i < p.getTuples().size()) 
					{
						Tuple current = p.getTuples().get(i);
		
						if (!current.getAttributes().get(y.getPrimaryPos()).equals(key)) 
							continue;
						
						// loop over the current tuple 
						for (int k = 0; k < current.getAttributes().size()-1; k++) 
						{
							System.out.printf("k=%d, %s\n",k,colnames.get(k));
							if (htblColNameValue.containsKey(colnames.get(k))) 
							{
								System.out.println(k+", before :"+current);
								current.getAttributes().setElementAt(htblColNameValue.get(colnames.get(k)), k);
								System.out.println(k+", after :"+current);
								// update the trees
								if(indexed.get(k))
								{
									
									if(types.get(k).equals("java.awt.Polygon"))
									{
										// TODO  update the R-tree 
									}
									else
									{
										BPTree t = y.getColNameBTreeIndex().get(colnames.get(k));
										Comparable old_value = (Comparable)current.getAttributes().get(k);
										Comparable new_value = (Comparable)htblColNameValue.get(colnames.get(k));
										t.delete(old_value, p_name);
										t.insert(new_value, new Ref(p_name));
									}
								}
							
							}
							
						}
						Date date = new Date();
						current.getAttributes().setElementAt(date, current.getAttributes().size()-1);
		
						i++;
					}
					System.out.println("page after: "+p);
					p.serialize();
				}
				
			}
			// if the key is not indexed use the Binary search
			else
			{
			String[] searchResult = SearchInTable(strTableName, strClusteringKey).split("#");
			Page p = Table.deserialize(searchResult[0]);
			int i = Integer.parseInt(searchResult[1]);
			
			int j = y.getPages().indexOf(searchResult[0]);
			boolean flag = true;
			while (j < y.getPages().size() && flag) 
			{
				p = Table.deserialize(y.getPages().get(j));
				
				while (i < p.getTuples().size()&& flag) 
				{
					Tuple current = p.getTuples().get(i);
	
					if (!current.getAttributes().get(y.getPrimaryPos()).equals(key)) 
					{
						flag = false;
						break;
					}
	
					for (int k = 0; k < current.getAttributes().size()-1; k++) 
					{
						System.out.printf("k=%d, %s\n",k,colnames.get(k));
						if (htblColNameValue.containsKey(colnames.get(k))) {
							System.out.println(k+", before :"+current);
							current.getAttributes().setElementAt(htblColNameValue.get(colnames.get(k)), k);
							System.out.println(k+", after :"+current);
							
							// update the trees
							if(indexed.get(k))
							{
								if(types.get(k).equals("java.awt.Polygon"))
								{
									// TODO  update the R-tree 
								}
								else
								{
								BPTree t = y.getColNameBTreeIndex().get(colnames.get(k));
								Comparable old_value = (Comparable)current.getAttributes().get(k);
								Comparable new_value = (Comparable)htblColNameValue.get(colnames.get(k));
								
								t.delete(old_value, searchResult[0]);
								t.insert(new_value, new Ref(searchResult[0]));
								}
								}
							
						}
						
					}
					Date date = new Date();
					current.getAttributes().setElementAt(date, current.getAttributes().size()-1);
	
					i++;
				}
				j++;
				i = 0;
				System.out.println("page after: "+p);
				p.serialize();
			}
			}
		}
		catch(ClassNotFoundException e) {
			throw new DBAppException("Class not found Exception");
		}
		
		serialize(y);
	}
	
	public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {
		
		Table y = deserialize(strTableName);
		Vector meta = readFile("data/metadata.csv");
		Vector<String[]> metaOfTable = new Vector();
		for (Object o : meta) {
			String[] line = (String[]) o;
			if (line[0].equals(strTableName)) {
				metaOfTable.add(line);
			}
		}
		String clusteringKey = isThereACluster(htblColNameValue, strTableName);
		y.deleteInTable(htblColNameValue, metaOfTable ,clusteringKey);
		serialize(y);
		
	}
	public String isThereACluster(Hashtable<String, Object> htblColNameValue , String strTableName) throws DBAppException
	{
		Vector meta = readFile("data/metadata.csv");
		Vector<String[]> metaOfTable = new Vector();
		for (Object o : meta) {
			String[] line = (String[]) o;
			if (line[0].equals(strTableName)) {
				if(line[3].equals("True"))
				{
					Set<String> keyss = htblColNameValue.keySet();
					for(String key : keyss)
					{
						if(key == line[1])
						{
							return line[3];
						}
					}
					
				}
			}
		}
		return null;
	}
	public static void serialize(Table table) throws DBAppException {
		try {
			FileOutputStream fileOut = new FileOutputStream("data/"+table.getTableName() + ".class");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(table);
			out.close();
			fileOut.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			throw new DBAppException("IO Exception");
		}
	}

	public static Table deserialize(String tableName) throws DBAppException {
		try {
			FileInputStream fileIn = new FileInputStream("data/"+tableName + ".class");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			
			Table xx = (Table) in.readObject();
			in.close();
			fileIn.close();
			
			return xx;
		}
		catch (IOException e) {
			e.printStackTrace();
			
			throw new DBAppException("IO Exception | Probably wrong table name (tried to operate on a table that does not exist !");
			//TODO: Fix the above line :D
		}
		catch (ClassNotFoundException e) {
			throw new DBAppException("Class Not Found Exception");
			//TODO: Fix the above line :D
		}
	}

	public static Vector readFile(String path) throws DBAppException {
		try {
			String currentLine = "";
			FileReader fileReader = new FileReader(path);
			BufferedReader br = new BufferedReader(fileReader);
			Vector metadata = new Vector();
			while ((currentLine = br.readLine()) != null) {
				metadata.add(currentLine.split(","));
			}
			return metadata;
		}
		catch(IOException e) {
			e.printStackTrace();
			throw new DBAppException("IO Exception");
		}
	}


	public String SearchInTable(String strTableName, String strKey) throws DBAppException {
		/*
		 * Table y = null; Object keyValue = null; for (Object x : tables) { y = (Table)
		 * x; if (y.getTableName().equals(strTableName)) { break; } } if (y == null) {
		 * System.err.println("NoSuchTable"); return "-1"; }
		 */
		try {
			Vector meta = readFile("data/metadata.csv");
			Comparable key = null;
			for (Object O : meta) {
				String[] curr = (String[]) O;
				if (curr[0].equals(strTableName) && curr[3].equals("True")) // search in metadata for the table name and the
																			// key
				{
					if (curr[2].equals("java.lang.Integer"))
						key = Integer.parseInt(strKey);
					else if (curr[2].equals("java.lang.Double"))
						key = Double.parseDouble(strKey);
					else if (curr[2].equals("java.util.Date"))
						key = Date.parse(strKey);
					else if (curr[2].equals("java.lang.Boolean"))
						key = Boolean.parseBoolean(strKey);
					else if (curr[2].equals("java.awt.Polygon"))
						key = (Comparable) Polygons.parsePolygon(strKey);
					else {
//						TODO:return "-1";
						throw new DBAppException("Searching for a key of unknown type !");
					}
				}
			}
	
			Table t = deserialize(strTableName);
			Vector<String> pages = t.getPages();
			// Vector<String> MinMax = t.getMin().toString() ;
	
			for (String s : pages) {
				Page p = Table.deserialize(s);
				int l = 0;
				int r = p.getTuples().size()-1;
	
				while (l <= r) {
					int m = l + (r - l) / 2;
	
					// Check if x is present at mid
					if (key.equals((p.getTuples().get(m)).getAttributes().get(t.getPrimaryPos()))) {
						while (m > 0 && key.equals((p.getTuples().get(m - 1)).getAttributes().get(t.getPrimaryPos()))) {
							m--;
						}
						return p.getPageName() + "#" + m;
					}
	
					// If x greater, ignore left half
					if (key.compareTo((p.getTuples().get(m)).getAttributes().get(t.getPrimaryPos())) < 0)
						r = m - 1;
	
					// If x is smaller, ignore right half
					else
						l = m + 1;
				}
//				p.serialize(); // added by abdo
			}
//			serialize(t); // addd by abdo
	
//			return "-1";
			throw new DBAppException("Searched for a tuple that does not exist in the table");
		}
		catch(ClassCastException e) {
			throw new DBAppException("Class Cast Exception");
		}
	}
	
	static Date parseDate(String strClusteringKey) throws DBAppException {
		try {
			SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
			Date d = s.parse(strClusteringKey);
			return d;
		}
		catch (ParseException e) {
			throw new DBAppException("Parse Exception : Entered a wrong date format");
		}
	}
	
	
	public void createBTreeIndex(String strTableName,String strColName) throws DBAppException, IOException{
		BPTree bTree=null;
		Vector meta = readFile("data/metadata.csv");
		String colType="";
		int colPosition = -1;
		for (Object O : meta) {
			String[] curr = (String[]) O;
			if (curr[0].equals(strTableName)) {
				colPosition++;
				if (curr[1].equals(strColName)) {
					colType = curr[2];
					curr[4] = "True";
					break;
				}
			}
		}

		FileWriter csvWriter = new FileWriter("O:\\6th Semester\\Data Bases II\\Project1\\kalabalaDBv4\\data\\metadata.csv");
		for (Object O : meta) {
			String[] curr = (String[]) O;
			for (int j = 0; j < curr.length; j++) {
				csvWriter.append(curr[j]);
				csvWriter.append(",");
			}
			csvWriter.append("\n");
		}
		csvWriter.flush();
		csvWriter.close();

		switch(colType){
			case "java.lang.Integer":bTree=new BPTree<Integer>(nodeSize);break;
			case "java.lang.Double":bTree=new BPTree<Double>(nodeSize);break;
			case "java.util.Date":bTree=new BPTree<Date>(nodeSize);break;
			case "java.lang.Boolean":bTree=new BPTree<Boolean>(nodeSize);break;
			case "java.lang.String":bTree=new BPTree<String>(nodeSize);break;
			case "java.awt.Polygon":bTree=new BPTree<Polygons>(nodeSize);break;
			default :throw new DBAppException("I've never seen this colType in my life");
		}
		Table table =deserialize(strTableName);
		table.createBTreeIndex(strColName,bTree,colPosition);
		serialize(table);
	}

	public Iterator selectFromTable(SQLTerm[] arrSQLTerms,
			 String[] strarrOperators)
			throws DBAppException {
		String strTableName=arrSQLTerms[0]._strTableName;
		Table t=deserialize(strTableName);
		Vector meta = readFile("data/metadata.csv");
		Vector<String[]> metaOfTable = new Vector();
		for (Object o : meta) {
			String[] line = (String[]) o;
			if (line[0].equals(strTableName)) {
				metaOfTable.add(line);
			}
		}
		Iterator<Tuple> out=t.selectFromTable(arrSQLTerms,strarrOperators,metaOfTable);
		serialize(t);
		return out;
	}
	public static void main(String[] args) throws DBAppException {
	/*	SQLTerm[] hai=new SQLTerm[2]; QUESTION
		for(int i=0;i<hai.length;i++) {
			SQLTerm x=new SQLTerm();
			hai[i]=x;
		}
		hai[0]._strTableName="a";
		System.out.println(hai[0]._strTableName); */

		
		//		clear();
////		/*
//		String strTableName = "Student";
//		DBApp dbApp = new DBApp();
//		Hashtable htblColNameType = new Hashtable();
//		htblColNameType.put("id", "java.lang.Integer");
//		htblColNameType.put("name", "java.lang.String");
//		htblColNameType.put("gpa", "java.lang.Double");
//		dbApp.createTable(strTableName, "id", htblColNameType);
//		 System.out.println("hii 1");
//		for (int i = 1; i <= 250; i += 2) {
//			Hashtable htblColNameValue = new Hashtable();
//			htblColNameValue.put("id", new Integer(i));
//			htblColNameValue.put("name", new String("Ahmed Noor"));
//			htblColNameValue.put("gpa", new Double(0.95));
//			dbApp.insertIntoTable(strTableName, htblColNameValue);
//		}
//		 System.out.println("hii 2");
//		dbApp.printAllPagesInAllTables();
//		for (int i = 2; i <= 250; i += 2) {
//			// System.out.println("hii"+i);
//			Hashtable htblColNameValue = new Hashtable();
//			htblColNameValue.put("id", new Integer(i));
//			htblColNameValue.put("name", new String("Ahmed Noor"));
//			htblColNameValue.put("gpa", new Double(0.95));
//			dbApp.insertIntoTable(strTableName, htblColNameValue);
//		}
//		 System.out.println("hii 3");
//		dbApp.printAllPagesInAllTables();
//		for (int i = 0; i < 250; i++) {
//			Hashtable htblColNameValue = new Hashtable();
//			htblColNameValue.put("id", new Integer(200));
//			htblColNameValue.put("name", new String("Ahmed Noor"));
//			htblColNameValue.put("gpa", new Double(0.95));
//			dbApp.insertIntoTable(strTableName, htblColNameValue);
//		}
//		System.out.println("hii 4");
//		dbApp.printAllPagesInAllTables();
////		 */
	}

	
//	public static void main(String[] args) throws IOException{
//		Properties properties = new Properties();
//		properties.setProperty("MaximumRowsCountinPage", "200");
//		properties.setProperty("NodeSize", "15");
//		OutputStream out = new FileOutputStream("config/DBApp.properties");
//		properties.store(out, "");
//		InputStream inStream = new FileInputStream("config/DBApp.properties");
//		Properties bal = new Properties();
//		bal.load(inStream);
//		Set s = bal.keySet();
//		for (Object x : s) {
//			String str = (String) x;
//			System.out.println(str+" "+bal.getProperty(str));
//		}
//	}
}