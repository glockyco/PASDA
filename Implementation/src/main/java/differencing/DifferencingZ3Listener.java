package differencing;

import com.microsoft.z3.*;
import differencing.domain.Model;
import differencing.models.Iteration;
import differencing.transformer.ModelToZ3Transformer;
import differencing.transformer.SpfToModelTransformer;
import differencing.transformer.ValueToModelTransformer;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.vm.*;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Objects;

public class DifferencingZ3Listener extends PropertyListenerAdapter implements AutoCloseable {

    private final MethodSpec areErrorsEquivalentSpec;
    private final MethodSpec areResultsEquivalentSpec;
    private final MethodSpec runSpec;
    private final DifferencingParameters parameters;

    private int partitionId = 1;

    private final ValueToModelTransformer valToModel = new ValueToModelTransformer();
    private final SpfToModelTransformer spfToModel = new SpfToModelTransformer();

    private final Context context = new Context();
    private final ModelToZ3Transformer modelToZ3 = new ModelToZ3Transformer(this.context);


    public DifferencingZ3Listener(Iteration iteration, DifferencingParameters parameters) {
        this.parameters = parameters;
        this.areErrorsEquivalentSpec = MethodSpec.createMethodSpec("*.IDiff" + parameters.getToolName() + iteration.iteration + ".areErrorsEquivalent");
        this.areResultsEquivalentSpec = MethodSpec.createMethodSpec("*.IDiff" + parameters.getToolName() + iteration.iteration + ".areResultsEquivalent");
        this.runSpec = MethodSpec.createMethodSpec("*.IDiff" + parameters.getToolName() + iteration.iteration + ".run");
    }

    @Override
    public void close() throws Exception {
        this.context.close();

    }

    public Context getContext() {
        return this.context;
    }


    @Override
    public void searchConstraintHit(Search search) {
        if (search.getVM().getCurrentThread().isFirstStepInsn()) {
            return;
        }
        this.startNextPartition();
    }

    @Override
    public void propertyViolated(Search search) {
        if (search.getVM().getCurrentThread().isFirstStepInsn()) {
            return;
        }
        this.startNextPartition();
    }

    @Override
    public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
        if (!(instructionToExecute instanceof JVMReturnInstruction)) {
            return;
        }

