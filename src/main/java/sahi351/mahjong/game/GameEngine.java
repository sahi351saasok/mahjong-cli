package sahi351.mahjong.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import sahi351.mahjong.ai.AiContext;
import sahi351.mahjong.ai.CallOption;
import sahi351.mahjong.ai.CallType;
import sahi351.mahjong.ai.PlayerPublicView;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.MeldType;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.WinContext;
import sahi351.mahjong.log.GameLogger;
import sahi351.mahjong.player.Player;
import sahi351.mahjong.score.HandScorer;
import sahi351.mahjong.score.ScoreResult;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;
import sahi351.mahjong.tile.Wall;

/**
 * 半荘・東風戦・1局のみの対局進行を司るゲームエンジン。
 */
public final class GameEngine {

    private final List<Player> players;
    private final GameMode mode;
    private final Random random;
    private final GameLogger logger;

    private Wind roundWind = Wind.EAST;
    private int kyokuNumber = 1;
    private int honba = 0;
    private int riichiSticks = 0;
    private int dealerSeatIndex = 0;

    private Wall wall;
    private int turnCount;
    private boolean callHappened;
    private boolean[] hasDrawn;
    private boolean kyokuEnded;
    private int kanCountThisKyoku;

    public GameEngine(List<Player> players, GameMode mode, Random random, GameLogger logger) {
        if (players.size() != 4) {
            throw new IllegalArgumentException("プレイヤーは4人必要です");
        }
        this.players = players;
        this.mode = mode;
        this.random = random;
        this.logger = logger;
    }

    public void run() {
        dealerSeatIndex = random.nextInt(4);
        logger.log("=== 対局開始 (" + (mode == GameMode.HANCHAN ? "半荘戦" : "1局のみ") + ") ===");
        logger.log("起家: " + players.get(dealerSeatIndex).name());
        logger.blank();

        while (true) {
            boolean renchan = playKyoku();

            if (mode == GameMode.SINGLE_KYOKU) {
                break;
            }

            boolean isLastKyokuOfHanchan = roundWind == Wind.SOUTH && kyokuNumber == 4;
            if (renchan) {
                honba++;
                if (isLastKyokuOfHanchan) {
                    continue;
                }
            } else {
                if (isLastKyokuOfHanchan) {
                    break;
                }
                honba = 0;
                dealerSeatIndex = (dealerSeatIndex + 1) % 4;
                if (kyokuNumber == 4) {
                    kyokuNumber = 1;
                    roundWind = roundWind.next();
                } else {
                    kyokuNumber++;
                }
            }
        }

        if (riichiSticks > 0) {
            Player top = players.stream().max((a, b) -> Integer.compare(a.points(), b.points())).orElseThrow();
            top.addPoints(riichiSticks * 1000);
            riichiSticks = 0;
        }

        printFinalResult();
    }

    /** @return 親が続投する（連荘）場合 true */
    private boolean playKyoku() {
        setupKyoku();
        logger.blank();
        logger.log(String.format("--- %s%d局%d本場 (親: %s) ドラ表示牌: %s ---",
                roundWind.label(), kyokuNumber, honba, players.get(dealerSeatIndex).name(),
                wall.doraIndicators()));

        int currentIndex = dealerSeatIndex;
        while (!kyokuEnded) {
            Player current = players.get(currentIndex);
            if (!wall.hasNextDraw()) {
                resolveRyuukyoku();
                break;
            }
            processTurn(current);
            if (kyokuEnded) {
                break;
            }
            currentIndex = (currentIndex + 1) % 4;
        }

        return lastKyokuWasRenchan;
    }

    private boolean lastKyokuWasRenchan;

    private void setupKyoku() {
        for (int i = 0; i < 4; i++) {
            Player p = players.get(i);
            p.resetForNewKyoku();
            p.setSeatWind(Wind.values()[(i - dealerSeatIndex + 4) % 4]);
        }
        wall = new Wall(random);
        for (int round = 0; round < 13; round++) {
            for (int offset = 0; offset < 4; offset++) {
                players.get((dealerSeatIndex + offset) % 4).hand().addTile(wall.draw());
            }
        }
        turnCount = 0;
        callHappened = false;
        hasDrawn = new boolean[4];
        kyokuEnded = false;
        kanCountThisKyoku = 0;
        lastKyokuWasRenchan = false;
    }

