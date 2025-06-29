package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
import io.github.clamentos.cachecruncher.mappers.CacheTraceMapper;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;

///..
import java.util.ArrayList;
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
public class CacheTraceDao extends Dao {

    ///
    private static final String INSERT_SQL = "INSERT INTO cache_trace (created_at,updated_at,description,name,statistics,data) VALUES (?,?,?,?,?,?)";

    private static final String SELECT_BY_ID_SQL = "SELECT id,created_at,updated_at,description,name,statistics,data FROM cache_trace WHERE id=?";

    private static final String SELECT_BY_FILTER_SQL = "SELECT id,created_at,updated_at,description,name FROM cache_trace WHERE name like ? AND created_at BETWEEN ? AND ? AND updated_at BETWEEN ? AND ?";

    private static final String SELECT_BY_FILTER_EXCLUDE_UPDATE_SQL = "SELECT id,created_at,updated_at,description,name FROM cache_trace WHERE name like ? AND created_at BETWEEN ? AND ?";

    private static final String UPDATE_SQL = "UPDATE cache_trace SET updated_at=?, description=?, name=?, statistics=?, data=? WHERE id=?";

    ///.
    private final CacheTraceMapper cacheTraceMapper;

    ///
    @Autowired
    public CacheTraceDao(

        final JdbcTemplate jdbcTemplate,
        final PropertyProvider propertyProvider,
        final CacheTraceMapper cacheTraceMapper

    ) throws BeanCreationException {

        super(jdbcTemplate, propertyProvider);
        this.cacheTraceMapper = cacheTraceMapper;
    }

    ///
    @Transactional(rollbackFor = DatabaseException.class)
    public void insert(final CacheTrace cacheTrace) throws DatabaseException {

        super.wrap(() -> 

            super.getJdbcTemplate().update(INSERT_SQL, preparedStatement -> {

                preparedStatement.setLong(1, cacheTrace.getCreatedAt());
                preparedStatement.setObject(2, cacheTrace.getUpdatedAt(), JDBCType.BIGINT.getVendorTypeNumber());
                preparedStatement.setString(3, cacheTrace.getDescription());
                preparedStatement.setString(4, cacheTrace.getName());
                preparedStatement.setString(5, cacheTrace.getStatistics());
                preparedStatement.setBinaryStream(6, cacheTraceMapper.serializeBody(cacheTrace.getTrace()));
            })
        );
    }

    ///..
    public CacheTrace selectById(final long id) throws DatabaseException {

        return super.wrap(() ->

            super.getJdbcTemplate().query(

                SELECT_BY_ID_SQL,
                preparedStatement -> preparedStatement.setLong(1, id),
                this::mapResultSetSingle
            )
        );
    }

    ///..
    public List<CacheTrace> selectMinimalByNameLikeAndDates(

        final String nameLike,
        final long createdAtStart,
        final long createdAtEnd,
        final long updatedAtStart,
        final long updatedAtEnd

    ) throws DatabaseException {

        return super.wrap(() ->

            super.getJdbcTemplate().query(

                SELECT_BY_FILTER_SQL,

                preparedStatement -> {

                    preparedStatement.setString(1, nameLike);
                    preparedStatement.setLong(2, createdAtStart);
                    preparedStatement.setLong(3, createdAtEnd);
                    preparedStatement.setLong(4, updatedAtStart);
                    preparedStatement.setLong(5, updatedAtEnd);
                },

                this::mapResultSet
            )
        );
    }

    ///..
    public List<CacheTrace> selectMinimalByNameLikeAndDate(final String nameLike, final long createdAtStart, final long createdAtEnd)
    throws DatabaseException {

        return super.wrap(() ->

            super.getJdbcTemplate().query(

                SELECT_BY_FILTER_EXCLUDE_UPDATE_SQL,

                preparedStatement -> {

                    preparedStatement.setString(1, nameLike);
                    preparedStatement.setLong(2, createdAtStart);
                    preparedStatement.setLong(3, createdAtEnd);
                },

                this::mapResultSet
            )
        );
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public void update(final CacheTrace cacheTrace) throws DatabaseException {

        super.wrap(() ->

            super.getJdbcTemplate().update(UPDATE_SQL, preparedStatement -> {

                preparedStatement.setObject(1, cacheTrace.getUpdatedAt(), JDBCType.BIGINT.getVendorTypeNumber());
                preparedStatement.setString(2, cacheTrace.getDescription());
                preparedStatement.setString(3, cacheTrace.getName());
                preparedStatement.setString(4, cacheTrace.getStatistics());
                preparedStatement.setBinaryStream(5, cacheTraceMapper.serializeBody(cacheTrace.getTrace()));
                preparedStatement.setLong(6, cacheTrace.getId());
            })
        );
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public int delete(final long id) throws DatabaseException {

        return super.deleteWhereIdEquals("cache_trace", id);
    }

    ///.
    private CacheTrace mapResultSetSingle(final ResultSet resultSet) throws SQLException {

        if(resultSet.next()) return this.constructFromResultSet(resultSet, true);
        return null;
    }

    ///..
    private List<CacheTrace> mapResultSet(final ResultSet resultSet) throws SQLException {

        final List<CacheTrace> cacheTraces = new ArrayList<>();
        while(resultSet.next()) cacheTraces.add(this.constructFromResultSet(resultSet, false));

        return cacheTraces;
    }

    ///..
    private CacheTrace constructFromResultSet(final ResultSet resultSet, final boolean full) throws SQLException {

        return new CacheTrace(

            resultSet.getLong(1),
            resultSet.getLong(2),
            (Long)resultSet.getObject(3),
            resultSet.getString(4),
            resultSet.getString(5),
            full ? resultSet.getString(6) : null,
            full ? cacheTraceMapper.deserializeBody(resultSet.getBinaryStream(7)) : null
        );
    }

    ///
}
