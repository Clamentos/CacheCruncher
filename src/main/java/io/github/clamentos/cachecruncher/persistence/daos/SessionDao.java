package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
import io.github.clamentos.cachecruncher.persistence.UserRole;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

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
public class SessionDao extends Dao {

    ///
    private static final String INSERT_SQL = "INSERT INTO session (user_id,expires_at,email,device,id,role) values (?,?,?,?,?,?)";
    private static final String SELECT_SQL = "SELECT user_id,expires_at,email,device,id,role FROM session";
    private static final String DELETE_BY_ID_SQL = "DELETE FROM session WHERE id=?";
    private static final String DELETE_ALL_BY_IDS_SQL = "DELETE FROM session WHERE id IN (";
    private static final String DELETE_ALL_EXPIRED = "DELETE FROM session WHERE expires_at <= ?";

    ///
    @Autowired
    public SessionDao(final JdbcTemplate jdbcTemplate, final PropertyProvider propertyProvider) throws BeanCreationException {

        super(jdbcTemplate, propertyProvider);
    }

    ///
    @Transactional(rollbackFor = DatabaseException.class)
    public void insert(final Session session) throws DatabaseException {

        super.wrap(() ->

            super.getJdbcTemplate().update(INSERT_SQL, preparedStatement -> {

                preparedStatement.setLong(1, session.getUserId());
                preparedStatement.setLong(2, session.getExpiresAt());
                preparedStatement.setString(3, session.getEmail());
                preparedStatement.setString(4, session.getDevice());
                preparedStatement.setString(5, session.getId());
                preparedStatement.setString(6, session.getRole().toString());
            })
        );
    }

    ///..
    public List<Session> selectAll() throws DatabaseException {

        return super.wrap(() -> super.getJdbcTemplate().query(SELECT_SQL, this::mapResultSet));
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public void delete(final String id) throws DatabaseException {

        super.wrap(() -> super.getJdbcTemplate().update(DELETE_BY_ID_SQL, preparedStatement -> preparedStatement.setString(1, id)));
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public int deleteAll(final Collection<String> ids) throws DatabaseException {

        int deleted = 0;

        if(!ids.isEmpty()) {

            final List<String> idsList = ids.stream().toList();
            int batchSize = super.getBatchSize();
            int start = 0;

            while(start < idsList.size()) {

                int end = Math.min(start + batchSize, idsList.size());
                deleted += this.doDelete(idsList.subList(start, end));
                start += end;
            }
        }

        return deleted;
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public void deleteExpired() throws DatabaseException {

        super.wrap(() ->

            super.getJdbcTemplate().update(

                DELETE_ALL_EXPIRED,
                preparedStatement -> preparedStatement.setLong(1, System.currentTimeMillis())
            )
        );
    }

    ///.
    private List<Session> mapResultSet(final ResultSet resultSet) throws SQLException {

        final List<Session> sessions = new ArrayList<>();

        while(resultSet.next()) {

            sessions.add(new Session(

                resultSet.getLong(1),
                resultSet.getLong(2),
                resultSet.getString(3),
                resultSet.getString(4),
                resultSet.getString(5),
                UserRole.valueOf(resultSet.getString(6))
            ));
        }

        return sessions;
    }

    ///..
    private int doDelete(final List<String> ids) throws DatabaseException {

        final StringBuilder idsString = new StringBuilder();

        for(final String id : ids) idsString.append("\"").append(id).append("\",");
        idsString.deleteCharAt(idsString.length() - 1).append(")");

        return super.wrap(() -> super.getJdbcTemplate().update(DELETE_ALL_BY_IDS_SQL + idsString));
    }

    ///
}
