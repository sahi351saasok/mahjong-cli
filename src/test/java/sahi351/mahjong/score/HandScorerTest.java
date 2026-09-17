package sahi351.mahjong.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.WinContext;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class HandScorerTest {

    private static List<Tile> parse(String spec) {
        List<Tile> tiles = new ArrayList<>();
        List<Integer> pendingRanks = new ArrayList<>();
        for (char c : spec.toCharArray()) {
            if (Character.isDigit(c)) {
                pendingRanks.add(c - '0');
            } else {
                Suit suit = switch (c) {
                    case 'm' -> Suit.MANZU;
                    case 'p' -> Suit.PINZU;
                    case 's' -> Suit.SOUZU;
                    case 'z' -> Suit.JIHAI;
                    default -> throw new IllegalArgumentException("unknown suit: " + c);
                };
                for (int rank : pendingRanks) {
                    tiles.add(Tile.of(suit, rank));
                }
                pendingRanks.clear();
            }
        }
        return tiles;
    }

    private static Hand handOf(String spec) {
        Hand hand = new Hand();
        for (Tile t : parse(spec)) {
            hand.addTile(t);
        }
        return hand;
    }

    private static WinContext ctx(Hand hand, Tile winTile, boolean tsumo, Wind round, Wind seat,
                                   boolean riichi, List<Tile> doraIndicators) {
        return new WinContext(hand, winTile, tsumo, round, seat, riichi, false, false,
                false, false, false, false, false, false,
                doraIndicators, List.of());
    }

    @Test
    void pinfuTsumoRyanmenIsTwentyFu() {
        // 234m567m789m 33p 5-6s + 7s(ツモ) 両面待ち・平和
        Hand hand = handOf("234m567m789m33p56s");
        Tile win = Tile.of(Suit.SOUZU, 7);
        hand.addTile(win);
        WinContext c = ctx(hand, win, true, Wind.EAST, Wind.SOUTH, false, List.of());
        ScoreResult result = HandScorer.score(c, 0);
        assertNotNull(result);
        assertEquals(20, result.fu());
        boolean hasPinfu = result.yakuList().stream().anyMatch(y -> y.name().equals("平和"));
        boolean hasTsumo = result.yakuList().stream().anyMatch(y -> y.name().equals("門前清自摸和"));
        assertTrue(hasPinfu);
        assertTrue(hasTsumo);
    }

    @Test
    void tanyaoOnlyRon() {
        Hand hand = handOf("234m567m22345p88s");
        // groups: 234m 567m 234p pair22p? let's just ensure a complete tanyao hand
        hand = handOf("234m567m345p22p88s");
        Tile win = Tile.of(Suit.SOUZU, 8);
        hand.addTile(win);
        WinContext c = ctx(hand, win, false, Wind.EAST, Wind.SOUTH, false, List.of());
        ScoreResult result = HandScorer.score(c, 0);
        assertNotNull(result);
        boolean hasTanyao = result.yakuList().stream().anyMatch(y -> y.name().equals("断幺九"));
        assertTrue(hasTanyao);
        assertTrue(result.han() >= 1);
    }

    @Test
    void noYakuHandReturnsNull() {
        // 123m 456m 234p 99s 567s（嵌張待ちでロン、平和・タンヤオ・役牌いずれも不成立）
        Hand hand = handOf("123m456m234p99s57s");
        Tile winTile = Tile.of(Suit.SOUZU, 6);
        hand.addTile(winTile);
        WinContext c = ctx(hand, winTile, false, Wind.EAST, Wind.SOUTH, false, List.of());
        ScoreResult result = HandScorer.score(c, 0);
        assertNull(result);
    }

    @Test
    void chiitoitsuScoring() {
        Hand hand = new Hand();
        for (String pair : List.of("11m", "22m", "33m", "44m", "55p", "66p", "77s")) {
            for (Tile t : parse(pair)) {
                hand.addTile(t);
            }
        }
        Tile win = Tile.of(Suit.SOUZU, 7);
        WinContext c = ctx(hand, win, false, Wind.EAST, Wind.SOUTH, false, List.of());
        ScoreResult result = HandScorer.score(c, 0);
        assertNotNull(result);
        assertEquals(25, result.fu());
        boolean hasChiitoi = result.yakuList().stream().anyMatch(y -> y.name().equals("七対子"));
        assertTrue(hasChiitoi);
    }

    @Test
    void kokushiYakuman() {
        Hand hand = new Hand();
        for (Tile t : List.of(
                Tile.of(Suit.MANZU, 1), Tile.of(Suit.MANZU, 9),
                Tile.of(Suit.PINZU, 1), Tile.of(Suit.PINZU, 9),
                Tile.of(Suit.SOUZU, 1), Tile.of(Suit.SOUZU, 9),
                Tile.of(Suit.JIHAI, 1), Tile.of(Suit.JIHAI, 2), Tile.of(Suit.JIHAI, 3),
                Tile.of(Suit.JIHAI, 4), Tile.of(Suit.JIHAI, 5), Tile.of(Suit.JIHAI, 6),
                Tile.of(Suit.JIHAI, 7))) {
            hand.addTile(t);
        }
        Tile win = Tile.of(Suit.MANZU, 1);
        hand.addTile(win);
        WinContext c = ctx(hand, win, false, Wind.EAST, Wind.SOUTH, false, List.of());
        ScoreResult result = HandScorer.score(c, 0);
        assertNotNull(result);
        assertEquals(ScoreTier.YAKUMAN, result.tier());
    }

    @Test
    void doraCountsTowardHan() {
        Hand hand = handOf("234m567m345p22p88s");
        Tile win = Tile.of(Suit.SOUZU, 8);
        hand.addTile(win);
        // dora indicator 1p -> dora is 2p, hand has 22p pair = 2 dora
        WinContext c = ctx(hand, win, false, Wind.EAST, Wind.SOUTH, false, List.of(Tile.of(Suit.PINZU, 1)));
        ScoreResult withoutDora = HandScorer.score(
                ctx(hand, win, false, Wind.EAST, Wind.SOUTH, false, List.of()), 0);
        ScoreResult withDora = HandScorer.score(c, 0);
        assertNotNull(withoutDora);
        assertNotNull(withDora);
        assertEquals(withoutDora.han() + 2, withDora.han());
    }
}
