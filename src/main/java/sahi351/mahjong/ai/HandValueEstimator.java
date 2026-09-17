package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.TileIndex;
import sahi351.mahjong.hand.WinContext;
import sahi351.mahjong.score.HandScorer;
import sahi351.mahjong.score.ScoreResult;
import sahi351.mahjong.tile.Tile;

/**
 * CPU思考ロジックが「満貫以上が見込めるか」を判断するための簡易見積もりユーティリティ。
 * 実際のツモ・裏ドラ等は考慮せず、現時点で見えているドラ表示牌のみを用いる。
 */
public final class HandValueEstimator {

    private HandValueEstimator() {
    }

    public static List<Tile> findWaits(List<Tile> concealed13, int meldCount) {
        List<Tile> waits = new ArrayList<>();
        for (int idx = 0; idx < TileIndex.SIZE; idx++) {
            Tile candidate = TileIndex.toTile(idx);
            List<Tile> trial = new ArrayList<>(concealed13);
            trial.add(candidate);
            if (ShantenCalculator.shanten(trial, meldCount) == -1) {
                waits.add(candidate);
            }
        }
        return waits;
    }

    /** 既にテンパイしている手牌（面前かどうかは問わない）の中で、最も高い基本点を見積もる。 */
    public static Integer bestScoreForTenpai(List<Tile> concealed13, List<Meld> melds,
                                              Wind roundWind, Wind seatWind, List<Tile> doraIndicators,
                                              boolean assumeRiichi) {
        List<Tile> waits = findWaits(concealed13, melds.size());
        int best = -1;
        for (Tile wait : waits) {
            Hand hand = new Hand();
            for (Tile t : concealed13) {
                hand.addTile(t);
            }
            hand.addTile(wait);
            for (Meld m : melds) {
                hand.addMeld(m);
            }
            WinContext ctx = new WinContext(hand, wait, false, roundWind, seatWind,
                    assumeRiichi, false, false, false, false, false, false, false, false,
                    doraIndicators, List.of());
            ScoreResult result = HandScorer.score(ctx, 0);
            if (result != null) {
                best = Math.max(best, result.basePoints());
            }
        }
        return best < 0 ? null : best;
    }

    /** まだ1枚多い状態（打牌前）の手牌から、最善の打牌を選んだ場合の最高基本点を見積もる。 */
    public static Integer bestReachableAfterOneDiscard(List<Tile> concealedWithExtra, List<Meld> melds,
                                                         Wind roundWind, Wind seatWind,
                                                         List<Tile> doraIndicators, boolean assumeRiichi) {
        Integer overallBest = null;
        List<Tile> distinctCandidates = new ArrayList<>();
        for (Tile t : concealedWithExtra) {
            boolean seen = false;
            for (Tile d : distinctCandidates) {
                if (d.isSameKind(t)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                distinctCandidates.add(t);
            }
        }
        for (Tile discard : distinctCandidates) {
            List<Tile> sub = new ArrayList<>(concealedWithExtra);
            sub.remove(discard);
            if (ShantenCalculator.shanten(sub, melds.size()) != 0) {
                continue;
            }
            Integer score = bestScoreForTenpai(sub, melds, roundWind, seatWind, doraIndicators, assumeRiichi);
            if (score != null && (overallBest == null || score > overallBest)) {
                overallBest = score;
            }
        }
        return overallBest;
    }

    public static final int MANGAN_BASE_POINTS = 2000;
}
