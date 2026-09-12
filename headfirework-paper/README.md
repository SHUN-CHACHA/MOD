# HeadFirework (Paper版)

火薬+染料+プレイヤーヘッドから専用の花火の星を作り、打ち上げると爆発の瞬間にそのプレイヤーの顔が浮かび上がる演出を追加するPaperプラグインです。クライアント側へのMOD導入は不要で、バニラクライアントのまま遊べます。

A Paper plugin that lets you craft firework stars from a player head, gunpowder and dye — when the rocket explodes, that player's face appears at the blast site. No client-side mod required; works with vanilla clients.

- 対応バージョン: Minecraft 26.2 / Paper
- シングルプレイ(統合サーバー)・マルチサーバーどちらも、このプラグインをサーバーへ導入するだけで動作(参加者側への配布は不要)
- Fabric MOD版(`../headfirework/`)とは完全に別実装です。同名ですが混同しないよう注意してください
- 使い方(クラフト方法・コマンド・GUI操作など)は [使用説明書(日本語)](docs/HeadFireworkPaper_使用説明書_JP.md) / [User Guide (English)](docs/HeadFireworkPaper_User_Guide_EN.md) を参照してください

以下は、ソースコードを改造・貢献したい方向けの開発者マニュアルです。

---

# HeadFirework Paper版 開発者マニュアル(改造・貢献ガイド)

このドキュメントは、GitHubでソースを公開する際に「他の人が読んで改造しやすくする」ことを目的にしたマニュアルです。ディレクトリ構成、各ファイルの役割、主要な変数・設定値の意味をまとめています。

対象バージョン: **v1.0.0**

---

## 1. ディレクトリ構成

```
headfirework-paper/
├─ build.gradle.kts            … ビルドスクリプト(依存関係・Javaツールチェーン)
├─ gradle.properties           … Gradle自体が使うJDKパスの固定(org.gradle.java.home)
├─ settings.gradle.kts         … プロジェクト名定義
├─ src/main/java/dev/shuncha/headfireworkpaper/
│   ├─ HeadFireworkPaperPlugin.java  … プラグイン本体(onEnable/onDisable、各コンポーネントの初期化)
│   ├─ HeadFireworkConfig.java       … 設定値の定義・config.ymlへの保存/読込
│   ├─ RecipeManager.java            … 星・ロケットのクラフト判定と生成(★最重要)
│   ├─ FireworkListener.java         … 爆発検知・顔の表示アニメーション(★最重要)
│   ├─ HeadFireworkCommand.java      … /headfirework コマンド一式
│   └─ HeadFireworkGuiListener.java  … /headfirework gui のチェストGUI
└─ src/main/resources/
    ├─ plugin.yml               … プラグインのメタ情報・コマンド/権限定義
    └─ config.yml               … 設定のデフォルト値
```

Fabric MOD版と違い、Mixinや独自レシピクラス(`CustomRecipe`継承)は使っていません。Bukkit APIには「材料の個数が可変のシェイプレスレシピ」を素直に登録する仕組みが無いため、`PrepareItemCraftEvent`でクラフト盤面を直接読み取り、結果アイテムを手動で組み立てる方式(`RecipeManager`)を採用しています。これがFabric版のMixinに相当する処理です。

**改造したい内容ごとの入口はここ:**

| やりたいこと | 触るファイル |
|---|---|
| 顔のサイズ・表示時間のデフォルト値を変える | `HeadFireworkConfig.java`(定数)・`config.yml`(初期配布値) |
| クラフトの材料・条件を変える | `RecipeManager.java` の `tryBuildStar` / `tryBuildRocket` |
| 爆発時の演出(拡大・フェード)のロジックを変える | `FireworkListener.java` の `spawnHead` |
| コマンドに項目を追加する | `HeadFireworkCommand.java` |
| GUIにボタンを追加する | `HeadFireworkGuiListener.java` |

---

## 2. `RecipeManager.java`(クラフト判定・生成)

Bukkitの通常のRecipeでは「染料や頭を複数個まとめて投入」のような可変個数の材料を表現できないため、`PrepareItemCraftEvent`でクラフト盤面(`CraftingInventory`)を直接検証し、結果アイテムを`inv.setResult()`で手動セットする方式を取っています。

