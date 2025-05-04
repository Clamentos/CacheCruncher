package io.github.clamentos.cachecruncher.persistence.daos;

///
import org.springframework.core.env.Environment;

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
}
