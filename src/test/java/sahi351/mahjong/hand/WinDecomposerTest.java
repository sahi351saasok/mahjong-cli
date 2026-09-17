package sahi351.mahjong.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class WinDecomposerTest {

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

    @Test
    void singleInterpretationHand() {
        List<Tile> hand = parse("123m456m789m123p11s");
        List<WinDecomposer.Decomposition> results = WinDecomposer.decompose(hand, 4);
        assertEquals(1, results.size());
        assertEquals(4, results.get(0).sets().size());
        assertEquals(GroupType.PAIR, results.get(0).pair().type());
    }

    @Test
    void ambiguousHandHasMultipleInterpretations() {
        // 111222333m + pair -> 刻子3つ or 順子3つ の2通り
        List<Tile> hand = parse("111222333m99p");
        List<WinDecomposer.Decomposition> results = WinDecomposer.decompose(hand, 3);
        assertTrue(results.size() >= 2);
    }

    @Test
    void toitoiOnlyHandHasOneStructuralInterpretation() {
        List<Tile> hand = parse("111m222p333s44z");
        // 44z is honor pair, 111m/222p/333s are triplets - only one interpretation
        List<WinDecomposer.Decomposition> results = WinDecomposer.decompose(hand, 3);
        assertEquals(1, results.size());
    }
}
