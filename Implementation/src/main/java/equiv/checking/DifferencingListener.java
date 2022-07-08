package equiv.checking;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.vm.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DifferencingListener extends PropertyListenerAdapter {
    protected final DifferencingParameters parameters;
    protected final List<MethodSpec> areEquivalentMethods = new ArrayList<>();

    protected int count =  0;

    public DifferencingListener(DifferencingParameters parameters) {
        this.parameters = parameters;

        // @TODO: Check if we can make do with fewer method specs.
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(int,int)"));
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(long,long)"));
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(short,short)"));
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(byte,byte)"));
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(float,float)"));
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(double,double)"));
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(boolean,boolean)"));
        // @TODO: Check if method spec for objects works.
        this.areEquivalentMethods.add(MethodSpec.createMethodSpec("*.areEquivalent(java.lang.Object,java.lang.Object)"));
    }

    @Override
    public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
        if (!(instructionToExecute instanceof JVMReturnInstruction)) {
            return;
        }

        MethodInfo mi = instructionToExecute.getMethodInfo();

        // Intercept execution when returning from one of the "areEquivalentMethods".
        if (this.areEquivalentMethods.stream().noneMatch(m -> m.matches(mi))) {
            return;
        }

        ThreadInfo threadInfo = vm.getCurrentThread();
        StackFrame stackFrame = threadInfo.getModifiableTopFrame();
        LocalVarInfo[] localVars = stackFrame.getLocalVars();

        // Our areEquivalent methods all have two method parameters
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
        int slotIndexA = localVars[0].getSlotIndex();
        Expression expressionA = (Expression) stackFrame.getSlotAttr(slotIndexA);
        // Get the concrete value of the first parameter:
        Object valueA = argumentValues[0];

        // Get the symbolic value of the second parameter:
        int slotIndexB = localVars[1].getSlotIndex();
        Expression expressionB = (Expression) stackFrame.getSlotAttr(slotIndexB);
        // Get the concrete value of the second parameter:
        Object valueB = argumentValues[1];

        // -------------------------------------------------------
        // Check equivalence of the two parameters using an SMT solver.

        this.count++;

        boolean areEquivalent;

        boolean aIsConcrete = expressionA == null;
        boolean bIsConcrete = expressionB == null;

        String pcString = pcConstraint != null ? pcConstraint.prefix_notation() : "true";
        String aString = aIsConcrete ? valueA.toString() : expressionA.prefix_notation();
        String bString = bIsConcrete ? valueB.toString() : expressionB.prefix_notation();

        boolean pcHasUninterpretedFunctions = pcString.contains("UF_");
        boolean aHasUninterpretedFunctions = aString.contains("UF_");
        boolean bHasUninterpretedFunctions = bString.contains("UF_");

        boolean hasUninterpretedFunctions = pcHasUninterpretedFunctions || aHasUninterpretedFunctions || bHasUninterpretedFunctions;

        // @TODO: Check if the path condition is satisfiable.

        String z3Query = "";
        z3Query += this.getDeclarations(pcString) + "\n\n";
        z3Query += "; Path Condition:\n";
        z3Query += "(assert " + pcString + ")\n\n";
        z3Query += "; Equivalence Check:\n";
        z3Query += "(assert (not (= " + aString + " " + bString + ")))\n\n";
        z3Query += "(check-sat)\n";
        z3Query += "(get-model)\n";

        try {
            String z3QueryFilename = this.parameters.getTargetClassName() + "-P" + this.count + "-ToSolve.txt";
            Path z3QueryPath  = Paths.get(this.parameters.getTargetDirectory(), z3QueryFilename).toAbsolutePath();
            Files.write(z3QueryPath, z3Query.getBytes());

            String mainCommand = ProjectPaths.z3 +" -smt2 " + z3QueryPath + " -T:1";

            Process z3Process = Runtime.getRuntime().exec(mainCommand);
            BufferedReader in = new BufferedReader(new InputStreamReader(z3Process.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(z3Process.getErrorStream()));
            String z3Answer = in.readLine();

            String line = "";

            String z3Model = "";
            while ((line = in.readLine()) != null) {
                z3Model += line + "\n";
            }

            String z3Errors = "";
            while ((line = err.readLine()) != null) {
                z3Errors += line + "\n";
            }

            String z3AnswerFilename = this.parameters.getTargetClassName() + "-P" + this.count + "-Answer.txt";
            Path z3AnswerPath  = Paths.get(this.parameters.getTargetDirectory(), z3AnswerFilename).toAbsolutePath();
            Files.write(z3AnswerPath, z3Answer.getBytes());

            if (z3Answer.startsWith("(error")) {
                throw new RuntimeException("z3 Error: " + z3Answer);
            }

            areEquivalent = z3Answer.equals("unsat");

            if (areEquivalent || !hasUninterpretedFunctions) {
                Files.write(z3AnswerPath, z3Answer.getBytes());
            } else { // if (!areEquivalent && hasUninterpretedFunctions) {
                Files.write(z3AnswerPath, "unknown".getBytes());
            }

            // A model (i.e., counterexample) only exists if the two programs are NOT equivalent.
            if (!areEquivalent) {
                String z3ModelFilename = this.parameters.getTargetClassName() + "-P" + this.count + "-Model.txt";
                Path z3ModelPath  = Paths.get(this.parameters.getTargetDirectory(), z3ModelFilename).toAbsolutePath();
                Files.write(z3ModelPath, z3Model.getBytes());
            }

            if (!z3Errors.isEmpty()) {
                String z3ErrorsFilename = this.parameters.getTargetClassName() + "-P" + this.count + "-Errors.txt";
                Path z3ErrorsPath  = Paths.get(this.parameters.getTargetDirectory(), z3ErrorsFilename).toAbsolutePath();
                Files.write(z3ErrorsPath, z3Errors.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // -------------------------------------------------------
        // Replace the return value of the intercepted method
        // with the result of the equivalence check.

        stackFrame.setOperand(0, Types.booleanToInt(areEquivalent), false);
    }

    private String getDeclarations(String pcString) {
        String intDeclarations = this.getIntDeclarations(pcString);
        String realDeclarations = this.getRealDeclarations(pcString);
        String powDeclaration = "(define-fun pow ((a Real) (b Real)) Real (^ a b))";

        return Stream.of(this.parameters.getZ3Declarations(), intDeclarations, realDeclarations, powDeclaration)
            .filter(s -> s != null && !s.isEmpty())
            .collect(Collectors.joining("\n"));
    }

    private String getIntDeclarations(String pcString) {
        Pattern pattern = Pattern.compile("(INT\\d+)");
        Matcher matcher = pattern.matcher(pcString);

        Set<String> matches = new HashSet<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return matches.stream()
            .map(match -> "(declare-fun " + match + " () Int)")
            .filter(declaration -> !this.parameters.getZ3Declarations().contains(declaration))
            .collect(Collectors.joining("\n"));
    }

    private String getRealDeclarations(String pcString) {
        Pattern pattern = Pattern.compile("(REAL_\\d+)");
        Matcher matcher = pattern.matcher(pcString);

        Set<String> matches = new HashSet<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return matches.stream()
            .map(match -> "(declare-fun " + match + " () Real)")
            .filter(declaration -> !this.parameters.getZ3Declarations().contains(declaration))
            .collect(Collectors.joining("\n"));
    }
}