### 星のレシピ(`tryBuildStar`)
成立条件:
```java
if (gunpowder != 1 || dyeColors.isEmpty() || headCount == 0) return null;
int shapeItems = fireCharge + feather + goldNugget;
if (shapeItems > 1) return null;          // 形状材料(業火の袋/羽根/金塊)は合計1個まで
if (diamond > 1 || glowstoneDust > 1) return null; // キラキラ・トレイルはそれぞれ0〜1個
```
- 火薬は**必ず1個**。頭は同一プレイヤーのものなら何個置いても構いません(結果は常に星1個ですが、`onCraftItem`で消費された頭の個数を+1して戻しているため、実質的に頭は消費されません)
- 異なるプレイヤーの頭が混在していると不成立(`return null`)になります
- 染料は複数個・複数色を同時に投入可能で、そのまま`FireworkEffect`の複数色として反映されます
- 形状: 未投入→小玉(`BALL`)、業火の袋→大玉(`BALL_LARGE`)、羽根→星形(`STAR`)、金塊→バースト(`BURST`)。クリーパー型(`CREEPER`)はこのレシピからは作れません(バニラのクリーパーヘッド花火と衝突しない設計のため)
- 結果アイテムの`PersistentDataContainer`(キー: `<プラグインID>:owners`)にオーナー名を1件だけ保存します。これが「頭付き花火かどうか」の判定にも使われます(`isOurStar`)

### ロケットのレシピ(`tryBuildRocket`)
```java
if (starCount == 0) return null;
if (paper != starCount) return null;         // 紙は星の個数と同数必須
if (gunpowder < 1 || gunpowder > 3) return null; // 火薬1〜3個で飛翔時間(Power)が変わる
```
- 投入した星(複数プレイヤー混在可・複数個可)の合計個数だけ`FireworkEffect`を集め、結果は**星の個数×3個**の`FIREWORK_ROCKET`になります
- 表示名はオーナー名の重複を除いた一覧を「・」で連結(例: 1人なら「Steve花火」、複数人なら「Steve・Alex花火」)
- PDCには効果の並び順と対応する「オーナー名をカンマ区切りにした文字列」を保存します。これは爆発時に「何番目の爆発エフェクトが誰の顔か」を`FireworkListener`側で引くための対応表です

### 頭の非消費処理(`onCraftItem`)
星のクラフトが成立した場合のみ、1tick後(バニラの消費処理が終わった後)に盤面上のプレイヤーヘッドの個数を+1して戻しています。ロケットのクラフトでは星自体は通常通り消費されます。

---

## 3. `FireworkListener.java`(爆発検知・顔の表示)

### 爆発検知
Bukkitには`FireworkExplodeEvent`が標準で存在するため、Fabric MOD版のような自前監視(`ENTITY_LOAD`+毎tick`isRemoved()`監視)は不要です。イベントから`Firework`エンティティの`FireworkMeta`を取得し、PDCに保存された「オーナー名のカンマ区切り文字列」を`効果(effects)`の並び順に対応させて1件ずつ`spawnHead`を呼びます。

### 顔の表示位置
複数人分の顔が同じ座標に重なって見えなくなるのを防ぐため、「顔が向いている方角に対して垂直な左右方向」に間隔を空けて配置します。
```java
double spacing = Math.max(1.0, targetScale * 0.8);
double offsetAmount = (index - (totalCount - 1) / 2.0) * spacing;
```
向いている方角と同じ軸でずらすと正面から見たときに奥行き方向へ重なって見えてしまうため、あえて垂直方向にずらしている点に注意してください。

### アニメーション
`BukkitRunnable`で毎tick(`runTaskTimer(plugin, 0L, 1L)`)スケールを再計算し、`ItemDisplay#setTransformation`で反映しています。

1. `tick < animationDuration` … 拡大区間(0 → targetScale)
2. `fadeStartTick <= tick < displayDuration` … フェードアウト区間(targetScale → 0)、`fadeStartTick = displayDuration - fadeDuration`
3. それ以外 … `targetScale`を維持

