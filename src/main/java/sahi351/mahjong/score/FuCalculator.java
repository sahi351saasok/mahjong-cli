package sahi351.mahjong.score;

import sahi351.mahjong.hand.Group;
import sahi351.mahjong.hand.GroupType;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.MeldType;
import sahi351.mahjong.hand.WinContext;
import sahi351.mahjong.hand.WinDecomposer.Decomposition;
import sahi351.mahjong.tile.Tile;
import sahi351.mahjong.yaku.WaitType;
import sahi351.mahjong.yaku.YakuChecker;

/**
 * 符計算。七対子は固定25符。標準形はパーツごとに加算し10符単位へ切り上げる。
 */
public final class FuCalculator {

    private FuCalculator() {
    }

    public static final int CHIITOITSU_FU = 25;

    public static int calculateStandard(WinContext ctx, Decomposition decomp) {
        Hand hand = ctx.hand();
        boolean menzen = hand.isMenzen();
        WaitType wait = YakuChecker.waitType(decomp, ctx);

        boolean pinfuShape = decomp.sets().stream().allMatch(g -> g.type() == GroupType.SEQUENCE)
                && hand.melds().isEmpty()
                && !isYakuhaiPair(decomp, ctx)
                && wait == WaitType.RYANMEN;

        int fu = 20;
        if (menzen && ctx.isRon()) {
            fu += 10;
        }
        if (ctx.isTsumo() && !(pinfuShape && hand.isMenzen())) {
            fu += 2;
        }

        for (Group g : decomp.sets()) {
            fu += tripletFu(g, ctx, false);
        }
        for (Meld m : hand.melds()) {
            if (m.type() == MeldType.CHI) {
                continue;
            }
            Group g = new Group(GroupType.TRIPLET, m.tiles());
            fu += meldFu(m, g);
        }

        Tile pairTile = decomp.pair().representative();
        if (pairTile.isDragonTile()) {
            fu += 2;
        }
        if (pairTile.isWindTile()) {
            if (pairTile.rank() == ctx.seatWind().tileRank()) {
                fu += 2;
            }
            if (pairTile.rank() == ctx.roundWind().tileRank()) {
                fu += 2;
            }
        }

        switch (wait) {
            case KANCHAN, PENCHAN, TANKI -> fu += 2;
            default -> {
            }
        }

        if (ctx.isRon() && fu == 20) {
            fu = 30;
        }

        return roundUp(fu);
    }

    private static boolean isYakuhaiPair(Decomposition decomp, WinContext ctx) {
        Tile t = decomp.pair().representative();
        if (t.isDragonTile()) {
            return true;
        }
        if (t.isWindTile()) {
            return t.rank() == ctx.seatWind().tileRank() || t.rank() == ctx.roundWind().tileRank();
        }
        return false;
    }

    private static int tripletFu(Group g, WinContext ctx, boolean fromMeld) {
        if (g.type() != GroupType.TRIPLET) {
            return 0;
        }
        boolean yaochuu = g.representative().isYaochuu();
        boolean ankou = !fromMeld && !YakuChecker.isRonCompletedTriplet(ctx, g);
        int base = ankou ? (yaochuu ? 8 : 4) : (yaochuu ? 4 : 2);
        return base;
    }

    private static int meldFu(Meld m, Group g) {
        boolean yaochuu = g.representative().isYaochuu();
        return switch (m.type()) {
            case PON -> yaochuu ? 4 : 2;
            case ANKAN -> yaochuu ? 32 : 16;
            case MINKAN, KAKAN -> yaochuu ? 16 : 8;
            default -> 0;
        };
    }

    private static int roundUp(int fu) {
        return ((fu + 9) / 10) * 10;
    }
}
