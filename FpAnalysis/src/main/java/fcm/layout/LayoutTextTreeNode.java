package fcm.layout;

import java.util.*;

public class LayoutTextTreeNode {

	public enum ViewTextType {
		VIEW_TEXT,   /*Texts extracted from the View's text attribute.*/
		PARENT_TEXT, /*Texts extracted from all the Views sharing the same parent (grandparent) View. */
		LAYOUT_TEXT,  /*Texts extracted from all the Views in current layout.*/
		NO_TEXT
	}
	
	public class ViewText {
		public ViewTextType textType;
		public String viewType;
		public Set<String> texts;
		public ViewText(ViewTextType textType, String viewType, Set<String> texts){
			this.textType = textType;
			this.viewType = viewType;
			this.texts = texts;
		}
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("TextType:"+textType+",");
			sb.append("ViewType:"+viewType+",");
			sb.append("ID:"+nodeID+",");
			sb.append("Text:"+texts);
			return sb.toString();
		}
	}
	
	public String nodeType;
	public int nodeID = 0;
	public Set<String> texts = new HashSet<>();
	public String name = "";
	
	public Set<String> allTexts = new HashSet<>();
	public ViewText textObj = null;
	
	public LayoutTextTreeNode parent = null;
	public List<LayoutTextTreeNode> children = null;
	
	public LayoutTextTreeNode(String type, LayoutTextTreeNode parent){
		this.nodeType = type;
		this.parent =parent;
	}
	public void addChildNode(LayoutTextTreeNode cn){
		if (children == null)
			children = new LinkedList<LayoutTextTreeNode>();
		
		children.add(cn);
	}
	
	public String toString(){
		return "<"+nodeType.trim()+", id:"+nodeID+", Text:"+texts +" >";
	}
	
	public List<String> extractTexts(){
		Set<String> set = new HashSet<>();
		extractTextsHelper(this,set);
		set.remove("");
		return new ArrayList<>(set);
	}
	
	private void extractTextsHelper(LayoutTextTreeNode node, Set<String> set){
		set.add(node.name.trim());
		for(String text: node.texts){
			set.add(text.trim());
		}
		if(node.children != null){
			for(LayoutTextTreeNode child : node.children)
				extractTextsHelper(child, set);
		}
	}
}