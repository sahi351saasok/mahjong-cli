package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.TileIndex;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書（守備的戦略）に基づく思考ロジック。
 * 打牌: 牌効率最優先。他家がリーチしている場合は安全牌を切って降りる。
 * リーチ: 自身から見える情報に基づき、待ち牌が4枚以上見込める場合のみリーチする。
 * 鳴き: イーシャンテンの場合のみ、シャンテンを進める鳴きを行う。
 * 暗槓・加槓: しない。
 */
public final class DefensiveStrategy implements PlayerStrategy {

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();
        List<Tile> visible = AiSupport.collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = AiSupport.doraTileKinds(ctx);

        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);

        boolean defending = ctx.anyOpponentRiichi();
        Comparator<TileEfficiencyEvaluator.Candidate> comparator;
        if (defending) {
            comparator = Comparator
                    .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -AiSupport.riichiSafetyScore(c.discard(), ctx))
                    .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                    .thenComparingInt(c -> -c.ukeire())
                    .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                            ctx.ownSeatWind(), ctx.roundWind()));
        } else {
            comparator = Comparator
                    .comparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                    .thenComparingInt(c -> -c.ukeire())
                    .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                            ctx.ownSeatWind(), ctx.roundWind()));
        }

        return candidates.stream().min(comparator)
                .map(TileEfficiencyEvaluator.Candidate::discard)
                .orElse(concealed.get(concealed.size() - 1));
    }

    @Override
    public boolean wantsRiichi(AiContext ctx, Tile plannedDiscard) {
        if (!ctx.ownHand().isMenzen()) {
            return false;
        }
        List<Tile> sub = new ArrayList<>(ctx.ownHand().concealedTiles());
        sub.remove(plannedDiscard);
        int meldCount = ctx.ownHand().melds().size();
        if (ShantenCalculator.shanten(sub, meldCount) != 0) {
            return false;
        }
        List<Tile> waits = HandValueEstimator.findWaits(sub, meldCount);
        int[] visibleCounts = TileIndex.toCounts(AiSupport.collectVisibleTiles(ctx));
        int totalWaitTiles = 0;
        for (Tile wait : waits) {
            totalWaitTiles += 4 - visibleCounts[TileIndex.of(wait)];
        }
        return totalWaitTiles >= 4;
    }

    @Override
    public CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions) {
        int currentShanten = ctx.ownHand().shanten();
        if (currentShanten != 1) {
            return CallOption.PASS;
        }
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();

        CallOption best = null;
        int bestShanten = Integer.MAX_VALUE;
        for (CallOption option : legalOptions) {
            if (option.type() == CallType.PASS) {
                continue;
            }
            List<Tile> resultingConcealed = new ArrayList<>(concealed);
            for (Tile used : option.tilesToUse()) {
                resultingConcealed.remove(used);
            }
            int resultingShanten = ShantenCalculator.shanten(resultingConcealed, meldCount + 1);
            if (resultingShanten >= currentShanten) {
                continue;
            }
            if (resultingShanten < bestShanten) {
                bestShanten = resultingShanten;
                best = option;
            }
        }
        return best == null ? CallOption.PASS : best;
    }

    @Override
    public boolean wantsAnkan(AiContext ctx, Tile kanTile) {
        return false;
    }

    @Override
    public boolean wantsKakan(AiContext ctx, Tile kanTile) {
        return false;
    }
}
