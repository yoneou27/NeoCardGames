package yoneyone.yone.yo.neocardgames;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class NeoCardGames extends JavaPlugin {
    static final String prefix = "§r[§4Neo§dCard§r]";
    static boolean battleStart = true;
    static short normalCardDurability = 0;
    static short rareCardDurability = 0;
    static short legendCardDurability = 0;
    static short eventCardDurability = 0;
    static List<Card> normalCards = new ArrayList<>();
    static List<Card> rareCards = new ArrayList<>();
    static List<Card> legendCards = new ArrayList<>();
    static List<Card> eventCards = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new NeoListener(this),this);
        NeoConfig config = new NeoConfig(this);
        config.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        NeoConfig neo = new NeoConfig(this);
        FileConfiguration testConfig = neo.getConfig();
        if (testConfig == null) {
            Bukkit.getLogger().info("ロードに失敗しました、config.ymlが存在しない可能性があります");
        } else {
            if (testConfig.contains("neobattle") && testConfig.contains("carddurability")) {
                //ゲーム開始していいか確認
                battleStart = testConfig.getBoolean("neobattle");
                //耐久値の確認初期値
                normalCardDurability = (short) testConfig.getInt("carddurability.normal");//通常
                rareCardDurability = (short) testConfig.getInt("carddurability.rare");//レア
                legendCardDurability = (short) testConfig.getInt("carddurability.legend");//レジェンド
                eventCardDurability = (short) testConfig.getInt("carddurability.event");//期間限定
            } else {
                Bukkit.getLogger().info("config.ymlに不備があります");
            }
        }
        //設定ファイルの確認
        File fileD = new File("plugins\\NeoCardGames");
        if (!fileD.exists()) {
            Bukkit.getLogger().info("NeoCardGamesディレクトリが存在しません、作成します");
            if (!fileD.mkdir()) {
                Bukkit.getLogger().info("ディレクトリ作成に失敗しました処理を終了します");
                return;
            }
        }
        File fileN = new File("plugins\\NeoCardGames\\normal");
        File fileR = new File("plugins\\NeoCardGames\\rare");
        File fileL = new File("plugins\\NeoCardGames\\legend");
        File fileE = new File("plugins\\NeoCardGames\\event");
        if (!fileN.exists()) {//存在しなければ
            if (!fileN.mkdir()) {//生成できなければ
                Bukkit.getLogger().info("NeoCardGamesのコンフィグエラーが発生しました、確認してください");
            }
        }
        if (!fileR.exists()) {
            if (!fileR.mkdir()) {
                Bukkit.getLogger().info("NeoCardGamesのコンフィグエラーが発生しました、確認してください");
            }
        }
        if (!fileL.exists()) {
            if (!fileL.mkdir()) {
                Bukkit.getLogger().info("NeoCardGamesのコンフィグエラーが発生しました、確認してください");
            }
        }
        if (!fileE.exists()) {
            if (!fileE.mkdir()) {
                Bukkit.getLogger().info("NeoCardGamesのコンフィグエラーが発生しました、確認してください");
            }
        }
        File[] listN = fileN.listFiles();
        File[] listR = fileR.listFiles();
        File[] listL = fileL.listFiles();
        File[] listE = fileE.listFiles();
        if (listN == null || listR == null || listL == null || listE == null) {
            Bukkit.getLogger().info("plugins\\NeoCardGamesの中身を確認してください、不備がある可能性があります");
            return;
        }
        //各カードの確認
        normalCards.clear();//リセット
        for (File file : listN) {
            String path = "normal\\"+ file.getName();
            Card card = fileToCard(path);
            if (card == null){
                continue;
            }
            normalCards.add(card);
        }
        rareCards.clear();
        for (File file : listR) {
            String path = "rare\\"+ file.getName();
            Card card = fileToCard(path);
            if (card == null){
                continue;
            }
            rareCards.add(card);
        }
        legendCards.clear();
        for (File file : listL) {
            String path = "legend\\"+ file.getName();
            Card card = fileToCard(path);
            if (card == null){
                continue;
            }
            legendCards.add(card);
        }
        eventCards.clear();
        for (File file : listE) {
            String path = "event\\"+ file.getName();
            Card card = fileToCard(path);
            if (card == null){
                continue;
            }
            eventCards.add(card);
        }
    }