標準の`ItemDisplay`には透過度(アルファ値)の仕組みが無いため、フェードアウトは「スケールを0まで縮小させる」疑似的な演出です(Fabric MOD版と同じ方式)。

**Paper版はBukkit公開APIの`setItemStack`/`setTransformation`/`setBillboard`をそのまま使えるため、Fabric版で必要だった「`entity.load()`が座標・アイテムをリセットしてしまう問題への毎tick再適用」は不要です。** これはPaper版ならではの実装の簡潔さです。

### 向きの制御
`Display.Billboard.FIXED` + `ItemDisplayTransform.NONE` + `setRotation(0f, 0f)`でエンティティ自体の向きをリセットしたうえで、`Transformation`のクォータニオン回転(Y軸のみ)で向きを制御しています。**顔自体が向く角度は`config.facingYawDegreesFor(ownerName)`で、その顔の持ち主本人の個人設定(無ければサーバーのデフォルト)を使います。一方、複数人の顔を横に並べる際の「並べる軸」の計算(`perpX`/`perpZ`)は、個人ごとに軸がバラバラにならないよう、あえてサーバー全体のデフォルト向き(`config.facingYawDegrees()`)のまま統一しています。** つまり「顔の向き」は個人設定、「顔を並べる位置」はサーバー共通、という役割分担です。実機検証済みの角度の値:

| 方角 | Yaw |
|---|---|
| 北 (north) | 0° |
| 西 (west) | 90° |
| 南 (south) | 180° |
| 東 (east) | 270° |

Fabric MOD版とは値の対応関係が異なるので、移植の際に混同しないよう注意してください。

### デバッグ用コマンドの実体(`spawnDebugHead`)
`/headfirework testhead <pitch> <roll> <yaw>`から呼ばれます。プレイヤーの視線方向3ブロック先に、指定した角度の頭を1つ出し、5秒(100tick)後に自動で消えます。再ビルド不要でその場に回転角度を確認できるデバッグ用の仕組みで、動作には影響しないため残したままにしてあります。

---

## 4. `HeadFireworkConfig.java` / `config.yml`(設定値)

```yaml
scale:
  small_ball: 5.0
  large_ball: 10.0
  star_burst: 14.0   # star / burst / creeper で共通
display_duration: 60     # 表示合計時間(tick、60tick=3秒)
animation_duration: 10   # 拡大アニメーションの時間(tick)
fade_duration: 10        # フェードアウトの時間(tick)
facing: south             # 顔が正面を向く方角(サーバー全体のデフォルト)
player_settings:          # プレイヤーごとの個人設定(/headfirework myface)
  ShunCha2525:
    facing: east
```
- `load()`でプラグイン起動時に`config.yml`から読み込み、`save()`で書き戻します(`plugin.onDisable()`時と、コマンド/GUIで値を変更した直後に呼ばれます)
- `resetScale`/`resetDisplayDuration`等のリセットメソッドは、実行時の値をコード上の`DEFAULT_*`定数に戻すだけで、`config.yml`ファイル自体は書き換えません(次の`save()`で上書きされます)
- クリーパー型(`CREEPER`)は`RecipeManager`のレシピからは生成されませんが、バニラの通常花火(頭無し)がクリーパー型で爆発した場合に備えて`getScale`のマッピングは残しています
- `player_settings.<プレイヤー名>.facing`は、プレイヤー本人が`/headfirework myface`で設定した「自分の花火に映る顔の向き」の個人設定です。`Map<String, BlockFace> playerFacing`(キーは小文字化したプレイヤー名)として保持し、`save()`時は`player_settings`セクションごと一度クリアしてから書き直すため、`/headfirework myface reset`で削除されたプレイヤーが古い値のまま残ることはありません。今は「向き」だけですが、将来サイズ等の個人設定を増やす場合もこのMapと同じ要領で拡張できます
- `facingYawDegreesFor(String playerName)`が、花火の持ち主(オーナー)ごとの実際のYaw角度を返す入口です。個人設定があればそれを、無ければサーバー全体の`facing`をフォールバックとして返します。**花火を作った時点の設定は保存されず、爆発した瞬間の"現在の"設定を都度参照する(現在設定方式)**という仕様です

---

