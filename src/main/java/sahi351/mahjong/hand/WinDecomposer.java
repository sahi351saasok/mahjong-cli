package sahi351.mahjong.hand;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * 完成形の手牌（面子+雀頭）を、あり得る全ての組み合わせに分解する。
 * ピンフ・一盃口の判定などで複数の解釈が発生し得るため、全パターンを列挙する。
 */
public final class WinDecomposer {

    private WinDecomposer() {
    }

    public record Decomposition(List<Group> sets, Group pair) {
    }

    private record StructGroup(GroupType type, int index) {
    }

    public static List<Decomposition> decompose(List<Tile> concealedTiles, int neededSets) {
        int[] counts = TileIndex.toCounts(concealedTiles);
        List<List<StructGroup>> results = new ArrayList<>();
        search(counts, 0, neededSets, new ArrayDeque<>(), -1, results);

        List<Decomposition> decompositions = new ArrayList<>();
        for (List<StructGroup> structure : results) {
            decompositions.add(materialize(concealedTiles, structure));
        }
        return decompositions;
    }

    private static void search(int[] counts, int index, int neededSets,
                                ArrayDeque<StructGroup> current, int pairIndex,
                                List<List<StructGroup>> results) {
        if (index >= TileIndex.SIZE) {
            if (current.size() == neededSets && pairIndex != -1) {
                results.add(new ArrayList<>(current));
            }
            return;
        }
        if (counts[index] == 0) {
            search(counts, index + 1, neededSets, current, pairIndex, results);
            return;
        }

        int posInSuit = index % 9;
        boolean suited = index < 27;

        if (counts[index] >= 3 && current.size() < neededSets) {
            counts[index] -= 3;
            current.addLast(new StructGroup(GroupType.TRIPLET, index));
            search(counts, index, neededSets, current, pairIndex, results);
            current.removeLast();
            counts[index] += 3;
        }

        if (suited && posInSuit <= 6 && current.size() < neededSets
                && counts[index] >= 1 && counts[index + 1] >= 1 && counts[index + 2] >= 1) {
            counts[index]--;
            counts[index + 1]--;
            counts[index + 2]--;
            current.addLast(new StructGroup(GroupType.SEQUENCE, index));
            search(counts, index, neededSets, current, pairIndex, results);
            current.removeLast();
            counts[index]++;
            counts[index + 1]++;
            counts[index + 2]++;
        }

        if (counts[index] >= 2 && pairIndex == -1) {
            counts[index] -= 2;
            search(counts, index, neededSets, current, index, results);
            counts[index] += 2;
        }
    }

    private static Decomposition materialize(List<Tile> concealedTiles, List<StructGroup> structure) {
        List<List<Tile>> buckets = new ArrayList<>(TileIndex.SIZE);
        for (int i = 0; i < TileIndex.SIZE; i++) {
            buckets.add(new ArrayList<>());
        }
        for (Tile t : concealedTiles) {
            buckets.get(TileIndex.of(t)).add(t);
        }

        List<Group> sets = new ArrayList<>();
        Group pair = null;
        for (StructGroup sg : structure) {
            List<Tile> tiles = new ArrayList<>();
            if (sg.type() == GroupType.TRIPLET) {
                List<Tile> bucket = buckets.get(sg.index());
                for (int i = 0; i < 3; i++) {
                    tiles.add(bucket.remove(bucket.size() - 1));
                }
                sets.add(new Group(GroupType.TRIPLET, tiles));
            } else if (sg.type() == GroupType.SEQUENCE) {
                for (int offset = 0; offset < 3; offset++) {
                    List<Tile> bucket = buckets.get(sg.index() + offset);
                    tiles.add(bucket.remove(bucket.size() - 1));
                }
                sets.add(new Group(GroupType.SEQUENCE, tiles));
            }
        }
        // pairIndex is recoverable: the only remaining bucket(s) with exactly 2 tiles left over
        for (int i = 0; i < TileIndex.SIZE; i++) {
            List<Tile> bucket = buckets.get(i);
            if (bucket.size() == 2) {
                pair = new Group(GroupType.PAIR, new ArrayList<>(bucket));
                bucket.clear();
                break;
            }
        }
        return new Decomposition(sets, pair);
    }
}
