package RTree;


import java.io.IOException;

import java.io.Serializable;
import java.util.ArrayList;

import BPTree.BPTreeLeafNode;
import BPTree.BPTreeNode;
import General.GeneralReference;
import General.Ref;
import kalabalaDB.DBAppException;

public class RTreeInnerNode<Polygons extends Comparable<Polygons>> extends RTreeNode<Polygons>  implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
//	private RTreeNode<T>[] children;
	private String[]childrenName;
	/**
	 * create RTreeNode given order.
	 * @param n
	 * @throws IOException 
	 * @throws DBAppException 
	 */
	@SuppressWarnings("unchecked")
	public RTreeInnerNode(int n) throws DBAppException, IOException 
	{
		super(n);
		keys = new Comparable[n];
		childrenName = new String[n+1];
	}

	/**
	 * get child with specified index
	 * @return Node which is child at specified index
	 * @throws DBAppException 
	 */
	public RTreeNode<Polygons> getChild(int index) throws DBAppException 
	{
		if (childrenName[index]==null) return null;
		RTreeNode<Polygons> child=deserializeNode(childrenName[index]);
		return child;
	}
	
	

	/**
	 * creating child at specified index
	 */
	public void setChild(int index, RTreeNode<Polygons> child) 
	{
		if (child==null) childrenName[index]=null;
		else	childrenName[index] = child.nodeName;
		//child.serializeNode();//TODO can i serialize ,myself??
	}
	/**
	 * get the first child of this node.
	 * @return first child node.
	 * @throws DBAppException 
	 */
	public RTreeNode<Polygons> getFirstChild() throws DBAppException
	{
		RTreeNode<Polygons> child=deserializeNode(childrenName[0]);
		return child;
	}
	/**
	 * get the last child of this node
	 * @return last child node.
	 * @throws DBAppException 
	 */
	public RTreeNode<Polygons> getLastChild() throws DBAppException
	{
		RTreeNode<Polygons> child=deserializeNode(childrenName[numberOfKeys]);
		return child;
	}
	/**
	 * @return the minimum keys values in InnerNode
	 */
	public int minKeys()
	{
		if(this.isRoot())
			return 1;
		return (order + 2) / 2 - 1;
	}
	/**
	 * insert given key in the corresponding index.
	 * @param key key to be inserted
	 * @param Ref reference which that inserted key is located
	 * @param parent parent of that inserted node
	 * @param ptr index of pointer in the parent node pointing to the current node
	 * @return value to be pushed up to the parent.
	 * @throws IOException 
	 * @throws DBAppException 
	 */
	public PushUp<Polygons> insert(Polygons key, Ref recordReference, RTreeInnerNode<Polygons> parent, int ptr) throws DBAppException, IOException
	{
		int index = findIndex(key);
		RTreeNode<Polygons> b=deserializeNode(childrenName[index]);
		PushUp<Polygons> pushUp = b.insert(key, recordReference, this, index); //TODO this or name of parent
		
		if(pushUp == null) 
		{
			b.serializeNode();
			return null;
		}
		
		if(this.isFull())
		{
			RTreeInnerNode<Polygons> newNode = this.split(pushUp);
			Comparable<Polygons> newKey = newNode.getFirstKey();
			newNode.deleteAt(0, 0);
			newNode.serializeNode();
		    b.serializeNode();
			return new PushUp<Polygons>(newNode, newKey); //TODO recheck
		}
		else
		{
			index = 0;
			while (index < numberOfKeys && getKey(index).compareTo(key) < 0)
				++index;
			this.insertRightAt(index, pushUp.key, pushUp.newNode);
			b.serializeNode();
			return null;
		}
	}
	/**
	 * split the inner node and adjust values and pointers.
	 * @param pushup key to be pushed up to the parent in case of splitting.
	 * @return Inner node after splitting
	 * @throws IOException 
	 * @throws DBAppException 
	 */
	@SuppressWarnings("unchecked")
	public RTreeInnerNode<Polygons> split(PushUp<Polygons> pushup) throws DBAppException, IOException 
	{
		int keyIndex = this.findIndex((Polygons)pushup.key);
		int midIndex = numberOfKeys / 2 - 1;
		if(keyIndex > midIndex)				//split nodes evenly
			++midIndex;		

		int totalKeys = numberOfKeys + 1;
		//move keys to a new node
		RTreeInnerNode<Polygons> newNode = new RTreeInnerNode<Polygons>(order);
		for (int i = midIndex; i < totalKeys - 1; ++i) 
		{	
			newNode.insertRightAt(i - midIndex, this.getKey(i), this.getChild(i+1));
			numberOfKeys--;
		}
		newNode.setChild(0, this.getChild(midIndex));
		
		//insert the new key
		System.out.println(midIndex);
		if(keyIndex < totalKeys / 2)
			this.insertRightAt(keyIndex, pushup.key, pushup.newNode);
		else
			newNode.insertRightAt(keyIndex - midIndex, pushup.key, pushup.newNode);
		
		return newNode;
	}
	/**
	 * find the correct place index of specified key in that node.
	 * @param key to be looked for
	 * @return index of that given key
	 */
	public int findIndex(Polygons key) 
	{
		for (int i = 0; i < numberOfKeys; ++i) 
		{
			int cmp = getKey(i).compareTo(key);
			if (cmp > 0)
				return i;
		}
		return numberOfKeys;
	}
	/**
	 * insert at given index a given key
	 * @param index where it inserts the key
	 * @param key to be inserted at index
	 * @throws DBAppException 
	 */
	private void insertAt(int index, Comparable<Polygons> key) throws DBAppException 
	{
		for (int i = numberOfKeys; i > index; --i) 
		{
			this.setKey(i, this.getKey(i - 1));
			this.setChild(i+1, this.getChild(i));
		}
		this.setKey(index, key);
		numberOfKeys++;
		
	}
	/**insert key and adjust left pointer with given child.
	 * @param index where key is inserted
	 * @param key to be inserted in that index
	 * @param leftChild child which this node points to with pointer at left of that index
	 * @throws DBAppException 
	 */
	public void insertLeftAt(int index, Comparable<Polygons> key, RTreeNode<Polygons> leftChild) throws DBAppException 
	{
		insertAt(index, key);
		this.setChild(index+1, this.getChild(index));
		this.setChild(index, leftChild);
	}
	/**insert key and adjust right pointer with given child.
	 * @param index where key is inserted
	 * @param key to be inserted in that index
	 * @param rightChild child which this node points to with pointer at right of that index
	 * @throws DBAppException 
	 */
	public void insertRightAt(int index, Comparable<Polygons> key, RTreeNode<Polygons> rightChild) throws DBAppException
	{
		insertAt(index, key);
		this.setChild(index + 1, rightChild);
	}
	/**
	 * delete key and return true or false if it is deleted or not
	 * @throws DBAppException 
	 */
	public boolean delete(Polygons key, RTreeInnerNode<Polygons> parent, int ptr) throws DBAppException //TODO parent
	{
		boolean done = false;
		for(int i = 0; !done && i < numberOfKeys; ++i)
			if(keys[i].compareTo(key) > 0) {
				RTreeNode<Polygons> b=deserializeNode(childrenName[i]);
				done = b.delete(key, this, i);
				b.serializeNode();
			}
			
		if(!done) {
			RTreeNode<Polygons> b=deserializeNode(childrenName[numberOfKeys]);
			done = b.delete(key, this, numberOfKeys);
		    b.serializeNode();
		}
		if(numberOfKeys < this.minKeys())
		{
			if(isRoot())
			{
				this.getFirstChild().setRoot(true);
				getFirstChild().serializeNode();
				this.setRoot(false);
				return done;
			}
			//1.try to borrow
			if(borrow(parent, ptr)) {
				parent.serializeNode();
				return done;
			}
			//2.merge
			merge(parent, ptr);
		    parent.serializeNode();
		}
		return done;
	}
	
	// delete Ref not entire key
	public boolean delete(Polygons key, RTreeInnerNode<Polygons> parent, int ptr, String page_name) throws DBAppException //TODO parent
