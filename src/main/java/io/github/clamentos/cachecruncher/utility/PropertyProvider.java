package io.github.clamentos.cachecruncher.utility;

///
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.stereotype.Component;

///
@Component

///
public class PropertyProvider {

    ///
    private final Environment environment;

    ///
    @Autowired
    public PropertyProvider(final Environment environment) {

        this.environment = environment;
    }

    ///
    public String getString(final String key, final String defaultValue) throws BeanCreationException {

        try { return environment.getProperty(key, String.class, defaultValue); }
        catch(final IllegalArgumentException | IllegalStateException exc) { throw this.failNotFound(key, exc); }
    }

    ///..
    public Boolean getBoolean(final String key, final Boolean defaultValue) throws BeanCreationException {

        try { return environment.getProperty(key, Boolean.class, defaultValue); }
        catch(final IllegalArgumentException | IllegalStateException exc) { throw this.failNotFound(key, exc); }
    }

    ///..
    @SuppressWarnings("squid:S2583")
    public Integer getInteger(final String key, final Integer defaultValue, final int low, final int high)
    throws BeanCreationException {

        try {

            final Integer value = environment.getProperty(key, Integer.class, defaultValue);

            if(value == null) return null;
            if(value.compareTo(low) >= 0 && value.compareTo(high) <= 0) return value;

            throw this.failNotValid(key, low, high);
        }

        catch(final IllegalArgumentException | IllegalStateException exc) {

            throw this.failNotFound(key, exc);
        }
    }

    ///..
    @SuppressWarnings("squid:S2583")
    public Long getLong(final String key, final Long defaultValue, final long low, final long high)
    throws BeanCreationException {

        try {

            final Long value = environment.getProperty(key, Long.class, defaultValue);

            if(value == null) return null;
            if(value.compareTo(low) >= 0 && value.compareTo(high) <= 0) return value;

            throw this.failNotValid(key, low, high);
        }

        catch(final IllegalArgumentException | IllegalStateException exc) {

            throw this.failNotFound(key, exc);
        }
    }

    ///.
    private BeanCreationException failNotFound(final String key, final Throwable exc) {

        return new BeanCreationException(ErrorMessages.PROPERTY_NOT_FOUND + key, exc);
    }

    ///..
    private BeanCreationException failNotValid(final String key, final long low, final long high) {

        return new BeanCreationException("Property: " + key + " is not between " + low + " and " + high);
    }

    ///
}
