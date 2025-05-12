package io.github.clamentos.cachecruncher.utility;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///.
import java.util.Collection;

///
public abstract class BasicValidator {

    ///
    public void requireNotNull(Object obj, String name) throws IllegalArgumentException {

        if(obj == null) {

            throw this.fail("BasicValidator.requireNotNull -> Argument cannot be null", name);
        }
    }

    ///..
    public void requireNotNullAll(Collection<?> objs, String name) throws IllegalArgumentException {

        if(objs == null) {

            throw this.fail("BasicValidator.requireNotNullAll -> Argument cannot be null", name);
        }

        int index = 0;

        for(Object obj : objs) {

            this.requireNotNull(obj, name + "[" + index + "]");
            index++;
        }
    }

    ///..
    public void requireNull(Object obj, String name) throws IllegalArgumentException {

        if(obj != null) {

            throw this.fail("BasicValidator.requireNull -> Argument must be null", name);
        }
    }

    ///..
    public void requireNotBlank(String str, String name) throws IllegalArgumentException {

        if(str == null || str.isBlank()) {

            throw this.fail("BasicValidator.requireNotBlank -> Argument cannot be null or blank", name);
        }
    }

    ///..
    public void requireNotEmpty(Collection<?> objs, String name) throws IllegalArgumentException {

        if(objs == null || objs.isEmpty()) {

            throw this.fail("BasicValidator.requireNotEmpty -> Argument cannot be null or empty", name);
        }
    }

    ///..
    public void requireBetween(Integer val, int low, int high, String name) throws IllegalArgumentException {

        if(val == null || val.compareTo(low) < 0 || val.compareTo(high) > 0) {

            throw this.fail(

                "BasicValidator.requireBetween -> Argument cannot be null and must be between " + low + " and " + high,
                name
            );
        }
    }

    ///.
    protected IllegalArgumentException fail(String message, String name) {

        return new IllegalArgumentException(new ErrorDetails(ErrorCode.VALIDATOR_BAD_FORMAT, name, message));
    }

    ///
}
