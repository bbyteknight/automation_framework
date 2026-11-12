package db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a single {@link ResultSet} row (already positioned via {@code next()}) to a POJO.
 * Implementations must not advance the cursor themselves — {@link DbClient} drives iteration.
 */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
