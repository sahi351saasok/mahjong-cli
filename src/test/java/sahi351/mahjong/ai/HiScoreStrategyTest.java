package sahi351.mahjong.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class HiScoreStrategyTest {

    private final HiScoreStrategy strategy = new HiScoreStrategy();

    @Test
    void discardsSafeTileWhenOpponentHasDeclaredRiichi() {
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        PlayerPublicView opponent = AiTestSupport.riichiOpponent("敵", 1, List.of(Tile.of(Suit.SOUZU, 1)));
        AiContext ctx = AiTestSupport.context(hand, List.of(opponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.SOUZU, 1), discard);
    }

    @Test
    void prefersDiscardThatReachesTheHigherValueTenpai() {
        // 234m567m345s88p789p (和了形)。9pを切れば78pの両面待ちとなり、
        // 6p和了でタンヤオ+ピンフ+リーチの3翻が見込める。
        // 一方7p/8pを切ると9pが残り、边张待ちでリーチのみの1翻にとどまる。
        Hand hand = AiTestSupport.handOf("234m567m345s88p789p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.PINZU, 9), discard);
    }

    @Test
    void riichiOnlyWhenAtLeastTwoHanAreConfirmed() {
        // 234m567m678p22p34s: リーチ・ピンフ・タンヤオで2翻以上確定。
        Hand hand = AiTestSupport.handOf("234m567m678p22p34s1z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void doesNotRiichiWhenOnlyOneHanIsConfirmed() {
        // 123m234m456p789s5sタンキ待ち: 一気通貫や三色にならないよう組み、
        // 役牌もタンヤオも平和も付かないため立直のみで1翻。
        Hand hand = AiTestSupport.handOf("123m234m456p789s5s1z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void passesWhenNoYakuReachesManganLevel() {
        // 234m567m11p45p77z9s: 中をポンしてもテンパイ時の打点は満貫に届かない。
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());
        Tile chun = Tile.of(Suit.JIHAI, 7);
        CallOption ponOption = new CallOption(CallType.PON, List.of(chun, chun));

        assertEquals(CallOption.PASS, strategy.decideNaki(ctx, chun, List.of(ponOption)));
    }

    @Test
    void declaresAnkanWhenAtOneShantenOrTenpai() {
        Hand hand = AiTestSupport.handOf("1111m234m567m88p9p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void neverDeclaresKakan() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p11s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsKakan(ctx, Tile.of(Suit.PINZU, 5)));
    }
}
