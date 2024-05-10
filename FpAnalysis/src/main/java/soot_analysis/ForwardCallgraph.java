package soot_analysis;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static soot_analysis.Utils.isSupportClass;
import static soot_analysis.Utils.print;

public class ForwardCallgraph {
	
	SootMethod startMethod;
	SootContext SC;
	int maxNNodes = 100000;

	public ForwardCallgraph(SootContext SC, SootMethod startMethod){
		this.SC = SC;
		this.startMethod = startMethod;
	}	

	public Tree<CallgraphState> run(){
		return run(maxNNodes);
	}
	
	public Tree<CallgraphState> run(int nnodes){
		Tree<CallgraphState> tree = new Tree<CallgraphState>();
		Node<CallgraphState> headNode = new Node<CallgraphState>(0);
		headNode.value = new CallgraphState(startMethod);
		tree.addHead(headNode);
		
        LinkedList<Node<CallgraphState>> queue = new LinkedList<Node<CallgraphState>>();
        queue.add(headNode);
        while (queue.size() > 0 && tree.nodeMap.size() <= nnodes){
            Node<CallgraphState> cn = queue.poll();
                     
            Collection<Tuple<Unit, SootMethod>> calledMethodsWithUnit = new LinkedList<>();
            Tuple<String, String> cm = getCallbackMethod(cn.value.method);
            if(cm!=null){
            	if(cm.y.contains("postDelayed")){
            		calledMethodsWithUnit = handlePostDelayed(cn.value.method, cn.value.unit, cn.parent.value.method);
            	}else{
            		//...
            	}
            }else{
            	calledMethodsWithUnit= SC.getCalleesWithUnit(cn.value.method);
            }

            for(Tuple<Unit, SootMethod> u_mm : calledMethodsWithUnit){
            	SootClass targetClass = u_mm.y.getDeclaringClass();
				Node<CallgraphState> nn = tree.addChild(cn, new CallgraphState(u_mm.y, u_mm.x));
				if(nn!=null && (  (getCallbackMethod(u_mm.y)!=null || (targetClass.isApplicationClass() && !isSupportClass(targetClass)))
				|| targetClass.getName().equals("android.hardware.fingerprint.FingerprintManager"))){
					queue.add(nn);
				}
            }
        }
        return tree;
	}
	
	private Collection<Tuple<Unit, SootMethod>> handlePostDelayed(SootMethod container, Unit uu, SootMethod callerMethod) {
		SootClass runnableClass = null;
		
		String reg = String.valueOf(SC.getInvokeExpr(uu).getArg(0));
		Slicer ss = new Slicer(SC, uu, reg, callerMethod);
		ss.skipNews = false;
		Tree<SlicerState> stree = ss.run(20);
		String type = SC.sliceToType(stree);
		if(type != null){
			runnableClass = SC.cm.get(type);
		}
		
		Collection<Tuple<Unit, SootMethod>> res = new LinkedList<>();
		for(SootMethod mm : SC.getRunnableRunMethods(runnableClass)){
			res.add(new Tuple<>(uu, mm));
		}
		
		return res; 
	}

	private Tuple<String, String> getCallbackMethod(SootMethod mm) {
		List<Tuple<String, String>> setters = new LinkedList<Tuple<String, String>>();
		setters.add(new Tuple<String, String>("android.os.Handler", "boolean postDelayed"));
		setters.add(new Tuple<String, String>("android.view.View", "boolean postDelayed"));

		SootClass sc = mm.getDeclaringClass();
		String mname = mm.getSubSignature();
		do{
			String cname = sc.getName();
			for(Tuple<String, String> ss : setters){
				if(ss.x.equals(cname) && mname.startsWith(ss.y)){
					return ss;
				}
			}
			try{
				sc = sc.getSuperclass();
			}catch(RuntimeException e){
				break;
			}
		}while(sc!=null && !sc.getName().equals("java.lang.Object"));
		
		return null;
	}

}


