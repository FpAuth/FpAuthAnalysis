package soot_analysis;

import cg.Common;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

import static comm.Common.analyzeDeleteKeywords;
import static soot_analysis.Utils.print;

public class ForwardSlicer {

    Unit startUnit;
    String startReg;
    SootMethod containerMethod;
    SootContext SC;

    public ForwardSlicer(SootContext SC, Unit startUnit, String startReg, SootMethod containerMethod) {
        this.SC = SC;
        this.startUnit = startUnit;
        this.startReg = startReg;
        this.containerMethod = containerMethod;
    }


    public Tree<SlicerState> run_track(int nnodes) {
        Tree<SlicerState> tree = new Tree<SlicerState>();
        Node<SlicerState> headNode = new Node<SlicerState>(0);
        headNode.value = new SlicerState(startReg, startUnit, containerMethod);
        tree.addHead(headNode);

        LinkedList<Node<SlicerState>> queue = new LinkedList<Node<SlicerState>>();
        queue.add(headNode);

        while (queue.size() > 0 && tree.nodeMap.size() <= nnodes) {
            Node<SlicerState> cn = queue.poll();
            SlicerState sstate_pre = cn.value;
            Collection<Tuple<Unit, SootMethod>> toExploreUnits = new LinkedList<>();
            boolean last_return = false;

            if (sstate_pre.reg.startsWith("return")) {
                last_return = true;
                Collection<CodeLocation> callers = newGetCallers(SC, sstate_pre.containerMethod);
                if (callers != null) {
                    for (CodeLocation cl : callers) {
                        if (cl != null && cl.sunit != null && cl.smethod != null) {
                            toExploreUnits.add(new Tuple(cl.sunit, cl.smethod));
                        }
                    }
                }
            } else {
                Collection<Unit> useUnits = SC.getUseUnits(sstate_pre.reg, sstate_pre.containerMethod);
                if (useUnits != null) {
                    for (Unit newUnit : useUnits) {
                        toExploreUnits.add(new Tuple(newUnit, sstate_pre.containerMethod));
                    }
                }
            }

            for (Tuple<Unit, SootMethod> tstate : toExploreUnits) {
                Unit newUnit = tstate.x;
                Stmt smt = (Stmt) newUnit;

                if (smt instanceof IfStmt) {
                    Stmt target = ((IfStmt) smt).getTarget();
                    Node<SlicerState> nn;
                    Node<SlicerState> nnn = null;
                    Node<SlicerState> suc_nnn = null;
                    if (target instanceof ReturnStmt) {
                        nn = tree.addChild(cn, new SlicerState("return", newUnit, tstate.y));
                        if (nn != null) {
                            queue.add(nn);
                        }
                    } else if (target instanceof RetStmt) {
                        nn = tree.addChild(cn, new SlicerState("WEAK", newUnit, tstate.y));
                    } else {
                        nn = tree.addChild(cn, new SlicerState("if", newUnit, tstate.y));
                        if (String.valueOf(target).contains("Phi(")) {
                            String intent_res = get_intent_classPara(tstate.y, target);
                            if (intent_res != null) {
                                if (intent_res.equals("return")) {
                                    nnn = tree.addChild(nn, new SlicerState("return", newUnit, tstate.y));
                                } else {
                                    nnn = tree.addChild(nn, new SlicerState(intent_res, target, tstate.y));
                                }
                            }
                        }
                    }

                    String suc_intent_res = get_intent_classPara(tstate.y, smt);
                    if (suc_intent_res != null && nn != null) {
                        if (suc_intent_res.equals("return")) {
                            suc_nnn = tree.addChild(nn, new SlicerState("return_suc", smt, tstate.y));
                        } else {
                            nnn = tree.addChild(nn, new SlicerState(suc_intent_res, smt, tstate.y));
                        }
                    }

                    if (nnn != null && nnn.value.reg.equals("return")) {
                        queue.add(nnn);
                    } else if (suc_nnn != null && suc_nnn.value.reg.equals("return_suc")) {
                        queue.add(suc_nnn);
                    }

                }
                if (smt instanceof RetStmt) {
                    Node<SlicerState> nn = tree.addChild(cn, new SlicerState("WEAK", newUnit, tstate.y));
                }
                if (smt instanceof ReturnStmt) {
                    Node<SlicerState> nn = tree.addChild(cn, new SlicerState("return", newUnit, tstate.y));
                    if (nn != null) {
                        queue.add(nn);
                    }
                }
                InvokeExpr inv = SC.getInvokeExpr(newUnit);
                if (inv != null) {
                    if (newUnit.getDefBoxes().size() > 0) {
                        Value reg = newUnit.getDefBoxes().get(0).getValue();
                        Node<SlicerState> nn = tree.addChild(cn, new SlicerState(String.valueOf(reg), newUnit, tstate.y));
                        if (nn != null) {
                            queue.add(nn);
                        }
                    } else if (last_return) {
                        Node<SlicerState> nn = tree.addChild(cn, new SlicerState("WEAK", newUnit, tstate.y));
                    }
                }


            }
        }
        return tree;

    }

