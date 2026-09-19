package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書（トップ狙い戦略）に基づく思考ロジック。
 * 打牌: 1位かつ2位と10000点以上差がある場合は振り込み回避を最優先。
 *       4位かつ南4局の場合は役満（国士無双）を目指す。それ以外（2位以下）は牌効率を最優先する。
 * リーチ: 親かつ他家未リーチなら必ずリーチ。それ以外は3翻以上確定時のみ。
 * 鳴き: 満貫以上の役が確定している場合のみ。
 * 暗槓: 門前でテンパイしている場合のみ。
 * 加槓: しない。
 */
public final class TopPrizeStrategy implements PlayerStrategy {

    private static final int TOP_PLACE_LEAD_THRESHOLD = 10000;

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();
        List<Tile> visible = AiSupport.collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = AiSupport.doraTileKinds(ctx);

        if (isProtectingTopPlace(ctx)) {
            List<TileEfficiencyEvaluator.Candidate> candidates =
                    TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);
            Comparator<TileEfficiencyEvaluator.Candidate> defend = Comparator
                    .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -AiSupport.riichiSafetyScore(c.discard(), ctx))
                    .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                    .thenComparingInt(c -> -c.ukeire())
                    .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                            ctx.ownSeatWind(), ctx.roundWind()));
            return candidates.stream().min(defend)
                    .map(TileEfficiencyEvaluator.Candidate::discard)
                    .orElse(concealed.get(concealed.size() - 1));
        }

        if (meldCount == 0 && isAimingForYakuman(ctx)) {
            return AiSupport.chooseKokushiDiscard(concealed);
        }

        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);
        Comparator<TileEfficiencyEvaluator.Candidate> efficiency = Comparator
                .comparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire())
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));
        return candidates.stream().min(efficiency)
                .map(TileEfficiencyEvaluator.Candidate::discard)
                .orElse(concealed.get(concealed.size() - 1));
    }

    private boolean isProtectingTopPlace(AiContext ctx) {
        return AiSupport.ownRank(ctx) == 1 && AiSupport.pointGapToSecondPlace(ctx) >= TOP_PLACE_LEAD_THRESHOLD;
    }

    private boolean isAimingForYakuman(AiContext ctx) {
        return AiSupport.ownRank(ctx) == 4 && ctx.roundWind() == Wind.SOUTH && ctx.kyokuNumber() == 4;
    }

    @Override
    public boolean wantsRiichi(AiContext ctx, Tile plannedDiscard) {
        if (!ctx.ownHand().isMenzen()) {
            return false;
        }
        List<Tile> sub = new ArrayList<>(ctx.ownHand().concealedTiles());
        sub.remove(plannedDiscard);
        if (ShantenCalculator.shanten(sub, ctx.ownHand().melds().size()) != 0) {
            return false;
        }
        if (ctx.isDealer() && !ctx.anyOpponentRiichi()) {
            return true;
        }
        Integer han = HandValueEstimator.bestHanForTenpai(sub, ctx.ownHand().melds(),
                ctx.roundWind(), ctx.ownSeatWind(), ctx.doraIndicators(), true);
        return han != null && han >= 3;
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
            if (potential == null || potential < HandValueEstimator.MANGAN_BASE_POINTS) {
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
        return hand.isMenzen() && hand.isTenpai()
                && AiSupport.resultingShantenAfterAnkan(hand, kanTile) <= 0;
    }

    @Override
    public boolean wantsKakan(AiContext ctx, Tile kanTile) {
        return false;
    }
}
