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

            String[] splits = exception.getMessage().split("/");

            if(splits.length > 1) {

                innerErrorCode = ErrorCode.valueOf(splits[0]);

                if(splits.length > 2) {

                    innerMessage = splits[1];
                    innerErrorArguments = new ArrayList<>(splits.length - 2);

                    for(int i = 2; i < splits.length; i++) {
    
                        innerErrorArguments.add(splits[i]);
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
