package differencing.repositories;

import differencing.models.Time;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TimeRepository extends Repository {
    private static final String INSERT_OR_UPDATE = "" +
        "INSERT INTO runtime(" +
        "benchmark, " +
        "tool, " +
        "topic, " +
        "task, " +
        "runtime, " +
        "step, " +
        "is_missing" +
        ") " +
        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
        "ON CONFLICT DO UPDATE SET " +
        "runtime = excluded.runtime, " +
        "step = excluded.step, " +
        "is_missing = excluded.is_missing";

    public static void insertOrUpdate(Iterable<Time> times) {
        for (Time time : times) {
            insertOrUpdate(time);
        }
    }

    public static void insertOrUpdate(Time time) {
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(INSERT_OR_UPDATE)) {
            ps.setObject(1, time.benchmark);
            ps.setObject(2, time.tool);
            ps.setObject(3, time.topic);
            ps.setObject(4, time.task);
            ps.setObject(5, time.runtime);
            ps.setObject(6, time.step);
            ps.setObject(7, time.isMissing);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
