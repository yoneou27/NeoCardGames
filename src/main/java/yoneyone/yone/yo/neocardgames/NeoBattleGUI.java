package yoneyone.yone.yo.neocardgames;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NeoBattleGUI {
    NeoCardGames neo;//プラグイン

    public NeoBattleGUI(NeoCardGames neo){
        this.neo = neo;
    }

    public void openChoice(Player player,String type){
        switch (type) {
            case "開始準備中":
                getCardGUI(player);
                break;
            case "試合中":
                battleGUI(player);
                break;
            case "終了処理中":
                player.sendMessage(NeoCardGames.prefix +"§l対戦終了のGUIはありません");
                break;
        }
    }

    public void getCardGUI(Player player){//開始前のカードをスキャンする部分
        //既に登録されていたらはじく
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        if (battle == null){
            return;
        }
        if (system.isPlayerA(player)){
            if (battle.cardA != null){
                player.sendMessage(NeoCardGames.prefix +"既にデッキが登録されていますので、開けません");
                return;
            }
        }else {
            if (battle.cardB != null){
                player.sendMessage(NeoCardGames.prefix +"既にデッキが登録されていますので、開けません");
                return;
            }
        }

        Inventory inv = Bukkit.createInventory(null,18,"§lカードを中に入れてください");
        List<String> lore = new ArrayList<>();
        lore.add("§f使うカードを15枚ピッタリ入れて");
        lore.add("§fこれを押すと決定できます");
        inv.setItem(17,createItemStack(Material.APPLE,"§c§l決定ボタン",lore));
        player.openInventory(inv);
    }

    public void battleGUI(Player player){//自分で自分の試合を見る
        battleGUI(player,player);
    }

    public void battleGUI(Player player,Player spectator){//対戦のメインGUI
        NeoGameSystem system = new NeoGameSystem(neo);
        Battle battle = system.getBattle(player);
        if (battle == null){
            spectator.sendMessage(NeoCardGames.prefix +"§4エラーが発生しました、GUIを開けません");
            return;
        }
        boolean isPlayerA = system.isPlayerA(player);
        boolean isPlayer;
        Inventory inv;
        if (player.getName().equals(spectator.getName())){//自分を見ているならば
            inv = Bukkit.createInventory(null,54,"§lNeoBattleGUI");
            isPlayer = true;
        }else {
            inv = Bukkit.createInventory(null,45,"§lNeoSpectatorGUI");
            isPlayer = false;
        }
        ItemStack redFrame = createItemStack(Material.STAINED_GLASS_PANE," ",(short) 14);
        ItemStack greenFrame = createItemStack(Material.STAINED_GLASS_PANE," ",(short) 5);
        //相手の枠12,13,14
        //自分の枠30,31,32
        if ((battle.isATurn && isPlayerA) || (!battle.isATurn && !isPlayerA)){//自分のターンならば
            //自分は緑
            inv.setItem(30,greenFrame);
            inv.setItem(31,greenFrame);
            inv.setItem(32,greenFrame);
            //相手は赤
            inv.setItem(12,redFrame);
            inv.setItem(13,redFrame);
            inv.setItem(14,redFrame);
        }else {//相手のターンならば
            //自分は赤
            inv.setItem(30,redFrame);
            inv.setItem(31,redFrame);
            inv.setItem(32,redFrame);
            //相手は緑
            inv.setItem(12,greenFrame);
            inv.setItem(13,greenFrame);
            inv.setItem(14,greenFrame);
        }
        //相手の手札0~8
        int opponentHand;
        if (isPlayerA){
            opponentHand = battle.handB.size();
        }else {
            opponentHand = battle.handA.size();
        }
        opponentHand = 8 - opponentHand;
        ItemStack dummyCard = createItemStack(Material.BOOK,"§d§l相手の手札");
        for (int i = 8;i > opponentHand;i--){
            inv.setItem(i,dummyCard);
        }
        //自分の手札36~44
        int myselfHand;
        if (isPlayerA){
            myselfHand = battle.handA.size();
        }else {
            myselfHand = battle.handB.size();
        }
        myselfHand += 36;
        if (isPlayer) {//自分ならば表示
            List<ItemStack> hand;
            if (isPlayerA){
                hand = battle.handA;
            }else {
                hand = battle.handB;
            }
            for (int i = 36;i < myselfHand;i++){
                ItemStack myCard = hand.get(i - 36);
                inv.setItem(i,myCard);
                //説明看板の表示
                Card card = system.getCard(myCard);
                List<String> cardText = new ArrayList<>();
                if (card.now){
                    cardText.add("§f§lこのターン発動する効果");
                    if (card.damageNow != -1)cardText.add("§f与えるダメージ"+ card.damageNow);
                    if (card.healNow != -1)cardText.add("§fHP回復"+ card.healNow);
                    if (card.reductionNow != -1)cardText.add("§fダメージ軽減"+ card.reductionNow);
                }
                if (card.next){
                    cardText.add("§f§l次の自分のターン発動する効果");
                    if (card.damageNext != -1)cardText.add("§f与えるダメージ"+ card.damageNext);
                    if (card.healNext != -1)cardText.add("§fHP回復"+ card.healNext);
                    if (card.reductionNext != -1)cardText.add("§fダメージ軽減"+ card.reductionNext);
                }
                inv.setItem(i + 9,createItemStack(Material.SIGN,"§a§l↑↑カード説明↑↑",cardText));
            }
        }else {//観戦者ならダミー
            ItemStack myDummy = createItemStack(Material.BOOK, "§a§l自分の手札");
            for (int i = 36;i < myselfHand;i++){
                inv.setItem(i,myDummy);
            }
        }

        //相手の山札9
        List<String> deckLore1 = new ArrayList<>();
        if (isPlayerA){
            deckLore1.add("§f§l"+ battle.deckB.size() +"枚");
        }else {
            deckLore1.add("§f§l"+ battle.deckA.size() +"枚");
        }
        inv.setItem(9,createItemStack(Material.DROPPER,"§l§d相手の山札",deckLore1));
        //相手の墓場10
        List<String> graveyardLore1 = new ArrayList<>();
        List<ItemStack> graveyards1;
        if (isPlayerA){
            graveyards1 = battle.graveyardB;
        }else {
            graveyards1 = battle.graveyardA;
        }
        for (ItemStack itemStack:graveyards1){
            graveyardLore1.add(itemStack.getItemMeta().getDisplayName());
        }
        inv.setItem(10,createItemStack(Material.CHEST,"§l§d相手の墓場",graveyardLore1));
        //自分の山札35
        List<String> deckLore2 = new ArrayList<>();
        if (isPlayerA){
            deckLore2.add("§f§l"+ battle.deckA.size() +"枚");
        }else {
            deckLore2.add("§f§l"+ battle.deckB.size() +"枚");
        }
        inv.setItem(35,createItemStack(Material.DROPPER,"§l§a山札",deckLore2));
        //自分の墓場34
        List<String> graveyardLore2 = new ArrayList<>();
        List<ItemStack> graveyards2;
        if (isPlayerA){
            graveyards2 = battle.graveyardA;
        }else {
            graveyards2 = battle.graveyardB;
        }
        for (ItemStack itemStack:graveyards2){
            graveyardLore2.add(itemStack.getItemMeta().getDisplayName());
        }
        inv.setItem(34,createItemStack(Material.CHEST,"§l§a墓場",graveyardLore2));
        //相手の付与効果21
        GrantEffect effect1;
        if (isPlayerA){
            effect1 = battle.effectB;
        }else {
            effect1 = battle.effectA;
        }
        List<String> effectLore1 = new ArrayList<>();
        if (effect1.damageReduction != -1)effectLore1.add("§f§lダメージ軽減:"+ effect1.damageReduction);
        //効果が追加されたらここ
        inv.setItem(21,createItemStack(Material.PAPER,"§l§d相手の付与効果",effectLore1));
        //自分の付与効果23
        GrantEffect effect2;
        if (isPlayerA){
            effect2 = battle.effectA;
        }else {
            effect2 = battle.effectB;
        }
        List<String> effectLore2 = new ArrayList<>();
        if (effect2.damageReduction != -1)effectLore2.add("§f§lダメージ軽減:"+ effect2.damageReduction);
        //効果が追加されたらここ
        inv.setItem(23,createItemStack(Material.PAPER,"§l§a付与効果",effectLore2));
        //相手の通常攻撃15
        List<String> swordLore1 = new ArrayList<>();
        swordLore1.add("§f§l相手に1ダメージを与えます");
        swordLore1.add("§f§lこのアイテムをクリックで使えます");
        inv.setItem(15,createItemStack(Material.STONE_SWORD,"§d§l相手の通常攻撃",swordLore1));
        //自分の通常攻撃29
        List<String> swordLore2 = new ArrayList<>();
        swordLore2.add("§f§l相手に1ダメージを与えます");
        swordLore2.add("§f§lこのアイテムをクリックで使えます");
        inv.setItem(29,createItemStack(Material.STONE_SWORD,"§a§l通常攻撃",swordLore2));
        //相手の永続効果11,自分の永続効果33
        if (isPlayerA){
            inv.setItem(11,battle.permanentB);
            inv.setItem(33,battle.permanentA);
        }else {
            inv.setItem(11,battle.permanentA);
            inv.setItem(33,battle.permanentB);
        }
        //ItemStackの0番目に10の位、1番目に1の位
        //相手のHP16,17
        ItemStack[] itemStackHPs1;
        if (isPlayerA){
            itemStackHPs1 = createItemStackHP(battle.HPB);
        }else {
            itemStackHPs1 = createItemStackHP(battle.HPA);
        }
        inv.setItem(16,itemStackHPs1[0]);
        inv.setItem(17,itemStackHPs1[1]);
        //自分のHP27,28
        ItemStack[] itemStackHPs2;
        if (isPlayerA){
            itemStackHPs2 = createItemStackHP(battle.HPA);
        }else {
            itemStackHPs2 = createItemStackHP(battle.HPB);
        }
        inv.setItem(27,itemStackHPs2[0]);
        inv.setItem(28,itemStackHPs2[1]);
        //次の相手のターン発動する効果20
        //次の自分のターン発動する効果24
        if (isPlayerA){
            inv.setItem(20,battle.nextCardB);
            inv.setItem(24,battle.nextCardA);
        }else {
            inv.setItem(20,battle.nextCardA);
            inv.setItem(24,battle.nextCardB);
        }
        //未実装,次の次のターン〇〇など実装予定18,19
        inv.setItem(18,createItemStack(Material.BARRIER,"§4未実装"));
        inv.setItem(19,createItemStack(Material.BARRIER,"§4未実装"));
        //未実装,次の次のターン〇〇など実装予定25,26
        inv.setItem(25,createItemStack(Material.BARRIER,"§4未実装"));
        inv.setItem(26,createItemStack(Material.BARRIER,"§4未実装"));
        //中央に置くもの22
        if (battle.center != null){
            inv.setItem(22,battle.center);
        }
        //開く
        spectator.openInventory(inv);
    }

    public void cardListMenu(Player player){//カード一覧のメニュー
        Inventory inv = Bukkit.createInventory(null,9,"§lカード一覧メニュー");
        inv.setItem(1,createItemStack(Material.APPLE,"§f§lノーマルカード一覧"));
        inv.setItem(3,createItemStack(Material.APPLE,"§f§lレアカード一覧"));
        inv.setItem(5,createItemStack(Material.APPLE,"§f§lレジェンドカード一覧"));
        inv.setItem(7,createItemStack(Material.APPLE,"§f§lイベントカード一覧"));
        player.openInventory(inv);
    }

    public void cardList(Player player,String type){
        cardList(player, type,1);
    }

    public void cardList(Player player,String type,int page){
        List<Card> cards;
        switch (type) {
            case "§f§lノーマルカード一覧":
                cards = NeoCardGames.normalCards;
                break;
            case "§f§lレアカード一覧":
                cards = NeoCardGames.rareCards;
                break;
            case "§f§lレジェンドカード一覧":
                cards = NeoCardGames.legendCards;
                break;
            case "§f§lイベントカード一覧":
                cards = NeoCardGames.eventCards;
                break;
            default:
                cards = null;
                break;
        }
        if (cards == null){
            return;
        }
        Inventory inv = Bukkit.createInventory(null,54,type);
        //ページチェック
        if (page <= 0){
            page = 1;
        }
        int allPages = cards.size() / 45 + 1;
        if (allPages < page){
            page = allPages;
        }
        int openPage = page;
        Iterator<Card> iterator = cards.iterator();
        for (int i = 1; i < (allPages + 1); i++) {
            if (openPage != i){
                for (int l = 0;l < 45;l++){
                    iterator.next();
                }
                continue;
            }
            for (int l = 0; l < 45; l++) {
                if (iterator.hasNext()) {
                    Card card = iterator.next();
                    NeoGameSystem system = new NeoGameSystem(neo);
                    ItemStack itemStack = system.getItemStack(card);
                    inv.setItem(l,itemStack);
                }
            }
            break;
        }
        //ページ変化用
        for (int i = 0; i < 9;i++){
            inv.setItem(i + 45,createItemStack(Material.STAINED_GLASS_PANE,(i + 1) +"ページへ",(short) i));
        }
        player.openInventory(inv);
    }

    public ItemStack createItemStack(Material type, String name){
        ItemStack itemStack = new ItemStack(type);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack createItemStack(Material type, String name, short itemType){
        ItemStack itemStack = new ItemStack(type,1,itemType);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack createItemStack(Material type, String name, List<String> lore){
        ItemStack itemStack = new ItemStack(type);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack[] createItemStackHP(int HP){
        //数字のテクスチャの減った耐久値 0の933~9の924
        ItemStack[] itemStacks = new ItemStack[2];
        ItemStack itemStack10 = new ItemStack(Material.DIAMOND_HOE);
        ItemStack itemStack1 = new ItemStack(Material.DIAMOND_HOE);
        int count10 = 0;//10の位
        while (HP >= 10){
            HP -= 10;
            count10 += 1;
        }
        int count1 = 0;//1の位
        while (HP >= 1){
            HP -= 1;
            count1 += 1;
        }
        //933-X
        itemStack10.setDurability((short) (933 - count10));
        itemStack1.setDurability((short) (933 - count1));
        //名前付け
        ItemMeta itemMeta10 = itemStack10.getItemMeta();
        itemMeta10.setDisplayName("§c§lHP");
        itemMeta10.setUnbreakable(true);
        itemMeta10.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        itemStack10.setItemMeta(itemMeta10);
        ItemMeta itemMeta1 = itemStack1.getItemMeta();
        itemMeta1.setDisplayName("§c§lHP");
        itemMeta1.setUnbreakable(true);
        itemMeta1.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        itemStack1.setItemMeta(itemMeta1);
        //終了
        itemStacks[0] = itemStack10;
        itemStacks[1] = itemStack1;
        return itemStacks;
    }
}