/*コンフィグ記入例（不要な部分は書きません）
nameとrarityは必須です
loreの値名は数字で書いて下さい

name: §l三色団子
rarity: rare
lore:
  1: §fおいしい団子
  2: §f食べると幸せになれる
now:
  heal: 2
  reduction: 1
next:
  heal: 1
*/
    private Card fileToCard(String path){//一つのカードコンフィグから、データを取る
        NeoConfig config = new NeoConfig(this,path);
        FileConfiguration configFile = config.getConfig();
        Card card = new Card();
        if (!(configFile.contains("name") && configFile.contains("rarity"))){
            Bukkit.getLogger().info(path +"の必須データに不備があります");
            return null;
        }
        card.name = configFile.getString("name");
        card.rarity = configFile.getString("rarity");
        if (configFile.contains("lore")){
            List<String> lore = new ArrayList<>();
            for (int i = 1;configFile.contains("lore."+ i);i++){//ロール表示
                lore.add(configFile.getString("lore."+ i));
            }
            String rarity = configFile.getString("rarity");
            lore.add("§6["+ rarity +"]");//レアリティ表示
            card.lore = lore;
        }else {
            List<String> lore = new ArrayList<>();
            String rarity = configFile.getString("rarity");
            lore.add("§6["+ rarity +"]");//レアリティ表示
            card.lore = lore;
        }

        if (configFile.contains("now")){
            card.now = true;
            if (configFile.contains("now.damage")){
                int amount = configFile.getInt("now.damage",-1);
                if (amount <= -1){//マイナスの時は-1にする
                    amount = -1;
                }
                card.damageNow = amount;
            }
            if (configFile.contains("now.heal")){
                int amount = configFile.getInt("now.heal",-1);
                if (amount <= -1){
                    amount = -1;
                }
                card.healNow = amount;
            }
            if (configFile.contains("now.reduction")){
                int amount = configFile.getInt("now.reduction",-1);
                if (amount <= -1){
                    amount = -1;
                }
                card.reductionNow = amount;
            }
        }
        if (configFile.contains("next")){
            card.next = true;
            if (configFile.contains("next.damage")){
                int amount = configFile.getInt("next.damage",-1);
                if (amount <= -1){
                    amount = -1;
                }
                card.damageNext = amount;
            }
            if (configFile.contains("next.heal")){
                int amount = configFile.getInt("next.heal",-1);
                if (amount <= -1){
                    amount = -1;
                }
                card.healNext = amount;
            }
            if (configFile.contains("next.reduction")){
                int amount = configFile.getInt("next.reduction",-1);
                if (amount <= -1){
                    amount = -1;
                }
                card.reductionNext = amount;
            }
        }
        //要素追加時にはここに書く
        return card;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage(prefix +"§4このコマンドはプレイヤーから実行してください");
            return true;
        }
        Player player = (Player) sender;
        NeoGameSystem system = new NeoGameSystem(this);
        if (label.equals("neo")){
            if (!battleStart){
                player.sendMessage(prefix +"§4現在カードゲームは停止しています");
                return true;
            }
            if (args.length == 0){
                showHelp(player);
                return true;
            }
            if (args[0].equals("check")){
                String message;
                if (args.length == 2){
                    if (!checkPlayer(args[1])){
                        player.sendMessage(prefix +"§4そのプレイヤーは存在しません");
                        return true;
                    }
                    message = system.playCheck(Bukkit.getPlayer(args[1]));
                }else {
                    message = system.playCheck(player);
                }
                player.sendMessage(prefix +"現在の状態："+message);
                return true;
            }
            if (args[0].equals("accept")){
                //ここに参加処理
                String name = system.getPlayer(player);
                if (name == null){
                    player.sendMessage(prefix +"§4あなたは対戦を申し込まれていません");
                    return true;
                }
                Battle battle = system.getBattle(player);
                if (battle != null){
                    if (!battle.type.equals(BattleProcessType.Before)){//対戦前なら可
                        player.sendMessage(prefix +"§4試合中は了承できません");
                        return true;
                    }
                }
                Player challenger = Bukkit.getPlayer(name);
                system.startBattle(challenger,player);
                return true;
            }
            if (args[0].equals("cancel")){
                //停止処理
                String name = system.getPlayer(player);
                if (name == null){
                    player.sendMessage(prefix +"§4あなたは対戦を申し込まれていません");
                    return true;
                }
                Battle battle = system.getBattle(player);
                if (battle != null){
                    if (!battle.type.equals(BattleProcessType.Before)) {//対戦前なら可
                        player.sendMessage(prefix + "§4対戦中にキャンセルはできません");
                        return true;
                    }
                }
                system.stopBattle(player);
                return true;
            }
            if (args[0].equals("open")){
                NeoBattleGUI GUI = new NeoBattleGUI(this);
                String message = system.playCheck(player);
                if (message.equals("何もしていません")){
                    player.sendMessage(prefix +"§4あなたは何もしていません");
                    return true;
                }
                if (message.equals("対戦申し込み中")){
                    player.sendMessage(prefix +"§4現在の状態に対応するGUIがありません");
                    return true;
                }
                GUI.openChoice(player,message);
                return true;
            }
            if (args.length == 1){
                showHelp(player);
                return true;
            }
            if (args[0].equals("watch")){//観戦
                if (system.getBattle(player) != null){
                    player.sendMessage(prefix +"§4あなたは対戦中です");
                    return true;
                }
                if (!checkPlayer(args[1])){
                    player.sendMessage(prefix +"§4そのプレイヤーは存在しません");
                    return true;
                }
                Player battlePlayer = Bukkit.getPlayer(args[1]);
                Battle battle = system.getBattle(battlePlayer);
                if (battle == null){
                    player.sendMessage(prefix +"§4そのプレイヤーは対戦をしていません");
                    return true;
                }
                NeoBattleGUI GUI = new NeoBattleGUI(this);
                if (battle.type.equals(BattleProcessType.Running)){
                    GUI.battleGUI(battle.playerA,player);
                    player.sendMessage(prefix +"§l観戦を開始しました");
                }else {
                    player.sendMessage(prefix +"§l対戦がまだ始まっていません");
                    player.sendMessage(prefix +"§l開始時にGUIが開きます");
                }
                if (!battle.spectators.contains(player.getName())){
                    player.sendMessage(prefix +"§lこの対戦の先攻側を見るコマンドです、ご注意ください");
                    battle.spectators.add(player.getName());
                    system.battleSendMessage(battle,prefix + player.getName() +"さんが観戦を始めました");
                }
                return true;
            }
            if (args[0].equals("b")){
                if (system.getBattle(player) != null){
                    player.sendMessage(prefix +"§4あなたは対戦中です");
                    return true;
                }
                if (!checkPlayer(args[1])){
                    player.sendMessage(prefix +"§4そのプレイヤーは存在しません");
                    return true;
                }
                Player opponent = Bukkit.getPlayer(args[1]);
                if (opponent.getName().equals(player.getName())){
                    player.sendMessage(prefix +"§4自分には対戦を申し込めません");
                    return true;
                }
                Battle battleB = system.getBattle(opponent);
                if (battleB != null){
                    player.sendMessage(prefix +"§4そのプレイヤーは対戦中です");
                    return true;
                }
                system.battleApply(player,opponent);
                return true;
            }
            if (args.length == 2){
                showHelp(player);
                return true;
            }
            showHelp(player);
            return true;
        }else if (label.equals("neop")){
            if (player.hasPermission("neo.op")){
                if (args.length == 0){
                    showHelp(player);
                    return true;
                }
                if (args[0].equals("on") || args[0].equals("off")){
                    String tof = args[0];
                    NeoConfig config = new NeoConfig(this);
                    if (tof.equals("on")){
                        config.getConfig().set("neobattle",true);
                        config.saveConfig();
                        player.sendMessage(prefix +"onにしました");
                    }else {
                        config.getConfig().set("neobattle",false);
                        config.saveConfig();
                        player.sendMessage(prefix +"offにしました");
                    }
                    reloadConfig();
                    return true;
                }
                if (args[0].equals("reload")){
                    player.sendMessage(prefix +"§lリロード開始中§kxxx");
                    reloadConfig();
                    player.sendMessage(prefix +"§6§lリロード完了！");
                    return true;
                }
                if (args[0].equals("cards")){
                    player.sendMessage(prefix +"各大区分のカードの枚数を表示します");
                    player.sendMessage(prefix +"ノーマルカードの数:"+ normalCards.size());
                    player.sendMessage(prefix +"レアカードの数:"+ rareCards.size());
                    player.sendMessage(prefix +"レジェンドカードの数:"+ legendCards.size());
                    player.sendMessage(prefix +"イベントカードの数:"+ eventCards.size());
                    return true;
                }
                if (args[0].equals("get")){
                    player.sendMessage(prefix +"§aカード一覧のGUIを開きます");
                    NeoBattleGUI GUI = new NeoBattleGUI(this);
                    GUI.cardListMenu(player);
                    return true;
                }
                if (args.length == 1){
                    showHelp(player);
                    return true;
                }
                if (args[0].equals("stop")){
                    Player stopPlayer = Bukkit.getPlayer(args[1]);
                    if (stopPlayer == null){
                        player.sendMessage(prefix +"§4そのプレイヤーは存在しません");
                        return true;
                    }
                    if (system.getBattle(player) == null){
                        player.sendMessage(prefix +"§4そのプレイヤーは対戦をしていません");
                        return true;
                    }
                    system.stopBattle(player);
                    player.sendMessage(prefix +"§a対戦を停止しました");
                    return true;
                }
                showHelp(player);
                return true;
            }else {
                player.sendMessage(prefix +"§4あなたには権限がありません");
            }
        }
        return true;
    }
    private void showHelp(Player player){
        player.sendMessage("§f§l-----NeoCardGamesコマンドの使い方----");
        player.sendMessage("§e/neo b <プレイヤー名> 対戦を申し込みます");
        player.sendMessage("§e/neo <accept/cancel> 申し込まれた対戦を、了承/拒否します");
        player.sendMessage("§e/neo check [プレイヤー名] 指定プレイヤーの状態を確認します、引数がない場合自分が対象です");
        player.sendMessage("§e/neo open 試合のインベントリを閉じていた場合開けます");
        player.sendMessage("§e/neo watch <プレイヤー名> 他の人の試合を見ることができます");
        if (player.hasPermission("neo.op")){
            player.sendMessage("§5§l-----以下管理者用-----");
            player.sendMessage("§e/neop <on/off> カードゲームをon/offにしてリロードします");
            player.sendMessage("§e/neop reload コンフィグをリロードします");
            player.sendMessage("§e/neop cards 読み込まれているカードの枚数を確認します");
            player.sendMessage("§e/neop get 読み込まれているカード一覧のGUIを見れます");
            player.sendMessage("§e/neop stop <プレイヤー名> このプレイヤーが実行しているゲームを停止します");
            player.sendMessage("§eこのコマンドを使った場合対象のプレイヤーが負けになります");
        }
    }
    private boolean checkPlayer(String playerName){
        return Bukkit.getServer().getPlayer(playerName) != null;
    }
}
class Battle{
    int ID = 0;

