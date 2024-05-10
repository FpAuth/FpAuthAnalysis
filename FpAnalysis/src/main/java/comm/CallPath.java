package comm;

import soot.SootMethod;

import java.util.ArrayList;

public  class CallPath {
    public SootMethod lastMethod;
    public ArrayList<SootMethod> path;

    public CallPath(SootMethod srcMethod) {
        lastMethod = srcMethod;
        path = new ArrayList<>();
        path.add(srcMethod);
    }

    public CallPath(CallPath rhs) {
        this.lastMethod = rhs.lastMethod;
        this.path = new ArrayList<>();
        this.path.addAll(rhs.path);
    }

    public void addCall(SootMethod sootMethod) {
        lastMethod = sootMethod;
        this.path.add(sootMethod);
    }

    public SootMethod getLast() {
        return lastMethod;
    }

    public boolean hasMethod(SootMethod sootMethod) {
        return path.contains(sootMethod);
    }

    public int size() {
        return path.size();
    }


    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("Call Path:\n");
        if (path.size() > 0) {
            for (int i = 0; i < path.size() - 1; i++) {
                SootMethod method = path.get(i);
                res.append("(Class=" + method.getDeclaringClass().getName() + "---Method=" + method.getName() + ")--->\n");
            }
            res.append("(Class=" + lastMethod.getDeclaringClass().getName() + "---Method=" + lastMethod.getName() + ")");
            res.append("\n");
        }
        return res.toString();
    }
}
