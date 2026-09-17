package sahi351.mahjong.ai;

import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * 他家の打牌に対して鳴ける選択肢。tilesToUse は自分の手牌から使う牌。
 */
public record CallOption(CallType type, List<Tile> tilesToUse) {
    public static final CallOption PASS = new CallOption(CallType.PASS, List.of());
}
