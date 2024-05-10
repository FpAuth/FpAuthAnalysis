package comm;

import soot.SootMethod;
import soot.Unit;

import java.util.Objects;

public class UniqueUnit {
    Unit unit;
    SootMethod sootMethod;

    public UniqueUnit(Unit unit, SootMethod sootMethod) {
        this.unit = unit;
        this.sootMethod = sootMethod;
    }

    public UniqueUnit(SootMethod sootMethod, Unit unit) {
        this.unit = unit;
        this.sootMethod = sootMethod;
    }

    public Unit getUnit() {
        return unit;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s", unit, sootMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootMethod, unit);
    }
}
