package yoneyone.yone.yo.neocardgames;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

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
                    event.getView().close();
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
            case "§f§lレジェンドカード一覧":
            case "§f§lイベントカード一覧":
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
                    neoBattleSystem.gameMaster(itemStack,player);
                    return;
                }
                if (slot >= 36 && slot <= 44){
                    neoBattleSystem.gameMaster(itemStack,player);
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
}
