package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.monitoring.logging.LogLevel;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Log;

///.
import java.sql.ResultSet;
import java.sql.SQLException;

///..
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class LogDao extends Dao {

    ///
    private static final String INSERT_SQL = "INSERT INTO log (created_at,thread,logger,message,level) values (?,?,?,?,?)";

    private static final String SELECT_SQL = "SELECT id,created_at,thread,logger,message,level FROM log WHERE created_at > ? AND created_at BETWEEN ? AND ? AND level IN (?, ?, ?, ?, ?) AND thread LIKE ? AND logger LIKE ? AND message LIKE ? LIMIT ?";

    private static final String COUNT_SQL = "SELECT level, COUNT(*) FROM log GROUP BY level";
    private static final String DELETE_SQL = "DELETE FROM log WHERE created_at BETWEEN ? and ?";

    ///
    @Autowired
    public LogDao(JdbcTemplate jdbcTemplate, Environment environment) {

        super(jdbcTemplate, environment);
    }

    ///
    @Transactional
    public void insert(Collection<Log> logs) throws DataAccessException {

        if(logs != null && !logs.isEmpty()) {

            super.getJdbcTemplate().batchUpdate(INSERT_SQL, logs, super.getBatchSize(), (preparedStatement, log) -> {

                preparedStatement.setLong(1, log.getCreatedAt());
                preparedStatement.setString(2, log.getThread());
                preparedStatement.setString(3, log.getLogger());
                preparedStatement.setString(4, log.getMessage());
                preparedStatement.setString(5, log.getLevel().toString());
            });
        }
    }

    ///..
    /**
     * Select all the logs that obey the specified parameters.
     * @param createdAtStart : The start of the creation date range.
     * @param createdAtEnd : The end of the creation date range.
     * @param levels : The desired log levels. (cannot be {@code null} or empty).
     * @param threadLike : The thread name pattern.
     * @param loggerLike : The logger name pattern.
     * @param messageLike : The log message pattern.
     * @param lastTimestamp : The log timestamp for pagination.
     * @param count : The maximum number of logs to be selected for pagination.
     * @return The never {@code null} list of selected logs.
     * @throws DataAccessException If any database access error occurs.
     * @throws NullPointerException If {@code levels} is {@code null}.
    */
    public List<Log> selectLogsByFilter(

        long createdAtStart,
        long createdAtEnd,
        Set<LogLevel> levels,
        String threadLike,
        String loggerLike,
        String messageLike,
        long lastTimestamp,
        int count

    ) throws DataAccessException, NullPointerException {

        return super.getJdbcTemplate().query(

            SELECT_SQL,

            preparedStatement -> {

                preparedStatement.setLong(1, lastTimestamp);
                preparedStatement.setLong(2, createdAtStart);
                preparedStatement.setLong(3, createdAtEnd);

                int index = 4;
                LogLevel lastElement = null;

                for(LogLevel level : levels) {

                    preparedStatement.setString(index++, level.toString());
                    lastElement = level;
                }

                while(index < 9) {

                    preparedStatement.setString(index++, lastElement.toString());
                }

                preparedStatement.setString(9, threadLike);
                preparedStatement.setString(10, loggerLike);
                preparedStatement.setString(11, messageLike);
                preparedStatement.setLong(12, count);
            },

            this::mapResultSet
        );
    }

    ///..
    public Map<LogLevel, Long> countByLevel() throws DataAccessException {

        return super.getJdbcTemplate().query(COUNT_SQL, resultSet -> {

            Map<LogLevel, Long> result = new EnumMap<>(LogLevel.class);

            while(resultSet.next()) {

                result.put(

                    LogLevel.valueOf(resultSet.getString(1)),
                    resultSet.getLong(2)
                );
            }

            return(result);
        });
    }

    ///..
    public int delete(long createdAtStart, long createdAtEnd) throws DataAccessException {

        return super.getJdbcTemplate().update(DELETE_SQL, createdAtStart, createdAtEnd);
    }

    ///.
    private List<Log> mapResultSet(ResultSet resultSet) throws SQLException {

        List<Log> logs = new ArrayList<>();

        while(resultSet.next()) {

            logs.add(new Log(

                resultSet.getLong(1),
                resultSet.getLong(2),
                resultSet.getString(3),
                resultSet.getString(4),
                resultSet.getString(5),
                LogLevel.valueOf(resultSet.getString(6))
            ));
        }

        return logs;
    }

    ///
}
