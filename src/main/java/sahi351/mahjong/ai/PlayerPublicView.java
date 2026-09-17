package sahi351.mahjong.ai;

import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.tile.Tile;

/**
 * 他家から見える公開情報のみを保持するビュー。
 */
public record PlayerPublicView(String name, int seatIndex, Wind seatWind, int points,
                                List<Tile> discards, List<Meld> melds, boolean riichi) {
    public boolean isDealer() {
        return seatWind == Wind.EAST;
    }
}