    /** 1人のツモ番を処理する。ポン・チー等で他家に手番が移った場合も含め、この呼び出し内で完結する。 */
    private void processTurn(Player player) {
        turnCount++;
        boolean isFirstDrawEver = !hasDrawn[player.seatIndex()] && !callHappened;
        hasDrawn[player.seatIndex()] = true;

        player.hand().addTile(wall.draw());

        boolean handledKan = handleKanOpportunities(player);
        if (kyokuEnded) {
            return;
        }

        boolean isTenhou = isFirstDrawEver && player.seatWind() == Wind.EAST && turnCount == 1;
        boolean isChiihou = isFirstDrawEver && player.seatWind() != Wind.EAST;
        boolean haitei = !wall.hasNextDraw();

        Tile currentDrawTile = player.hand().concealedTiles().get(player.hand().concealedTiles().size() - 1);
        if (!handledKan) {
            ScoreResult tsumoResult = tryTsumo(player, currentDrawTile, isTenhou, isChiihou, haitei, false);
            if (tsumoResult != null) {
                finishWithTsumo(player, tsumoResult);
                return;
            }
        }

        Tile discard;
        boolean declaringRiichi = false;
        if (player.isRiichi()) {
            if (player.isIppatsuActive()) {
                player.clearIppatsu();
            }
            discard = currentDrawTile;
        } else {
            AiContext ctx = buildAiContext(player);
            discard = player.strategy().chooseDiscard(ctx);
            if (canDeclareRiichi(player)) {
                List<Tile> sub = new ArrayList<>(player.hand().concealedTiles());
                sub.remove(discard);
                if (ShantenCalculator.shanten(sub, player.hand().melds().size()) == 0
                        && player.strategy().wantsRiichi(ctx, discard)) {
                    declaringRiichi = true;
                }
            }
        }

        player.hand().removeTile(discard);
        player.addDiscard(discard, declaringRiichi);

        if (declaringRiichi) {
            boolean doubleRiichi = player.isDoubleRiichiEligible() && isPlayersFirstDiscard(player);
            player.declareRiichi(turnCount, doubleRiichi);
            player.addPoints(-1000);
            riichiSticks++;
            logger.log(player.name() + ": リーチ宣言 (" + discard + (doubleRiichi ? ") [ダブルリーチ]" : ")"));
        } else {
            logger.log(player.name() + ": 打 " + discard);
        }

        resolveAfterDiscard(player, discard);
    }

    private boolean isPlayersFirstDiscard(Player player) {
        return player.discards().size() == 1;
    }

    private boolean canDeclareRiichi(Player player) {
        return !player.isRiichi() && player.hand().isMenzen()
                && player.points() >= 1000 && wall.remainingLiveTiles() >= 4;
    }

    /** 暗槓・加槓の判断と実行。嶺上ツモ・槍槓が発生した場合は kyokuEnded を立てる。 */
    private boolean handleKanOpportunities(Player player) {
        boolean any = false;
        boolean continueChecking = true;
        while (continueChecking) {
            continueChecking = false;
            if (player.isRiichi() || kanCountThisKyoku >= 4) {
                break;
            }
            Tile ankanTarget = findAnkanCandidate(player);
            if (ankanTarget != null) {
                AiContext ctx = buildAiContext(player);
                if (player.strategy().wantsAnkan(ctx, ankanTarget)) {
                    performAnkan(player, ankanTarget);
                    any = true;
                    if (kyokuEnded) {
                        return true;
                    }
                    continueChecking = true;
                    continue;
                }
            }
            Meld kakanTarget = findKakanCandidate(player);
            if (kakanTarget != null) {
                Tile tile = kakanTarget.representativeTile();
                AiContext ctx = buildAiContext(player);
                if (player.strategy().wantsKakan(ctx, tile)) {
                    boolean chankan = performKakan(player, kakanTarget, tile);
                    any = true;
                    if (chankan || kyokuEnded) {
                        return true;
                    }
                    continueChecking = true;
                }
            }
        }
        return any;
    }

