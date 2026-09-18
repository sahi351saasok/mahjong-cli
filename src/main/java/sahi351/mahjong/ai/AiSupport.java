package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.tile.Tile;

/**
 * 各戦略クラス（{@link PlayerStrategy} 実装）が共通して利用する補助ロジック。
 */
final class AiSupport {

    private AiSupport() {
    }

    static List<Tile> collectVisibleTiles(AiContext ctx) {
        List<Tile> visible = new ArrayList<>(ctx.ownHand().allTiles());
        visible.addAll(ctx.doraIndicators());
        for (PlayerPublicView p : ctx.others()) {
            visible.addAll(p.discards());
            for (Meld m : p.melds()) {
                visible.addAll(m.tiles());
            }
        }
        return visible;
    }

    static List<Tile> doraTileKinds(AiContext ctx) {
        List<Tile> result = new ArrayList<>();
        for (Tile indicator : ctx.doraIndicators()) {
            result.add(indicator.nextForDora());
        }
        return result;
    }

    /** リーチしている他家に対する現物・字牌考慮の安全度スコア（大きいほど安全）。 */
    static int riichiSafetyScore(Tile tile, AiContext ctx) {
        int score = 0;
        for (PlayerPublicView p : ctx.others()) {
            if (!p.riichi()) {
                continue;
            }
            boolean genbutsu = p.discards().stream().anyMatch(d -> d.isSameKind(tile));
            if (genbutsu) {
                score += 100;
            } else if (tile.isHonor()) {
                score += 1;
            }
        }
        return score;
    }

    /** いずれかの他家の捨て牌に既に存在する牌（放銃リスクが低いと見なせる目安）。 */
    static boolean isLikelySafe(Tile tile, AiContext ctx) {
        for (PlayerPublicView p : ctx.others()) {
            if (p.discards().stream().anyMatch(d -> d.isSameKind(tile))) {
                return true;
            }
        }
        return false;
    }

    static Meld buildHypotheticalMeld(CallOption option, Tile calledTile) {
        List<Tile> tiles = new ArrayList<>(option.tilesToUse());
        tiles.add(calledTile);
        return switch (option.type()) {
            case CHI -> Meld.chi(tiles, calledTile, -1);
            case PON -> Meld.pon(tiles, calledTile, -1);
            case KAN -> Meld.minkan(tiles, calledTile, -1);
            case PASS -> throw new IllegalArgumentException("PASSはMeldにできない");
        };
    }

    /** 自身の現在の順位（1〜4）を、同点の場合は起家に近い側を上位として求める。 */
    static int ownRank(AiContext ctx) {
        int rank = 1;
        for (PlayerPublicView p : ctx.others()) {
            if (isAhead(p.points(), p.seatWind(), ctx.ownPoints(), ctx.ownSeatWind())) {
                rank++;
            }
        }
        return rank;
    }

    private static boolean isAhead(int otherPoints, Wind otherWind, int ownPoints, Wind ownWind) {
        if (otherPoints != ownPoints) {
            return otherPoints > ownPoints;
        }
        return otherWind.ordinal() < ownWind.ordinal();
    }

    /** 自身が1位である前提で、2位との点差を求める。 */
    static int pointGapToSecondPlace(AiContext ctx) {
        int secondPlacePoints = ctx.others().stream()
                .mapToInt(PlayerPublicView::points)
                .max()
                .orElse(Integer.MIN_VALUE);
        return ctx.ownPoints() - secondPlacePoints;
    }

    /** 指定した牌で暗槓した場合のシャンテン数を求める。 */
    static int resultingShantenAfterAnkan(Hand hand, Tile kanTile) {
        List<Tile> sub = new ArrayList<>(hand.concealedTiles());
        int removed = 0;
        for (int i = sub.size() - 1; i >= 0 && removed < 4; i--) {
            if (sub.get(i).isSameKind(kanTile)) {
                sub.remove(i);
                removed++;
            }
        }
        return ShantenCalculator.shanten(sub, hand.melds().size() + 1);
    }
}
