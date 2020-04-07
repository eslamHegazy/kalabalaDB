package General;

import java.io.IOException;

import BPTree.BPTreeLeafNode;
import kalabalaDB.DBAppException;

public interface TreeIndex<T extends Comparable<T>> {
	public GeneralReference search(T key) throws DBAppException;
	public Ref searchForInsertion(T key) throws DBAppException ;
	public boolean delete(T key) throws DBAppException;
	public boolean delete(T key, String Page_name) throws DBAppException, IOException;
	public void insert(T key, Ref recordReference) throws DBAppException, IOException;
	public LeafNode getLeftmostLeaf() throws DBAppException ;
}