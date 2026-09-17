package sahi351.mahjong.score;

import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.TileIndex;
import sahi351.mahjong.hand.WinContext;
import sahi351.mahjong.hand.WinDecomposer;
import sahi351.mahjong.hand.WinDecomposer.Decomposition;
import sahi351.mahjong.tile.Tile;
import sahi351.mahjong.yaku.YakuChecker;
import sahi351.mahjong.yaku.YakuResult;

/**
 * 役判定・符計算・点数計算をまとめて実行する。
 */
public final class HandScorer {

    private HandScorer() {
    }

    /** 役がない場合は空を返す（形式テンパイのみで和了不可）。 */
    public static ScoreResult score(WinContext ctx, int honba) {
        Hand hand = ctx.hand();
        int meldCount = hand.melds().size();
        List<Tile> concealed = hand.concealedTiles();

        boolean standardComplete = ShantenCalculator.standardShanten(TileIndex.toCounts(concealed), meldCount) == -1;
        boolean chiitoiComplete = meldCount == 0 && ShantenCalculator.chiitoiShanten(TileIndex.toCounts(concealed)) == -1;
        boolean kokushiComplete = meldCount == 0 && ShantenCalculator.kokushiShanten(TileIndex.toCounts(concealed)) == -1;

        Candidate best = null;

        if (kokushiComplete) {
            List<YakuResult> yaku = YakuChecker.checkKokushi(ctx);
            best = maxCandidate(best, new Candidate(yaku, 0));
        }

        if (chiitoiComplete) {
            List<YakuResult> yakuman = YakuChecker.checkYakumanChiitoi(ctx);
            if (!yakuman.isEmpty()) {
                best = maxCandidate(best, new Candidate(yakuman, FuCalculator.CHIITOITSU_FU));
            } else {
                List<YakuResult> normal = YakuChecker.checkNormalChiitoi(ctx);
                normal = withDora(ctx, normal, true);
                best = maxCandidate(best, new Candidate(normal, FuCalculator.CHIITOITSU_FU));
            }
        }

        if (standardComplete) {
            List<Decomposition> decompositions = WinDecomposer.decompose(concealed, 4 - meldCount);
            for (Decomposition decomp : decompositions) {
                List<YakuResult> yakuman = YakuChecker.checkYakumanStandard(ctx, decomp);
                if (!yakuman.isEmpty()) {
                    best = maxCandidate(best, new Candidate(yakuman, FuCalculator.calculateStandard(ctx, decomp)));
                } else {
                    List<YakuResult> normal = YakuChecker.checkNormalStandard(ctx, decomp);
                    int fu = FuCalculator.calculateStandard(ctx, decomp);
                    boolean hasDoraOnlyYaku = !normal.isEmpty();
                    normal = withDora(ctx, normal, hasDoraOnlyYaku);
                    best = maxCandidate(best, new Candidate(normal, fu));
                }
            }
        }

        if (best == null || !hasRealYaku(best.yaku)) {
            return null;
        }

        return buildResult(ctx, best, honba);
    }

    private static boolean hasRealYaku(List<YakuResult> yaku) {
        for (YakuResult y : yaku) {
            if (!y.name().startsWith("ドラ") && !y.name().startsWith("赤ドラ") && !y.name().startsWith("裏ドラ")) {
                return true;
            }
        }
        return false;
    }

    private static List<YakuResult> withDora(WinContext ctx, List<YakuResult> base, boolean addDora) {
        List<YakuResult> result = new ArrayList<>(base);
        if (!addDora) {
            return result;
        }
        int dora = countDora(ctx.hand().allTiles(), ctx.doraIndicators());
        if (dora > 0) {
            result.add(YakuResult.normal("ドラ", dora));
        }
        int aka = (int) ctx.hand().allTiles().stream().filter(Tile::isRedFive).count();
        if (aka > 0) {
            result.add(YakuResult.normal("赤ドラ", aka));
        }
        if (ctx.isRiichi() || ctx.isDoubleRiichi()) {
            int ura = countDora(ctx.hand().allTiles(), ctx.uraDoraIndicators());
            if (ura > 0) {
                result.add(YakuResult.normal("裏ドラ", ura));
            }
        }
        return result;
    }

    private static int countDora(List<Tile> tiles, List<Tile> indicators) {
        int count = 0;
        for (Tile indicator : indicators) {
            Tile doraTile = indicator.nextForDora();
            for (Tile t : tiles) {
                if (t.isSameKind(doraTile)) {
                    count++;
                }
            }
        }
        return count;
    }

    private record Candidate(List<YakuResult> yaku, int fu) {
        int totalHan() {
            return yaku.stream().mapToInt(YakuResult::han).sum();
        }
    }

    private static Candidate maxCandidate(Candidate a, Candidate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        ScoreTable.BasePoints aPoints = ScoreTable.basePoints(a.totalHan(), a.fu());
        ScoreTable.BasePoints bPoints = ScoreTable.basePoints(b.totalHan(), b.fu());
        return aPoints.value() >= bPoints.value() ? a : b;
    }

    private static ScoreResult buildResult(WinContext ctx, Candidate candidate, int honba) {
        int han = candidate.totalHan();
        int fu = candidate.fu();
        ScoreTable.BasePoints bp = ScoreTable.basePoints(han, fu);
        boolean dealer = ctx.isDealer();
        boolean tsumo = ctx.isTsumo();

        int ronPayment = 0;
        int tsumoDealerPayment = 0;
        int tsumoNonDealerPayment = 0;

        if (tsumo) {
            if (dealer) {
                tsumoNonDealerPayment = ScoreTable.roundUpTo100(bp.value() * 2);
            } else {
                tsumoDealerPayment = ScoreTable.roundUpTo100(bp.value() * 2);
                tsumoNonDealerPayment = ScoreTable.roundUpTo100(bp.value());
            }
        } else {
            ronPayment = ScoreTable.roundUpTo100(bp.value() * (dealer ? 6 : 4));
        }

        return new ScoreResult(candidate.yaku(), han, fu, bp.tier(), bp.value(), dealer, tsumo,
                ronPayment, tsumoDealerPayment, tsumoNonDealerPayment, honba);
    }
}
