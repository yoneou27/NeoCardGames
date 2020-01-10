package yoneyone.yone.yo.neocardgames;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeoListener implements Listener {
    NeoCardGames neo;//プラグイン
    public NeoListener(NeoCardGames neo){
        this.neo = neo;
    }

    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event){
        if (!checkInventoryAndPlayer(event)){
            return;
        }
        Player player = (Player) event.getView().getPlayer();
        NeoBattleGUI GUI = new NeoBattleGUI(this.neo);
        String title = event.getView().getTitle();
        switch (title){
            case "§lカードを中に入れてください":
                if (!checkItem(event.getCurrentItem())){
                    return;
                }
                if (event.getCurrentItem().getItemMeta().getDisplayName().equals("§c§l決定ボタン")){
                    event.setCancelled(true);
                    NeoGameSystem system = new NeoGameSystem(this.neo);
                    system.cardCheck(event.getInventory(),(Player) event.getView().getPlayer());
                    if (player.getOpenInventory() != null){
                        if (player.getOpenInventory().getTitle().equals("§lカードを中に入れてください")){
                            event.getView().close();
                        }
                    }
                    return;
                }
                break;
            case "§lカード一覧メニュー":
                event.setCancelled(true);
                if (!checkItem(event.getCurrentItem())){
                    return;
                }
                GUI.cardList(player,event.getCurrentItem().getItemMeta().getDisplayName());
                break;
            case "§f§lノーマルカード一覧":
            case "§f§lレアカード一覧":
            case "§f§lスーパーレアカード一覧":
            case "§f§lレジェンドカード一覧":
            case "§f§lシークレットカード一覧":
            case "§f§lその他カード一覧":
            case "§f§lカードパック一覧":
                if (event.getSlot() >= 45){
                    event.setCancelled(true);
                    if (!checkItem(event.getCurrentItem())){
                        return;
                    }
                    String[] names = event.getCurrentItem().getItemMeta().getDisplayName().split("ペ");//1ページへ の「ペ」
                    int num = Integer.parseInt(names[0]);
                    GUI.cardList(player,event.getView().getTitle(),num);
                }
                break;
            case "§lNeoSpectatorGUI":
                event.setCancelled(true);
                break;
            case "§lNeoBattleGUI":
                event.setCancelled(true);
                int slot = event.getSlot();
                NeoBattleSystem neoBattleSystem = new NeoBattleSystem(neo);
                NeoGameSystem system = new NeoGameSystem(neo);
                if (system.getBattle(player) == null){//対戦終了していたら何も出来ない
                    return;
                }
                if (system.getBattle(player).isProcessing){//処理中ならば何も出来ない
                    return;
                }
                if (!checkItem(event.getCurrentItem())){//何もクリックしていなければリターン
                    return;
                }
                Battle battle = system.getBattle(player);
                if (system.isPlayerA(player)){//相手のターンなら何も出来ない
                    if (!battle.isATurn){
                        return;
                    }
                }else {
                    if (battle.isATurn){
                        return;
                    }
                }
                ItemStack itemStack = event.getCurrentItem();
                if (slot == 29){
                    NeoBattleSystem.GameMaster gameMaster = neoBattleSystem.new GameMaster(itemStack,player);
                    gameMaster.start();
                    return;
                }
                if (slot >= 36 && slot <= 44){
                    NeoBattleSystem.GameMaster gameMaster = neoBattleSystem.new GameMaster(itemStack,player);
                    gameMaster.start();
                    return;
                }
                break;
        }
    }
    @EventHandler
    public void InventoryCloseEvent(InventoryCloseEvent event){
        NeoGameSystem system = new NeoGameSystem(this.neo);
        String name = event.getInventory().getName();
        switch (name){
            case "§lカードを中に入れてください":
                system.returnItem(event.getInventory(),(Player) event.getPlayer());
                break;
        }
    }

    private boolean checkInventoryAndPlayer(InventoryClickEvent event){
        int slot = event.getSlot();
        if (event.getView() == null) return false;
        if (slot == -1) return false;
        if (event.getView().getItem(slot) == null) return false;
        if (event.getView().getItem(slot).getItemMeta() == null) return false;
        return event.getView().getPlayer() instanceof Player;
    }
    private boolean checkItem(ItemStack itemStack){
        if (itemStack == null)return false;
        if (itemStack.getItemMeta() == null)return false;
        return itemStack.getItemMeta().getDisplayName() != null;
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event){
        NeoGameSystem system = new NeoGameSystem(this.neo);
        Player player = event.getPlayer();
        //観戦者チェック
        system.removeSpectators(player);
        //対戦者チェック
        system.stopBattle(player);
    }
    //カードパック用
    @EventHandler
    public void PlayerInteractEvent(PlayerInteractEvent event){
        if (event.getItem() == null) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack eventItem = event.getItem();
        if (eventItem.getItemMeta() == null) {
            return;
        }
        if (!eventItem.getType().equals(Material.DIAMOND_HOE)){//ダイヤ鍬以外ははじく
            return;
        }
        if (eventItem.getItemMeta().getDisplayName() == null) {
            return;
        }
        String itemName = eventItem.getItemMeta().getDisplayName();
        if (eventItem.getDurability() != NeoCardGames.cardPackDurability){
            return;
        }
        CardPack cardPack = null;
        List<CardPack> packs = NeoCardGames.cardPacks;
        for (CardPack checkPack:packs){
            if (checkPack.name.equals(itemName)){
                cardPack = checkPack;
            }
        }
        if (cardPack == null){//存在しないカードパックなら
            return;
        }
        Player player = event.getPlayer();
        if (!NeoCardGames.battleStart){
            player.sendMessage(NeoCardGames.prefix +"§l現在カードゲームは停止しています");
            return;
        }
        if (cardPack.allAmount == 0){
            player.sendMessage(NeoCardGames.prefix +"§lこのカードパックは排出されるカードが設定されていません");
            return;
        }
        //以下開封処理
        player.sendMessage(NeoCardGames.prefix + cardPack.name +"§r§l§aを開封！");
        eventItem.setAmount(eventItem.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 2.0F);//チェストを開ける音2倍速で効果音
        for (int i = 0;i < cardPack.cardAmount;i++){
            Random random = new Random();
            int result = random.nextInt(cardPack.allAmount) + 1;//乱数の取得
            //どのカードが出るか
            int allAmount = 0;
            boolean isFinish = false;
            List<Card> checkEndCards = new ArrayList<>();//処理済みチェック
            for (Card card:cardPack.specialProbability.keySet()){//各カードのチェック
                allAmount += cardPack.specialProbability.get(card);//見つけたら追加
                checkEndCards.add(card);
                if (result <= allAmount){
                    ItemStack itemStack = (new NeoGameSystem(neo)).getItemStack(card);
                    player.getInventory().addItem(itemStack);
                    if (card.rarity.equals("シークレット")){
                        Bukkit.broadcastMessage(NeoCardGames.prefix +"§l§6"+ player.getName() +"さんがシークレットカード§r"+ card.name +"§r§l§6を当てました！§kxxx");
                    }
                    isFinish = true;
                    break;
                }
            }
            if (isFinish){
                continue;
            }
            for (String rarityName:cardPack.defaultProbability.keySet()){//レアリティごとのチェック
                int amount = cardPack.defaultProbability.get(rarityName);
                for (Card card:NeoCardGames.cards){
                    if (card.rarity.equals(rarityName)){//該当のレアリティならば
                        if (!checkEndCards.contains(card)){//個別設定されていなければ
                            allAmount += amount;
                            if (result <= allAmount){
                                ItemStack itemStack = (new NeoGameSystem(neo)).getItemStack(card);
                                player.getInventory().addItem(itemStack);
                                if (card.rarity.equals("シークレット")){
                                    Bukkit.broadcastMessage(NeoCardGames.prefix +"§l§6"+ player.getName() +"さんがシークレットカード§r"+ card.name +"§r§l§6を当てました！§kxxx");
                                }
                                isFinish = true;
                                break;
                            }
                        }
                    }
                }
                if (isFinish){
                    break;
                }
            }
            if (isFinish){
                continue;
            }
            player.sendMessage(NeoCardGames.prefix +"§4§lエラー:カードが排出されませんでした、管理者に報告してください");
        }
    }
}
