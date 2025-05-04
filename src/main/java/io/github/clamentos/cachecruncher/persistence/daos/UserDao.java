package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.persistence.entities.User;

///.
import java.sql.JDBCType;
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

///..
import org.springframework.transaction.annotation.Transactional;

///
@Repository

///
public class UserDao extends Dao {

    ///
    private static final String INSERT_SQL = "INSERT INTO user (locked_until,created_at,email,password,failed_accesses,is_admin) values (?,?,?,?,?,?)";

    private static final String EXISTS_SQL = "SELECT count(*) > 0 FROM user WHERE email = ?";
    private static final String SELECT_PRIVILEGE_SQL = "SELECT is_admin FROM user WHERE id = ?";

    private static final String SELECT_BY_USERNAME_SQL = "SELECT id,locked_until,created_at,email,password,failed_accesses,is_admin FROM user WHERE email = ?";

    private static final String SELECT_ALL_SQL = "SELECT id,locked_until,created_at,email,failed_accesses,is_admin FROM user";
    private static final String UPDATE_SQL = "UPDATE user SET locked_until = ?, failed_accesses = ? WHERE id = ?";
    private static final String UPDATE_PRIVILEGE_SQL = "UPDATE user SET is_admin = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM user WHERE id = ?";

    ///
    @Autowired
    public UserDao(JdbcTemplate jdbcTemplate, Environment environment) {

        super(jdbcTemplate, environment);
    }

    ///
    @Transactional
    public void insert(User user) throws DataAccessException {

        super.getJdbcTemplate().update(INSERT_SQL, preparedStatement -> {

            preparedStatement.setObject(1, user.getLockedUntil(), JDBCType.BIGINT.getVendorTypeNumber());
            preparedStatement.setLong(2, user.getCreatedAt());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPassword());
            preparedStatement.setShort(5, user.getFailedAccesses());
            preparedStatement.setBoolean(6, user.isAdmin());
        });
    }

    ///..
    public boolean exists(String username) throws DataAccessException {

        Boolean exists = super.getJdbcTemplate().query(

            EXISTS_SQL,
            preparedStatement -> preparedStatement.setString(1, username),
            this::mapBoolean
        );

        return exists != null && exists.booleanValue();
    }

    ///..
    public Boolean getPrivilege(long id) throws DataAccessException {

        return super.getJdbcTemplate().query(

            SELECT_PRIVILEGE_SQL,
            preparedStatement -> preparedStatement.setLong(1, id),
            this::mapBoolean
        );
    }

    ///..
    public User selectByUsername(String username) throws DataAccessException {

        return super.getJdbcTemplate().query(

            SELECT_BY_USERNAME_SQL,
            preparedStatement -> preparedStatement.setString(1, username),
            this::mapResultSetSingle
        );
    }

    ///..
    @Transactional
    public void updateForLogin(long id, Long lockedUntil, short failedAccesses) throws DataAccessException {

        super.getJdbcTemplate().update(UPDATE_SQL, preparedStatement -> {

            if(lockedUntil == null) {

                preparedStatement.setNull(1, JDBCType.BIGINT.getVendorTypeNumber());
            }

            else {

                preparedStatement.setLong(1, lockedUntil);
            }

            preparedStatement.setShort(2, failedAccesses);
            preparedStatement.setLong(3, id);
        });
    }

    ///..
    public List<User> selectAll() throws DataAccessException {

        return super.getJdbcTemplate().query(SELECT_ALL_SQL, _ -> {}, this::mapResultSet);
    }

    ///..
    @Transactional
    public void updatePrivilege(long id, boolean privilege) throws DataAccessException {

        super.getJdbcTemplate().update(UPDATE_PRIVILEGE_SQL, preparedStatement -> {

            preparedStatement.setBoolean(1, privilege);
            preparedStatement.setLong(2, id);
        });
    }

    ///..
    @Transactional
    public void delete(long id) throws DataAccessException {

        super.getJdbcTemplate().update(DELETE_SQL, preparedStatement -> preparedStatement.setLong(1, id));
    }

    ///.
    private User mapResultSetSingle(ResultSet resultSet) throws SQLException {

        if(resultSet.next()) return this.newUser(resultSet, true);
        return null;
    }

    ///..
    private List<User> mapResultSet(ResultSet resultSet) throws SQLException {

        List<User> users = new ArrayList<>();

        while(resultSet.next()) {

            users.add(this.newUser(resultSet, false));
        }

        return users;
    }

    ///..
    private Boolean mapBoolean(ResultSet resultSet) throws SQLException {

        if(resultSet.next()) return resultSet.getBoolean(1);
        return null;
    }

    ///..
    private User newUser(ResultSet resultSet, boolean mapPassword) throws SQLException {

        return new User(

            resultSet.getLong(1),
            (Long)resultSet.getObject(2),
            resultSet.getLong(3),
            resultSet.getString(4),
            mapPassword ? resultSet.getString(5) : null,
            resultSet.getShort(6),
            resultSet.getBoolean(7)
        );
    }

    ///
}