        MethodInfo mi = instructionToExecute.getMethodInfo();
        if (this.runSpec.matches(mi)) {
            this.startNextPartition();
        } else if (this.areErrorsEquivalentSpec.matches(mi)) {
            ThreadInfo threadInfo = vm.getCurrentThread();
            StackFrame stackFrame = threadInfo.getModifiableTopFrame();
            LocalVarInfo[] localVars = stackFrame.getLocalVars();

            // Our areErrorsEquivalent methods all have two method parameters
            // (a and b) and no other local variables, so the total
            // number of local variables should always be two.
            assert localVars.length == 2;

            // Get the current path condition:
            PathCondition pathCondition = PathCondition.getPC(vm);
            Constraint pcConstraint = pathCondition.header;

            // Get the concrete values of the two parameters:
            Object[] argumentValues = stackFrame.getArgumentValues(threadInfo);
            Object v1Value = argumentValues[0];
            Object v2Value = argumentValues[1];

            String v1Error = v1Value == null ? null : ((DynamicElementInfo) v1Value).getClassInfo().getName();
            String v2Error = v2Value == null ? null : ((DynamicElementInfo) v2Value).getClassInfo().getName();

            boolean areEquivalent = Objects.equals(v1Error, v2Error);

            Model pcModel = this.spfToModel.transform(pcConstraint);

            Expr<BoolSort> pcExpr = (Expr<BoolSort>) modelToZ3.transform(pcModel);

            Solver solverEq = this.context.mkSolver();
            solverEq.add(pcExpr);

            Solver solverNEq = this.context.mkSolver();
            solverNEq.add(pcExpr);

            if (areEquivalent) {
                solverEq.add(this.context.mkBool(true));
                solverNEq.add(this.context.mkBool(false));
            } else {
                solverEq.add(this.context.mkBool(false));
                solverNEq.add(this.context.mkBool(true));
            }

            writePathCondition(this.partitionId, solverEq.toString(), true);
            writePathCondition(this.partitionId, solverNEq.toString(), false);
        } else if (this.areResultsEquivalentSpec.matches(mi)) {
            ThreadInfo threadInfo = vm.getCurrentThread();
            StackFrame stackFrame = threadInfo.getModifiableTopFrame();
            LocalVarInfo[] localVars = stackFrame.getLocalVars();

            // Our areResultsEquivalent methods all have two method parameters
            // (a and b) and no other local variables, so the total
            // number of local variables should always be two.
            assert localVars.length == 2;

            // -------------------------------------------------------
            // Get the current symbolic state of the program.

            // Get the current path condition:
            PathCondition pathCondition = PathCondition.getPC(vm);
            Constraint pcConstraint = pathCondition.header;

            Object[] argumentValues = stackFrame.getArgumentValues(threadInfo);

            // Get the symbolic value of the first parameter:
            int v1SlotIndex = localVars[0].getSlotIndex();
            Expression v1Expression = (Expression) stackFrame.getSlotAttr(v1SlotIndex);
            // Get the concrete value of the first parameter:
            Object v1Value = argumentValues[0];

            // Get the symbolic value of the second parameter:
            int v2SlotIndex = localVars[1].getSlotIndex();
            Expression v2Expression = (Expression) stackFrame.getSlotAttr(v2SlotIndex);
            // Get the concrete value of the second parameter:
            Object v2Value = argumentValues[1];

            // -------------------------------------------------------
            // Check equivalence of the two parameters using an SMT solver.

            boolean v1IsConcrete = v1Expression == null;
            boolean v2IsConcrete = v2Expression == null;

            Model pcModel = this.spfToModel.transform(pcConstraint);
            Model v1Model = v1IsConcrete ? this.valToModel.transform(v1Value) : this.spfToModel.transform(v1Expression);
            Model v2Model = v2IsConcrete ? this.valToModel.transform(v2Value) : this.spfToModel.transform(v2Expression);

            Expr<BoolSort> pcExpr = (Expr<BoolSort>) modelToZ3.transform(pcModel);
            Expr<?> v1Expr = modelToZ3.transform(v1Model);
            Expr<?> v2Expr = modelToZ3.transform(v2Model);

            Solver solverEq = this.context.mkSolver();

            Solver solverNEq = this.context.mkSolver();

            solverEq.add(pcExpr);
            solverEq.add(this.context.mkEq(v1Expr, v2Expr)); // This part is for the Z3 additional notaiton

            solverNEq.add(pcExpr);
            solverNEq.add(this.context.mkNot(this.context.mkEq(v1Expr, v2Expr))); // This part is for the Z3 additional notaiton
            //solver = this.removeFuncDeclsForBuiltIns(solver);
            writePathCondition(this.partitionId, solverEq.toString(), true);
            writePathCondition(this.partitionId, solverNEq.toString(), false);
            //String z3Representation = solver.toString();
            //System.out.println("++++++++++ "  + z3Representation);


        }
    }

    private void writePathCondition(int partition, String z3rap, boolean isEqualitySolver) {
        String filename;
        if (isEqualitySolver) {
            filename = this.parameters.getTargetClassName() + "-P" + partition + "-Z3Eq-PC.z3";
        } else {
            filename = this.parameters.getTargetClassName() + "-P" + partition + "-Z3NEq-PC.z3";
        }

        Path path = Paths.get(this.parameters.getTargetDirectory(), filename).toAbsolutePath();

        try {
            Files.write(path, z3rap.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startNextPartition() {
        this.partitionId++;
    }
}
