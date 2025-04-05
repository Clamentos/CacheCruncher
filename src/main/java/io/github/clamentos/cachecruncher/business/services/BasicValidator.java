package io.github.clamentos.cachecruncher.business.services;

import java.util.Collection;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///
public abstract class BasicValidator {

    ///
    public void requireNotNull(Object obj, String name) throws IllegalArgumentException {

        if(obj == null) {

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "BasicValidator.requireNotNull -> Argument cannot be null",
                name
            ));
        }
    }

    ///..
    public void requireNotNullAll(Collection<?> objs, String name) throws IllegalArgumentException {

        if(objs == null) {

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "BasicValidator.requireNotNullAll -> Argument cannot be null",
                name
            ));
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

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "BasicValidator.requireNull -> Argument must be null",
                name
            ));
        }
    }

    ///..
    public void requireNotBlank(String str, String name) throws IllegalArgumentException {

        if(str == null || str.isBlank()) {

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "BasicValidator.requireNotBlank -> Argument cannot be null or blank",
                name
            ));
        }
    }

    ///..
    public void requireNotEmpty(Collection<?> objs, String name) throws IllegalArgumentException {

        if(objs == null || objs.isEmpty()) {

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "BasicValidator.requireNotEmpty -> Argument cannot be null or empty",
                name
            ));
        }
    }

    ///..
    public void requirePositive(Integer val, String name) throws IllegalArgumentException {

        if(val == null || val.compareTo(0) <= 0) {

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "BasicValidator.requirePositive -> Argument cannot be null or less than zero",
                name
            ));
        }
    }

    ///
}
