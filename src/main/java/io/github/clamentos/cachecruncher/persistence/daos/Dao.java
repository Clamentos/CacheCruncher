package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.util.function.Supplier;

///.
import lombok.Getter;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.jdbc.core.JdbcTemplate;

///
@Getter

///
public abstract class Dao {

    ///
    private final JdbcTemplate jdbcTemplate;

    ///..
    private final int batchSize;

    ///
    protected Dao(final JdbcTemplate jdbcTemplate, final PropertyProvider propertyProvider) throws BeanCreationException {

        this.jdbcTemplate = jdbcTemplate;
        batchSize = propertyProvider.getInteger("cache-cruncher.jdbc.batchSize", 64, 1, Integer.MAX_VALUE);
    }

    ///
    protected int deleteWhereIdEquals(final String tableName, final long id) throws DatabaseException {

        try {

            return jdbcTemplate.update(

                "DELETE FROM " + tableName + " WHERE id=?",
                preparedStatement -> preparedStatement.setLong(1, id)
            );
        }

        catch(final DataAccessException exc) {

            throw new DatabaseException(exc);
        }
    }

    ///..
    protected <T> T wrap(final Supplier<T> action) throws DatabaseException {

        try { return action.get(); }
        catch(final DataAccessException exc) { throw new DatabaseException(exc); }
    }

    ///
}
