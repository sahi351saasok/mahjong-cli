package sahi351.mahjong.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * config/players.yaml からCPUプレイヤーの名前・思考ロジック指定を読み込む。
 */
public final class PlayersConfigLoader {

    private PlayersConfigLoader() {
    }

    @SuppressWarnings("unchecked")
    public static List<PlayerConfig> load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            if (root == null || !(root.get("players") instanceof List<?> rawPlayers)) {
                throw new IllegalStateException("players.yaml の形式が不正です: players リストがありません");
            }
            List<PlayerConfig> result = new ArrayList<>();
            for (Object entry : rawPlayers) {
                Map<String, Object> map = (Map<String, Object>) entry;
                String name = String.valueOf(map.get("name"));
                String strategy = String.valueOf(map.get("strategy"));
                result.add(new PlayerConfig(name, strategy));
            }
            if (result.size() != 4) {
                throw new IllegalStateException("players.yaml にはプレイヤーを4人分定義してください（現在: " + result.size() + "人）");
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("players.yaml の読み込みに失敗しました: " + path, e);
        }
    }
}
