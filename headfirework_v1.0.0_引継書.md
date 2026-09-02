この会話は長くなってきたので、このまま貼り付けて新しいチャットに引き継げる引継書を作成しました。

---

## HeadFirework MOD開発の引き継ぎ(v1.0.0 完成・機能拡張フェーズ)

**目的**: プレイヤーヘッド花火MOD。花火ロケットが通常通り打ち上がり、爆発の瞬間にプレイヤーの頭が数秒間、花火の形状(小玉/大玉/星型等)に応じたサイズで、小さい状態から拡大しながら出現・表示される演出。クラフトは「火薬+染料(複数可)+プレイヤーヘッド+(業火の袋/羽根/金塊のいずれか1つ・任意)+(ダイヤモンド・任意)+(蓄光石・任意)→専用の星」「星+紙+火薬(1〜3個)→ロケット」(バニラと同じ配置自由のシェイプレスレシピ)。シングルプレイでもマルチサーバーでも同じMOD1つで動作すること(参加者側への配布は不要)が要件。

**環境**: Windows、PrismLauncher(インスタンス名「アツクラシーズン2」、実機フォルダ`D:\PrismLauncher\instances\アツクラシーズン２\minecraft`)、実際のプレイ環境はMinecraft 26.2。プロジェクトパスは`path\to\headfirework-262`。パッケージ名は`dev.shuncha.headfirework`、作者名はshuncha。

**バージョン**: 1.0.0として区切り。主要機能は全て実装・実機確認済み。

### v1.0.0で完成している機能(すべて実機確認済み)

1. **基本機能**: 爆発検知(`ServerEntityEvents.ENTITY_LOAD`+毎ティック`isRemoved()`監視)、星・ロケットの2レシピ(`CustomRecipe`継承、`data/headfirework/recipe/`配下にレシピJSON必須)、バニラレシピとの競合回避Mixin
2. **形状連動サイズ**: `HeadFireworkConfig.scaleForShape(FireworkExplosion.Shape)`で小玉/大玉/星型ごとにスケール変更。デフォルト値は小玉5.0、大玉10.0、星型/クリーパー/バースト14.0
3. **拡大アニメーション**: 爆発直後は目標サイズの20%から始まり、10tick(0.5秒)かけてlerpで目標サイズまで拡大
4. **飛翔時間・形状効果**: 火薬の個数(1〜3)で飛翔時間が変化(バニラ準拠)。業火の袋→大玉、羽根→星形、金塊→バーストのいずれか1つ選択可。ダイヤモンド→キラキラ効果、蓄光石→トレイル効果。染料は複数個で複数色対応
5. **`/headfirework config`コマンド**: `scale <形状> <値>`、`display_duration <tick数>`、`animation_duration <tick数>`、`show`(現在値確認)。op権限(`Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`)必須
6. **GUI設定画面**: Ctrl+Jキーで開く。小玉/大玉/星型のスライダー、適用ボタン(裏側で`/headfirework config scale`コマンドを自動送信、マルチサーバーでのMOD配布不要要件を維持)、RESETボタン(スライダー表示のみデフォルト値に戻す。実際の反映には別途「適用」が必要)
7. **MODアイコン**: `fabric.mod.json`の`icon`欄+`assets/headfirework/icon.png`でMod Menu上に表示
8. **プレイヤーヘッド花火の名前付け**(直近の作業、ビルド未確認): 星・ロケット双方に`DataComponents.CUSTOM_NAME`で「(プレイヤー名)花火」、名前不明時は「プレイヤー(名称不明)花火」を設定。`PlayerHeadFireworkStarRecipe.buildFireworkName(ResolvableProfile)`という共通静的メソッドを実装し、ロケット側からも呼び出す方式

### 直前の作業内容(ビルド・実機確認まだ)

