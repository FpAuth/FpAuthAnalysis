package soot_analysis;

import cg.Common;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.*;

import java.util.*;

import static soot_analysis.Utils.isSupportClass;
import static soot_analysis.Utils.print;


public class SootContext {
    public Scene scene;
    public Hierarchy ch;
    public HashMap<String, SootClass> cm = new HashMap<String, SootClass>();
    public HashMap<SootMethod, HashSet<CodeLocation>> callers_cache = new HashMap<SootMethod, HashSet<CodeLocation>>();
    public HashMap<SootMethod, HashMap<String, Unit>> def_cache = new HashMap<SootMethod, HashMap<String, Unit>>();
    public HashMap<SootMethod, HashMap<String, Collection<Unit>>> use_cache = new HashMap<SootMethod, HashMap<String, Collection<Unit>>>();
    public HashMap<SootField, HashSet<Tuple<Unit, SootMethod>>> field_cache = new HashMap<>();

    private Collection<SootMethod> runnableRunMethods;

    public SootContext(Scene s) {
        this.scene = s;
        ch = new Hierarchy();
        for (SootClass sc : this.scene.getClasses()) {
            cm.put(sc.getName(), sc);
            if (sc.resolvingLevel() == SootClass.HIERARCHY) {
                SootResolver.v().reResolve(sc, SootClass.SIGNATURES);
            }
        }

        long old = System.currentTimeMillis();
        for (SootClass sc : this.scene.getClasses()) {
            if (sc.resolvingLevel() == SootClass.BODIES) {
                for (SootMethod sm : sc.getMethods()) {
                    if (!sm.hasActiveBody()) {
                        continue;
                    }
                    for (Unit uu : sm.getActiveBody().getUnits()) {
                        for (ValueBox db : uu.getDefBoxes()) {
                            Value vv = db.getValue();
                            try {
                                FieldRef iff = (FieldRef) vv;
                                SootField ff = iff.getField();
                                HashSet<Tuple<Unit, SootMethod>> current_set = field_cache.get(ff);
                                if (current_set == null) {
                                    current_set = new HashSet<Tuple<Unit, SootMethod>>();
                                    field_cache.put(ff, current_set);
                                }
                                current_set.add(new Tuple(uu, sm));
                            } catch (ClassCastException e) {
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    public List<Tuple<Unit, InvokeExpr>> getInvokesWithUnit(SootMethod m) {
        List<Tuple<Unit, InvokeExpr>> res = new ArrayList<Tuple<Unit, InvokeExpr>>();
        if (m.hasActiveBody()) {
            Body bb = m.getActiveBody();
            for (Unit uu : bb.getUnits()) {
                InvokeExpr ie = getInvokeExpr(uu);
                if (ie != null) {
                    res.add(new Tuple<Unit, InvokeExpr>(uu, ie));
                }
            }
        }
        return res;
    }

    public InvokeExpr getInvokeExpr(Unit uu) {
        Stmt ss = null;
        InvokeExpr res = null;
        try {
            ss = (Stmt) uu;
        } catch (ClassCastException e) {
            return null;
        }
        try {
            res = ss.getInvokeExpr();
        } catch (RuntimeException e) {
            return null;
        }
        return res;
    }

    public List<SootMethod> getCallees(InvokeExpr ie, SootMethod container) {
        SootMethod called = (SootMethod) ie.getMethodRef().resolve();

        if ((ie instanceof JVirtualInvokeExpr) || (ie instanceof JInterfaceInvokeExpr)) {
            SootClass target = ie.getMethodRef().getDeclaringClass();
            List<SootMethod> tt;
            try {
                tt = ch.resolveAbstractDispatch(target, called);
            } catch (RuntimeException e) {
                tt = new LinkedList<SootMethod>();
            }

            if (tt.size() == 0 && !target.isInterface()) {
                tt = new LinkedList<SootMethod>();
                try {
                    SootMethod resm = ch.resolveConcreteDispatch(target, called);
                    tt.add(resm);
                } catch (RuntimeException e) {
                    ;
                }
            }

            return tt;
        } else if (ie instanceof JStaticInvokeExpr) {
            SootClass target = ie.getMethodRef().getDeclaringClass();
            SootMethod resm = ch.resolveConcreteDispatch(target, called);
            List<SootMethod> res = new LinkedList<SootMethod>();
            res.add(resm);
            return res;
        } else if (ie instanceof JSpecialInvokeExpr) {
            SootMethod resm = ch.resolveSpecialDispatch((JSpecialInvokeExpr) ie, container);
            List<SootMethod> res = new ArrayList<SootMethod>();
            res.add(resm);
            return res;
        }
        return null;
    }

    public Collection<Tuple<Unit, SootMethod>> getCalleesWithUnit(SootMethod m) {
        HashSet<Tuple<Unit, SootMethod>> res = new LinkedHashSet<>();
        List<Tuple<Unit, InvokeExpr>> u_ieList = getInvokesWithUnit(m);
        for (Tuple<Unit, InvokeExpr> u_ie : u_ieList) {
            for (SootMethod calledMethod : getCallees(u_ie.y, m)) {
                res.add(new Tuple<Unit, SootMethod>(u_ie.x, calledMethod));
            }
        }
        return res;
    }

    public Collection<CodeLocation> getCallers(SootMethod method) {
        Collection<CodeLocation> res = new LinkedHashSet<CodeLocation>();
        if (!Common.CalleeToCallerMap.containsKey(method) || Common.CalleeToCallerMap.get(method).isEmpty())
//			return null;
            return res;
        Collection<SootMethod> callers_m = Common.CalleeToCallerMap.get(method);
        for (SootMethod tm : callers_m) {
            if (tm.hasActiveBody()) {
                Body bb = tm.getActiveBody();
                for (Unit uu : bb.getUnits()) {
                    InvokeExpr ie = getInvokeExpr(uu);
                    if (ie != null) {
                        if (ie.getMethod().toString().equals(method.toString())) {
                            List<SootMethod> targets = getCallees(ie, tm);
                            if (targets.contains(method)) {
                                res.add(new CodeLocation(tm.getDeclaringClass(), tm, uu));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public Collection<SootMethod> getOverrides(SootMethod mm) {
        HashSet<SootMethod> res = new LinkedHashSet<SootMethod>();
        SootClass sclass = cm.get(mm.getDeclaringClass().getName());

        List<SootClass> sclist;
        if (sclass.isInterface()) {
            sclist = ch.getImplementersOf(sclass);
        } else {
            sclist = ch.getSubclassesOf(sclass);
        }

        for (SootClass sc : sclist) {
            for (SootMethod sm : sc.getMethods()) {
                if (sm.getSubSignature().equals(mm.getSubSignature())) {
                    res.add(sm);
                }
            }
        }
        return res;
    }

    public Unit getDefUnit(String reg, SootMethod containerMethod, boolean skipNews) {
        HashMap<String, Unit> defMap = def_cache.get(containerMethod);
        if (defMap == null) {
            defMap = new HashMap<String, Unit>();
            Body bb = containerMethod.getActiveBody();
            for (Unit uu : bb.getUnits()) {
                for (ValueBox df : uu.getDefBoxes()) {
                    String cname = df.getClass().getSimpleName();
                    if (cname.equals("LinkedVariableBox") || cname.equals("JimpleLocalBox")) {

                        boolean isNewAssignment = isNewAssignment(uu);
                        if (isNewAssignment && skipNews) {
                            Unit uuReal = null;
                            for (Unit uu2 : bb.getUnits()) {
                                InvokeExpr ie = getInvokeExpr(uu2);
                                if (ie != null) {
                                    if (ie instanceof InstanceInvokeExpr) {
                                        String nreg = ((InstanceInvokeExpr) ie).getBase().toString();
                                        if (nreg.equals(df.getValue().toString()) && ie.getMethod().getSubSignature().startsWith("void <init>")) {
                                            uuReal = uu2;
                                        }
                                    }
                                }
                            }
                            if (uuReal != null) {
                                defMap.put(df.getValue().toString(), uuReal);
                                break;
                            }
                        }
                        defMap.put(df.getValue().toString(), uu);
                        break;
                    }
                }
            }
            def_cache.put(containerMethod, defMap);
        }
        Unit res = defMap.get(reg);
        return res;
    }

    public boolean isNewAssignment(Unit uu) {
        String newType = null;
        try {
            newType = (((JNewExpr) ((JAssignStmt) uu).getRightOp()).getBaseType().toString());
        } catch (ClassCastException | NullPointerException e) {
            ;
        }
        boolean res = newType != null;
        return res;
    }

    public Collection<Unit> getUseUnits(String reg, SootMethod containerMethod) {
        HashMap<String, Collection<Unit>> useMap = use_cache.get(containerMethod);
        if (useMap == null) {
            useMap = new HashMap<String, Collection<Unit>>();
            Body bb = containerMethod.getActiveBody();
            for (Unit uu : bb.getUnits()) {
                for (ValueBox df : uu.getUseBoxes()) {
                    String reg2 = df.getValue().toString();
                    if (!reg2.startsWith("$")) {
                        continue;
                    }
                    Collection<Unit> useList = useMap.get(reg2);
                    if (useList == null) {
                        useList = new LinkedList<Unit>();
                        useList.add(uu);
                        useMap.put(reg2, useList);
                    } else {
                        useList.add(uu);
                    }
                }
            }
            use_cache.put(containerMethod, useMap);
        }
        Collection<Unit> res = useMap.get(reg);
        if (res == null) {
            res = new LinkedList<Unit>();
        }
        return res;
    }

    public SootMethod resolveMethod(String className, String methodName) {
        return resolveMethod(className, methodName, false);
    }

    public SootMethod resolveMethod(String className, String methodName, boolean fuzzy) {
        SootClass sclass = null;
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (sootClass.getName().equals(className)) {
                sclass = sootClass;
                break;
            }
        }
        if (sclass == null) {
            return null;
        }
        if (sclass.resolvingLevel() != SootClass.BODIES) {
            SootResolver.v().reResolve(sclass, SootClass.BODIES);
        }
        SootMethod res = null;
        for (SootMethod m : sclass.getMethods()) {
            boolean match;
            if (fuzzy) {
                match = m.getSubSignature().startsWith(methodName);
            } else {
                match = m.getSubSignature().equals(methodName);
            }
            if (match) {
                return m;
            }
        }
        return res;
    }

    public Collection<SootMethod> resolveMethods(String className, String methodName, boolean fuzzy) {
        SootClass sclass = null;
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (sootClass.getName().equals(className)) {
                sclass = sootClass;
                break;
            }
        }
        if (sclass == null) {
            return null;
        }
        if (sclass.resolvingLevel() != SootClass.BODIES) {
            SootResolver.v().reResolve(sclass, SootClass.BODIES);
        }
        List<SootMethod> res = new LinkedList<SootMethod>();
        for (SootMethod m : sclass.getMethods()) {
            boolean match;
            if (fuzzy) {
                match = m.getSubSignature().startsWith(methodName);
            } else {
                match = m.getSubSignature().equals(methodName);
            }
            if (match) {
                res.add(m);
            }
        }
        return res;
    }

    public Collection<CodeLocation> getAPIUsage(String className, String methodName, boolean fuzzy, boolean removeSupport) {
        List<String> classNames = new LinkedList<String>();
        classNames.add(className);
        return getAPIUsage(classNames, methodName, fuzzy, removeSupport);
    }

    public Collection<CodeLocation> getAPIUsage(Collection<String> classNames, String methodName, boolean fuzzy, boolean removeSupport) {
        Collection<CodeLocation> usages = new LinkedList<CodeLocation>();
        for (String currentClassName : classNames) {
            Collection<SootMethod> ms = resolveMethods(currentClassName, methodName, fuzzy);
            if (ms == null || ms.isEmpty()) {
                continue;
            }
            for (SootMethod mm : ms) {
                Collection<CodeLocation> callers = getCallers(mm);
                usages.addAll(callers);
            }
        }
        Collection<CodeLocation> use_filtered = new LinkedList<CodeLocation>();
        if (removeSupport) {
            for (CodeLocation use : usages) {
                if (Utils.isSupportClass(use.sclass)) {
                    continue;
                }
                use_filtered.add(use);
            }
        } else {
            use_filtered = usages;
        }
        return use_filtered;
    }

    public Collection<SootMethod> getRunnableRunMethods(SootClass upperClass) {
        Collection<SootMethod> filteredRunnableRunMethods;
        if (runnableRunMethods == null) {
            Collection<SootMethod> tmp = getOverrides(resolveMethod("java.lang.Runnable", "void run()"));
            runnableRunMethods = new LinkedList<SootMethod>();
            for (SootMethod m : tmp) {
                SootClass d = m.getDeclaringClass();
                if (d.isApplicationClass() && !isSupportClass(d)) {
                    runnableRunMethods.add(m);
                }
            }
        }

        if (upperClass == null) {
            filteredRunnableRunMethods = runnableRunMethods;
        } else {
            filteredRunnableRunMethods = new LinkedList<SootMethod>();

            HashSet<SootClass> cset;
            if (!upperClass.isInterface()) {
                cset = new HashSet<>(ch.getSubclassesOfIncluding(upperClass));
            } else {
                cset = new HashSet<>(ch.getImplementersOf(upperClass));
            }

            for (SootMethod m : runnableRunMethods) {
                if (cset.contains(m.getDeclaringClass())) {
                    filteredRunnableRunMethods.add(m);
                }
            }
        }
        return filteredRunnableRunMethods;
    }

    public String sliceToType(Tree<SlicerState> stree) {
        Node<SlicerState> cnode = stree.head;
        SlicerState res = null;
        while (true) {
            List<Node<SlicerState>> nonNullChildren = new LinkedList<Node<SlicerState>>();
            for (Node<SlicerState> c : cnode.children) {
                if (c.value.reg != null || isNewAssignment(c.value.unit)) { //new is a null node, but we still want it
                    nonNullChildren.add(c);
                }
            }

            if (cnode.value.unit.getClass().getSimpleName().equals("JAssignStmt")) {
                JAssignStmt as = (JAssignStmt) cnode.value.unit;
                return String.valueOf(as.getRightOp().getType());
            }

            if (nonNullChildren.size() == 1) {
                cnode = nonNullChildren.get(0);
            } else {
                return null;
            }
        }
    }

    public Tuple<Unit, InvokeExpr> recoverEdge(SootMethod m, SootMethod parentMethod) {
        List<Tuple<Unit, InvokeExpr>> iel = getInvokesWithUnit(parentMethod);
        for (Tuple<Unit, InvokeExpr> ie : iel) {
            if (ie.y.getMethod() == m) {
                return ie;
            }
        }
        return null;
    }

    public SootField getFieldAccess(Unit uu) {
        for (ValueBox vb : uu.getUseBoxes()) {
            try {
                Value vv = vb.getValue();
                FieldRef iff = (FieldRef) vv;
                SootField ff = iff.getField();
                return ff;
            } catch (ClassCastException e) {
                continue;
            }
        }

        return null;
    }

    public SootField getFieldAccess(ValueBox vb) {
        try {
            Value vv = vb.getValue();
            FieldRef iff = (FieldRef) vv;
            SootField ff = iff.getField();
            return ff;
        } catch (ClassCastException e) {
            return null;
        }
    }
}
