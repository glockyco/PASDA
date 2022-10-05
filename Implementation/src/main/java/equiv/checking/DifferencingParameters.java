package equiv.checking;

import equiv.checking.models.Classification;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class DifferencingParameters implements Serializable {
    private static final String BENCHMARKS_DIR = "../benchmarks/";
    private static final Path BENCHMARKS_PATH = Paths.get(BENCHMARKS_DIR);

    private final String directory;
    private final String toolName;
    private final MethodDescription oldMethodDescription;
    private final MethodDescription newMethodDescription;
    private final MethodDescription diffMethodDescription;

    public DifferencingParameters(
        String directory,
        String toolName,
        MethodDescription oldMethodDescription,
        MethodDescription newMethodDescription,
        MethodDescription diffMethodDescription
    ) {
        this.directory = directory;
        this.toolName = toolName;
        this.oldMethodDescription = oldMethodDescription;
        this.newMethodDescription = newMethodDescription;
        this.diffMethodDescription = diffMethodDescription;
    }

    public String getToolName() {
        return this.toolName;
    }

    public String getTargetDirectory() {
        return this.directory;
    }

    public String getBenchmarkName() {
        Path path = BENCHMARKS_PATH.relativize(Paths.get(this.directory));
        return path.subpath(0, 3).toString();
    }

    public String getExpectedResult() {
        Path path = BENCHMARKS_PATH.relativize(Paths.get(this.directory));

        // While some benchmarks have the Eq/NEq folder at the second level
        // of the hierarchy (e.g., ModDiff/Eq/Add), others have it at the
        // third level (e.g., power/test/Eq), so we have to check both.

        String name1 = path.getName(1).toString();
        String name2 = path.getName(2).toString();

        if (name1.equals("Eq") || name2.equals("Eq")) {
            return Classification.EQ.toString();
        } else if (name1.equals("NEq") || name2.equals("NEq")) {
            return Classification.NEQ.toString();
        }

        throw new RuntimeException("Cannot determine expected result for " + this.directory + ".");
    }

    public String getParameterFile() {
        return Paths.get(this.directory, "IDiff" + this.toolName + "-Parameters.txt").toString();
    }

    public String getJavaFile() {
        return Paths.get(this.directory, "IDiff" + this.toolName + ".java").toString();
    }

    public String getJpfFile() {
        return Paths.get(this.directory, "IDiff" + this.toolName + ".jpf").toString();
    }

    public String getOutputFile() {
        return Paths.get(this.directory, "IDiff" + this.toolName + "-Output.txt").toString();
    }

    public String getErrorFile() {
        return Paths.get(this.directory, "IDiff" + this.toolName + "-Error.txt").toString();
    }

    public String getResultFile() {
        return Paths.get(this.directory, "IDiff" + this.toolName + "-Result.txt").toString();
    }

    public String getBaseToolOutputFile() {
        return Paths.get(this.directory, "..", "outputs", this.toolName + ".txt").toString();
    }

    public String[] getZ3QueryFiles() throws IOException {
        return this.getFiles("glob:**/IDiff" + this.toolName + "-P*-ToSolve*.txt");
    }

    public String[] getZ3AnswerFiles() throws IOException {
        return this.getFiles("glob:**/IDiff" + this.toolName + "-P*-Answer*.txt");
    }

    public String[] getZ3ModelFiles() throws IOException {
        return this.getFiles("glob:**/IDiff" + this.toolName + "-P*-Model*.txt");
    }

    public String[] getHasUifFiles() throws IOException {
        return this.getFiles("glob:**/IDiff" + this.toolName + "-P*-HasUIF*.txt");
    }

    public String[] getJsonFiles() throws IOException {
        return this.getFiles("glob:**/IDiff" + this.toolName + "-P*-JSON*.json");
    }

    private String[] getFiles(String glob) throws IOException {
        List<String> answerFiles = new ArrayList<>();

        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);
        Files.walkFileTree(Paths.get(this.getTargetDirectory()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                if (pathMatcher.matches(path)) {
                    answerFiles.add(path.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return answerFiles.toArray(new String[0]);
    }

    public String[] getGeneratedFiles() throws IOException {
        List<String> generatedFiles = new ArrayList<>();

        generatedFiles.add(this.getJavaFile());
        generatedFiles.add(this.getJpfFile());
        generatedFiles.addAll(Arrays.asList(this.getZ3QueryFiles()));
        generatedFiles.addAll(Arrays.asList(this.getZ3AnswerFiles()));
        generatedFiles.addAll(Arrays.asList(this.getZ3ModelFiles()));
        generatedFiles.addAll(Arrays.asList(this.getHasUifFiles()));
        generatedFiles.addAll(Arrays.asList(this.getJsonFiles()));
        generatedFiles.add(this.getOutputFile());
        generatedFiles.add(this.getErrorFile());
        generatedFiles.add(this.getResultFile());

        return generatedFiles.toArray(new String[0]);
    }

    public String getTargetNamespace() {
        return this.diffMethodDescription.getNamespace();
    }

    public String getTargetClassName() {
        return this.diffMethodDescription.getClassName();
    }

    public String getSymbolicParameters() {
        int parameterCount = this.diffMethodDescription.getParameters().size();
        return String.join("#", Collections.nCopies(parameterCount, "sym"));
    }

    public String getInputParameters() {
        return this.diffMethodDescription.getParameters().stream()
            .map(parameter -> parameter.getDataType() + " " + parameter.getName())
            .collect(Collectors.joining(", "));
    }

    public String getInputVariables() {
        return this.diffMethodDescription.getParameters().stream()
            .map(MethodParameterDescription::getName)
            .collect(Collectors.joining(", "));
    }

    public String getInputValues() {
        return this.diffMethodDescription.getParameters().stream()
            .map(MethodParameterDescription::getPlaceholderValue)
            .collect(Collectors.joining(", "));
    }

    public String getOldNamespace() {
        return this.oldMethodDescription.getNamespace();
    }

    public String getOldClassName() {
        return this.oldMethodDescription.getClassName();
    }

    public String getOldReturnType() {
        return this.oldMethodDescription.getResult().getDataType();
    }

    public String getOldResultDefaultValue() {
        return this.oldMethodDescription.getResult().getPlaceholderValue();
    }

    public String getNewNamespace() {
        return this.newMethodDescription.getNamespace();
    }

    public String getNewClassName() {
        return this.newMethodDescription.getClassName();
    }

    public String getNewReturnType() {
        return this.newMethodDescription.getResult().getDataType();
    }

    public String getNewResultDefaultValue() {
        return this.newMethodDescription.getResult().getPlaceholderValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DifferencingParameters that = (DifferencingParameters) o;
        return Objects.equals(directory, that.directory)
            && Objects.equals(oldMethodDescription, that.oldMethodDescription)
            && Objects.equals(newMethodDescription, that.newMethodDescription)
            && Objects.equals(diffMethodDescription, that.diffMethodDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            directory,
            oldMethodDescription,
            newMethodDescription,
            diffMethodDescription
        );
    }
}


