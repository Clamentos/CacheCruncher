package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Metric;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.sql.ResultSet;
import java.sql.SQLException;

///..
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.jdbc.core.JdbcTemplate;

///..
import org.springframework.stereotype.Repository;

///..
import org.springframework.transaction.annotation.Transactional;

///
@Repository

///
public class MetricDao extends Dao {

    ///
    private static final String INSERT_SQL = "INSERT INTO metric (created_at,timestamp,endpoint,data,status) values (?,?,?,?,?)";

    private static final String SELECT_SQL = "SELECT id,createdAt,timestamp,endpoint,data,status FROM metric WHERE created_at > ? and created_at BETWEEN ? AND ? LIMIT ?";

    private static final String DELETE_SQL = "DELETE FROM metric WHERE created_at BETWEEN ? and ?";

    ///
    @Autowired
    public MetricDao(final JdbcTemplate jdbcTemplate, final PropertyProvider propertyProvider) throws BeanCreationException {

        super(jdbcTemplate, propertyProvider);
    }

    ///
    @Transactional(rollbackFor = DatabaseException.class)
    public void insert(final Collection<Metric> metrics) throws DatabaseException {

        if(!metrics.isEmpty()) {

            super.wrap(() ->

                super.getJdbcTemplate().batchUpdate(INSERT_SQL, metrics, super.getBatchSize(), (preparedStatement, metric) -> {

                    preparedStatement.setLong(1, metric.getCreatedAt());
                    preparedStatement.setLong(2, metric.getTimestamp());
                    preparedStatement.setString(3, metric.getEndpoint());
                    preparedStatement.setString(4, metric.getData());
                    preparedStatement.setShort(5, metric.getStatus());
                })
            );
        }
    }

    ///..
    public List<Metric> selectMetricsByFilter(
        
        final long lastTimestamp,
        final int count,
        final long createdAtStart,
        final long createdAtEnd

    ) throws DatabaseException {

        return super.wrap(() ->

            super.getJdbcTemplate().query(

                SELECT_SQL,

                preparedStatement -> {

                    preparedStatement.setLong(1, lastTimestamp);
                    preparedStatement.setLong(2, createdAtStart);
                    preparedStatement.setLong(3, createdAtEnd);
                    preparedStatement.setLong(4, count);
                },

                this::mapResultSet
            )
        );
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public int delete(final long createdAtStart, final long createdAtEnd) throws DatabaseException {

        return super.wrap(() -> super.getJdbcTemplate().update(DELETE_SQL, createdAtStart, createdAtEnd));
    }

    ///.
    private List<Metric> mapResultSet(final ResultSet resultSet) throws SQLException {

        final List<Metric> metrics = new ArrayList<>();

        while(resultSet.next()) {

            metrics.add(new Metric(

                resultSet.getLong(1),
                resultSet.getLong(2),
                resultSet.getLong(3),
                resultSet.getString(4),
                resultSet.getString(5),
                resultSet.getShort(6)
            ));
        }

        return metrics;
    }

    ///
}