## 5. `HeadFireworkCommand.java` / `HeadFireworkGuiListener.java`(設定変更手段)

サーバー全体の設定(管理者用)と、プレイヤー本人の個人設定の2系統があります。

| コマンド | 権限 | 内容 |
|---|---|---|
| `/headfirework config ...` | `headfirework.admin` | サーバー全体のデフォルト設定 |
| `/headfirework gui` | `headfirework.admin` | サーバー全体のデフォルト設定用チェストGUI |
| `/headfirework myface <方角\|show\|reset>` | 誰でも(`headfirework.use`) | 自分の花火の顔の向きの個人設定 |
| `/headfirework mygui` | 誰でも(`headfirework.use`) | 個人設定用の簡易チェストGUI(方角ボタン+リセット+閉じるのみ) |
| `/headfirework testhead <pitch> <roll> <yaw>` | 誰でも(`headfirework.use`) | デバッグ用 |

**権限モデルの注意点(過去の記載の誤りを修正):** 以前は`plugin.yml`の`commands.headfirework.permission`に直接`headfirework.admin`を指定していたため、`/headfirework`コマンド全体(`testhead`も含む)がop専用になっていました。現在は誰でも実行できる`headfirework.use`(デフォルトtrue)をコマンド自体の権限にし、`config`/`gui`サブコマンドだけ`HeadFireworkCommand.onCommand()`内で`sender.hasPermission("headfirework.admin")`を個別チェックする方式に変更しています。新しく管理者専用のサブコマンドを追加する場合は、同じように内部チェックを足してください(`plugin.yml`側の権限を変更する必要はありません)。

GUIには`animation_duration`(拡大アニメーション時間)のスライダーがありません(管理者用GUIのみの制約で、個人設定GUIにはそもそも対象外)。追加する場合は`HeadFireworkGuiListener.render()`にスロットを割り当てて`onClick`に分岐を追加してください(手順はコマンド側の`display_duration`/`fade_duration`の実装を参考にすると早いです)。

個人設定GUI(`/headfirework mygui`)は、管理者用GUIとは別の`InventoryHolder`実装(`PersonalHolder`、開いたプレイヤーのUUIDを保持)を使って見分けています。`onClick`は`getHolder()`の型で管理者用/個人用に処理を振り分ける構造です。`InventoryHolder`実装クラスの`getInventory()`を正しく実装しないと、内部的に呼ばれた際にクライアントが切断されるバグを踏みます(開発中に実際に発生した問題です)。

---

## 6. ビルド方法

```
cd path\to\headfirework-paper
.\gradlew.bat build
```
成功すると`build/libs/headfirework-paper-<バージョン>.jar`が生成されます。`gradle.properties`の`org.gradle.java.home`でGradle自体が使うJDK17のパスを固定しつつ、`build.gradle.kts`のtoolchain指定でコンパイルにはJDK25を使う構成になっています(Fabric MOD版と同じ考え方です)。

### 動作確認手順
```
cd C:\MC-test-server
& "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" -Xmx2G -Xms2G -jar paper-26.2-123.jar --nogui
```
ビルド後は`build/libs/headfirework-paper-<バージョン>.jar`をテストサーバーの`plugins`フォルダにコピーして再起動してください。

---

## 7. 既知の残作業・TODO

- `RecipeManager.java`・`FireworkListener.java`内の`[DEBUG]`プレフィックス付きログ、および`/headfirework testhead`コマンドは開発中のデバッグ用として残したままです(削除するかどうかは未確定)
- GUIに`animation_duration`の調整項目が無い(現状はコマンドのみ対応)
- マルチサーバー環境での動作確認は未実施
- 複数人組み合わせ(異なるプレイヤーの星を1発のロケットにまとめて打ち上げる場合)の追加検証は未実施
- このフォルダはまだGitHubリポジトリ(`headfirework-262`)の外にあり、未push

## 8. コントリビュート時のお願い

- 日本語コメント・文字列を含むファイルをテキストエディタで編集する場合、文字コードは**UTF-8(BOM無し)**で保存してください(PowerShell経由の書き込みは文字化けしやすいため非推奨です)
- Pull Requestを送る際は、テストサーバーでの動作確認をお願いします