`PlayerHeadFireworkStarRecipe.java`と`PlayerHeadFireworkRocketRecipe.java`の両方に、`profile.name()`(Optional<String>)からプレイヤー名を取得し、`Component.literal(...)`で`DataComponents.CUSTOM_NAME`にセットする処理を追加した。`ResolvableProfile.name()`の返り値の型が26.2で変わっている可能性があり、ビルドエラーが出た場合は`javap`で`ResolvableProfile`の構造を確認する必要がある。

### 中止・未着手の項目

- **透過度50%**: 標準の`Display.ItemDisplay`にはアルファ(透過度)を設定する仕組みが存在しないため中止済み。再開する場合はクライアント側レンダリングへのMixin実装(難易度高)が必要
- フェードアウト演出(現状は表示時間経過後に瞬間消滅)は未着手のまま

### 次にやること

1. 名前付け機能のビルド・実機確認(星・ロケットをホバーした際に「(プレイヤー名)花火」と表示されるか)
2. 問題があれば`javap`でminecraft-merged.jar(`C:\Users\syun_\.gradle\caches\fabric-loom\26.2\minecraft-merged.jar`)を調査しながら1つずつ修正、という進め方で継続

### この会話で得た教訓(次のトラブルシューティングに有用)

- **26.2はMojang公式マッピング(非難読化)**。1.20.4のYarn実装をそのまま流用できず、都度`javap`で実クラス・実メソッド名を確認する必要がある
- **PowerShellでのファイル書き込みは日本語が文字化けしやすい**。日本語を含む変更は必ずNotepadで直接編集し、「名前を付けて保存」でUTF-8(BOM無し)を明示する
- **`display.load(input)`は座標・アイテムスロットの両方を巻き戻す**。毎tick呼ぶ場合は、その都度`setPos()`・アイテム再セットも一緒に行う必要がある
- **レシピシリアライザーの登録だけでは不十分**で、`data/<mod_id>/recipe/`配下にレシピJSON(typeフィールドでシリアライザーを指定)が必須
- **26.1以降、染料の色は`DataComponents.DYE`コンポーネント方式**(旧`DyeItem`ハードコード判定は機能しない)
- **ロケットの爆発形状は`DataComponents.FIREWORK_EXPLOSION`ではなく`DataComponents.FIREWORKS`(Fireworksレコード、explosions()リスト)に入っている**。星の時点では`FIREWORK_EXPLOSION`だが、ロケットになると`FIREWORKS`にラップされる点に注意
- **26.2のクライアントAPIは大幅刷新されている**: `GuiGraphics`は廃止され`extractRenderState(GuiGraphicsExtractor, ...)`方式に、`KeyBindingHelper`→`KeyMappingHelper`(パッケージも`keybinding`→`keymapping`)、`KeyMapping.Category`は`register(Identifier)`で作る専用Recordクラスに、`client.setScreen`→`setScreenAndShow`に変更されている
- **26.2の権限チェックAPIも刷新**: 旧`hasPermission(int level)`は廃止され、`Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`のような`PermissionCheck`定数+`PermissionProviderCheck`(Predicate実装)方式になっている
- **`javap`の対象がネストしたRecord/内部クラスの場合、`$`を含むクラス名はPowerShellでは`'net.minecraft.client.KeyMapping$Category'`のようにシングルクォートで囲むか、`` `$ ``でエスケープしないと変数展開されて意図通りに渡らない**
- **ユーザーが操作設定画面で一度手動でキーバインドを変更すると、コード側のデフォルト値を変えても上書きされない**(保存済み設定が優先される)。キー変更時は操作設定画面での手動再設定も必要になる場合がある
- ビルド後にコマンドを実行する際、PowerShellの作業ディレクトリが`javap`調査用のフォルダ(`.gradle\caches\...`)のままになっていることが多発した。ビルド前は必ずプロジェクトフォルダ(`cd "path\to\headfirework-262"`)に戻ってから`.\gradlew.bat build`を実行すること
