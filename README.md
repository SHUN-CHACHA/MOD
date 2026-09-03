# HeadFirework

花火ロケットが通常通り打ち上がり、爆発の瞬間に、使用したプレイヤーヘッドの持ち主の顔が花火の形状に応じたサイズで拡大しながら出現し、しばらく表示された後にフェードアウトして消える演出を追加するFabric MODです。

Fire a rocket, and when it explodes, the face of the player behind the head you crafted it with appears — growing in, then fading away.

- **Modrinth**: [https://modrinth.com/mod/headfirework-mod](https://modrinth.com/mod/headfirework-mod)
- 対応バージョン: Minecraft 26.2 / Fabric Loader / Fabric API
- シングルプレイ・マルチサーバーどちらも同じMOD1つで動作(参加者側への配布は不要)
- 使い方(クラフト方法・GUI操作など)は [使用説明書(日本語)](docs/HeadFirework_使用説明書_JP.md) / [User Guide (English)](docs/HeadFirework_User_Guide_EN.md) を参照してください

以下は、ソースコードを改造・貢献したい方向けの開発者マニュアルです。

---

# HeadFirework 開発者マニュアル(改造・貢献ガイド)

このドキュメントは、GitHubでソースを公開する際に「他の人が読んで改造しやすくする」ことを目的にしたマニュアルです。
ディレクトリ構成、各ファイルの役割、主要な変数・設定値の意味をまとめています。

対象バージョン: **v1.1.0**

---

## 1. ディレクトリ構成

```
headfirework-262/
├─ gradle.properties          … ビルド設定・バージョン番号(mod_version)・使用JDKパス等
├─ build.gradle                … ビルドスクリプト(Loom, 依存関係)
├─ src/main/java/dev/shuncha/headfirework/
│   ├─ HeadFireworkMod.java            … サーバー側メインロジック(★最重要)
│   ├─ HeadFireworkConfig.java         … 設定値の定義・保存/読込
│   ├─ HeadFireworkModClient.java      … クライアント専用エントリーポイント(キーバインド登録)
│   ├─ HeadFireworkConfigScreen.java   … GUI設定画面
│   ├─ PlayerHeadFireworkStarRecipe.java    … 「星」のクラフトレシピ
│   ├─ PlayerHeadFireworkRocketRecipe.java  … 「ロケット」のクラフトレシピ
│   └─ mixin/
│       ├─ FireworkStarRecipeMixin.java     … バニラの星レシピとの競合回避
│       └─ FireworkRocketRecipeMixin.java   … バニラのロケットレシピとの競合回避
├─ src/main/resources/
│   ├─ fabric.mod.json          … MODのメタ情報(ID, バージョン, エントリーポイント等)
│   ├─ headfirework.mixins.json … Mixin設定ファイル
│   ├─ assets/headfirework/
│   │   ├─ icon.png             … MODアイコン
│   │   └─ lang/
│   │       ├─ en_us.json       … 英語翻訳
│   │       └─ ja_jp.json       … 日本語翻訳
│   └─ data/headfirework/recipe/
│       ├─ crafting_player_head_firework_star.json   … 星レシピの登録JSON
│       └─ crafting_player_head_firework_rocket.json … ロケットレシピの登録JSON
```

**改造したい内容ごとの入口はここ:**

| やりたいこと | 触るファイル |
|---|---|
| 顔のサイズ・表示時間のデフォルト値を変える | `HeadFireworkConfig.java` |
| クラフトの材料・条件を変える | `PlayerHeadFireworkStarRecipe.java` / `PlayerHeadFireworkRocketRecipe.java` |
| 爆発時の演出(拡大・フェード)のロジックを変える | `HeadFireworkMod.java` の `onServerTick` / `HeadAnimation` |
| GUIにボタン・スライダーを追加する | `HeadFireworkConfigScreen.java` |
| 表示文言・翻訳を追加する | `assets/headfirework/lang/*.json` |
| キーバインドを変える | `HeadFireworkModClient.java` |

---

## 2. `HeadFireworkMod.java`(サーバー側メインロジック)

サーバー側で動く、このMODの心臓部です。3つの役割があります。

### (a) 爆発検知
```java
private static final Map<Integer, FireworkRocketEntity> watchedRockets = new ConcurrentHashMap<>();
```
- `ServerEntityEvents.ENTITY_LOAD` で花火ロケットの出現を検知し、このMapに登録
- 毎tick(`ServerTickEvents.END_SERVER_TICK`)で `isRemoved()` を監視し、消えたら「爆発した」とみなす
- Minecraftには「花火が爆発した」というイベントが存在しないため、この自作の監視方式を採用しています

### (b) 顔の表示・アニメーション
```java
private static final Map<UUID, HeadAnimation> animatingHeads = new ConcurrentHashMap<>();

private record HeadAnimation(
    Display.ItemDisplay display, ServerLevel serverLevel, ItemStack headStack,
    double posX, double posY, double posZ,
    float targetScale, int startTick,
    int durationTicks,        // 拡大アニメーションの長さ(tick)
    int displayDurationTicks, // 出現〜消滅までの合計表示時間(tick)
    int fadeDurationTicks     // 末尾でのフェードアウト(縮小)時間(tick)
) {}
```

`onServerTick`内の該当ループで、経過tick数(`elapsed`)に応じてスケールを3段階で計算しています。

1. `elapsed < durationTicks` … 拡大区間(小さい状態→`targetScale`まで`Mth.lerp`で拡大)
2. `fadeStartTick <= elapsed < displayDurationTicks` … フェードアウト区間(`targetScale`→0まで縮小)
   - `fadeStartTick = displayDurationTicks - fadeDurationTicks`
3. それ以外 … `targetScale`を維持(拡大後・フェード前の「静止区間」)

**重要な注意点:** `display.load(input)` を呼ぶと座標とアイテムスロットの両方がリセットされてしまう仕様があります。そのため毎tick、`setHeadTransformation()`(スケール適用)→`setPos()`→`getSlot(0).set()`の3点をすべて再適用しています。ここを省略すると位置ズレやアイテム消失のバグが再発するので注意してください。

透過度(アルファ値)による本来のフェードは、標準の`Display.ItemDisplay`には実装されていないため未対応です。対応するにはクライアント側レンダリングへのMixinが必要です(難易度高)。

### (c) コマンド
`/headfirework config ...` の各サブコマンドをBrigadierで登録しています。権限は `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`(オペレーター相当)が必要です。

---

## 3. `HeadFireworkConfig.java`(設定値)

```java
public float scaleSmallBall = 5.0f;   // 小玉のサイズ倍率
public float scaleLargeBall = 10.0f;  // 大玉
public float scaleStar = 14.0f;       // 星型
public float scaleCreeper = 14.0f;    // クリーパー
public float scaleBurst = 14.0f;      // バースト

public int animationDurationTicks = 10;   // 拡大にかける時間(1tick=1/20秒)
public float animationStartRatio = 0.2f;  // 拡大開始時のサイズ比率(targetScaleの何%から始まるか)
public int displayDurationTicks = 60;     // 表示合計時間(=3秒)
public int fadeDurationTicks = 10;        // フェードアウト時間
```

- 実行時の値は `config/headfirework.json` に自動保存され、次回起動時に読み込まれます(Gson使用)
- `DEFAULT_*` という定数が別途あり、GUIのRESETボタンや `resetToDefaults()` はこちらを参照します。**バランス調整するときはデフォルト値の変更なのか、実行時値の変更なのかを意識してください**(デフォルト値だけ変えても、既に保存済みの`headfirework.json`がある環境では上書きされません)
- `scaleForShape(FireworkExplosion.Shape)` が形状→サイズの変換を行う入口です。新しい形状を追加したい場合はここに分岐を足します

---

## 4. クラフトレシピ(`PlayerHeadFireworkStarRecipe` / `PlayerHeadFireworkRocketRecipe`)

どちらも `CustomRecipe` を継承したシェイプレスレシピで、`matches()`(材料の判定)と `assemble()`(結果アイテムの生成)の2メソッドが中心です。

### 星のレシピの条件(`matches`)
```java
return gunpowder == 1
    && head == 1
    && dye >= 1 && dye <= 8
    && shapeItems <= 1     // 業火の袋+羽根+金塊の合計が1個以下
    && glowstoneDust <= 1
    && diamond <= 1
    && other == 0;         // 上記以外のアイテムが混ざっていたら不成立
```
材料の種類・上限個数を変えたい場合はここを編集します。例えば染料の上限を増やしたいなら `dye <= 8` の数字を変更するだけです。

### 形状の決定ロジック(`assemble`)
```java
if (hasFireCharge) shape = FireworkExplosion.Shape.LARGE_BALL;
else if (hasFeather) shape = FireworkExplosion.Shape.STAR;
else if (hasGoldNugget) shape = FireworkExplosion.Shape.BURST;
else shape = FireworkExplosion.Shape.SMALL_BALL;
```
新しい材料で新しい形状(例: `CREEPER`)を選べるようにしたい場合は、`matches`側の判定と`assemble`側のif分岐の両方に追記が必要です。

### 名前付け
`PlayerHeadFireworkStarRecipe.buildFireworkName(ResolvableProfile)` が「(プレイヤー名)花火」という名前を生成する共通メソッドです。ロケット側もこれを呼び出しているので、命名ルールを変えたい場合はここ1箇所を直せば両方に反映されます。

### レシピJSON(`data/headfirework/recipe/*.json`)
`RecipeSerializer` を登録しただけではMinecraftはレシピを認識しません。`type`フィールドでシリアライザーを指定したJSONファイルが別途必須です(既存の2ファイルを参考にしてください)。

---

## 5. Mixin(`mixin/`パッケージ)

バニラの `FireworkStarRecipe` / `FireworkRocketRecipe` の `matches()` にHEAD injectし、「オーナー情報付きプレイヤーヘッド」または「専用の星」が含まれる場合は `false` を返すことで、バニラレシピが先に反応してしまう競合を防いでいます。

新しいバニラレシピと競合するようになった場合は、同じパターンでMixinを追加してください。オーバーロードされたメソッドをinjectする際は `@Injectメソッド` 指定にディスクリプタの完全指定が必要な点に注意してください。

---

## 6. GUI(`HeadFireworkConfigScreen.java`)

- `Ctrl+J` で開く設定画面。`ScaleSlider`(小数値用)と`TickSlider`(tick整数値用)の2種類のカスタムスライダークラスがあります
- 各スライダーは`Component.translatable(翻訳キー, ...)`でラベルを表示するため、新しい項目を追加する場合は`lang/en_us.json`・`lang/ja_jp.json`にも対応するキーを追加してください
- 「適用」ボタンは、画面上の値をそのまま`/headfirework config ...`コマンドとしてサーバーに送信する仕組みです(`sendConfigCommand()`)。**GUIの見た目を変えても、対応するコマンドが存在しないと何も反映されない**ので注意してください
- 各スライダー横の「R」ボタンは、その項目だけ`clearWidgets()`→`init()`で再構築して初期値に戻す仕組みです。他の項目の入力中の値は`this.smallBall`等のフィールドに保持されているため消えません

新しい設定項目を追加する手順の目安:
1. `HeadFireworkConfig.java` にフィールド+デフォルト定数を追加
2. `HeadFireworkMod.java` の `registerCommands()` に対応するコマンド分岐を追加
3. `HeadFireworkConfigScreen.java` に `addTickRow`/`addScaleRow` の呼び出しを追加
4. `lang/*.json` に表示用の翻訳キーを追加

---

## 7. 26.2特有の注意点(Minecraft本体側の仕様)

Mojang公式マッピング(非難読化)のため、1.20.4時代のYarnマッピング名は流用できません。実クラス名が分からない場合は、`javap`で `minecraft-merged.jar`(Loomのキャッシュ内)を直接調査するのが確実です。

- 独自レシピは `CustomRecipe`(旧`SpecialCraftingRecipe`)を継承
- アイテムのオーナー情報・爆発形状などはNBT直接操作ではなく `DataComponents`(`PROFILE`, `FIREWORK_EXPLOSION`, `FIREWORKS`, `DYE`等)経由
- `Display.ItemDisplay`へのtransformation設定は `entity.load(ValueInput)` 経由(`TagValueInput.create(...)`で生成)。ただしアイテムスロットの設定は公開APIの `getSlot(int).set(ItemStack)` で直接可能
- 権限チェックは `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` 方式(旧`hasPermission(int)`は廃止)
- クライアントAPIは `GuiGraphics` が廃止され `extractRenderState(GuiGraphicsExtractor, ...)` 方式に、`KeyBindingHelper`→`KeyMappingHelper`に変更されています

---

## 8. ビルド方法

```
cd "path\to\headfirework-262"
.\gradlew.bat build
```
成功すると `build/libs/` にjarファイルが生成されます。`gradle.properties` の `org.gradle.java.home` でJDK25のパスを固定しているため、通常は事前のPATH切り替え作業は不要です。

## 9. コントリビュート時のお願い

- 日本語コメント・文字列を含むファイルをテキストエディタで編集する場合、文字コードは **UTF-8(BOM無し)** で保存してください(PowerShell経由の書き込みは文字化けしやすいため非推奨です)
- Pull Requestを送る際は、実機(シングルプレイ)での動作確認をお願いします
