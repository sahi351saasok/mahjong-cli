package sahi351.mahjong.ai;

import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.tile.Tile;

/**
 * CPU思考ロジックに渡す、公開情報のみで構成されたゲーム状況のスナップショット。
 */
public record AiContext(Hand ownHand, Wind ownSeatWind, Wind roundWind, int kyokuNumber, int honba,
                         int wallRemaining, List<Tile> doraIndicators,
                         List<PlayerPublicView> others, int turnNumber, int ownPoints) {

    public boolean anyOpponentRiichi() {
        return others.stream().anyMatch(PlayerPublicView::riichi);
    }

    public boolean isDealer() {
        return ownSeatWind == Wind.EAST;
    }
}
