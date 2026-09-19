package sahi351.mahjong.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 対局データ（局ごとの結果・半荘ごとの最終結果）を SQLite データベースに記録し、
 * 通算成績・役の通算回数を集計する。
 */
public final class GameDatabase implements AutoCloseable {

    private final Connection connection;
    private final Map<String, Long> playerIdCache = new HashMap<>();

    public GameDatabase(Path dbFile) {
        try {
            Path parent = dbFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            createSchema();
        } catch (SQLException | java.io.IOException e) {
            throw new IllegalStateException("データベースの初期化に失敗しました: " + dbFile, e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS games (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        played_at TEXT NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS game_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        game_id INTEGER NOT NULL REFERENCES games(id),
                        player_id INTEGER NOT NULL REFERENCES players(id),
                        final_score INTEGER NOT NULL,
                        final_point REAL NOT NULL,
                        final_rank INTEGER NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS kyoku_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        game_id INTEGER NOT NULL REFERENCES games(id),
                        player_id INTEGER NOT NULL REFERENCES players(id),
                        kyoku_label TEXT NOT NULL,
                        honba INTEGER NOT NULL,
                        won INTEGER NOT NULL,
                        naki INTEGER NOT NULL,
                        riichi INTEGER NOT NULL,
                        score INTEGER
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS kyoku_yaku (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        kyoku_result_id INTEGER NOT NULL REFERENCES kyoku_results(id),
                        yaku_name TEXT NOT NULL
                    )
                    """);
        }
    }

    /** 新しい対局（半荘・1局のみ）を開始し、対局IDを発行する。 */
    public long startGame() {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO games (played_at) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.executeUpdate();
            return generatedId(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("対局の記録開始に失敗しました", e);
        }
    }

    /** 局の終了時に、1プレイヤーの和了の有無・副露の有無・リーチの有無・和了時の打点・和了役を記録する。 */
    public void recordKyokuResult(long gameId, String kyokuLabel, int honba, String playerName,
                                   boolean won, boolean naki, boolean riichi, Integer score,
                                   List<String> yakuNames) {
        try {
            long playerId = playerId(playerName);
            long kyokuResultId;
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO kyoku_results
                        (game_id, player_id, kyoku_label, honba, won, naki, riichi, score)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, gameId);
                ps.setLong(2, playerId);
                ps.setString(3, kyokuLabel);
                ps.setInt(4, honba);
                ps.setInt(5, won ? 1 : 0);
                ps.setInt(6, naki ? 1 : 0);
                ps.setInt(7, riichi ? 1 : 0);
                if (score != null) {
                    ps.setInt(8, score);
                } else {
                    ps.setNull(8, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
                kyokuResultId = generatedId(ps);
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO kyoku_yaku (kyoku_result_id, yaku_name) VALUES (?, ?)")) {
                for (String yakuName : yakuNames) {
                    ps.setLong(1, kyokuResultId);
                    ps.setString(2, yakuName);
                    ps.addBatch();
                }
                if (!yakuNames.isEmpty()) {
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("局結果の記録に失敗しました", e);
        }
    }

    /** 半荘の終了時に、1プレイヤーの最終点数・最終ポイント・最終順位を記録する。 */
    public void recordGameResult(long gameId, String playerName, int finalScore, double finalPoint,
                                  int finalRank) {
        try {
            long playerId = playerId(playerName);
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO game_results (game_id, player_id, final_score, final_point, final_rank)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                ps.setLong(1, gameId);
                ps.setLong(2, playerId);
                ps.setInt(3, finalScore);
                ps.setDouble(4, finalPoint);
                ps.setInt(5, finalRank);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("半荘結果の記録に失敗しました", e);
        }
    }

    /** プレイヤーの通算ポイント数・平均順位・和了率・副露率・平均打点・リーチ率を集計する。 */
    public PlayerStats statsFor(String playerName) {
        try {
            long playerId = playerId(playerName);
            double totalPoints = 0;
            double avgRank = 0;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COALESCE(SUM(final_point), 0), COALESCE(AVG(final_rank), 0) "
                            + "FROM game_results WHERE player_id = ?")) {
                ps.setLong(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    totalPoints = rs.getDouble(1);
                    avgRank = rs.getDouble(2);
                }
            }
            int kyokuCount = 0;
            int wonCount = 0;
            int nakiCount = 0;
            int riichiCount = 0;
            double avgScore = 0;
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT COUNT(*),
                           COALESCE(SUM(won), 0),
                           COALESCE(SUM(naki), 0),
                           COALESCE(SUM(riichi), 0),
                           COALESCE(AVG(CASE WHEN won = 1 THEN score END), 0)
                    FROM kyoku_results WHERE player_id = ?
                    """)) {
                ps.setLong(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    kyokuCount = rs.getInt(1);
                    wonCount = rs.getInt(2);
                    nakiCount = rs.getInt(3);
                    riichiCount = rs.getInt(4);
                    avgScore = rs.getDouble(5);
                }
            }
            double winRate = kyokuCount == 0 ? 0 : (double) wonCount / kyokuCount;
            double nakiRate = kyokuCount == 0 ? 0 : (double) nakiCount / kyokuCount;
            double riichiRate = kyokuCount == 0 ? 0 : (double) riichiCount / kyokuCount;
            return new PlayerStats(totalPoints, avgRank, winRate, nakiRate, avgScore, riichiRate);
        } catch (SQLException e) {
            throw new IllegalStateException("通算成績の集計に失敗しました", e);
        }
    }

    /** 出現した役とその通算回数を、回数の多い順に集計する（0回の役は含まれない）。 */
    public List<YakuCount> yakuCounts() {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT yaku_name, COUNT(*) AS cnt FROM kyoku_yaku
                GROUP BY yaku_name ORDER BY cnt DESC
                """);
             ResultSet rs = ps.executeQuery()) {
            List<YakuCount> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new YakuCount(rs.getString(1), rs.getInt(2)));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("役の通算回数の集計に失敗しました", e);
        }
    }

    private long playerId(String name) throws SQLException {
        Long cached = playerIdCache.get(name);
        if (cached != null) {
            return cached;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM players WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    playerIdCache.put(name, id);
                    return id;
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO players (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            long id = generatedId(ps);
            playerIdCache.put(name, id);
            return id;
        }
    }

    private long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("データベース接続のクローズに失敗しました", e);
        }
    }
}
