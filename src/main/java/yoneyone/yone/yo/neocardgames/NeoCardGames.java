package yoneyone.yone.yo.neocardgames;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NeoCardGames extends JavaPlugin {
    static final String prefix = "§r[§4Neo§dCard§r]";
    static boolean battleStart = true;
    static short normalCardDurability = 0;
    static short rareCardDurability = 0;
    static short legendCardDurability = 0;
    static short eventCardDurability = 0;
    static List<Card> cards = new ArrayList<>();//すべてのカード
    static short cardPackDurability = 0;
    static List<CardPack> cardPacks = new ArrayList<>();//カードパック
    static List<String> cardSeries = new ArrayList<>();//どのシリーズがあるか
    final String separator = File.separator;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new NeoListener(this),this);
        NeoConfig config = new NeoConfig(this);
        config.saveDefaultConfig();
        NeoConfig config2 = new NeoConfig(this,"example_card.yml");
        config2.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        NeoConfig neo = new NeoConfig(this);
        FileConfiguration testConfig = neo.getConfig();
        if (testConfig == null) {
            Bukkit.getLogger().info("ロードに失敗しました、config.ymlが存在しない可能性があります");
        } else {
            if (testConfig.contains("neobattle") && testConfig.contains("carddurability") && testConfig.contains("packdurability")) {
                //ゲーム開始していいか確認
                battleStart = testConfig.getBoolean("neobattle");
                //耐久値の確認初期値
                normalCardDurability = (short) testConfig.getInt("carddurability.normal");//通常
                rareCardDurability = (short) testConfig.getInt("carddurability.rare");//レア
                legendCardDurability = (short) testConfig.getInt("carddurability.legend");//レジェンド
                eventCardDurability = (short) testConfig.getInt("carddurability.event");//期間限定
                //カードパック用
                cardPackDurability = (short) testConfig.getInt("packdurability");
            } else {
                Bukkit.getLogger().info("config.ymlに不備があります");
                Bukkit.getLogger().info("プラグインのの機能を停止します");
                Bukkit.getLogger().info("必ず確認をしてから稼働させてください");
                battleStart = false;
            }
        }
        //設定ファイルの確認
        File fileD = new File("plugins"+ separator +"NeoCardGames");
        if (!fileD.exists()) {
            Bukkit.getLogger().info("NeoCardGamesディレクトリが存在しません、作成します");
            if (!fileD.mkdir()) {
                Bukkit.getLogger().info("ディレクトリ作成に失敗しました処理を終了します");
                return;
            }
        }
        File fileC = new File("plugins"+ separator +"NeoCardGames"+ separator +"cards");
        File fileP = new File("plugins"+ separator +"NeoCardGames"+ separator +"pack");
        if (!fileC.exists()) {//存在しなければ
            if (!fileC.mkdir()) {//生成できなければ
                Bukkit.getLogger().info("NeoCardGamesのコンフィグエラーが発生しました、確認してください");
            }
        }
        if (!fileP.exists()){
            if (!fileP.mkdir()) {
                Bukkit.getLogger().info("NeoCardGamesのコンフィグエラーが発生しました、確認してください");
            }
        }
        File[] listC = fileC.listFiles();
        File[] listP = fileP.listFiles();
        if (listC == null || listP == null) {
            Bukkit.getLogger().info("plugins"+ separator +"NeoCardGamesの中身を確認してください、不備がある可能性があります");
            return;
        }
        //チュートリアルの.txtファイルの出力
        try {
            File fileTxt = new File("plugins"+ separator +"NeoCardGames"+ separator +"cards"+ separator +"readme.txt");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileTxt)));

            pw.println("このディレクトリはカードデータファイルの保存のために使います");
            pw.println("cards"+ separator +"任意の名前のシリーズディレクトリ"+ separator +"ノーマル、レア、スーパーレア、レジェンド、シークレットのいずれかのレアリティディレクトリ"+ separator +"カードデータ.yml");
            pw.println("というように保存します");

            pw.close();
        }catch (IOException e){
            Bukkit.getLogger().info("説明用txtファイルの生成に失敗しました、確認してください");
        }
        //各カードの確認
        //ネオカードゲーム\\カード\\各種シリーズ\\存在するレアリティ\\カード.yml
        cards.clear();//リセット
        cardSeries.clear();
        for (File seriesFile:listC) {
            if (seriesFile.isDirectory()){//もしもフォルダなら
                File[] listCS = seriesFile.listFiles();
                if (listCS == null){
                    continue;
                }
                cardSeries.add(seriesFile.getName());
                for (File rarityFile:listCS){
                    if (rarityFile.isDirectory()){//もしもフォルダなら
                        short cardDurability;
                        switch (rarityFile.getName()){
                            case "ノーマル":
                            case "レア":
                                cardDurability = normalCardDurability;
                                break;
                            case "スーパーレア":
                            case "レジェンド":
                                cardDurability = rareCardDurability;
                                break;
                            case "シークレット":
                                cardDurability = legendCardDurability;
                                break;
                            case "イベント":
                                cardDurability = eventCardDurability;
                                break;
                            default:
                                Bukkit.getLogger().info(rarityFile.getPath() +"のレアリティは不正です");
                                continue;
                        }
                        File[] listCSR = rarityFile.listFiles();
                        if (listCSR == null){
                            continue;
                        }
                        for (File cardFile:listCSR){
                            if (cardFile.isFile()){//もしもカードデータファイルなら
                                String path = "cards"+ separator + seriesFile.getName() + separator + rarityFile.getName() + separator + cardFile.getName();
                                Card card = fileToCard(path);
                                if (card == null){
                                    continue;
                                }
                                card.fileName = getPreffix(cardFile.getName());//識別用名
                                boolean isExists = false;
                                for (Card checkCard:cards){
                                    if (checkCard.fileName.equals(card.fileName) || checkCard.name.equals(card.name)){
                                        Bukkit.getLogger().info(path +"のカードと同じファイル名のカードもしくは同じカード名のファイルが既に存在します");
                                        Bukkit.getLogger().info("バグを防ぐため、このファイルの保存を停止します");
                                        isExists = true;
                                        break;
                                    }
                                }
                                if (isExists){//存在するなら
                                    continue;
                                }
                                card.cardDurability = cardDurability;//テクスチャ設定
                                List<String> lore = card.lore;
                                if (card.rarity == null){
                                    card.rarity = rarityFile.getName();
                                    lore.add("§6["+ card.rarity +"]");
                                }
                                card.series = seriesFile.getName();
                                lore.add("§6["+ card.series +"]");
                                cards.add(card);
                            }
                        }
                    }
                }
            }
        }
        //カードパック読み取り処理
        cardPacks.clear();//リセット
        for (File packFile:listP){
            String path = "pack"+ separator + packFile.getName();
            CardPack pack = fileToCardPack(path);
            if (pack == null){
                continue;
            }
            boolean isExists = false;
            for (CardPack cardPack:cardPacks){
                if (cardPack.name.equals(pack.name)){
                    Bukkit.getLogger().info(path +"のカードと同じ名前のカードパックファイルが既に存在します");
                    Bukkit.getLogger().info("バグを防ぐため、このファイルの保存を停止します");
                    isExists = true;
                    break;
                }
            }
            if (isExists){//存在するなら
                continue;
            }
            cardPacks.add(pack);
        }
    }

    public String getPreffix(String fileName) {
        if (fileName == null) {
            return null;
        }
        int point = fileName.lastIndexOf(".");
        if (point != -1) {
            return fileName.substring(0, point);
        }
        return fileName;
    }
