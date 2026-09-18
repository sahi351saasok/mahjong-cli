package sahi351.mahjong.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class AggressiveStrategyTest {

    private final AggressiveStrategy strategy = new AggressiveStrategy();

    @Test
    void ignoresOpponentRiichiWhenChoosingDiscard() {
        // 123m456m789m12p11s7z: 7zを切れば123m456m789m12p11sで1シャンテン(3p待ちの1向聴)。
        // 1sを切ると対子が崩れシャンテン数が悪化するため、牌効率のみを見れば7zを切るのが最善。
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        PlayerPublicView opponent = AiTestSupport.riichiOpponent("敵", 1, List.of(Tile.of(Suit.SOUZU, 1)));
        AiContext ctx = AiTestSupport.context(hand, List.of(opponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.JIHAI, 7), discard);
    }

    @Test
    void declaresRiichiAsSoonAsTenpaiRegardlessOfValue() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p9s1z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void doesNotDeclareRiichiWhenHandIsOpen() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p9s");
        hand.addMeld(Meld.pon(
                List.of(Tile.of(Suit.PINZU, 5), Tile.of(Suit.PINZU, 5)), Tile.of(Suit.PINZU, 5), 2));
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 9)));
    }

    @Test
    void passesWhenNoCallOptionsAreLegal() {
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        CallOption result = strategy.decideNaki(ctx, Tile.of(Suit.JIHAI, 7), List.of());

        assertEquals(CallOption.PASS, result);
    }

    @Test
    void callsPonWhenItLowersShantenWithConfirmedYaku() {
        // 234m567m11p45p77z9s (1シャンテン)。中(7z)をポンすれば役牌が確定し、
        // 9sを切ってテンパイに乗せられる。
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());
        Tile chun = Tile.of(Suit.JIHAI, 7);
        CallOption ponOption = new CallOption(CallType.PON, List.of(chun, chun));

        CallOption result = strategy.decideNaki(ctx, chun, List.of(ponOption));

        assertEquals(ponOption, result);
    }

    @Test
    void declaresAnkanWhenMenzenAndOneShantenOrBetter() {
        // 1111m234m567m88p9p (門前1シャンテン)。1mを暗槓しても88p9pの形は崩れず1シャンテンのまま。
        Hand hand = AiTestSupport.handOf("1111m234m567m88p9p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void doesNotDeclareAnkanWhenHandIsOpen() {
        Hand hand = AiTestSupport.handOf("1111m234m567m88p");
        hand.addMeld(Meld.pon(
                List.of(Tile.of(Suit.PINZU, 9), Tile.of(Suit.PINZU, 9)), Tile.of(Suit.PINZU, 9), 2));
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void neverDeclaresKakan() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p11s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsKakan(ctx, Tile.of(Suit.PINZU, 5)));
    }
}
