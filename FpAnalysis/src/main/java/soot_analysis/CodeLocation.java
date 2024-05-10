package soot_analysis;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import static soot_analysis.Utils.join;

public class CodeLocation {
	public SootClass sclass;
	public SootMethod smethod;
	public Unit sunit;
	
	public CodeLocation(SootClass sclass, SootMethod smethod, Unit sunit){
		this.sclass = sclass;
		this.smethod = smethod;
		this.sunit = sunit;
	}

	public String toString(){
		return join("/", sclass, smethod.getSubSignature(), sunit);
	}
		
}