    //次の次のターン以降と、永続は、未実装です

    Player playerA = null;//対戦者
    List<ItemStack> cardA = null;//全部のカード
    List<ItemStack> deckA = null;//山札
    List<ItemStack> handA = new ArrayList<>();//手札
    List<ItemStack> graveyardA = new ArrayList<>();//墓場
    ItemStack nextCardA = null;//次の自分のターン実行されるカード
    ItemStack permanentA = null;//永続効果
    GrantEffect effectA = new GrantEffect();//付与効果
    int HPA = 10;//HP
    int offenseA = 0;//反則回数

    Player playerB = null;//対戦者2
    List<ItemStack> cardB = null;
    List<ItemStack> deckB = null;
    List<ItemStack> handB = new ArrayList<>();
    List<ItemStack> graveyardB = new ArrayList<>();
    ItemStack nextCardB = null;
    ItemStack permanentB = null;
    GrantEffect effectB = new GrantEffect();
    int HPB = 10;
    int offenseB = 0;

    ItemStack center = null;//中央に現在表示されているもの
    boolean isATurn = true;//Aのターンかどうか
    boolean isProcessing = true;//処理中（プレイヤー待機状態）かどうか
    BattleProcessType type = BattleProcessType.Before;//進行状態
    int turn = 0;//ターン数
    List<String> spectators = new ArrayList<>();//観戦者名
}
class GrantEffect{
    //自分のターンだけの効果

    //相手のターンまでの効果
    int damageReduction = -1;
}
enum BattleProcessType{
    Before,Preparation,Running,End
}
enum CardRarityType{
    normal,rare,legend,event
}
class Card{
    //以下必須項目
    String name = null;//§l三色団子 など
    List<String> lore = null;//説明文
    String rarity = null;//ノーマル、レア、スーパーレア、レジェンド、シークレット の5種類とその他限定ランク
    //以下任意項目
    //-1の時は発動しないことにします
    //このターン発動する効果 Now
    boolean now = false;//発動するか
    int damageNow = -1;//ダメージ値
    int healNow = -1;//回復値
    int reductionNow = -1;//次の相手のターンダメージ軽減値
    //次の自分のターン開始時発動する効果 Next
    boolean next = false;
    int damageNext = -1;
    int healNext = -1;
    int reductionNext = -1;
}