    private Tile findAnkanCandidate(Player player) {
        List<Tile> concealed = player.hand().concealedTiles();
        for (Tile t : concealed) {
            long count = concealed.stream().filter(x -> x.isSameKind(t)).count();
            if (count == 4) {
                return t;
            }
        }
        return null;
    }

    private Meld findKakanCandidate(Player player) {
        for (Meld m : player.hand().melds()) {
            if (m.type() == MeldType.PON) {
                Tile t = m.representativeTile();
                if (player.hand().concealedTiles().stream().anyMatch(x -> x.isSameKind(t))) {
                    return m;
                }
            }
        }
        return null;
    }

    private void performAnkan(Player player, Tile kind) {
        List<Tile> concealed = new ArrayList<>(player.hand().concealedTiles());
        List<Tile> used = new ArrayList<>();
        for (Tile t : concealed) {
            if (t.isSameKind(kind) && used.size() < 4) {
                used.add(t);
            }
        }
        for (Tile t : used) {
            player.hand().removeTile(t);
        }
        player.hand().addMeld(Meld.ankan(used));
        callHappened = true;
        kanCountThisKyoku++;
        onCallHappened();
        wall.revealNewDoraIndicator();
        logger.log(player.name() + ": 暗槓 " + kind);

        Tile rinshan = wall.drawRinshan();
        player.hand().addTile(rinshan);
        ScoreResult result = tryTsumo(player, rinshan, false, false, false, true);
        if (result != null) {
            finishWithTsumo(player, result);
        }
    }

    /** @return 槍槓が発生して局が終了した場合 true */
    private boolean performKakan(Player player, Meld pon, Tile kind) {
        List<Tile> newTiles = new ArrayList<>(pon.tiles());
        Tile fromHand = player.hand().concealedTiles().stream()
                .filter(t -> t.isSameKind(kind)).findFirst().orElseThrow();
        player.hand().removeTile(fromHand);
        newTiles.add(fromHand);
        Meld kakan = Meld.kakan(newTiles, pon.calledTile(), pon.fromSeat());

        ScoreResult chankanResult = null;
        Player chankanWinner = null;
        for (int offset = 1; offset <= 3; offset++) {
            Player other = players.get((player.seatIndex() + offset) % 4);
            ScoreResult r = tryRon(other, fromHand, false, true);
            if (r != null) {
                chankanResult = r;
                chankanWinner = other;
                break;
            }
        }
        if (chankanResult != null) {
            logger.log(player.name() + ": 加槓 " + kind + " → " + chankanWinner.name() + " 槍槓!");
            finishWithRon(chankanWinner, player, chankanResult);
            return true;
        }

        player.hand().upgradePonToKakan(pon, kakan);
        callHappened = true;
        kanCountThisKyoku++;
        onCallHappened();
        wall.revealNewDoraIndicator();
        logger.log(player.name() + ": 加槓 " + kind);

        Tile rinshan = wall.drawRinshan();
        player.hand().addTile(rinshan);
        ScoreResult result = tryTsumo(player, rinshan, false, false, false, true);
        if (result != null) {
            finishWithTsumo(player, result);
        }
        return false;
    }

    private void onCallHappened() {
        for (Player p : players) {
            p.clearIppatsu();
            if (!p.isRiichi()) {
                p.disqualifyDoubleRiichi();
            }
        }
    }

    private ScoreResult tryTsumo(Player player, Tile winTile, boolean tenhou, boolean chiihou,
                                  boolean haitei, boolean rinshan) {
        WinContext ctx = new WinContext(player.hand(), winTile, true, roundWind, player.seatWind(),
                player.isRiichi(), player.isDoubleRiichi(), player.isIppatsuActive(),
                haitei, false, rinshan, false, tenhou, chiihou,
                wall.doraIndicators(), wall.uraDoraIndicators());
        return HandScorer.score(ctx, honba);
    }

