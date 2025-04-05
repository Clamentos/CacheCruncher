package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///.
import java.sql.ResultSet;
import java.sql.SQLException;

///..
import java.util.ArrayList;
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

///
@Repository

///
public class CacheTraceDao extends Dao {

    ///
    private static final String INSERT = "INSERT INTO cache_trace (created_at,updated_at,description,name,data) VALUES (?,?,?,?,?)";
    private static final String EXISTS_BY_ID = "SELECT COUNT(*) >= 1 FROM cache_trace WHERE id = ?";
    private static final String EXISTS_BY_NAME = "SELECT COUNT(*) >= 1 FROM cache_trace WHERE name = ?";
    private static final String SELECT_BY_ID = "SELECT id,created_at,updated_at,description,name,data FROM cache_trace WHERE id = ?";

    private static final String SELECT_MINIMAL = "SELECT id,created_at,updated_at,description,name FROM cache_trace WHERE name like ? AND created_at BETWEEN ? AND ? AND updated_at BETWEEN ? AND ?";

    private static final String UPDATE = "UPDATE cache_trace SET updated_at = ?, description = ?, name = ?, data = ? WHERE id = ?";
    private static final String DELETE_BY_ID = "DELETE FROM cache_trace WHERE id = ?";

    ///
    @Autowired
    public CacheTraceDao(JdbcTemplate jdbcTemplate, Environment environment) {

        super(jdbcTemplate, environment);
    }

    ///
    public void insert(CacheTrace cacheTrace) throws DataAccessException, NullPointerException {

        super.getJdbcTemplate().update(INSERT, preparedStatement -> {

            preparedStatement.setLong(1, cacheTrace.getCreatedAt());
            preparedStatement.setLong(2, cacheTrace.getUpdatedAt());
            preparedStatement.setString(3, cacheTrace.getDescription());
            preparedStatement.setString(4, cacheTrace.getName());
            preparedStatement.setString(5, cacheTrace.getData());
        });
    }

    ///..
    public boolean existsById(long id) throws DataAccessException {

        Boolean exists = super.getJdbcTemplate().query(

            EXISTS_BY_ID,
            preparedStatement -> preparedStatement.setLong(1, id),

            resultSet -> {

                resultSet.next();
                return resultSet.getLong(1) == 1L;
            }
        );

        return exists != null && exists.booleanValue();
    }

    ///..
    public boolean existsByName(String name) throws DataAccessException {

        Boolean exists = super.getJdbcTemplate().query(

            EXISTS_BY_NAME,
            preparedStatement -> preparedStatement.setString(1, name),

            resultSet -> {

                resultSet.next();
                return resultSet.getLong(1) == 1L;
            }
        );

        return exists != null && exists.booleanValue(); 
    }

    ///..
    public CacheTrace selectById(long id) throws DataAccessException {

        return super.getJdbcTemplate().query(

            SELECT_BY_ID,
            preparedStatement -> preparedStatement.setLong(1, id),
            this::mapResultSetSingle
        );
    }

    ///..
    public List<CacheTrace> selectMinimalByNameLikeAndDates(

        String nameLike,
        long createdAtStart,
        long createdAtEnd,
        long updatedAtStart,
        long updatedAtEnd

    ) throws DataAccessException {

        return super.getJdbcTemplate().query(

            SELECT_MINIMAL,

            preparedStatement -> {

                preparedStatement.setString(1, nameLike);
                preparedStatement.setLong(2, createdAtStart);
                preparedStatement.setLong(3, createdAtEnd);
                preparedStatement.setLong(4, updatedAtStart);
                preparedStatement.setLong(5, updatedAtEnd);
            },

            this::mapResultSet
        );
    }

    ///..
    public void update(CacheTrace cacheTrace) {

        super.getJdbcTemplate().update(UPDATE, preparedStatement -> {

            preparedStatement.setLong(1, cacheTrace.getUpdatedAt());
            preparedStatement.setString(2, cacheTrace.getDescription());
            preparedStatement.setString(3, cacheTrace.getName());
            preparedStatement.setString(4, cacheTrace.getData());
            preparedStatement.setLong(5, cacheTrace.getId());
        });
    }

    ///..
    public void delete(long id) throws DataAccessException {

        super.getJdbcTemplate().update(

            DELETE_BY_ID,
            preparedStatement -> preparedStatement.setLong(1, id)
        );
    }

    ///.
    private CacheTrace mapResultSetSingle(ResultSet resultSet) throws SQLException {

        if(resultSet.next()) return constructFromResultSet(resultSet, true);
        return null;
    }

    ///..
    private List<CacheTrace> mapResultSet(ResultSet resultSet) throws SQLException {

        List<CacheTrace> entities = new ArrayList<>();

        while(resultSet.next()) {

            entities.add(constructFromResultSet(resultSet, false));
        }

        return entities;
    }

    ///..
    private CacheTrace constructFromResultSet(ResultSet resultSet, boolean full) throws SQLException {

        return new CacheTrace(

            resultSet.getLong(1),
            resultSet.getLong(2),
            resultSet.getLong(3),
            resultSet.getString(4),
            resultSet.getString(5),
            full ? resultSet.getString(6) : null
        );
    }

    ///
}