/*コンフィグ記入例（不要な部分は書きません）
nameは必須です
loreの値名は数字で書いて下さい
cards\任意の名前のシリーズディレクトリ\ノーマル、レア、スーパーレア、レジェンド、シークレットのいずれかのレアリティディレクトリ\このファイル.yml
に置いてください

name: §l三色団子
lore:
  1: §fおいしい団子
  2: §f食べると幸せになれる
now:
  heal: 2
  reduction: 1
  draw: 1
next:
  heal: 1
*/
    private Card fileToCard(String path) {//一つのカードコンフィグから、データを取る
        NeoConfig config = new NeoConfig(this, path);
        FileConfiguration configFile = config.getConfig();
        Card card = new Card();
        if (!(configFile.contains("name"))) {
            Bukkit.getLogger().info(path + "の必須データに不備があります");
            return null;
        }
        card.name = configFile.getString("name");
        if (configFile.contains("rarity")) {//レアリティ、未設定の場合ディレクトリのものが設定される
            card.rarity = configFile.getString("rarity");
        }
        if (configFile.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (int i = 1; configFile.contains("lore." + i); i++) {//ロール表示
                lore.add(configFile.getString("lore." + i));
            }
            if (card.rarity != null) {
                lore.add("§6[" + card.rarity + "]");//レアリティ表示
            }
            //lore.add("§6["+ card.series +"]");//シリーズ表示
            card.lore = lore;
        } else {
            List<String> lore = new ArrayList<>();
            if (card.rarity != null) {
                lore.add("§6[" + card.rarity + "]");//レアリティ表示
            }
            //lore.add("§6["+ card.series +"]");//シリーズ表示
            card.lore = lore;
        }

        if (configFile.contains("now")) {
            card.now = true;
            if (configFile.contains("now.damage")) {
                int amount = configFile.getInt("now.damage");
                if (amount <= -1) {//マイナスの時は-1にする
                    amount = -1;
                }
                card.damageNow = amount;
            }
            if (configFile.contains("now.heal")) {
                int amount = configFile.getInt("now.heal");
                if (amount <= -1) {
                    amount = -1;
                }
                card.healNow = amount;
            }
            if (configFile.contains("now.reduction")) {
                int amount = configFile.getInt("now.reduction");
                if (amount <= -1) {
                    amount = -1;
                }
                card.reductionNow = amount;
            }
            if (configFile.contains("now.draw")) {
                int amount = configFile.getInt("now.draw");
                if (amount <= -1) {
                    amount = -1;
                }
                card.drawNow = amount;
            }
        }

        if (configFile.contains("next")) {
            card.next = true;
            if (configFile.contains("next.damage")) {
                int amount = configFile.getInt("next.damage");
                if (amount <= -1) {
                    amount = -1;
                }
                card.damageNext = amount;
            }
            if (configFile.contains("next.heal")) {
                int amount = configFile.getInt("next.heal");
                if (amount <= -1) {
                    amount = -1;
                }
                card.healNext = amount;
            }
            if (configFile.contains("next.reduction")) {
                int amount = configFile.getInt("next.reduction");
                if (amount <= -1) {
                    amount = -1;
                }
                card.reductionNext = amount;
            }
            if (configFile.contains("next.draw")) {
                int amount = configFile.getInt("next.draw");
                if (amount <= -1) {
                    amount = -1;
                }
                card.drawNext = amount;
            }
        }

        if (configFile.contains("permanent")) {
            card.permanent = true;
            if (configFile.contains("permanent.damage")) {
                int amount = configFile.getInt("permanent.damage");
                if (amount <= -1) {
                    amount = -1;
                }
                card.damagePermanent = amount;
            }
            if (configFile.contains("permanent.heal")) {
                int amount = configFile.getInt("permanent.heal");
                if (amount <= -1) {
                    amount = -1;
                }
                card.healPermanent = amount;
            }
            if (configFile.contains("permanent.reduction")) {
                int amount = configFile.getInt("permanent.reduction");
                if (amount <= -1) {
                    amount = -1;
                }
                card.reductionPermanent = amount;
            }
            if (configFile.contains("permanent.draw")) {
                int amount = configFile.getInt("permanent.draw");
                if (amount <= -1) {
                    amount = -1;
                }
                card.drawPermanent = amount;
            }
        }
        //要素追加時にはここに書く
        return card;
    }