    private ScoreResult tryRon(Player player, Tile winTile, boolean houtei, boolean chankan) {
        if (!player.hand().isTenpai()) {
            return null;
        }
        List<Tile> waits = sahi351.mahjong.ai.HandValueEstimator.findWaits(
                player.hand().concealedTiles(), player.hand().melds().size());
        boolean isWait = waits.stream().anyMatch(w -> w.isSameKind(winTile));
        if (!isWait) {
            return null;
        }
        if (player.isFuriten(waits)) {
            return null;
        }
        Hand hand = player.hand();
        hand.addTile(winTile);
        WinContext ctx = new WinContext(hand, winTile, false, roundWind, player.seatWind(),
                player.isRiichi(), player.isDoubleRiichi(), player.isIppatsuActive(),
                false, houtei, false, chankan, false, false,
                wall.doraIndicators(), wall.uraDoraIndicators());
        ScoreResult result = HandScorer.score(ctx, honba);
        if (result == null) {
            hand.removeTile(winTile);
        }
        return result;
    }

    private void resolveAfterDiscard(Player discarder, Tile discardedTile) {
        boolean houtei = !wall.hasNextDraw();

        List<Player> ronWinners = new ArrayList<>();
        List<ScoreResult> ronResults = new ArrayList<>();
        for (int offset = 1; offset <= 3; offset++) {
            Player other = players.get((discarder.seatIndex() + offset) % 4);
            ScoreResult result = tryRon(other, discardedTile, houtei, false);
            if (result != null) {
                ronWinners.add(other);
                ronResults.add(result);
            }
        }
        if (!ronWinners.isEmpty()) {
            finishWithMultiRon(discarder, ronWinners, ronResults);
            return;
        }

        // ポン・カン判定（discarder に近い順）
        for (int offset = 1; offset <= 3; offset++) {
            Player other = players.get((discarder.seatIndex() + offset) % 4);
            if (other.isRiichi()) {
                continue;
            }
            List<CallOption> options = buildPonKanOptions(other, discardedTile);
            if (options.isEmpty()) {
                continue;
            }
            AiContext ctx = buildAiContext(other);
            CallOption chosen = other.strategy().decideNaki(ctx, discardedTile, options);
            if (chosen.type() != CallType.PASS) {
                executeCall(discarder, other, discardedTile, chosen);
                return;
            }
        }

        // チー判定（下家のみ）
        Player shimocha = players.get((discarder.seatIndex() + 1) % 4);
        List<CallOption> chiOptions = shimocha.isRiichi()
                ? List.of() : buildChiOptions(shimocha, discardedTile);
        if (!chiOptions.isEmpty()) {
            AiContext ctx = buildAiContext(shimocha);
            CallOption chosen = shimocha.strategy().decideNaki(ctx, discardedTile, chiOptions);
            if (chosen.type() != CallType.PASS) {
                executeCall(discarder, shimocha, discardedTile, chosen);
                return;
            }
        }

        if (houtei) {
            resolveRyuukyoku();
        }
    }

    private List<CallOption> buildPonKanOptions(Player player, Tile discarded) {
        List<CallOption> options = new ArrayList<>();
        List<Tile> matching = player.hand().concealedTiles().stream()
                .filter(t -> t.isSameKind(discarded)).toList();
        if (matching.size() >= 2) {
            options.add(new CallOption(CallType.PON, matching.subList(0, 2)));
        }
        if (matching.size() >= 3 && kanCountThisKyoku < 4) {
            options.add(new CallOption(CallType.KAN, matching.subList(0, 3)));
        }
        return options;
    }

    private List<CallOption> buildChiOptions(Player player, Tile discarded) {
        List<CallOption> options = new ArrayList<>();
        if (discarded.isHonor()) {
            return options;
        }
        Suit suit = discarded.suit();
        int rank = discarded.rank();
        int[][] patterns = {{-2, -1}, {-1, 1}, {1, 2}};
        for (int[] pattern : patterns) {
            int r1 = rank + pattern[0];
            int r2 = rank + pattern[1];
            if (r1 < 1 || r1 > 9 || r2 < 1 || r2 > 9) {
                continue;
            }
            Tile t1 = findInHand(player, suit, r1);
            Tile t2 = findInHand(player, suit, r2);
            if (t1 != null && t2 != null) {
                options.add(new CallOption(CallType.CHI, List.of(t1, t2)));
            }
        }
        return options;
    }

