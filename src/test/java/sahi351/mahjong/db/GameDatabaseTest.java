package sahi351.mahjong.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameDatabaseTest {

    @TempDir
    Path tempDir;

    private GameDatabase newDatabase() {
        return new GameDatabase(tempDir.resolve("mahjong.db"));
    }

    @Test
    void createsDatabaseFileIfMissing() {
        Path dbFile = tempDir.resolve("nested").resolve("mahjong.db");
        try (GameDatabase database = new GameDatabase(dbFile)) {
            database.startGame();
        }
        assertTrue(Files.exists(dbFile));
    }

    @Test
    void aggregatesWinNakiRiichiRatesAndAverageScoreAcrossKyoku() {
        try (GameDatabase database = newDatabase()) {
            long gameId = database.startGame();

            database.recordKyokuResult(gameId, "東1局", 0, "Alice",
                    true, false, true, 8000, List.of("リーチ", "ツモ"));
            database.recordKyokuResult(gameId, "東2局", 0, "Alice",
                    false, true, false, null, List.of());
            database.recordKyokuResult(gameId, "東3局", 0, "Alice",
                    true, true, false, 2000, List.of("役牌"));
            database.recordKyokuResult(gameId, "東4局", 0, "Alice",
                    false, false, false, null, List.of());

            PlayerStats stats = database.statsFor("Alice");
            assertEquals(0.5, stats.winRate(), 1e-9);
            assertEquals(0.5, stats.nakiRate(), 1e-9);
            assertEquals(0.25, stats.riichiRate(), 1e-9);
            assertEquals(5000.0, stats.avgScore(), 1e-9);
        }
    }

    @Test
    void aggregatesFinalPointsAndRankAcrossGames() {
        try (GameDatabase database = newDatabase()) {
            long game1 = database.startGame();
            database.recordGameResult(game1, "Alice", 35000, 45.0, 1);
            long game2 = database.startGame();
            database.recordGameResult(game2, "Alice", 15000, -15.0, 4);

            PlayerStats stats = database.statsFor("Alice");
            assertEquals(30.0, stats.totalPoints(), 1e-9);
            assertEquals(2.5, stats.avgRank(), 1e-9);
        }
    }

    @Test
    void returnsZeroedStatsForUnknownPlayer() {
        try (GameDatabase database = newDatabase()) {
            PlayerStats stats = database.statsFor("誰か");
            assertEquals(0.0, stats.totalPoints());
            assertEquals(0.0, stats.avgRank());
            assertEquals(0.0, stats.winRate());
            assertEquals(0.0, stats.nakiRate());
            assertEquals(0.0, stats.avgScore());
            assertEquals(0.0, stats.riichiRate());
        }
    }

    @Test
    void yakuCountsAreSortedDescendingAndExcludeZeroCounts() {
        try (GameDatabase database = newDatabase()) {
            long gameId = database.startGame();

            database.recordKyokuResult(gameId, "東1局", 0, "Alice",
                    true, false, true, 8000, List.of("リーチ", "ツモ"));
            database.recordKyokuResult(gameId, "東2局", 0, "Bob",
                    true, false, true, 2000, List.of("リーチ"));
            database.recordKyokuResult(gameId, "東3局", 0, "Carol",
                    false, false, false, null, List.of());

            List<YakuCount> counts = database.yakuCounts();
            assertEquals(2, counts.size());
            assertEquals("リーチ", counts.get(0).name());
            assertEquals(2, counts.get(0).count());
            assertEquals("ツモ", counts.get(1).name());
            assertEquals(1, counts.get(1).count());
        }
    }
}
