package soot_analysis;

import soot.SootMethod;
import soot.Unit;

public class CallgraphState implements Hashable{
	public SootMethod method;
	public Unit unit = null;
	
	CallgraphState(SootMethod method){
		this.method = method;		
	}
	
	CallgraphState(SootMethod method, Unit unit){
		this.method = method;
		this.unit = unit;
	}	
	
	public String toString(){
		return method.getSignature() + ":" + String.valueOf(unit);	
	}
	
	public String getHash(){
		return String.valueOf(method.getSignature()) + String.valueOf(unit);
	}

}
