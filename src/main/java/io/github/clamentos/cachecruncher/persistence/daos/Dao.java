package io.github.clamentos.cachecruncher.persistence.daos;

///
import org.springframework.core.env.Environment;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.jdbc.core.JdbcTemplate;

///.
import lombok.Getter;

///
@Getter

///
public abstract class Dao {

    ///
    private final JdbcTemplate jdbcTemplate;
    private final int batchSize;

    ///
    protected Dao(JdbcTemplate jdbcTemplate, Environment environment) {

        this.jdbcTemplate = jdbcTemplate;
        batchSize = environment.getProperty("cache-cruncher.jdbc.batchSize", Integer.class, 64);
    }

    ///
    protected int deleteWhereIdEquals(String tableName, long id) throws DataAccessException {

        return jdbcTemplate.update(

            "DELETE FROM " + tableName + " WHERE id=?",
            preparedStatement -> preparedStatement.setLong(1, id)
        );
    }

    ///
}
