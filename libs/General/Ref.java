package General;

import java.io.Serializable;

public class Ref extends GeneralReference implements Serializable
{
	public boolean equals(Object o) {
		Ref x=(Ref)o;
		Ref y=(Ref)this;
		return (x.pageNo).equals(y.pageNo);
	}
	/**
	 * This class represents a pointer to the record. It is used at the leaves of the B+ tree 
	 */
	private static final long serialVersionUID = 1L;
	private String pageNo;//, indexInPage;
	
	public Ref(String pageNo)
	{
		this.pageNo = pageNo;
//		this.indexInPage = indexInPage;
	}
	
	/**
	 * @return the page at which the record is saved on the hard disk
	 */
	public String getPage()
	{
		return pageNo;
	}
	
	public void setPage(String pageNo) {
		this.pageNo=pageNo;
	}
	
	/**
	 * @return the index at which the record is saved in the page
	 */
//	public int getIndexInPage()
//	{
//		return indexInPage;
//	}
//	
	public boolean isOverflow() {
		return false;
	}
	public boolean isRecord() {
		return true;
	}
	public String toString() {
		return pageNo+"";
	}

	public void updateRef(String oldpage, String newpage, int tableNameLength) {
		updateRef(oldpage, newpage);
	}
	
	public void updateRef(String oldpage, String newpage) {
		pageNo=newpage;
		
	}
}