package sahi351.mahjong.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class ShantenCalculatorTest {

    private static List<Tile> parse(String spec) {
        // 例: "123m456m789m123p11s"
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

    @Test
    void completeStandardHandIsAgari() {
        List<Tile> hand = parse("123m456m789m123p11s");
        assertEquals(-1, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void tankiWaitIsTenpai() {
        List<Tile> hand = parse("123m456m789m123p1s");
        assertEquals(0, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void oneShantenHand() {
        // 123m456m789m12p13s -> 13枚、1シャンテン想定
        List<Tile> hand = parse("123m456m789m12p13s");
        assertEquals(1, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void chiitoitsuAgari() {
        List<Tile> hand = parse("1122334455667p");
        // 7 pairs: 11 22 33 44 55 66 7... need exactly 7 kinds pairs, fix spec below
        hand = new ArrayList<>();
        hand.addAll(parse("11p"));
        hand.addAll(parse("22p"));
        hand.addAll(parse("33p"));
        hand.addAll(parse("44p"));
        hand.addAll(parse("55p"));
        hand.addAll(parse("66p"));
        hand.addAll(parse("77p"));
        assertEquals(-1, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void chiitoitsuTenpai() {
        List<Tile> hand = new ArrayList<>();
        hand.addAll(parse("11p"));
        hand.addAll(parse("22p"));
        hand.addAll(parse("33p"));
        hand.addAll(parse("44p"));
        hand.addAll(parse("55p"));
        hand.addAll(parse("66p"));
        hand.add(Tile.of(Suit.PINZU, 7));
        assertEquals(0, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void kokushiAgari() {
        List<Tile> hand = new ArrayList<>();
        hand.add(Tile.of(Suit.MANZU, 1));
        hand.add(Tile.of(Suit.MANZU, 9));
        hand.add(Tile.of(Suit.PINZU, 1));
        hand.add(Tile.of(Suit.PINZU, 9));
        hand.add(Tile.of(Suit.SOUZU, 1));
        hand.add(Tile.of(Suit.SOUZU, 9));
        for (int i = 1; i <= 7; i++) {
            hand.add(Tile.of(Suit.JIHAI, i));
        }
        hand.add(Tile.of(Suit.MANZU, 1));
        assertEquals(-1, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void kokushiTenpai() {
        List<Tile> hand = new ArrayList<>();
        hand.add(Tile.of(Suit.MANZU, 1));
        hand.add(Tile.of(Suit.MANZU, 9));
        hand.add(Tile.of(Suit.PINZU, 1));
        hand.add(Tile.of(Suit.PINZU, 9));
        hand.add(Tile.of(Suit.SOUZU, 1));
        hand.add(Tile.of(Suit.SOUZU, 9));
        for (int i = 1; i <= 7; i++) {
            hand.add(Tile.of(Suit.JIHAI, i));
        }
        assertEquals(0, ShantenCalculator.shanten(hand, 0));
    }

    @Test
    void shantenWithOpenMelds() {
        // 副露1つ（面子1つ済み扱い）+ 残り10枚で3シャンテン相当を確認
        List<Tile> concealed = parse("19m19p19s1234z");
        // ばらばらな10枚 + 面子1つ -> shantenは大きくなるはず
        int shanten = ShantenCalculator.shanten(concealed, 1);
        assertEquals(true, shanten >= 5);
    }
}
