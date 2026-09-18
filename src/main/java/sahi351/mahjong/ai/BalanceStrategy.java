package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書（バランス戦略）に基づく思考ロジック。
 * 打牌: 牌効率最優先。ただしイーシャンテン以下では安全牌を1枚常に抱え、
 *       テンパイに乗る打牌では抱えていた安全牌を切る。他家リーチ時は安全牌を切って降りる。
 * リーチ: 他家がリーチしていない場合のみリーチする。
 * 鳴き: 1翻以上の役が確定しており、かつ自身の手牌がテンパイになる場合のみ鳴く。
 * 暗槓: 門前でテンパイしている場合のみ行う。
 * 加槓: しない。
 */
public final class BalanceStrategy implements PlayerStrategy {

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();
        List<Tile> visible = AiSupport.collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = AiSupport.doraTileKinds(ctx);
        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);

        if (ctx.anyOpponentRiichi()) {
            return candidates.stream().min(defendComparator(ctx, doraTileKinds))
                    .map(TileEfficiencyEvaluator.Candidate::discard)
                    .orElse(concealed.get(concealed.size() - 1));
        }

        Comparator<TileEfficiencyEvaluator.Candidate> efficiency = efficiencyComparator(ctx, doraTileKinds);
        TileEfficiencyEvaluator.Candidate best = candidates.stream().min(efficiency)
                .orElseThrow(() -> new IllegalStateException("捨て牌候補が存在しない"));

        if (best.shanten() == 0) {
            Optional<TileEfficiencyEvaluator.Candidate> safeTenpai = candidates.stream()
                    .filter(c -> c.shanten() == 0)
                    .filter(c -> AiSupport.isLikelySafe(c.discard(), ctx))
                    .max(Comparator.comparingInt(TileEfficiencyEvaluator.Candidate::ukeire));
            if (safeTenpai.isPresent()) {
                return safeTenpai.get().discard();
            }
            return best.discard();
        }

        List<TileEfficiencyEvaluator.Candidate> nonSafe = candidates.stream()
                .filter(c -> !AiSupport.isLikelySafe(c.discard(), ctx))
                .toList();
        if (!nonSafe.isEmpty()) {
            return nonSafe.stream().min(efficiency)
                    .map(TileEfficiencyEvaluator.Candidate::discard)
                    .orElse(best.discard());
        }
        return best.discard();
    }

    private Comparator<TileEfficiencyEvaluator.Candidate> efficiencyComparator(AiContext ctx, List<Tile> doraTileKinds) {
        return Comparator
                .comparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire())
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));
    }

    private Comparator<TileEfficiencyEvaluator.Candidate> defendComparator(AiContext ctx, List<Tile> doraTileKinds) {
        return Comparator
                .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -AiSupport.riichiSafetyScore(c.discard(), ctx))
                .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire())
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));
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
        return !ctx.anyOpponentRiichi();
    }

    @Override
    public CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();

        for (CallOption option : legalOptions) {
            if (option.type() == CallType.PASS) {
                continue;
            }
            List<Tile> resultingConcealed = new ArrayList<>(concealed);
            for (Tile used : option.tilesToUse()) {
                resultingConcealed.remove(used);
            }
            List<Meld> meldsAfter = new ArrayList<>(ctx.ownHand().melds());
            meldsAfter.add(AiSupport.buildHypotheticalMeld(option, discardedTile));
            Integer potential = HandValueEstimator.bestReachableAfterOneDiscard(
                    resultingConcealed, meldsAfter, ctx.roundWind(), ctx.ownSeatWind(),
                    ctx.doraIndicators(), false);
            if (potential != null) {
                return option;
            }
        }
        return CallOption.PASS;
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
