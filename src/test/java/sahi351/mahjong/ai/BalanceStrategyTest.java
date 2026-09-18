package sahi351.mahjong.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class BalanceStrategyTest {

    private final BalanceStrategy strategy = new BalanceStrategy();

    @Test
    void discardsSafeTileWhenOpponentHasDeclaredRiichi() {
        // 123m456m789m12p11s7z: 効率だけなら7zが最善だが、他家がリーチしているので
        // 現物の1sを切って降りる。
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        PlayerPublicView opponent = AiTestSupport.riichiOpponent("敵", 1, List.of(Tile.of(Suit.SOUZU, 1)));
        AiContext ctx = AiTestSupport.context(hand, List.of(opponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.SOUZU, 1), discard);
    }

    @Test
    void keepsAHeldSafeTileWhileBelowTenpai() {
        // 234m567m88p345s1z (1シャンテン、8pが下家の現物)。8pを抱えたまま
        // 効率上最善な打牌(1z)を選ぶ。
        Hand hand = AiTestSupport.handOf("234m567m88p345s1z");
        PlayerPublicView opponent = AiTestSupport.quietOpponent("下家", 1, List.of(Tile.of(Suit.PINZU, 8)));
        AiContext ctx = AiTestSupport.context(hand, List.of(opponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.JIHAI, 1), discard);
    }

    @Test
    void riichiOnlyWhenNoOpponentHasDeclaredRiichi() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p9s1z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void doesNotRiichiWhenAnOpponentHasAlreadyDeclared() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p9s1z");
        PlayerPublicView opponent = AiTestSupport.riichiOpponent("敵", 1, List.of());
        AiContext ctx = AiTestSupport.context(hand, List.of(opponent));

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void passesWhenNoCallOptionsAreLegal() {
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertEquals(CallOption.PASS, strategy.decideNaki(ctx, Tile.of(Suit.JIHAI, 7), List.of()));
    }

    @Test
    void callsOnlyWhenTheCallItselfReachesTenpaiWithConfirmedYaku() {
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());
        Tile chun = Tile.of(Suit.JIHAI, 7);
        CallOption ponOption = new CallOption(CallType.PON, List.of(chun, chun));

        CallOption result = strategy.decideNaki(ctx, chun, List.of(ponOption));

        assertEquals(ponOption, result);
    }

    @Test
    void declaresAnkanOnlyWhenMenzenAndAlreadyTenpai() {
        // 1111m234m567m88p9p (門前1シャンテン、テンパイではない) -> しない
        Hand oneShanten = AiTestSupport.handOf("1111m234m567m88p9p");
        AiContext ctxOneShanten = AiTestSupport.context(oneShanten, List.of());
        assertFalse(strategy.wantsAnkan(ctxOneShanten, Tile.of(Suit.MANZU, 1)));

        // 1111m234m567m789p (門前テンパイ、1mタンキ待ちではなく完成メンツのみ) -> する
        Hand tenpai = AiTestSupport.handOf("1111m234m567m789p");
        AiContext ctxTenpai = AiTestSupport.context(tenpai, List.of());
        assertTrue(strategy.wantsAnkan(ctxTenpai, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void neverDeclaresKakan() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p11s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsKakan(ctx, Tile.of(Suit.PINZU, 5)));
    }
}