    private Tile findInHand(Player player, Suit suit, int rank) {
        return player.hand().concealedTiles().stream()
                .filter(t -> t.suit() == suit && t.rank() == rank)
                .findFirst().orElse(null);
    }

    private void executeCall(Player discarder, Player caller, Tile discardedTile, CallOption option) {
        for (Tile t : option.tilesToUse()) {
            caller.hand().removeTile(t);
        }
        List<Tile> meldTiles = new ArrayList<>(option.tilesToUse());
        meldTiles.add(discardedTile);
        Meld meld = switch (option.type()) {
            case CHI -> Meld.chi(meldTiles, discardedTile, discarder.seatIndex());
            case PON -> Meld.pon(meldTiles, discardedTile, discarder.seatIndex());
            case KAN -> Meld.minkan(meldTiles, discardedTile, discarder.seatIndex());
            case PASS -> throw new IllegalStateException();
        };
        caller.hand().addMeld(meld);
        callHappened = true;
        onCallHappened();
        logger.log(caller.name() + ": " + labelFor(option.type()) + " " + discardedTile
                + " (" + discarder.name() + "の牌)");

        if (option.type() == CallType.KAN) {
            kanCountThisKyoku++;
            wall.revealNewDoraIndicator();
            Tile rinshan = wall.drawRinshan();
            caller.hand().addTile(rinshan);
            ScoreResult result = tryTsumo(caller, rinshan, false, false, false, true);
            if (result != null) {
                finishWithTsumo(caller, result);
                return;
            }
            handleKanOpportunities(caller);
            if (kyokuEnded) {
                return;
            }
        }

        AiContext ctx = buildAiContext(caller);
        Tile discard = caller.strategy().chooseDiscard(ctx);
        caller.hand().removeTile(discard);
        caller.addDiscard(discard, false);
        logger.log(caller.name() + ": 打 " + discard);
        resolveAfterDiscard(caller, discard);
    }

    private String labelFor(CallType type) {
        return switch (type) {
            case CHI -> "チー";
            case PON -> "ポン";
            case KAN -> "大明槓";
            case PASS -> "";
        };
    }

    private AiContext buildAiContext(Player self) {
        List<PlayerPublicView> others = new ArrayList<>();
        for (int offset = 1; offset <= 3; offset++) {
            Player p = players.get((self.seatIndex() + offset) % 4);
            others.add(new PlayerPublicView(p.name(), p.seatIndex(), p.seatWind(), p.points(),
                    p.discards(), p.hand().melds(), p.isRiichi()));
        }
        return new AiContext(self.hand(), self.seatWind(), roundWind, honba,
                wall.remainingLiveTiles(), wall.doraIndicators(), others, turnCount, self.points());
    }

    private void finishWithTsumo(Player winner, ScoreResult result) {
        logger.log(winner.name() + ": ツモ和了!");
        logScoreResult(winner, result);
        int stickBonus = riichiSticks * 1000;
        if (result.isDealer()) {
            for (int offset = 1; offset <= 3; offset++) {
                players.get((winner.seatIndex() + offset) % 4).addPoints(-result.tsumoNonDealerPayment());
            }
            winner.addPoints(result.tsumoNonDealerPayment() * 3 + stickBonus);
        } else {
            for (int offset = 1; offset <= 3; offset++) {
                Player other = players.get((winner.seatIndex() + offset) % 4);
                if (other.seatWind() == Wind.EAST) {
                    other.addPoints(-result.tsumoDealerPayment());
                } else {
                    other.addPoints(-result.tsumoNonDealerPayment());
                }
            }
            winner.addPoints(result.tsumoDealerPayment() + result.tsumoNonDealerPayment() * 2 + stickBonus);
        }
        riichiSticks = 0;
        lastKyokuWasRenchan = result.isDealer();
        kyokuEnded = true;
    }

    private void finishWithRon(Player winner, Player discarder, ScoreResult result) {
        finishWithMultiRon(discarder, List.of(winner), List.of(result));
    }

