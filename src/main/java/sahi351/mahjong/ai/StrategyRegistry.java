package sahi351.mahjong.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 思考ロジックの識別子とその実装を対応付けるレジストリ。
 * 将来的な戦略追加時はここに登録するだけでよい。
 */
public final class StrategyRegistry {

    private final Map<String, Supplier<PlayerStrategy>> registry = new HashMap<>();

    public StrategyRegistry() {
        register("standard", StandardStrategy::new);
        register("aggressive", AggressiveStrategy::new);
        register("balance", BalanceStrategy::new);
        register("defensive", DefensiveStrategy::new);
        register("hi-score", HiScoreStrategy::new);
        register("top-prize", TopPrizeStrategy::new);
        register("flexible", FlexibleStrategy::new);
    }

    public void register(String id, Supplier<PlayerStrategy> factory) {
        registry.put(id, factory);
    }

    public PlayerStrategy create(String id) {
        Supplier<PlayerStrategy> factory = registry.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("未知の思考ロジックID: " + id);
        }
        return factory.get();
    }
}
