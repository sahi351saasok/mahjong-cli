package sahi351.mahjong;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import sahi351.mahjong.ai.PlayerStrategy;
import sahi351.mahjong.ai.StrategyRegistry;
import sahi351.mahjong.config.PlayerConfig;
import sahi351.mahjong.config.PlayersConfigLoader;
import sahi351.mahjong.game.GameEngine;
import sahi351.mahjong.game.GameMode;
import sahi351.mahjong.log.GameLogger;
import sahi351.mahjong.player.Player;

public final class App {

    public static void main(String[] args) {
        String modeArg = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--mode") && i + 1 < args.length) {
                modeArg = args[i + 1];
            }
        }
        GameMode mode = GameMode.fromArg(modeArg);

        List<PlayerConfig> configs = PlayersConfigLoader.load(Path.of("config", "players.yaml"));
        StrategyRegistry registry = new StrategyRegistry();

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            PlayerConfig cfg = configs.get(i);
            PlayerStrategy strategy = registry.create(cfg.strategy());
            players.add(new Player(cfg.name(), strategy, i));
        }

        try (GameLogger logger = new GameLogger(Path.of("logs"))) {
            GameEngine engine = new GameEngine(players, mode, new Random(), logger);
            engine.run();
        }
    }
}