    private void finishWithMultiRon(Player discarder, List<Player> winners, List<ScoreResult> results) {
        int stickBonus = riichiSticks * 1000;
        boolean firstWinnerGetsSticks = true;
        for (int i = 0; i < winners.size(); i++) {
            Player winner = winners.get(i);
            ScoreResult result = results.get(i);
            logger.log(winner.name() + ": ロン和了! (放銃: " + discarder.name() + ")");
            logScoreResult(winner, result);
            int gain = result.ronPayment() + (firstWinnerGetsSticks ? stickBonus : 0);
            firstWinnerGetsSticks = false;
            winner.addPoints(gain);
            discarder.addPoints(-result.ronPayment());
        }
        riichiSticks = 0;
        lastKyokuWasRenchan = winners.stream().anyMatch(w -> w.seatWind() == Wind.EAST);
        kyokuEnded = true;
    }

    private void logScoreResult(Player winner, ScoreResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("  手牌: ").append(winner.hand().sortedConcealedTiles());
        if (!winner.hand().melds().isEmpty()) {
            sb.append(" 副露: ").append(winner.hand().melds());
        }
        logger.log(sb.toString());
        StringBuilder yakuLine = new StringBuilder("  役: ");
        for (var y : result.yakuList()) {
            yakuLine.append(y.name());
            if (!y.yakuman()) {
                yakuLine.append("(").append(y.han()).append("翻)");
            }
            yakuLine.append(' ');
        }
        logger.log(yakuLine.toString());
        String scoreLine = result.tier() == sahi351.mahjong.score.ScoreTier.NORMAL
                ? String.format("  %d翻%d符 %d点", result.han(), result.fu(), result.totalPoints())
                : String.format("  %d翻%d符 %s %d点", result.han(), result.fu(), result.tier().label(), result.totalPoints());
        logger.log(scoreLine);
    }

    private void resolveRyuukyoku() {
        logger.log("--- 流局 ---");
        List<Player> tenpaiPlayers = new ArrayList<>();
        for (Player p : players) {
            boolean tenpai = p.hand().isTenpai();
            logger.log(p.name() + ": " + (tenpai ? "テンパイ " + p.hand().sortedConcealedTiles() : "ノーテン"));
            if (tenpai) {
                tenpaiPlayers.add(p);
            }
        }
        int tenpaiCount = tenpaiPlayers.size();
        if (tenpaiCount > 0 && tenpaiCount < 4) {
            int totalPot = 3000;
            int gainEach = totalPot / tenpaiCount;
            int loseEach = totalPot / (4 - tenpaiCount);
            for (Player p : players) {
                if (tenpaiPlayers.contains(p)) {
                    p.addPoints(gainEach);
                } else {
                    p.addPoints(-loseEach);
                }
            }
        }
        Player dealer = players.get(dealerSeatIndex);
        lastKyokuWasRenchan = dealer.hand().isTenpai();
        kyokuEnded = true;
    }

    private void printFinalResult() {
        logger.blank();
        logger.log("=== 対局結果 ===");
        List<Player> ranked = new ArrayList<>(players);
        ranked.sort((a, b) -> {
            if (b.points() != a.points()) {
                return Integer.compare(b.points(), a.points());
            }
            return Integer.compare(a.seatIndex(), b.seatIndex());
        });

        double[] umaByRank = {50, 10, -10, -30};
        double[] pointsByPlayer = new double[4];
        int i = 0;
        while (i < 4) {
            int j = i;
            while (j + 1 < 4 && ranked.get(j + 1).points() == ranked.get(i).points()) {
                j++;
            }
            double umaSum = 0;
            for (int k = i; k <= j; k++) {
                umaSum += umaByRank[k];
            }
            double umaEach = umaSum / (j - i + 1);
            for (int k = i; k <= j; k++) {
                Player p = ranked.get(k);
                double base = p.points() / 1000.0;
                pointsByPlayer[players.indexOf(p)] = base + umaEach;
            }
            i = j + 1;
        }

        for (int rank = 0; rank < 4; rank++) {
            Player p = ranked.get(rank);
            double pt = pointsByPlayer[players.indexOf(p)];
            logger.log(String.format("%d位: %s  %d点  %.1fpt", rank + 1, p.name(), p.points(), pt));
        }
    }
}
