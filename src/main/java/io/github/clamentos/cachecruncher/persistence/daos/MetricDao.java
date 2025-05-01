package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.persistence.entities.Metric;

///.
import java.sql.ResultSet;
import java.sql.SQLException;

///..
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.dao.DataAccessException;

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
    private static final String INSERT_SQL = "INSERT INTO metric (created_at,second,endpoint,status,data) values (?,?,?,?,?)";

    private static final String SELECT_SQL = "SELECT id,createdAt,second,endpoint,status,data FROM metric WHERE created_at > ? and created_at BETWEEN ? AND ? LIMIT ?";

    private static final String DELETE_SQL = "DELETE FROM metric WHERE created_at BETWEEN ? and ?";

    ///
    @Autowired
    public MetricDao(JdbcTemplate jdbcTemplate, Environment environment) {

        super(jdbcTemplate, environment);
    }

    ///
    @Transactional
    public void insert(Collection<Metric> metrics) throws DataAccessException {

        if(metrics != null && !metrics.isEmpty()) {

            super.getJdbcTemplate().batchUpdate(INSERT_SQL, metrics, super.getBatchSize(), (preparedStatement, metric) -> {

                preparedStatement.setLong(1, metric.getCreatedAt());
                preparedStatement.setInt(2, metric.getSecond());
                preparedStatement.setString(3, metric.getEndpoint());
                preparedStatement.setShort(4, metric.getStatus());
                preparedStatement.setString(5, metric.getData());
            });
        }
    }

    ///..
    public List<Metric> selectMetricsByFilter(

        Long createdAtStart,
        Long createdAtEnd,
        long lastTimestamp,
        int count

    ) throws DataAccessException {

        return super.getJdbcTemplate().query(

            SELECT_SQL,

            preparedStatement -> {

                preparedStatement.setLong(1, lastTimestamp);
                preparedStatement.setLong(2, createdAtStart);
                preparedStatement.setLong(3, createdAtEnd);
                preparedStatement.setLong(4, count);
            },

            this::mapResultSet
        );
    }

    ///..
    @Transactional
    public int delete(long createdAtStart, long createdAtEnd) throws DataAccessException {

        return super.getJdbcTemplate().update(DELETE_SQL, createdAtStart, createdAtEnd);
    }

    ///.
    private List<Metric> mapResultSet(ResultSet resultSet) throws SQLException {

        List<Metric> metrics = new ArrayList<>();

        while(resultSet.next()) {

            metrics.add(new Metric(

                resultSet.getLong(1),
                resultSet.getLong(2),
                resultSet.getInt(3),
                resultSet.getString(4),
                resultSet.getShort(5),
                resultSet.getString(6)
            ));
        }

        return metrics;
    }

    ///
}
