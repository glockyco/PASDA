package differencing;

import differencing.domain.*;

public class HasUifVisitor extends ModelVisitor {
    private boolean hasUif = false;

    public static boolean hasUif(Model model) {
        if (model == null) {
            return false;
        }

        HasUifVisitor visitor = new HasUifVisitor();
        model.accept(visitor);
        return visitor.hasUif();
    }

    public boolean hasUif() {
        return this.hasUif;
    }

    @Override
    public void preVisit(SymbolicIntegerFunction function) {
        this.hasUif = true;
    }

    @Override
    public void preVisit(SymbolicRealFunction function) {
        this.hasUif = true;
    }

    @Override
    public void preVisit(SymbolicStringFunction function) {
        this.hasUif = true;
    }
}