, IOException
	{
		boolean done = false;
		for(int i = 0; !done && i < numberOfKeys; ++i)
			if(keys[i].compareTo(key) > 0) {
				RTreeNode<Polygons> b=deserializeNode(childrenName[i]);
				done = b.delete(key, this, i,page_name);
				b.serializeNode();
			}
			
		if(!done) {
			RTreeNode<Polygons> b=deserializeNode(childrenName[numberOfKeys]);
			done = b.delete(key, this, numberOfKeys,page_name);
		    b.serializeNode();
		}
		if(numberOfKeys < this.minKeys())
		{
			if(this.isRoot())
			{
				this.getFirstChild().setRoot(true);
				getFirstChild().serializeNode();
				this.setRoot(false);
				return done;
			}
			//1.try to borrow
			if(borrow(parent, ptr)) {
				parent.serializeNode();
				return done;
			}
			//2.merge
			merge(parent, ptr);
		    parent.serializeNode();
		}
		return done;
	}
	
	
	/**
	 * borrow from the right sibling or left sibling in case of overflow.
	 * @param parent of the current node
	 * @param ptr index of pointer in the parent node pointing to the current node 
	 * @return true or false if it can borrow form right sibling or left sibling or it can not
	 * @throws DBAppException 
	 */
	public boolean borrow(RTreeInnerNode<Polygons> parent, int ptr) throws DBAppException
	{
		//check left sibling
		if(ptr > 0)
		{
			RTreeInnerNode<Polygons> leftSibling = (RTreeInnerNode<Polygons>) parent.getChild(ptr-1);
			if(leftSibling.numberOfKeys > leftSibling.minKeys())
			{
				this.insertLeftAt(0, parent.getKey(ptr-1), leftSibling.getLastChild());
				leftSibling.getLastChild().serializeNode(); 
				parent.deleteAt(ptr-1);
				parent.insertRightAt(ptr-1, leftSibling.getLastKey(), this);
				leftSibling.deleteAt(leftSibling.numberOfKeys - 1);
				leftSibling.serializeNode();
				return true;
			}
		}

		//check right sibling
		if(ptr < parent.numberOfKeys)
		{
			RTreeInnerNode<Polygons> rightSibling = (RTreeInnerNode<Polygons>) parent.getChild(ptr+1);
			if(rightSibling.numberOfKeys > rightSibling.minKeys())
			{
				this.insertRightAt(this.numberOfKeys, parent.getKey(ptr), rightSibling.getFirstChild());
				rightSibling.getFirstChild().serializeNode(); 
				parent.deleteAt(ptr);
				parent.insertRightAt(ptr, rightSibling.getFirstKey(), rightSibling);
				rightSibling.deleteAt(0, 0);
				rightSibling.serializeNode();
				return true;
			}
		}
		return false;
	}
	/**
	 * try to merge with left or right sibling in case of overflow
	 * @param parent of the current node 
	 * @param ptr index of pointer in the parent node pointing to the current node
	 * @throws DBAppException 
	 */
	public void merge(RTreeInnerNode<Polygons> parent, int ptr) throws DBAppException
	{
		if(ptr > 0)
		{
			//merge with left
			RTreeInnerNode<Polygons> leftSibling = (RTreeInnerNode<Polygons>) parent.getChild(ptr-1);
			leftSibling.merge(parent.getKey(ptr-1), this);
			parent.deleteAt(ptr-1);		
			leftSibling.serializeNode();
		}
		else
		{
			//merge with right
			RTreeInnerNode<Polygons> rightSibling = (RTreeInnerNode<Polygons>) parent.getChild(ptr+1);
			this.merge(parent.getKey(ptr), rightSibling);
			parent.deleteAt(ptr);
			rightSibling.serializeNode();
		}
	}
	
	/**
	 * merge the current node with the passed node and pulling the passed key from the parent
	 * to be inserted with the merged node
	 * @param parentKey the pulled key from the parent to be inserted in the merged node
	 * @param foreignNode the node to be merged with the current node
	 * @throws DBAppException 
	 */
	public void merge(Comparable<Polygons> parentKey, RTreeInnerNode<Polygons> foreignNode) throws DBAppException
	{
		this.insertRightAt(numberOfKeys, parentKey, foreignNode.getFirstChild());
		for(int i = 0; i < foreignNode.numberOfKeys; ++i)
			this.insertRightAt(numberOfKeys, foreignNode.getKey(i), foreignNode.getChild(i+1));
	}

	/**
	 * delete the key at the specified index with the option to delete the right or left pointer
	 * @param keyIndex the index whose key will be deleted
	 * @param childPtr 0 for deleting the left pointer and 1 for deleting the right pointer
	 */
	public void deleteAt(int keyIndex, int childPtr)	//0 for left and 1 for right
	{
		for(int i = keyIndex; i < numberOfKeys - 1; ++i)
		{
			keys[i] = keys[i+1];
			childrenName[i+childPtr] = childrenName[i+childPtr+1];
		}
		if(childPtr == 0)
			childrenName[numberOfKeys-1] = childrenName[numberOfKeys];
		numberOfKeys--;
	}
	
	/**
	 * searches for the record reference of the specified key
	 * @throws DBAppException 
	 */
	@Override
	public GeneralReference search(Polygons key) throws DBAppException 
	{
		RTreeNode <Polygons> b=deserializeNode(childrenName[findIndex(key)]);
		GeneralReference x= b.search(key);
		b.serializeNode();		//TODO: Can I remove this ?
		return x;
	}
	public Ref searchForInsertion(Polygons key)throws DBAppException
	{
		RTreeNode <Polygons> b=deserializeNode(childrenName[findIndex(key)]);
		Ref x= b.searchForInsertion(key);
		b.serializeNode();		//TODO: Can I remove this ?
		return x;
	}
	/**
	 * delete the key at the given index and deleting its right child
	 */
	public void deleteAt(int index) 
	{
		deleteAt(index, 1);	
	}

	public ArrayList<GeneralReference> searchMTE(Polygons key) throws DBAppException{ 
		RTreeNode <Polygons> b=deserializeNode(childrenName[findIndex(key)]);
		ArrayList<GeneralReference> res =  b.searchMTE(key);
//		b.serializeNode();		//TODO: Can I remove this ?
		return res;
	}
	public ArrayList<GeneralReference> searchMT(Polygons key) throws DBAppException{ 
		RTreeNode <Polygons> b=deserializeNode(childrenName[findIndex(key)]);
		ArrayList<GeneralReference> res =  b.searchMT(key);
//		b.serializeNode();		//TODO: Can I remove this ?
		return res;
	}
	
//	public ArrayList<GeneralReference> searchLTE(Polygons key) throws DBAppException{ 
//		RTreeNode <Polygons> b=deserializeNode(childrenName[0]);
//		ArrayList<GeneralReference> res =  b.searchLTE(key);
////		b.serializeNode();		//TODO: Can I remove this ?
//		return res;
//	}
//	public ArrayList<GeneralReference> searchLT(Polygons key) throws DBAppException{ 
//		RTreeNode <Polygons> b=deserializeNode(childrenName[0]);
//		ArrayList<GeneralReference> res =  b.searchLT(key);
////		b.serializeNode();		//TODO: Can I remove this ?
//		return res;
//	}
	
	public RTreeLeafNode searchForUpdateRef(Polygons key) throws DBAppException{
		RTreeNode b=deserializeNode(childrenName[findIndex(key)]);
		RTreeLeafNode x= b.searchForUpdateRef(key);
//		b.serializeNode();	//TODO: Can I remove this ?
		return x;
	}
	
	

}