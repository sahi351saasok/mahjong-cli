package sahi351.mahjong.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sahi351.mahjong.ai.StandardStrategy;
import sahi351.mahjong.log.GameLogger;
import sahi351.mahjong.player.Player;

class GameEngineIntegrationTest {

    @TempDir
    Path tempDir;

    private List<Player> newPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(new Player("CPU" + (i + 1), new StandardStrategy(), i));
        }
        return players;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void singleKyokuGameConservesTotalPoints(int seed) {
        List<Player> players = newPlayers();
        try (GameLogger logger = new GameLogger(tempDir)) {
            GameEngine engine = new GameEngine(players, GameMode.SINGLE_KYOKU, new Random(seed), logger);
            engine.run();
        }
        int total = players.stream().mapToInt(Player::points).sum();
        assertEquals(100000, total);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20, 30})
    void hanchanGameConservesTotalPoints(int seed) {
        List<Player> players = newPlayers();
        try (GameLogger logger = new GameLogger(tempDir)) {
            GameEngine engine = new GameEngine(players, GameMode.HANCHAN, new Random(seed), logger);
            engine.run();
        }
        int total = players.stream().mapToInt(Player::points).sum();
        assertEquals(100000, total);
    }
}
