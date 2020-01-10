package yoneyone.yone.yo.neocardgames;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class NeoBattleSystem {
    //全てのメソッドで、〇〇する側のプレイヤーを使います
    NeoCardGames neo;
    public NeoBattleSystem(NeoCardGames neo){
        this.neo = neo;
    }

    //メイン処理
    public class GameMaster extends Thread{
        ItemStack itemStack;
        Player player;
        public GameMaster(ItemStack itemStack,Player player) {
            this.itemStack = itemStack;
            this.player = player;
        }

        @Override
        public void run() {
            NeoGameSystem system = new NeoGameSystem(neo);
            Battle battle = system.getBattle(player);
            boolean isPlayerA = system.isPlayerA(player);
            Card card = system.getCard(itemStack);
            battle.isProcessing = true;//処理開始
            UpdateTread tread = new UpdateTread(battle);//updateをするだけのスレッドインスタンス

            system.battleSendMessage(battle,NeoCardGames.prefix + itemStack.getItemMeta().getDisplayName() +"§r§lを使った！");

            battle.center = itemStack;
            Bukkit.getScheduler().runTask(neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            if (itemStack.getType().equals(Material.STONE_SWORD)) {//通常攻撃なら
                normalAttack(player);
                battle.center = null;
                Bukkit.getScheduler().runTask(neo,tread);
                try {//待機時間
                    tread.join();
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                turnEnd(player);
                return;
            }
            //このターン効果
            if (card.now) {
                //ダメージ
                if (card.damageNow != -1) {
                    damageCalculation(player, card.damageNow, DamageType.card, null);
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (gameEndCheck(player)){//もしも攻撃処理で対戦が終了していたら停止する
                    return;
                }
                //回復
                if (card.healNow != -1) {
                    healCalculation(player, card.healNow);
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                //軽減付与
                if (card.reductionNow != -1) {
                    if (isPlayerA) {
                        if (battle.effectA.damageReduction != -1) {
                            battle.effectA.damageReduction += card.reductionNow;
                        } else {
                            battle.effectA.damageReduction = card.reductionNow;
                        }
                    } else {
                        if (battle.effectB.damageReduction != -1) {
                            battle.effectB.damageReduction += card.reductionNow;
                        } else {
                            battle.effectB.damageReduction = card.reductionNow;
                        }
                    }
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (card.drawNow != -1){
                    draw(player,card.drawNow);
                }
            }
            //永続効果
            if (card.permanent){
                ItemStack nextItemStack = itemStack.clone();
                ItemMeta nextItemMeta = nextItemStack.getItemMeta();
                List<String> lore = nextItemMeta.getLore();
                lore.add("§e永続効果");
                nextItemMeta.setLore(lore);
                nextItemStack.setItemMeta(nextItemMeta);
                if (isPlayerA) {//次のターンだと分かるように
                    battle.permanentA = nextItemStack;
                } else {
                    battle.permanentB = nextItemStack;
                }
                Bukkit.getScheduler().runTask(neo,tread);
                try {//待機時間
                    tread.join();
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            //次のターン効果
            if (card.next) {//上書き登録する
                ItemStack nextItemStack = itemStack.clone();
                ItemMeta nextItemMeta = nextItemStack.getItemMeta();
                List<String> lore = nextItemMeta.getLore();
                lore.add("§e次のターン効果");
                nextItemMeta.setLore(lore);
                nextItemStack.setItemMeta(nextItemMeta);
                if (isPlayerA) {//次のターンだと分かるように
                    battle.nextCardA = nextItemStack;
                } else {
                    battle.nextCardB = nextItemStack;
                }
                Bukkit.getScheduler().runTask(neo,tread);
                try {//待機時間
                    tread.join();
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            battle.center = null;
            if (isPlayerA){
                battle.handA.remove(itemStack);
            }else {
                battle.handB.remove(itemStack);
            }
            Bukkit.getScheduler().runTask(neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            turnEnd(player);//ターン終了
        }
    }

    //遅延でこのターン発動する場合はこっち
    public class GameMaster2 extends Thread{
        ItemStack itemStack;
        Player player;
        public GameMaster2(ItemStack itemStack,Player player){
            this.itemStack = itemStack;
            this.player = player;
        }

        @Override
        public void run() {
            List<String> lore = itemStack.getItemMeta().getLore();
            if (lore.isEmpty()){//もしも空なら何もしない
                return;
            }
            NeoGameSystem system = new NeoGameSystem(neo);
            Battle battle = system.getBattle(player);
            boolean isPlayerA = system.isPlayerA(player);
            UpdateTread tread = new UpdateTread(battle);//updateをするだけのスレッドインスタンス

            system.battleSendMessage(battle,NeoCardGames.prefix + itemStack.getItemMeta().getDisplayName() +"§r§lの効果が発動！");

            Card card = system.getCard(itemStack);
            battle.center = itemStack;
            Bukkit.getScheduler().runTask(neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            //永続効果
            if (lore.get(lore.size() - 1).equals("§e永続効果")){
                if (card.damagePermanent != -1){
                    damageCalculation(player,card.damagePermanent,DamageType.card,null);
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (gameEndCheck(player)){//もしも攻撃処理で対戦が終了していたら停止する
                    return;
                }
                if (card.healPermanent != -1){
                    healCalculation(player,card.healPermanent);
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (card.reductionPermanent != -1){//軽減付与
                    if (isPlayerA){
                        if (battle.effectA.damageReduction != -1){
                            battle.effectA.damageReduction += card.reductionPermanent;
                        }else {
                            battle.effectA.damageReduction = card.reductionPermanent;
                        }
                    }else {
                        if (battle.effectB.damageReduction != -1){
                            battle.effectB.damageReduction += card.reductionPermanent;
                        }else {
                            battle.effectB.damageReduction = card.reductionPermanent;
                        }
                    }
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (card.drawPermanent != -1){//ドロー
                    draw(player,card.drawPermanent);
                }
            }
            //次のターン発動する効果
            if (lore.get(lore.size() - 1).equals("§e次のターン効果")){
                if (card.damageNext != -1){
                    damageCalculation(player,card.damageNext,DamageType.card,null);
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (gameEndCheck(player)){//もしも攻撃処理で対戦が終了していたら停止する
                    return;
                }
                if (card.healNext != -1){
                    healCalculation(player,card.healNext);
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (card.reductionNext != -1){//軽減付与
                    if (isPlayerA){
                        if (battle.effectA.damageReduction != -1){
                            battle.effectA.damageReduction += card.reductionNext;
                        }else {
                            battle.effectA.damageReduction = card.reductionNext;
                        }
                    }else {
                        if (battle.effectB.damageReduction != -1){
                            battle.effectB.damageReduction += card.reductionNext;
                        }else {
                            battle.effectB.damageReduction = card.reductionNext;
                        }
                    }
                    Bukkit.getScheduler().runTask(neo,tread);
                    try {//待機時間
                        tread.join();
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (card.drawNext != -1){//ドロー
                    draw(player,card.drawNext);
                }
            }
            battle.center = null;
            Bukkit.getScheduler().runTask(neo,tread);
        }
    }

    //ターン終了と次のターン開始
    public void turnEnd(Player player){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        battle.isProcessing = true;//処理開始
        UpdateTread tread = new UpdateTread(battle);

        battle.center = null;//中央をリセット
        Bukkit.getScheduler().runTask(this.neo,tread);

        //対戦終了かチェック
        if (gameEndCheck(player)){
            return;//終了なら何もしない
        }

        //付与効果削除
        if (system.isPlayerA(player)) {
            //相手の効果削除
            battle.effectB.damageReduction = -1;
            //自分の効果削除
        }else {
            //相手の効果削除
            battle.effectA.damageReduction = -1;
            //自分の効果削除
        }

        battle.isATurn = !battle.isATurn;//相手のターンにする

        system.startTurn(player);//ターン開始
    }

    //どちらかもしくは両方のHPが0になっているかどうか
    public boolean gameEndCheck(Player player){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        trimHP(battle);//チェック
        int HPA = battle.HPA;
        int HPB = battle.HPB;
        if (HPA == 0 && HPB == 0){
            drawGame(player);
            return true;
        }else if (HPA == 0){
            defeat(battle.playerA);
            return true;
        }else if (HPB == 0){
            defeat(battle.playerB);
            return true;
        }
        return false;
    }

    //カードを引く（複数）
    public void draw(Player player,int amount){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        UpdateTread tread = new UpdateTread(battle);//updateをするだけのスレッドインスタンス
        system.battleSendMessage(battle,NeoCardGames.prefix + player.getName() +"§lがカードを"+ amount +"回ドロー！");
        for (int i = 0;i < amount;i++){
            draw(player);
            Bukkit.getScheduler().runTask(neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    //カードを引く
    public void draw(Player player){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        boolean isPlayerA = system.isPlayerA(player);

        //相手が攻撃してる判定
        if (isPlayerA){
            if (!battle.deckA.isEmpty()){
                if (battle.handA.size() == 9){//手札がいっぱいだったら
                    battle.playerA.sendMessage(NeoCardGames.prefix +"§c§l手札がいっぱいです、カードが消えます");
                    battle.deckA.remove(0);
                }else {
                    battle.handA.add(battle.deckA.remove(0));
                }
            }else {
                battle.playerA.sendMessage(NeoCardGames.prefix +"§c§lデッキ切れでカードをドローできませんでした");
                damageCalculation(Bukkit.getPlayer(system.getPlayer(player)),1,DamageType.other,"デッキ切れ");
            }
        }else {
            if (!battle.deckB.isEmpty()){
                if (battle.handB.size() == 9){
                    battle.playerB.sendMessage(NeoCardGames.prefix +"§c§l手札がいっぱいです、カードが消えます");
                    battle.deckB.remove(0);
                }else {
                    battle.handB.add(battle.deckB.remove(0));
                }
            }else {
                battle.playerB.sendMessage(NeoCardGames.prefix +"§c§lデッキ切れでカードをドローできませんでした");
                damageCalculation(Bukkit.getPlayer(system.getPlayer(player)),1,DamageType.other,"デッキ切れ");
            }
        }
    }

    //通常攻撃
    private void normalAttack(Player player){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);

        system.battleSendMessage(battle,NeoCardGames.prefix +"§l"+ player.getName() +"の通常攻撃！");
        try {//待機時間
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        damageCalculation(player,1,DamageType.normal,null);
    }

    //回復計算と処理
    private void healCalculation(Player player,int defaultAmount){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        boolean isPlayerA = system.isPlayerA(player);

        //効果習得
        GrantEffect myEffect;
        GrantEffect opponentEffect;
        if (isPlayerA){
            myEffect = battle.effectA;
            opponentEffect = battle.effectB;
        }else {
            myEffect = battle.effectB;
            opponentEffect = battle.effectA;
        }

        //強化
        //（現在はない）
        //弱体
        //（現在はない）
        //調整
        if (defaultAmount < 0){//マイナスにはならない
            defaultAmount = 0;
        }
        //自分に発動
        if (isPlayerA){
            battle.HPA += defaultAmount;
        }else{
            battle.HPB += defaultAmount;
        }
        system.battleSendMessage(battle,NeoCardGames.prefix +"§a§l"+ player.getName() +"のHPが"+ defaultAmount +"回復した！");
    }

    //与えるダメージ計算と処理
    private void damageCalculation(Player player,int defaultAmount,DamageType type,String reason){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        boolean isPlayerA = system.isPlayerA(player);

        //効果習得
        GrantEffect myEffect;
        GrantEffect opponentEffect;
        if (isPlayerA){
            myEffect = battle.effectA;
            opponentEffect = battle.effectB;
        }else {
            myEffect = battle.effectB;
            opponentEffect = battle.effectA;
        }

        //強化
        //（現在はない）
        //耐性
        if (opponentEffect.damageReduction != -1){
            defaultAmount -= opponentEffect.damageReduction;
        }
        //調整
        if (defaultAmount < 0){//マイナスにはならない
            defaultAmount = 0;
        }

        //相手に発動
        if (isPlayerA){
            battle.HPB -= defaultAmount;
        }else{
            battle.HPA -= defaultAmount;
        }
        if (reason == null){
            system.battleSendMessage(battle,NeoCardGames.prefix +"§c§l"+ system.getPlayer(player) +"に"+ defaultAmount +"のダメージ！");
        }else {
            system.battleSendMessage(battle,NeoCardGames.prefix +"§c§l"+ system.getPlayer(player) +"に"+ reason +"で"+ defaultAmount +"のダメージ！");
        }
    }

    //負け
    public void defeat(Player player){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        String winnerName = system.getPlayer(player);
        system.battleSendMessage(battle,NeoCardGames.prefix +"勝者："+ winnerName +"！！");
        battle.type = BattleProcessType.End;
        system.stopBattle(player);
    }

    //引き分け
    public void drawGame(Player player){
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        system.battleSendMessage(battle, NeoCardGames.prefix +"両者敗北…");
        battle.type = BattleProcessType.End;
        system.stopBattle(player);
    }

    //HPが上限下限を越さないように整える
    private void trimHP(Battle battle){
        int HPA = battle.HPA;
        int HPB = battle.HPB;
        if (HPA< 0) {//0未満ならば
            HPA = 0;
        }else if (HPA > 10) {//10より大きければ
            HPA = 10;
        }
        if (HPB< 0) {//0未満ならば
            HPB = 0;
        }else if (HPB > 10) {//10より大きければ
            HPB = 10;
        }
        battle.HPA = HPA;
        battle.HPB = HPB;
    }

    //画面更新（開いてる人だけ）
    //メインスレッドで開いてください
    private void screenUpdate(Battle battle){
        trimHP(battle);
        NeoBattleGUI GUI = new NeoBattleGUI(neo);
        InventoryView viewA = battle.playerA.getOpenInventory();
        if (viewA != null){
            if (viewA.getTitle().equals("§lNeoBattleGUI")){//開いていたら
                //GUI.battleGUI(battle.playerA);
                GUI.updateBattleGUI(viewA.getTopInventory(),battle.playerA);
            }
        }
        InventoryView viewB = battle.playerB.getOpenInventory();
        if (viewB != null){
            if (viewB.getTitle().equals("§lNeoBattleGUI")){//開いていたら
                //GUI.battleGUI(battle.playerB);
                GUI.updateBattleGUI(viewB.getTopInventory(),battle.playerB);
            }
        }
        for (String name:battle.spectators){
            Player player = Bukkit.getPlayer(name);
            InventoryView viewSp = player.getOpenInventory();
            if (viewSp != null){
                if (viewSp.getTitle().equals("§lNeoSpectatorGUI")){//開いていたら
                    //GUI.battleGUI(battle.playerA,player);
                    GUI.updateBattleGUI(viewSp.getTopInventory(),battle.playerA,player);
                }
            }
        }
    }
    public class UpdateTread extends Thread{
        Battle battle;
        public UpdateTread(Battle battle){
            this.battle = battle;
        }

        @Override
        public void run() {
            try {
                screenUpdate(battle);
            }catch (Exception e){
                Bukkit.getLogger().info(e.getMessage());
            }
        }
    }
    public class StartDrawThread extends Thread{
        Battle battle;
        NeoCardGames neo;
        public StartDrawThread(Battle battle,NeoCardGames neo){
            this.battle = battle;
            this.neo = neo;
        }

        @Override
        public void run() {
            UpdateTread tread = new UpdateTread(battle);
            try {//待機時間
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            draw(battle.playerA);
            draw(battle.playerB);
            Bukkit.getScheduler().runTask(this.neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            draw(battle.playerA);
            draw(battle.playerB);
            Bukkit.getScheduler().runTask(this.neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            draw(battle.playerB);
            Bukkit.getScheduler().runTask(this.neo,tread);
            try {//待機時間
                tread.join();
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
enum DamageType{
    normal,card,other
}
