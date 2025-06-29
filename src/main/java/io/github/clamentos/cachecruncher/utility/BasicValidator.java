package io.github.clamentos.cachecruncher.utility;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///.
import java.util.Collection;

///
public abstract class BasicValidator {

    ///
    public void requireNotNull(final Object obj, final String name) throws ValidationException {

        if(obj == null) throw this.fail(ErrorMessages.VALIDATOR_NN, name);
    }

    ///..
    public void requireNotNullAll(final Collection<?> objs, final String name) throws ValidationException {

        if(objs == null) throw this.fail(ErrorMessages.VALIDATOR_NN, name);
        int index = 0;

        for(Object obj : objs) {

            this.requireNotNull(obj, name + "[" + index + "]");
            index++;
        }
    }

    ///..
    public void requireNull(final Object obj, final String name) throws ValidationException {

        if(obj != null) throw this.fail(ErrorMessages.VALIDATOR_N, name);
    }

    ///..
    public void requireNotBlank(final String str, final String name) throws ValidationException {

        if(str == null || str.isBlank()) throw this.fail(ErrorMessages.VALIDATOR_NB, name);
    }

    ///..
    public void requireNotEmpty(final Collection<?> objs, final String name) throws ValidationException {

        if(objs == null || objs.isEmpty()) throw this.fail(ErrorMessages.VALIDATOR_NE, name);
    }

    ///..
    public <T extends Comparable<T>> void requireBetween(final T val, final T low, final T high, final String name)
    throws ValidationException {

        if(val == null || val.compareTo(low) < 0 || val.compareTo(high) > 0) {

            throw this.fail(ErrorMessages.VALIDATOR_B + low + " and " + high, name);
        }
    }

    ///.
    protected ValidationException fail(final String message, final String name) {

        return new ValidationException(ErrorCode.VALIDATOR_BAD_FORMAT, name, message);
    }

    ///
}
