package io.github.clamentos.cachecruncher.error;

///
public final class ErrorFactory {

    ///
    private ErrorFactory() {}

    ///
    // <errorCode>/<message>/args[0]/args[1]/...
    public static String create(ErrorCode errorCode, String message, Object... args) {

        StringBuilder stringBuilder = new StringBuilder(errorCode != null ? errorCode.name() : ErrorCode.getDefault().name());

        stringBuilder.append("/");
        stringBuilder.append(message);
        stringBuilder.append("/");

        for(Object arg : args) {

            stringBuilder.append(arg).append("/");
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    ///
}