/*コンフィグ記入例（不要な部分は書きません）
シリーズのところは、ディレクトリ名を書きます（存在しないと無視されます）
一つも設定しなかった場合、個別の部分だけ反映されます
個別で設定するとそのカードの確率が上書きされます
specialの欄にはファイル名を入れます
そうすることで個別設定ができます
何も排出されない設定にした場合
カードパックを使用できなくなります

name: §7§lスタンダード§r§l初期セット
amount: 5
lore:
  1: §fスタンダードな初期セットのパック
series:
  1: 初期セット
default:
  ノーマル: 700
  レア: 260
  スーパーレア: 30
  レジェンド: 9
  シークレット: 1
special:
  三色団子: 350
 */
    private CardPack fileToCardPack(String path){
        NeoConfig config = new NeoConfig(this,path);
        FileConfiguration configFile = config.getConfig();
        CardPack cardPack = new CardPack();
        if (!(configFile.contains("name"))){
            Bukkit.getLogger().info(path +"の必須データに不備があります");
            return null;
        }
        cardPack.name = configFile.getString("name");//名前設定
        if (configFile.contains("amount")){//排出枚数設定
            cardPack.cardAmount = configFile.getInt("amount");
        }
        List<String> lore = new ArrayList<>();
        if (configFile.contains("lore")){
            for (int i = 1;configFile.contains("lore."+ i);i++){//ロール表示
                lore.add(configFile.getString("lore."+ i));
            }
        }
        cardPack.lore = lore;

        List<String> series = new ArrayList<>();
        if (configFile.contains("series")){//排出されるシリーズの設定
            for (int i = 1;configFile.contains("series."+ i);i++){//排出されるシリーズの設定
                String series1text = configFile.getString("series."+ i);
                if (!cardSeries.contains(series1text)){//ない場合ははじかれる
                    Bukkit.getLogger().info(series1text +"というシリーズは存在しません");
                    continue;
                }
                series.add(series1text);
            }
        }
        cardPack.series = series;

        Map<String,Integer> defaultProbability = new HashMap<>();
        if (configFile.contains("default")){//デフォルトの確率
            for (String key : configFile.getConfigurationSection("default").getKeys(false)) {
                int rarityProbability = configFile.getInt("default." + key,0);
                defaultProbability.put(key,rarityProbability);
            }
        }
        cardPack.defaultProbability = defaultProbability;

        Map<Card,Integer> specialProbability = new HashMap<>();
        if (configFile.contains("special")){//個別の確率
            for (String key : configFile.getConfigurationSection("special").getKeys(false)) {
                int cardProbability = configFile.getInt("special." + key,0);
                Card card = null;
                for (Card checkCard:cards){
                    if (checkCard.fileName.equals(key)){
                        card = checkCard;
                        break;
                    }
                }
                if (card == null){
                    Bukkit.getLogger().info(path +"の"+ key +"というカードは存在しません");
                    continue;
                }
                specialProbability.put(card,cardProbability);
            }
        }
        cardPack.specialProbability = specialProbability;

        int allAmount = 0;
        List<Card> checkEndCards = new ArrayList<>();
        for (Card card:cardPack.specialProbability.keySet()){
            allAmount += cardPack.specialProbability.get(card);//見つけたら追加
            checkEndCards.add(card);
        }
        for (String rarityName:cardPack.defaultProbability.keySet()){//全てのカードの設定
            int amount = cardPack.defaultProbability.get(rarityName);
            for (Card card:cards){
                if (card.rarity.equals(rarityName)){//該当のレアリティならば
                    if (!checkEndCards.contains(card)){//個別設定されていなければ
                        allAmount += amount;
                    }
                }
            }
        }
        if (allAmount == 0){//もしも何もなかった時の処理
            Bukkit.getLogger().info(path +"のカードパックは何も排出されない設定です");
            Bukkit.getLogger().info("使うとエラーメッセージが出て、何も起こりません");
        }
        cardPack.allAmount = allAmount;
        return cardPack;
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
                    player.sendMessage(prefix +"各レアリティのカードの枚数を表示します");
                    int normalCardAmount = 0;
                    int rareCardAmount = 0;
                    int sRareCardAmount = 0;
                    int legendCardAmount = 0;
                    int secretCardAmount = 0;
                    int otherCardAmount = 0;
                    for (Card card:cards){
                        switch (card.rarity){
                            case "ノーマル":
                                normalCardAmount++;
                                continue;
                            case "レア":
                                rareCardAmount++;
                                continue;
                            case "スーパーレア":
                                sRareCardAmount++;
                                continue;
                            case "レジェンド":
                                legendCardAmount++;
                                continue;
                            case "シークレット":
                                secretCardAmount++;
                                continue;
                            default:
                                otherCardAmount++;
                        }
                    }
                    player.sendMessage(prefix +"全てのカードの数:"+ cards.size());
                    player.sendMessage(prefix +"ノーマルカードの数:"+ normalCardAmount);
                    player.sendMessage(prefix +"レアカードの数:"+ rareCardAmount);
                    player.sendMessage(prefix +"スーパーレアカードの数:"+ sRareCardAmount);
                    player.sendMessage(prefix +"レジェンドカードの数:"+ legendCardAmount);
                    player.sendMessage(prefix +"シークレットカードの数:"+ secretCardAmount);
                    player.sendMessage(prefix +"その他カードの数:"+ otherCardAmount);
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
            player.sendMessage("§e/neop get 読み込まれているカードとカードパック一覧のGUIを見れます");
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
    ItemStack permanentA = null;//永続効果のカード
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
enum BattleProcessType{//進行状態
    Before,Preparation,Running,End
}
class Card{
    //カード識別用
    String fileName = null;
    //以下必須項目
    String name = null;//§l三色団子 など
    String series = null;//初期セット などディレクトリで設定
    short cardDurability = 0;//テクスチャの耐久値 レアリティで自動設定
    String rarity = null;//ノーマル、レア、スーパーレア、レジェンド、シークレット、イベント の6種類　ディレクトリで設定
    //以下任意項目
    List<String> lore = null;//説明文
    //-1の時は発動しないことにします
    //このターン発動する効果 Now
    boolean now = false;//発動するか
    int damageNow = -1;//ダメージ値
    int healNow = -1;//回復値
    int reductionNow = -1;//次の相手のターンダメージ軽減値
    int drawNow = -1;//ドローする枚数
    //次の自分のターン開始時発動する効果 Next
    boolean next = false;
    int damageNext = -1;
    int healNext = -1;
    int reductionNext = -1;
    int drawNext = -1;
    //永続効果（毎ターン開始時実行） permanent
    boolean permanent = false;
    int damagePermanent = -1;
    int healPermanent = -1;
    int reductionPermanent = -1;
    int drawPermanent = -1;
}
class CardPack{
    String name = null;//名前
    List<String> lore = null;//説明文
    int cardAmount = 1;//排出されるカードの数
    List<String> series = null;//どのシリーズが出るか
    //全ての確率の合計値
    int allAmount = 0;
    //確率
    Map<String,Integer> defaultProbability = null;//各カードの未設定時のレアリティの確率（デフォルトでは0）
    Map<Card,Integer> specialProbability = null;//それぞれのカードの確率を上書き（デフォルトでは0）
}