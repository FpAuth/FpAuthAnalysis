package soot_analysis;

import soot.*;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;

import java.util.*;
import java.util.Map.Entry;

import static soot_analysis.Utils.*;


public class SootAnalysis {
	public static Tree<CallgraphState> intersectTrees(Tree<CallgraphState> ft, Tree<CallgraphState> bt){
		HashMap<SootMethod, Node<CallgraphState>> ftmap = new HashMap<>();
		for(Node<CallgraphState> n : ft.nodeMap.values()){
			SootMethod mm = n.value.method;
			int level = n.level;
			if(! ftmap.containsKey(mm) || ftmap.get(mm).level > level){
				ftmap.put(mm, n);
			}
		}
		HashMap<SootMethod, Node<CallgraphState>> btmap = new HashMap<>();
		for(Node<CallgraphState> n : bt.nodeMap.values()){
			SootMethod mm = n.value.method;
			int level = n.level;
			if(! btmap.containsKey(mm) || btmap.get(mm).level > level){
				btmap.put(mm, n);
			}
		}
		
		int candidateDepthF = Integer.MAX_VALUE;
		int candidateDepthB = Integer.MAX_VALUE;
		Node<CallgraphState> c1 = null;
		Node<CallgraphState> c2 = null;
		for(Entry<SootMethod, Node<CallgraphState>>  ee : ftmap.entrySet()){
			SootMethod mm = ee.getKey();
			Node<CallgraphState> n1 = ee.getValue();
			Node<CallgraphState> n2 = btmap.get(mm);
			if(n2!=null){
				int depthF = n1.level;
				int depthB = n2.level;
				if(depthB < candidateDepthB || (depthB == candidateDepthB && depthF < candidateDepthF)){
					candidateDepthF = depthF;
					candidateDepthB = depthB;
					c1 = n1;
					c2 = n2;
				}
			}	
		}

		Tree<CallgraphState> res = null;
		if(c1!=null && c2!=null){
			res = new Tree<>();
			Node<CallgraphState> cnode = c1;
			List<Node<CallgraphState>> nlist = new LinkedList<>();
			while(cnode != null){
				nlist.add(0, cnode);
				cnode = cnode.parent;
			}
			Node<CallgraphState> prev = new Node<CallgraphState>(nlist.get(0));
			prev.level = 0;
			nlist.remove(0);
			res.addHead(prev);
			for(Node<CallgraphState> n : nlist){
				prev = res.addChild(prev, n.value);
			}
			cnode = c2;
			while(cnode != null){
				prev = res.addChild(prev, cnode.value);
				cnode = cnode.parent;
			}
		}
		return res;
	}

	public static boolean handleIntFlag(SootContext SC, CodeLocation cl, Value sv, int targetFlag, String matchType){
		int finalValue;
		String valueString = sv.toString();

		if(targetFlag == 0 & valueString.equals("null")){
			if(matchType.equals("equal")){
				return true;
			}
		}
		if(sv.getType().toString().equals("int")){
			while(valueString.startsWith("$")){
				Unit newUnit = SC.getDefUnit(valueString, cl.smethod, true);
				if(newUnit == null){
					print("***[SA] handleIntFlag***", "getDefUnit returns null");
				}
				String newValue = "";
				for(ValueBox vb : newUnit.getUseBoxes()) {
					String boxType = vb.getClass().getSimpleName();
					InvokeExpr ie = SC.getInvokeExpr(newUnit);
					boolean isNewAssignment = SC.isNewAssignment(newUnit);
					if (stringInList(boxType, Arrays.asList((new String[]{"ImmediateBox", "SValueUnitPair", "JimpleLocalBox", "IdentityRefBox"}))) ||
							(boxType.equals("LinkedRValueBox") && (ie == null || isNewAssignment))) {
						newValue = vb.getValue().toString();
						if (newValue.equals("")) {
							continue;
						} else if (isReg(newValue) || newValue.startsWith("@")) {
							break;
						}
					}
				}

				if(newValue.length() == 1 && Character.isDigit(newValue.charAt(0))) {
					valueString = newValue;
					break;
				}

				int narg = Integer.parseInt(Utils.strExtract(newValue, "@parameter", ": "));

				Collection<CodeLocation> callers = SC.getCallers(cl.smethod);
				for(CodeLocation caller: callers) {
					if(caller != null){
						Value vv = SC.getInvokeExpr(caller.sunit).getArg(narg);
						if(String.valueOf(vv).startsWith("$")) {
							valueString = vv.toString();
							cl = caller;
							break;
						}
						if(vv.toString().length() == 1 && Character.isDigit(vv.toString().charAt(0))) {
							valueString = vv.toString();
							break;
						}
						valueString = vv.toString();
						cl = caller;
					}
				}
			}
			finalValue = Integer.parseInt(valueString);
			if (matchType.equals("and")) {
				return (finalValue & targetFlag) != 0;
			} else if (matchType.equals("equal")) {
				return finalValue == targetFlag;
			}
		}
		return false;
	}


	public static Value getInvokeParameter(SootContext SC, Unit uu, int argIndex){
		return SC.getInvokeExpr(uu).getArgs().get(argIndex);
	}