    public Collection<CodeLocation> newGetCallers(SootContext SC, SootMethod method) {
        Collection<CodeLocation> res = new LinkedList<>();
        if (!Common.CalleeToCallerMap.containsKey(method) || Common.CalleeToCallerMap.get(method).isEmpty())
            return res;
        Collection<SootMethod> callers_m = Common.CalleeToCallerMap.get(method);
        for (SootMethod tm : callers_m) {
            if (tm.hasActiveBody()) {
                Body bb = tm.getActiveBody();
                for (Unit uu : bb.getUnits()) {
                    InvokeExpr ie = SC.getInvokeExpr(uu);
                    if (ie != null) {
                        if (ie.getMethod().toString().equals(method.toString())) {
                            List<SootMethod> targets = SC.getCallees(ie, tm);
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

    public String get_intent_classPara(SootMethod smethod, Stmt target) {
        Body body = smethod.retrieveActiveBody();
        UnitGraph g = new BriefUnitGraph(body);

        Stmt currentStmt = target;
        Set<Stmt> visitedStmts = new HashSet<>();
        while (currentStmt != null) {
            if (visitedStmts.contains(currentStmt)) {
                return "";
            }
            visitedStmts.add(currentStmt);
            List<Unit> succs = g.getSuccsOf(currentStmt);
            if (succs.isEmpty()) {
                break;
            }

            Unit succ = succs.get(0);
            print(String.valueOf(succ.getClass()));
            Stmt stmtsuc = (Stmt) succ;

            for (String analyzeDeleteKeyword : analyzeDeleteKeywords) {
                if (succ.toString().contains(analyzeDeleteKeyword)) {
                    InvokeExpr invokeExpr = stmtsuc.getInvokeExpr();
                    Value para = null;
                    if (invokeExpr.getArgs().size() == 1) {
                        para = invokeExpr.getArgs().get(0);
                    } else if (invokeExpr.getArgs().size() > 1) {
                        para = invokeExpr.getArgs().get(1);
                    }
                    return "keyword:" + para;
                }
            }

            if (stmtsuc instanceof InvokeStmt) {
                InvokeExpr invokeExpr = ((InvokeStmt) stmtsuc).getInvokeExpr();

                String signature = invokeExpr.getMethod().getSignature();
                print(signature);
                if (signature.contains("android.content.Intent: void <init>")) {
                    if (invokeExpr.getArgs().size() < 1) {
                        return "intent:";
                    } else if (invokeExpr.getArgs().size() < 2) {
                        return "intent:" + invokeExpr.getArgs().get(0);
                    } else {
                        Value para = invokeExpr.getArgs().get(1);
                        return "intent:" + para;
                    }
                }

                String subSignature = invokeExpr.getMethod().getSubSignature();
                print(subSignature);
                if (subSignature.contains("authenticate")) {
                    return "authenticate:" + String.valueOf(succ);
                }
            } else if (stmtsuc instanceof InvokeExpr) {
                print("succ instanceof InvokeExpr");
            }
            else if (stmtsuc instanceof ReturnStmt) {
                return "return";
            }
            currentStmt = (Stmt) succ;
        }
        return "";
    }
}


