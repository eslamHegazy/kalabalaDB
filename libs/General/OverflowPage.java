package General;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import kalabalaDB.DBAppException;
import kalabalaDB.Page;

//TODO serializing overflowpages
public class OverflowPage implements Serializable{

	private String next; //the name of the overFlowPage
	private Vector<Ref> refs;

	private int maxSize;// node size
	private String pageName;
	//private String treeName;
	
	public OverflowPage(int maxSize) throws DBAppException, IOException {
		this.maxSize=maxSize;
//		refs = new RecordReference[maxSize];
		refs = new Vector<Ref>(maxSize);
		next = null;
		//treeName=tree;
		String lastin=getFromMetaDataTree();
		pageName="Node"+lastin;	
	} 
	public Vector<Ref> getRefs() {
		return refs;
	}
	public void setRefs(Vector<Ref> refs) {
		this.refs = refs;
	}
	
	public int getTotalSize() throws DBAppException
	{
		if(next == null)
			return refs.size();
		
		OverflowPage n = deserialize(next);
		return refs.size() + n.getTotalSize();
	}

	public void addRecord(Ref recordRef) throws DBAppException, IOException {
		if (refs.size()<maxSize) 
		{
			refs.add(recordRef);
		}
		else {
			OverflowPage nextPage;
			if (next==null) 
			{
				nextPage = new OverflowPage(maxSize);
				next=nextPage.getPageName();	
			}else{
				nextPage=deserialize(next);
			}
			
			nextPage.addRecord(recordRef);
			nextPage.serialize();
		}
	}
	
	public void deleteRecord(String page_name) throws DBAppException {
		boolean deleted = false;
		
		for(Ref r: refs)
		{
			if(r.getPage().equals(page_name))
			{
				refs.remove(r);
				deleted = true;
				break;
			}
		}
			
			if(!deleted)
			{
				if(next == null)
					throw new DBAppException("The ref not found");
				
				OverflowPage n = deserialize(next);
				n.deleteRecord(page_name);
				if(n.refs.size() == 0)
					{
					this.next = n.next;
					// TODO 7ad y3ml delete ll next from the DISK
					File f = new File("data/"+n.getPageName()+".class");
					System.out.println("/////||||\\\\\\\\\\\\\\\\\\deleting file "+n.getPageName());
					f.delete();
					return;
					}
				n.serialize();
			}
		
		this.serialize();
	}

	public String getNext() {
		return next;
	}
	public OverflowPage getNext1() throws DBAppException {
		if(next==null)

			return null;
		return deserialize(next);
	}
	public void setNext(String next) {
		this.next = next;
	}
	public String getPageName() {
		return pageName;
	}
	public void updateRef(String oldpage, String newpage, int tableNameLength) throws DBAppException  {
//		int i=0;
//		int old = Integer.parseInt(oldpage.substring(tableNameLength));
//		for (;i<refs.size()&&Integer.parseInt(refs.get(i).getPage().substring(tableNameLength))<=old;i++);
//		//i--;
//		if (i==0) {
//			return false;
//		}
//		if (i<refs.size()) {
//			refs.get(i-1).setPage(newpage);
//			return true;
//		}
//		if (i==refs.size()) {
//	//		System.out.println(next);
//			OverflowPage nextPage;
//			if (next!=null&& (nextPage=deserialize(next)).updateRef(oldpage, newpage, tableNameLength)) {
//				nextPage.serialize();
//				return true;
//			}
//			else {
//				refs.get(i-1).setPage(newpage);
//			}
//		}
//		return false;
		int i=0;
		for(;i<refs.size();i++){
			if(oldpage.equals(refs.get(i).getPage())){
				refs.get(i).setPage(newpage);
				return;
			}
		}
		if(i==refs.size()){
			OverflowPage nextPage;
			if (next!=null ) {
				(nextPage=deserialize(next)).updateRef(oldpage, newpage, tableNameLength);
				nextPage.serialize();
			}	
		}
	}
	
	public void serialize() throws DBAppException{
		try {
			System.out.println("IO||||	 serialize:overflow Page:"+this.pageName);
			FileOutputStream fileOut = new FileOutputStream("data/"+ this.getPageName() + ".class"); //TODO  l name 
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
		}
		catch(IOException e) {
			throw new DBAppException("IO Exception");
		}
	}
	public OverflowPage deserialize(String name) throws DBAppException{ 
		try {
			System.out.println("IO||||	 deserialize:overflow Page:"+name);
			FileInputStream fileIn = new FileInputStream("data/"+ name + ".class");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			OverflowPage OFP = (OverflowPage) in.readObject();
			in.close();
			fileIn.close();
			return OFP;
		}
		catch(IOException e) {
			throw new DBAppException("IO Exception");
		}
		catch(ClassNotFoundException e) {
			throw new DBAppException("Class Not Found Exception");
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

	protected String getFromMetaDataTree() throws DBAppException , IOException
	{
		String lastin = "";
		Vector meta = readFile("data/metaBPtree.csv");
		int overrideLastin = 0;
		for (Object O : meta) {
			String[] curr = (String[]) O;
			lastin = curr[0];
			overrideLastin = Integer.parseInt(curr[0])+1;
			curr[0] = overrideLastin + "";
			break;
			
		}
		FileWriter csvWriter = new FileWriter("data/metaBPtree.csv");
		for (Object O : meta) {
			String[] curr = (String[]) O;
			csvWriter.append(curr[0]);
			break;
		}
		csvWriter.flush();
		csvWriter.close();
		return lastin;
	}
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("The overflow page:" + pageName + ": \n");
		for(Ref r : refs)
		{
			sb.append(r+" , ");
		}
		sb.append("\n");
		if(this.next == null)
			return sb.toString();
		try 
		{
			sb.append(deserialize(next).toString());
		}
		catch(DBAppException e)
		{
			System.out.println("PAGE EXCEPTION");
		}
		return sb.toString();
		
	}
	
	public ArrayList<Ref> ALLgetRefs() throws DBAppException {
		ArrayList<Ref> result = new ArrayList<Ref>(); 
		result.addAll(refs);
		if(next != null)
		{
			try
			{
			result.addAll(deserialize(next).ALLgetRefs());
			}
			catch(DBAppException e)
			{
				throw new DBAppException("can't find overflow page in disk");
			}
		}	
		return result;
	}
	public Ref getLastRef() throws DBAppException {
		if(next!=null){
			OverflowPage nextPage=deserialize(next);
			Ref ref=nextPage.getLastRef();
			nextPage.serialize();
			return ref;
		}else{
			return refs.get(refs.size()-1);
		}
	}
	public Ref getMaxRefPage(int tableLength) throws DBAppException {
		return getMaxRefPage(tableLength,refs.get(0));
	}
	private Ref getMaxRefPage(int tableLength, Ref ref) throws DBAppException {
		for(int i=0;i<refs.size();i++){
			if(getIntInRefPage(ref, tableLength)<getIntInRefPage(refs.get(i), tableLength)){
				ref=refs.get(i);
			}
		}
		if(next==null){
			return ref;
		}else{
			OverflowPage nextPage=deserialize(next);
			Ref reff=nextPage.getMaxRefPage(tableLength, ref);
			nextPage.serialize();
			return reff;
		}
	}
	private static int getIntInRefPage(Ref ref,int tableLength){
		return Integer.parseInt(ref.getPage().substring(tableLength));
	}
}