	public static Value getInvokeParameter_resolve(SootContext SC, Unit uu, int argIndex, CodeLocation cl) {
		Value sv = SC.getInvokeExpr(uu).getArgs().get(argIndex);
		String valueString = sv.toString();
		if (!valueString.contains("$")) {
			return sv;
		} else if (valueString.startsWith("$")) {
			int cnt = 0;
			while (valueString.startsWith("$")){
				cnt += 1;
				if(cnt >= 100) {
					break;
				}
				Unit newUnit = SC.getDefUnit(valueString, cl.smethod, true);
				if (newUnit == null) {
					return sv;
				}
				String newValueString = "";
				Value newValue = null;
				for (ValueBox vb : newUnit.getUseBoxes()) {
					String boxType = vb.getClass().getSimpleName();
					InvokeExpr ie = SC.getInvokeExpr(newUnit);
					boolean isNewAssignment = SC.isNewAssignment(newUnit);
					if (stringInList(boxType, Arrays.asList((new String[]{"ImmediateBox", "SValueUnitPair", "JimpleLocalBox", "IdentityRefBox"}))) ||
							(boxType.equals("LinkedRValueBox") && (ie == null || isNewAssignment))) {
						newValue = vb.getValue();
						newValueString = vb.getValue().toString();
						if (newValueString.equals("")) {
							continue;
						} else if (isReg(newValueString) || newValueString.startsWith("@")) {
							break;
						}
						if (newValueString.equals("1") || newValueString.equals("0")) {
							return vb.getValue();
						}
					}
				}

				if(newValueString.startsWith("$")) {
					valueString = newValueString;
					continue;
				}
				if(!newValueString.contains("parameter")) {
					return newValue;
				}
				int narg = Integer.parseInt(Utils.strExtract(newValueString, "@parameter", ": "));

				Collection<CodeLocation> callers = SC.getCallers(cl.smethod);
				for (CodeLocation caller : callers) {
					if (caller != null) {
						Value vv = SC.getInvokeExpr(caller.sunit).getArg(narg);
						String vvString = vv.toString();
						valueString = vvString;
						if (vv.getType().toString().equals("boolean")) {
							valueString = vv.toString();
							if(valueString.equals("1") || valueString.equals("0")){
								return vv;
							}
							if(valueString.startsWith("$")) {
								cl = caller;
								break;
							}
						}
						else if(vvString.equals("0") || vvString.equals("1")) {
							return vv;
						}
					}
				}
			}
		}
		return sv;
	}

	public static Value getInvokeParameter_resolve_int(SootContext SC, Unit uu, int argIndex, CodeLocation cl) {
		Value sv = SC.getInvokeExpr(uu).getArgs().get(argIndex);
		String valueString = sv.toString();
		if (!valueString.contains("$")) {
			return sv;
		} else if (valueString.startsWith("$")) {
			int cnt = 0;
			while (valueString.startsWith("$")){
				cnt += 1;
				if(cnt >= 100) {
					break;
				}
				Unit newUnit = SC.getDefUnit(valueString, cl.smethod, true);
				if (newUnit == null) {
					return sv;
				}
				String newValueString = "";
				Value newValue = null;
				for (ValueBox vb : newUnit.getUseBoxes()) {
					String boxType = vb.getClass().getSimpleName();
					InvokeExpr ie = SC.getInvokeExpr(newUnit);
					boolean isNewAssignment = SC.isNewAssignment(newUnit);
					if (stringInList(boxType, Arrays.asList((new String[]{"ImmediateBox", "SValueUnitPair", "JimpleLocalBox", "IdentityRefBox"}))) ||
							(boxType.equals("LinkedRValueBox") && (ie == null || isNewAssignment))) {
						newValue = vb.getValue();
						newValueString = vb.getValue().toString();
						if (newValueString.equals("")) {
							continue;
						} else if (isReg(newValueString) || newValueString.startsWith("@")) {
							break;
						}
						if(isDigits(newValueString)) {
							return vb.getValue();
						}
					}
				}
				if(newValueString.startsWith("$")) {
					valueString = newValueString;
					continue;
				}
				if(!newValueString.contains("parameter")) {
					return newValue;
				}
				int narg = Integer.parseInt(Utils.strExtract(newValueString, "@parameter", ": "));

				Collection<CodeLocation> callers = SC.getCallers(cl.smethod);
				for (CodeLocation caller : callers) {
					if (caller != null) {
						Value vv = SC.getInvokeExpr(caller.sunit).getArg(narg);
						String vvString = vv.toString();
						valueString = vvString;
						if (vv.getType().toString().equals("boolean")) {
							valueString = vv.toString();
							if(isDigits(valueString)) {
								return vv;
							}
							if(valueString.startsWith("$")) {
								cl = caller;
								break;
							}
						}
						else if(isDigits(vvString)) {
							return vv;
						}
					}
				}
			}
		}
		return sv;
	}

	public static boolean isDigits(String str) {
		if (str == null || str.length() == 0) {
			return false;
		}
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isSliceToConstant(Tree<SlicerState> stree) {
		SlicerState leaf = null;
		for(SlicerState ss : stree.getLeaves()){
			if(! String.valueOf(ss.reg).equals("return")){
				if(leaf != null){
					return false;
				}else{
					leaf = ss;
				}
			}
		}
		if(leaf!=null){
			if(leaf.unit.getUseBoxes().size() == 1){
				if(Constant.class.isAssignableFrom(leaf.unit.getUseBoxes().get(0).getValue().getClass())){
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isNullSliceForAuthenticate(Tree<SlicerState> stree) {

		for(SlicerState ss : stree.getLeaves()){
			if(stringInList(String.valueOf(ss.reg), Arrays.asList(new String[] {"field", "nullreg"}))){
				continue;
			}
			if(String.valueOf(ss.reg).startsWith("@this")){
				continue;
			}
			if(String.valueOf(ss.reg).equals("return")){
				if(String.valueOf(ss.unit).contains("android.hardware.fingerprint.FingerprintManager$CryptoObject: void <init>")){
					continue;
				}
			}
			return false;
		}
		return true;
	}
}
