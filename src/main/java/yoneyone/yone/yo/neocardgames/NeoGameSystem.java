package yoneyone.yone.yo.neocardgames;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class NeoGameSystem {
    static CopyOnWriteArrayList<Battle> games = new CopyOnWriteArrayList<>();//ゲーム一覧
    NeoCardGames neo;//プラグイン
    static final String NEO = "§c§l§kxx§r§c§l！NEO！§kxx";
    private int nextGameID = 0;//取る時に1増やす

    public NeoGameSystem(NeoCardGames neo){
        this.neo = neo;
    }

    public void battleApply(Player challenger,Player opponent){
        Battle battle = new Battle();
        Random random = new Random();
        boolean first = random.nextBoolean();//挑戦者が先攻かどうか
        if (first){
            battle.playerA = challenger;
            battle.playerB = opponent;
        }else {
            battle.playerB = challenger;
            battle.playerA = opponent;
        }
        battle.ID = getNextBattleID();
        games.add(battle);
        challenger.sendMessage(NeoCardGames.prefix +"§6§l"+ opponent.getName() +"§f§lさんに対戦を申し込みました");
        opponent.sendMessage(NeoCardGames.prefix +"§6§l"+ challenger.getName() +"§f§lさんに対戦を申し込まれました");
        opponent.sendMessage(NeoCardGames.prefix +"§f§l/neo accept で対戦を了承できます");
        opponent.sendMessage(NeoCardGames.prefix +"§f§l/neo cancel で対戦を断れます（1分経過で自動キャンセル）");
        Bukkit.getScheduler().runTaskAsynchronously(neo,() -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
            }
            Battle battle2 = getBattle(challenger);
            if (battle2 == null){
                return;
            }
            //参加してなければ中止
            if (battle2.ID == battle.ID){//同じ試合で
                if (battle2.type.equals(BattleProcessType.Before)){//開始前なら
                    challenger.sendMessage(NeoCardGames.prefix +"§c§l時間切れで終了処理に入ります");
                    opponent.sendMessage(NeoCardGames.prefix +"§c§l時間切れで終了処理に入ります");
                    stopBattle(challenger);//参加中止処理
                }
            }
        });
    }

    public Battle getBattle(Player player){
        for (Battle battle:games){
            if (battle.playerA.getName().equals(player.getName())){
                return battle;
            }
            if (battle.playerB.getName().equals(player.getName())){
                return battle;
            }
        }
        return null;
    }

    public boolean isPlayerA(Player player){
        return getBattle(player).playerA.getName().equals(player.getName());
    }

    public void removeSpectators(Player player){//すべての観戦を取り消し
        for (Battle battle:games){
            for (String spectatorsName:battle.spectators){
                if (spectatorsName.equals(player.getName())){
                    battle.spectators.remove(player.getName());
                }
            }
        }
    }

    private int getNextBattleID(){
        nextGameID += 1;
        return nextGameID;
    }

    public void startBattle(Player challenger,Player opponent){
        getBattle(challenger).type = BattleProcessType.Preparation;
        challenger.sendMessage(NeoCardGames.prefix +"§a対戦準備に入ります、制限時間は1分です§kxxx");
        opponent.sendMessage(NeoCardGames.prefix +"§a対戦準備に入ります、制限時間は1分です§kxxx");
        //カードGUIを開く（openで再度可能）
        NeoBattleGUI GUI = new NeoBattleGUI(this.neo);
        GUI.getCardGUI(challenger);
        GUI.getCardGUI(opponent);
        int ID = getBattle(challenger).ID;
        Bukkit.getScheduler().runTaskAsynchronously(this.neo,() -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
            }
            Battle battle = getBattle(challenger);
            if (battle == null){
                return;
            }
            if (battle.ID == ID) {
                if (battle.type.equals(BattleProcessType.Preparation)) {
                    challenger.sendMessage(NeoCardGames.prefix +"§c§l時間切れで終了処理に入ります");
                    opponent.sendMessage(NeoCardGames.prefix +"§c§l時間切れで終了処理に入ります");
                    stopBattle(challenger);
                }
            }
        });
    }

    public void startMatch(Player player){
        Battle battle = getBattle(player);
        battle.type = BattleProcessType.Running;
        battleSendMessage(battle,NeoCardGames.prefix +"§a両者のデッキが揃いました、対戦を開始します§kxxx");
        NeoBattleGUI GUI = new NeoBattleGUI(this.neo);
        //対戦開始前処理
        List<ItemStack> deckA = new ArrayList<>(battle.cardA);
        List<ItemStack> deckB = new ArrayList<>(battle.cardB);
        Collections.shuffle(deckA);
        Collections.shuffle(deckB);
        battle.deckA = deckA;
        battle.deckB = deckB;
        //対戦開始時処理
        GUI.battleGUI(battle.playerA);//開く
        GUI.battleGUI(battle.playerB);//こっちも
        for (String name:battle.spectators){//観戦者も開く
            Player spectator = Bukkit.getPlayer(name);
            if (spectator != null){
                GUI.battleGUI(battle.playerA,spectator);
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.neo,() -> {
            //ゲーム開始時のドロー
            NeoBattleSystem battleSystem = new NeoBattleSystem(neo);
            NeoBattleSystem.StartDrawThread thread = battleSystem.new StartDrawThread(battle,neo);
            thread.start();
            try {
                thread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            //ターン開始
            battleSendMessage(battle,NeoCardGames.prefix +"§aそれでは、スタート！！");
            startTurn(player);
        });
    }

    public void stopBattle(Player player){
        Battle battle = getBattle(player);
        if (battle == null){//バトルが存在しなければ終了
            return;
        }
        if (battle.type.equals(BattleProcessType.Running)){//試合中ならば
            NeoBattleSystem battleSystem = new NeoBattleSystem(neo);
            battleSendMessage(battle,NeoCardGames.prefix +"§4§l"+ player.getName() +"が対戦を放棄したため、終了します");
            battleSystem.defeat(player);
            games.remove(battle);
            return;
        }else if (battle.type.equals(BattleProcessType.End)){//試合終了時ならば
            battleSendMessage(battle, NeoCardGames.prefix +"§l対戦が終了しました");
            games.remove(battle);
        }else {//それ以外ならば
            battleSendMessage(battle,NeoCardGames.prefix +"§l対戦が停止しました"
                    ,NeoCardGames.prefix +"§l"+ battle.playerA.getName() +"対"+ battle.playerB.getName() +"の対戦が停止しました");
        }
        games.remove(battle);//対戦情報を削除
    }

    public String getPlayer(Player player){
        Battle battle = getBattle(player);
        if (battle == null){
            return null;
        }
        if (isPlayerA(player)){//相手をget
            return battle.playerB.getName();
        }else {
            return battle.playerA.getName();
        }
    }

    public String playCheck(Player player){//対戦状態のStringをget
        Battle battle = getBattle(player);
        if (battle != null) {
            String process = "終了処理中";
            if (battle.type.equals(BattleProcessType.Preparation)) {
                process = "開始準備中";
            } else if (battle.type.equals(BattleProcessType.Running)) {
                process = "試合中";
            } else if (battle.type.equals(BattleProcessType.Before)) {
                process = "対戦申し込み中";
            }
            return process;
        }
        return "何もしていません";
    }

    public void returnItem(Inventory inv,Player player){
        ItemStack[] itemStacks = inv.getContents();
        List<ItemStack> returnItemStacks = new ArrayList<>();//返す用
        for (ItemStack itemStack:itemStacks) {
            if (itemStack == null) {
                continue;
            }
            if (itemStack.getType().equals(Material.APPLE)) {
                if (itemStack.getItemMeta().getDisplayName().equals("§c§l決定ボタン")) {
                    continue;//決定ボタンがインベントリに加えられない用
                }
            }
            returnItemStacks.add(itemStack);
        }
        //配列変換からのインベントリに追加
        ItemStack[] returnItemStacks2 = new ItemStack[returnItemStacks.size()];
        for (int i = 0;i < returnItemStacks2.length;i++){
            returnItemStacks2[i] = returnItemStacks.get(i);
        }
        player.getInventory().addItem(returnItemStacks2);
        player.sendMessage(NeoCardGames.prefix +"§lインベントリにカードを戻しました");
    }

    public void cardCheck(Inventory inv,Player player){
        ItemStack[] itemStacks = inv.getContents();
        List<ItemStack> cards = new ArrayList<>();//デッキ用
        boolean canDeck = true;
        for (ItemStack itemStack:itemStacks){
            if (itemStack == null){
                continue;
            }
            ItemStack cardItemStack = itemStack.clone();
            if (!cardItemStack.getType().equals(Material.DIAMOND_HOE)){
                continue;
            }
            //不正して盗った時の防止のため、試合中はロールの一番上に目印が付きます
            if (cardItemStack.getItemMeta().getLore().get(0).equals(NEO)){//違法カードの場合読み取れない
                continue;
            }
            if(isCard(cardItemStack)){
                ItemMeta itemMeta = cardItemStack.getItemMeta();
                List<String> lore = itemMeta.getLore();
                lore.add(0,NEO);
                itemMeta.setLore(lore);
                cardItemStack.setItemMeta(itemMeta);
                //目印セット完了
                cards.add(cardItemStack);
            }
        }

        if (cards.size() == 15){
            //入れていいカードかかチェック
            List<String> checkList1 = new ArrayList<>();
            List<String> checkList2 = new ArrayList<>();
            for (ItemStack itemStack:cards){
                if (!checkList1.contains(itemStack.getItemMeta().getDisplayName())){
                    checkList1.add(itemStack.getItemMeta().getDisplayName());
                }else {
                    Card card = getCard(itemStack);
                    String rarity = card.rarity;
                    if (rarity.equals("スーパーレア") || rarity.equals("レジェンド") || rarity.equals("シークレット")){//スーパーレア以上の時の処理
                        player.sendMessage(NeoCardGames.prefix +"§l同じカードは2枚までしか入れられません");
                        player.sendMessage(NeoCardGames.prefix +"§lスーパーレア以上のカードは1枚までです");
                        canDeck = false;
                        break;
                    }
                    if (!checkList2.contains(itemStack.getItemMeta().getDisplayName())){
                        checkList2.add(itemStack.getItemMeta().getDisplayName());
                    }else {
                        player.sendMessage(NeoCardGames.prefix +"§l同じカードは2枚までしか入れられません");
                        player.sendMessage(NeoCardGames.prefix +"§lスーパーレア以上のカードは1枚までです");
                        canDeck = false;
                        break;
                    }
                }
            }
        }else {
            player.sendMessage(NeoCardGames.prefix +"§lデッキの枚数が異常です、15枚ぴったり入れてください");
            canDeck = false;
        }
        if (canDeck){//もしも問題が無ければ実装
            Battle battle = getBattle(player);
            if (battle == null){
                player.sendMessage(NeoCardGames.prefix +"§4§lエラー§fあなたの参加している対戦が見つかりません");
            }else {
                if (isPlayerA(player)){
                    battle.cardA = cards;
                }else {
                    battle.cardB = cards;
                }
                player.sendMessage(NeoCardGames.prefix +"デッキを登録しました");
            }
        }else {
            player.sendMessage(NeoCardGames.prefix +"§4デッキを登録できませんでした");
        }
        //両方ともセット完了ならば、次のプロセスへ
        Battle battle = getBattle(player);
        if (battle != null){
            if (battle.cardA != null && battle.cardB != null){
                startMatch(player);
            }
        }
    }

    public ItemStack getItemStack(Card card){
        return getItemStack(card,false);
    }

    public ItemStack getItemStack(Card card,boolean isNeo){
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta hoeMeta = hoe.getItemMeta();
        hoeMeta.setDisplayName(card.name);
        if (isNeo){//NEO表示ありかどうか
            List<String> lore = new ArrayList<>();
            lore.add(NEO);
            lore.addAll(card.lore);
            hoeMeta.setLore(lore);
        }else {
            hoeMeta.setLore(card.lore);
        }
        hoeMeta.setUnbreakable(true);
        hoeMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);//後で能力隠しも付けます
        hoe.setItemMeta(hoeMeta);
        //レアリティでテクスチャを変える
        hoe.setDurability(card.cardDurability);
        return hoe;
    }

    public boolean isCard(ItemStack itemStack){
        return getCard(itemStack) != null;
    }

    public Card getCard(ItemStack itemStack){
        if (!itemStack.getType().equals(Material.DIAMOND_HOE)) {
            return null;
        }
        String name = itemStack.getItemMeta().getDisplayName();
        if (name == null){
            return null;
        }
        short durability = itemStack.getDurability();
        if (durability == NeoCardGames.normalCardDurability || durability == NeoCardGames.rareCardDurability
                || durability == NeoCardGames.legendCardDurability || durability == NeoCardGames.eventCardDurability){//ノーマル
            for (Card card: NeoCardGames.cards){
                if (name.equals(card.name)){
                    List<String> lore = itemStack.getItemMeta().getLore();
                    if (lore != null){//ロールがないカードなんてない
                        if (lore.size() != 0){//長さが0はないけど、一応
                            return card;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void battleSendMessage(Battle battle,String message){
        battleSendMessage(battle,message,message);
    }

    public void battleSendMessage(Battle battle,String playerSendMessage,String spectatorSendMessage){
        Player playerA = battle.playerA;
        Player playerB = battle.playerB;
        playerA.sendMessage(playerSendMessage);
        playerB.sendMessage(playerSendMessage);
        List<String> spectators = battle.spectators;
        for (String name:spectators){
            Player player = Bukkit.getPlayer(name);
            if (player != null){
                player.sendMessage(spectatorSendMessage);
            }
        }
    }

    public void startTurn(Player player){
        Bukkit.getScheduler().runTaskAsynchronously(this.neo,() -> {
            Battle battle = getBattle(player);
            Player turnPlayer;
            if (battle.isATurn) {
                turnPlayer = battle.playerA;
            } else {
                turnPlayer = battle.playerB;
            }
            NeoBattleSystem battleSystem = new NeoBattleSystem(neo);
            battle.turn += 1;//開始時にターンカウントを1増やす
            battleSendMessage(battle, NeoCardGames.prefix + "§6§l" + battle.turn + "ターン目");
            battleSendMessage(battle, NeoCardGames.prefix + "§6§l" + turnPlayer.getName() + "さんのターンです");
            battleSystem.draw(turnPlayer);//カードを引く
            NeoBattleSystem.UpdateTread tread = battleSystem.new UpdateTread(battle);
            Bukkit.getScheduler().runTask(this.neo,tread);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            if (battleSystem.gameEndCheck(player)){//もしもデッキ切れ処理で対戦が終了していたら停止する
                return;
            }
            Battle battle2 = getBattle(player);
            ItemStack permanentToNow;//永続効果
            if (battle2.isATurn) {
                permanentToNow = battle2.permanentA;
            } else {
                permanentToNow = battle2.permanentB;
            }
            if (permanentToNow != null) {//発動する効果があるならば
                NeoBattleSystem.GameMaster2 gameMaster2 = battleSystem.new GameMaster2(permanentToNow,turnPlayer);
                gameMaster2.start();
                try {
                    gameMaster2.join();
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                //使用終了
                //永続なので消えない
                Bukkit.getScheduler().runTask(this.neo,tread);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }//更新中に次のステップに進まない予防用
            }
            ItemStack nextToNow;
            if (battle2.isATurn) {
                nextToNow = battle2.nextCardA;
            } else {
                nextToNow = battle2.nextCardB;
            }
            if (nextToNow != null) {//このターン発動する効果があるならば
                NeoBattleSystem.GameMaster2 gameMaster2 = battleSystem.new GameMaster2(nextToNow,turnPlayer);
                gameMaster2.start();
                try {
                    gameMaster2.join();
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                //使用終了
                if (getBattle(player).isATurn) {
                    battle2.nextCardA = null;
                } else {
                    battle2.nextCardB = null;
                }
                Bukkit.getScheduler().runTask(this.neo,tread);
            }
            battle2.isProcessing = false;//プレイヤーを待っている

            int turn = battle2.turn;
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ignored) {
            }
            if (getBattle(player) == null || turn != battle2.turn || battle2.isProcessing) {//別のターンもしくは処理中なら終了
                return;
            }
            battleSendMessage(battle2, NeoCardGames.prefix + "§b§lあと30秒です§kxxxx");
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ignored) {
            }
            if (getBattle(player) == null || turn != battle2.turn || battle2.isProcessing) {
                return;
            }
            battleSendMessage(battle2, NeoCardGames.prefix + "§b§lあと15秒です§kxxx");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
            if (getBattle(player) == null || turn != battle2.turn || battle2.isProcessing) {
                return;
            }
            battleSendMessage(battle2, NeoCardGames.prefix + "§b§lあと5秒です§kxx");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            if (getBattle(player) == null || turn != battle2.turn || battle2.isProcessing) {
                return;
            }
            battleSendMessage(battle2, NeoCardGames.prefix + "§b§lあと3秒です§kx");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            if (getBattle(player) == null || turn != battle2.turn || battle2.isProcessing) {
                return;
            }
            battleSendMessage(battle2, NeoCardGames.prefix + "§b§l時間切れ！");
            battle2.isProcessing = true;
            battleSendMessage(battle2, NeoCardGames.prefix + "§b§l反則カウントをします（2回目以降は負け）");
            //ここに時間切れ処理
            //反則回数をカウント
            if (battle2.isATurn) {
                battle2.offenseA += 1;
                if (battle2.offenseA >= 2) {//2以上なら反則負け
                    battleSystem.defeat(turnPlayer);
                    return;
                }
            } else {
                battle2.offenseB += 1;
                if (battle2.offenseB >= 2) {
                    battleSystem.defeat(turnPlayer);
                    return;
                }
            }
            NeoBattleGUI GUI = new NeoBattleGUI(neo);
            NeoBattleSystem.GameMaster gameMaster = battleSystem.new GameMaster(GUI.createItemStack(Material.STONE_SWORD, "§a§l通常攻撃"), turnPlayer);
            //通常攻撃をする
            gameMaster.start();
        });
    }
}
