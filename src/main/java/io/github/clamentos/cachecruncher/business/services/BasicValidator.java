package io.github.clamentos.cachecruncher.business.services;

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

    ///
}
