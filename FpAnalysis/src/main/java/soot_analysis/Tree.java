package soot_analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class Tree<T extends Hashable>{
	public HashMap<String, Node<T>> nodeMap = new HashMap<String, Node<T>>();
	public Node<T> head = new Node<T>(0);
	
	public void addHead(Node<T> h){
		this.head = h;
		nodeMap.put(h.value.getHash(), h);
	}
	
	public Node<T> addChild(Node<T> b, T newNodeValue){
		Node<T> newNode = nodeMap.get(newNodeValue.getHash());
		if(newNode == null){
			newNode = new Node<T>(b.level + 1);
			newNode.value = newNodeValue;
			newNode.parent = b;
			nodeMap.put(newNodeValue.getHash(), newNode);
			b.children.add(newNode);
			return newNode;
		}else{
			b.children.add(newNode);
			return null;
//			return newNode;
		}
	}
	
	public Collection<T> getLeaves(){
		Collection<T> res = new LinkedList<T>();
		for(Node<T> n : nodeMap.values()){
			if(n.children.size() == 0){
				res.add(n.value);
			}
		}
		return res;
	}

	public Collection<T> getAllNodes(){
		Collection<T> res = new LinkedList<T>();
		for(Node<T> n : nodeMap.values()){
			res.add(n.value);
		}
		return res;
	}
	
	public String toString(){
		HashSet<String> limiter = new HashSet<String>();
		return "T(" + String.valueOf(nodeMap.size()) + "):" + head.toString(limiter, 0);
	}

	public Node<T> getNode(T nodeValue){
		Node<T> newNode = nodeMap.get(nodeValue.getHash());
		return newNode;
	}
}
