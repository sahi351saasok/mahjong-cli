package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書（積極的戦略）に基づく思考ロジック。
 * 打牌: 牌効率最優先。テンパイを目指し、常に最も効率の悪い牌を捨てる（守備は考慮しない）。
 * リーチ: テンパイしたら即リーチする。
 * 鳴き: 1翻以上の役が確定しており、かつシャンテン数を下げられる場合に鳴く。
 * 暗槓: 門前でイーシャンテンもしくはテンパイしている場合に行う。
 * 加槓: しない。
 */
public final class AggressiveStrategy implements PlayerStrategy {

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();
        List<Tile> visible = AiSupport.collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = AiSupport.doraTileKinds(ctx);

        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);

        Comparator<TileEfficiencyEvaluator.Candidate> comparator = Comparator
                .comparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire())
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));

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
        return ShantenCalculator.shanten(sub, ctx.ownHand().melds().size()) == 0;
    }

    @Override
    public CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions) {
        int currentShanten = ctx.ownHand().shanten();
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
            List<Meld> meldsAfter = new ArrayList<>(ctx.ownHand().melds());
            meldsAfter.add(AiSupport.buildHypotheticalMeld(option, discardedTile));
            Integer potential = HandValueEstimator.bestReachableAfterOneDiscard(
                    resultingConcealed, meldsAfter, ctx.roundWind(), ctx.ownSeatWind(),
                    ctx.doraIndicators(), false);
            if (potential == null) {
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
        Hand hand = ctx.ownHand();
        if (!hand.isMenzen()) {
            return false;
        }
        int shanten = hand.shanten();
        if (shanten != 0 && shanten != 1) {
            return false;
        }
        return AiSupport.resultingShantenAfterAnkan(hand, kanTile) <= shanten;
    }

    @Override
    public boolean wantsKakan(AiContext ctx, Tile kanTile) {
        return false;
    }
}
