package io.github.clamentos.cachecruncher.persistence.daos;

///
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
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
    private static final String INSERT_SQL = "INSERT INTO user (locked_until,created_at,validated_at,email,password,failed_accesses,is_admin) values (?,?,?,?,?,?,?)";

    private static final String EXISTS_SQL = "SELECT count(*) > 0 FROM user WHERE email = ?";
    private static final String SELECT_PRIVILEGE_SQL = "SELECT is_admin FROM user WHERE id = ?";

    private static final String SELECT_BY_EMAIL_SQL = "SELECT id,locked_until,created_at,validated_at,email,password,failed_accesses,is_admin FROM user WHERE email=?";

    private static final String SELECT_ALL_SQL = "SELECT id,locked_until,created_at,validated_at,email,failed_accesses,is_admin FROM user";

    private static final String UPDATE_SQL = "UPDATE user SET locked_until=?, failed_accesses=? WHERE id=?";
    private static final String UPDATE_EMAIL_VALID_SQL = "UPDATE user SET validated_at=? WHERE email=?";
    private static final String UPDATE_PRIVILEGE_SQL = "UPDATE user SET is_admin=? WHERE id=?";

    ///
    @Autowired
    public UserDao(final JdbcTemplate jdbcTemplate, final Environment environment) {

        super(jdbcTemplate, environment);
    }

    ///
    @Transactional(rollbackFor = DatabaseException.class)
    public void insert(final User user) throws DatabaseException {

        final int bigintTypeNumber = JDBCType.BIGINT.getVendorTypeNumber();

        super.wrap(() ->

            super.getJdbcTemplate().update(INSERT_SQL, preparedStatement -> {

                preparedStatement.setObject(1, user.getLockedUntil(), bigintTypeNumber);
                preparedStatement.setLong(2, user.getCreatedAt());
                preparedStatement.setObject(3, user.getValidatedAt(), bigintTypeNumber);
                preparedStatement.setString(4, user.getEmail());
                preparedStatement.setString(5, user.getPassword());
                preparedStatement.setShort(6, user.getFailedAccesses());
                preparedStatement.setBoolean(7, user.isAdmin());
            })
        );
    }

    ///..
    public boolean exists(final String username) throws DatabaseException {

        final Boolean exists = super.wrap(() ->

            super.getJdbcTemplate().query(

                EXISTS_SQL,
                preparedStatement -> preparedStatement.setString(1, username),
                this::mapBoolean
            )
        );

        return exists != null && exists.booleanValue();
    }

    ///..
    public Boolean getPrivilege(final long id) throws DatabaseException {

        return super.wrap(() ->

            super.getJdbcTemplate().query(

                SELECT_PRIVILEGE_SQL,
                preparedStatement -> preparedStatement.setLong(1, id),
                this::mapBoolean
            )
        );
    }

    ///..
    public User selectByEmail(final String username) throws DatabaseException {

        return super.wrap(() ->

            super.getJdbcTemplate().query(

                SELECT_BY_EMAIL_SQL,
                preparedStatement -> preparedStatement.setString(1, username),
                this::mapResultSetSingle
            )
        );
    }

    ///..
    public List<User> selectAll() throws DatabaseException {

        return super.wrap(() -> super.getJdbcTemplate().query(SELECT_ALL_SQL, _ -> {}, this::mapResultSet));
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public void updateForLogin(final long id, final Long lockedUntil, final short failedAccesses) throws DatabaseException {

        super.wrap(() ->

            super.getJdbcTemplate().update(UPDATE_SQL, preparedStatement -> {

                if(lockedUntil == null) preparedStatement.setNull(1, JDBCType.BIGINT.getVendorTypeNumber());
                else preparedStatement.setLong(1, lockedUntil);

                preparedStatement.setShort(2, failedAccesses);
                preparedStatement.setLong(3, id);
            })
        );
    }

    ///..
    public boolean updateForEmailValidation(final String email, final long validatedAt) throws DatabaseException {

        final int rowsAffected = super.wrap(() ->

            super.getJdbcTemplate().update(UPDATE_EMAIL_VALID_SQL, preparedStatement -> {

                preparedStatement.setLong(1, validatedAt);
                preparedStatement.setString(2, email);
            })
        );

        return rowsAffected > 0;
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public boolean updatePrivilege(final long id, final boolean privilege) throws DatabaseException {

        final int rowsAffected = super.wrap(() ->

            super.getJdbcTemplate().update(UPDATE_PRIVILEGE_SQL, preparedStatement -> {

                preparedStatement.setBoolean(1, privilege);
                preparedStatement.setLong(2, id);
            })
        );

        return rowsAffected > 0;
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public int delete(final long id) throws DatabaseException {

        return super.deleteWhereIdEquals("user", id);
    }

    ///.
    private User mapResultSetSingle(ResultSet resultSet) throws SQLException {

        if(resultSet.next()) return this.newUser(resultSet, true);
        return null;
    }

    ///..
    private List<User> mapResultSet(final ResultSet resultSet) throws SQLException {

        final List<User> users = new ArrayList<>();
        while(resultSet.next()) users.add(this.newUser(resultSet, false));

        return users;
    }

    ///..
    private Boolean mapBoolean(final ResultSet resultSet) throws SQLException {

        if(resultSet.next()) return resultSet.getBoolean(1);
        return null;
    }

    ///..
    private User newUser(final ResultSet resultSet, final boolean mapPassword) throws SQLException {

        return new User(

            resultSet.getLong(1),
            (Long)resultSet.getObject(2),
            resultSet.getLong(3),
            (Long)resultSet.getObject(4),
            resultSet.getString(5),
            mapPassword ? resultSet.getString(6) : null,
            resultSet.getShort(mapPassword ? 7 : 6),
            resultSet.getBoolean(mapPassword ? 8 : 7)
        );
    }

    ///
}
