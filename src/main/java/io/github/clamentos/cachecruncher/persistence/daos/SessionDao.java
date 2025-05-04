package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.persistence.entities.Session;

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
public class SessionDao extends Dao {

    ///
    private static final String INSERT_SQL = "INSERT INTO session (user_id,expires_at,email,device,id,is_admin) values (?,?,?,?,?,?)";
    private static final String SELECT_SQL = "SELECT user_id,expires_at,email,device,id,is_admin FROM session";
    private static final String DELETE_BY_ID_SQL = "DELETE FROM session WHERE id = ?";
    private static final String DELETE_ALL_BY_IDS_SQL = "DELETE FROM session WHERE id IN (";

    ///
    @Autowired
    public SessionDao(JdbcTemplate jdbcTemplate, Environment environment) {

        super(jdbcTemplate, environment);
    }

    ///
    @Transactional
    public void insert(Session session) throws DataAccessException {

        super.getJdbcTemplate().update(INSERT_SQL, preparedStatement -> {

            preparedStatement.setLong(1, session.getUserId());
            preparedStatement.setLong(2, session.getExpiresAt());
            preparedStatement.setString(3, session.getEmail());
            preparedStatement.setString(4, session.getDevice());
            preparedStatement.setString(5, session.getId());
            preparedStatement.setBoolean(6, session.isAdmin());
        });
    }

    ///..
    public List<Session> selectAll() throws DataAccessException {

        return super.getJdbcTemplate().query(SELECT_SQL, this::mapResultSet);
    }

    ///..
    @Transactional
    public void delete(String id) throws DataAccessException {

        super.getJdbcTemplate().update(DELETE_BY_ID_SQL, preparedStatement -> preparedStatement.setString(1, id));
    }

    ///..
    @Transactional
    public int deleteAll(Collection<String> ids) throws DataAccessException {

        int deleted = 0;

        if(!ids.isEmpty()) {

            List<String> idsList = ids.stream().toList();
            int batchSize = super.getBatchSize();
            int start = 0;

            while(start < idsList.size()) {

                deleted += this.doDelete(idsList.subList(start, start + batchSize));
                start += batchSize;
            }
        }

        return deleted;
    }

    ///.
    private List<Session> mapResultSet(ResultSet resultSet) throws SQLException {

        List<Session> sessions = new ArrayList<>();

        while(resultSet.next()) {

            sessions.add(new Session(

                resultSet.getLong(1),
                resultSet.getLong(2),
                resultSet.getString(3),
                resultSet.getString(4),
                resultSet.getString(5),
                resultSet.getBoolean(6)
            ));
        }

        return sessions;
    }

    ///..
    private int doDelete(List<String> ids) throws DataAccessException {

        StringBuilder idsString = new StringBuilder();

        for(String id : ids) {

            idsString.append(id).append(",");
        }

        idsString.deleteCharAt(idsString.length()).append(")");
        return super.getJdbcTemplate().update(DELETE_ALL_BY_IDS_SQL + idsString);
    }

    ///
}
