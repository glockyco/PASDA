package equiv.checking;

import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class DifferencingParameterFactory {
    public DifferencingParameters create(
        String toolName,
        String directory,
        String namespace,
        String oldClassName,
        String newClassName,
        String methodSignature,
        String[] methodParameters,
        Map<String, String> parameterTypes,
        String z3Declarations
    ) {
        List<MethodParameterDescription> parameterDescriptions = this.createParameterDescriptions(methodParameters, parameterTypes);
        MethodResultDescription resultDescription = this.createMethodResult(methodSignature);

        MethodDescription oldMethodDescription = new MethodDescription(
            namespace,
            oldClassName + toolName,
            "snippet",
            parameterDescriptions,
            resultDescription
        );

        MethodDescription newMethodDescription = new MethodDescription(
            namespace,
            newClassName + toolName,
            "snippet",
            parameterDescriptions,
            resultDescription
        );

        MethodDescription diffMethodDescription = new MethodDescription(
            namespace,
            "IDiff" + toolName,
            "run",
            parameterDescriptions,
            resultDescription
        );

        return new DifferencingParameters(
            directory,
            z3Declarations,
            oldMethodDescription,
            newMethodDescription,
            diffMethodDescription
        );
    }

    protected List<MethodParameterDescription> createParameterDescriptions(
        String[] methodParameters, Map<String, String> parameterTypes
    ) {
        return Arrays.stream(methodParameters)
            .map(parameterName -> {
                String dataType = Type.getType(parameterTypes.get(parameterName)).getClassName();
                return new MethodParameterDescription(parameterName, dataType);
            }).collect(Collectors.toList());
    }

    protected MethodResultDescription createMethodResult(String methodSignature) {
        return new MethodResultDescription(Type.getMethodType(methodSignature).getReturnType().getClassName());
    }
}
