package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.TileIndex;
import sahi351.mahjong.tile.Tile;

/**
 * 牌効率評価。候補となる打牌それぞれについて、打牌後のシャンテン数と受け入れ枚数を求める。
 */
public final class TileEfficiencyEvaluator {

    private TileEfficiencyEvaluator() {
    }

    public record Candidate(Tile discard, int shanten, int ukeire) {
    }

    public static List<Candidate> evaluate(List<Tile> concealed14, int meldCount, List<Tile> visibleTiles) {
        int[] visibleCounts = TileIndex.toCounts(visibleTiles);
        List<Candidate> result = new ArrayList<>();
        List<Tile> distinct = new ArrayList<>();
        for (Tile t : concealed14) {
            boolean seen = distinct.stream().anyMatch(d -> d.isSameKind(t));
            if (!seen) {
                distinct.add(t);
            }
        }
        for (Tile discard : distinct) {
            List<Tile> sub = new ArrayList<>(concealed14);
            sub.remove(discard);
            int shanten = ShantenCalculator.shanten(sub, meldCount);
            int ukeire = 0;
            if (shanten >= 0) {
                for (int idx = 0; idx < TileIndex.SIZE; idx++) {
                    int remaining = 4 - visibleCounts[idx];
                    if (remaining <= 0) {
                        continue;
                    }
                    Tile candidate = TileIndex.toTile(idx);
                    List<Tile> trial = new ArrayList<>(sub);
                    trial.add(candidate);
                    if (ShantenCalculator.shanten(trial, meldCount) < shanten) {
                        ukeire += remaining;
                    }
                }
            }
            result.add(new Candidate(discard, shanten, ukeire));
        }
        return result;
    }

    public static int keepValue(Tile t, List<Tile> doraTileKinds, sahi351.mahjong.game.Wind seatWind,
                                 sahi351.mahjong.game.Wind roundWind) {
        int value = 0;
        if (doraTileKinds.stream().anyMatch(d -> d.isSameKind(t)) || t.isRedFive()) {
            value += 3;
        }
        if (t.isDragonTile()) {
            value += 2;
        } else if (t.isWindTile() && (t.rank() == seatWind.tileRank() || t.rank() == roundWind.tileRank())) {
            value += 2;
        } else if (!t.isYaochuu()) {
            value += 1;
        }
        return value;
    }
}
