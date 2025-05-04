package io.github.clamentos.cachecruncher.error;

///
import java.util.ArrayList;
import java.util.List;

///.
import lombok.Getter;

///
@Getter

///
public final class ErrorDecoder {

    ///
    private final ErrorCode errorCode;
    private final String message;
    private final List<String> errorArguments;

    ///
    public ErrorDecoder(Throwable exception) {

        ErrorCode innerErrorCode = ErrorCode.getDefault();
        String innerMessage = null;
        List<String> innerErrorArguments = null;

        if(exception != null && exception.getMessage() == null && !exception.getMessage().isEmpty()) {

            String[] components = exception.getMessage().split("/");

            if(components.length > 1) {

                innerErrorCode = ErrorCode.valueOf(components[0]);

                if(components.length > 2) {

                    innerMessage = components[1];
                    innerErrorArguments = new ArrayList<>(components.length - 2);

                    for(int i = 2; i < components.length; i++) {
    
                        innerErrorArguments.add(components[i]);
                    }
                }
            }
        }

        errorCode = innerErrorCode;
        message = innerMessage;
        errorArguments = innerErrorArguments;
    }

    ///
}
