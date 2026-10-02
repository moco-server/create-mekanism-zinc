# クライアント受け入れ確認

現行`0.3.0-dev`はKotlin化した版です。下記はJava版の検収履歴であり、Kotlin版のGUI試遊は未実施です。試遊時は別profileへKFF 5.5.0を追加し、既存profileを保持してください。ビルド・33 GameTests・保存互換の結果は[verification.md](verification.md)に記録しています。

2026-10-01、2倍と4倍までの確認報告に続き、ユーザーから「5倍ラインも正常動作を確認した」と報告を受けました。2〜5倍加工ラインのクライアント動作確認は検収済みです。入力全種類の個別実測や7素材すべての表示のチェックは別途未記録です。今回の確認は Issue #7 の全体完了や公開配布を意味しません。

## キャンバスから切り出した版 (`0.2.3-dev`)

ユーザーが期待していたのは、提示した1枚の生成キャンバスに描かれた5つのアイコンそのものでした。`0.2.2-dev` では統括がキャンバスを参考案と誤って扱い、別に描いた16×16のPNGを同梱していたため、提示画像とゲーム内画像が食い違いました。

`0.2.3-dev` はそのキャンバスから5枚を切り出し、縮小・描き直し・色変更をせず、透明余白で512×512へ整えた版です。切り出し範囲は [`artwork/zinc-v2-crops.json`](../artwork/zinc-v2-crops.json)、プレビューは実際の本番PNGを並べた [`artwork/previews/zinc-v2-cropped.png`](../artwork/previews/zinc-v2-cropped.png) です。表示名 **汚れた亜鉛の粉** と **亜鉛の凝塊** は維持します。2026-10-02、ユーザーから「確認した。十分実用的なので良さそう」と報告を受け、切り出した5アイコンのゲーム内表示をクライアント検収済みとしました。2〜5倍加工の検収結果と合わせ、`0.2.3-dev` を今回の検収版として記録します。

通常JARは親の `dist/cmz-trial/create-mekanism-zinc-0.2.3-dev.jar` に用意し、停止中のWindows試遊クライアントへ導入しました。元キャンバスとの全RGBA値、本番PNG・プレビュー・JAR内画像の一致、ビルド、再生成差分0を統括が検収済みです。初回08:00 JSTはJavaのGCスレッドでネイティブクラッシュしましたが、同一構成で再起動し、08:03 JSTのログで新版・JEI・NVIDIA描画・音声・GUI atlasの初期化を確認しました。プロファイルIDは `moco-cmz-trial-0.2.0-dev-client` のまま、JEI・保存ワールド・設定を保持し、専用サーバーは起動していません。

## 過去の参考画像に合わせた改訂版 (`0.2.2-dev`)

ユーザーの実ゲーム内での指摘を受け、zinc-v1の見た目を採用確定とはせず、5枚をzinc-v2へ描き直しました。添付5枚は形・粒感・陰影の参考とし、元画像の加工・トレースではなく新規ドット配置から作成しています。粉は低く広い粒の山、凝塊は一体の鉱物片、欠片は短い折れた形、結晶は斜めの1本へ変更しました。

表示名は **汚れた亜鉛の粉** と **亜鉛の凝塊** に修正しました。ほかの翻訳とスラリー名はこの改訂では変更していません。通常JARは親プロジェクトの `dist/cmz-trial/create-mekanism-zinc-0.2.2-dev.jar`、プレビューは [`artwork/previews/zinc-v2.png`](../artwork/previews/zinc-v2.png) です。レシピ・タグ・本番Javaクラスは前版とバイト単位で同一です。新版の表示確認はユーザー確認待ちです。Windows Javaが停止中だったため、2026-10-02 07:39 JSTに試遊インスタンスを新版へ更新し、サーバー接続指定なしで再起動しました。旧0.2.1-dev JARをmods外へ退避し、JEI・ワールド・設定を保持しました。ログで0.2.2-dev・JEI・NVIDIA描画・GUI atlas作成を確認しました。下記の0.2.1-dev起動記録は履歴として維持します。

## 新規アイコン版の確認

`0.2.1-dev` は5アイテムの16×16画像を新規制作した版です。加工確認済みの `0.2.0-dev` とは別のJARとして用意しました。2026-10-02、ユーザーのシングルプレイ確認希望により、停止中のWindows試遊インスタンスを `0.2.1-dev` へ更新して起動しました。プロファイルIDは `moco-cmz-trial-0.2.0-dev-client` のままです。旧JARはmods外へ退避し、Create・Mekanism・JEIのハッシュを照合して保持しました。ワールドと設定は編集していません。07:20 JSTのログで新版とJEIの読み込み、NVIDIA描画、GUI atlas作成を確認しました。起動コマンドにサーバー接続指定はなく、専用サーバーは起動していません。見た目のユーザー確認はまだ待ちです。制作方法と外部画像のライセンス確認は [画像の記録](assets.md) にあります。

親プロジェクトの `dist/cmz-trial/create-mekanism-zinc-0.2.1-dev.jar` を利用できます。既存プロファイルで試す場合はゲームを終了してから、同じMODの旧JARを新版へ置き換え、二重に導入しないでください。マルチプレイで新版を試す場合は、検証用サーバーにも同じ版の通常JARを使います。現在の25566試遊サーバーは `0.2.0-dev` のままです。以下の加工検収結果と専用サーバー／Windowsプロファイルの構成は、実際に確認した `0.2.0-dev` の記録を維持します。

## 対象プロファイル

- 新規Prismインスタンス `moco-cmz-trial-0.2.0-dev-client` はコントローラーが作成し、Minecraft 1.21.1 / NeoForge 21.1.252 / LWJGL 3.3.3、通常3 JARのSHA-512、21レシピ、fixture非同梱を独立確認済みです。保存先は `C:\Users\wwwdd\AppData\Roaming\PrismLauncher\instances\moco-cmz-trial-0.2.0-dev-client` です。ワールド、既存設定、認証情報はコピーしていません。
- 2026-10-01 20:37 JST、Windows版Prismから新版を起動し、0.2.0-devの読み込みとNVIDIAによる描画初期化をログで確認しました。20:39 JSTにクライアントと専用サーバー両側のログで接続・ワールド参加も確認しました。この時点では7素材の表示と3〜5倍の実加工はユーザー確認待ちでした。後続の2〜5倍の確認報告は冒頭の状態に反映しています。旧0.1.0-devインスタンスのワールド／MOD構成は変更していません。
- Minecraft 1.21.1、NeoForge 21.1.252、Java 21、Create 6.0.10、Mekanism 10.7.19.85 (Modrinth build 85)。対応範囲は[互換性表](compatibility.md)の固定対象のみです。
- 現在のクライアントaddonは `dist/cmz-trial/create-mekanism-zinc-0.2.3-dev.jar`（独立MOD内では `neoforge/build/libs/create-mekanism-zinc-0.2.3-dev.jar`）です。加工検収は旧 `0.2.0-dev` で行いました。GameTest fixtureの `-gametests.jar` は入れません。
- 試遊サーバーは専用の新規ワールドで、IPv4 `127.0.0.1:25566` に接続します。`localhost` ではなくIPv4を直接指定してください。Windowsで `localhost` がIPv6へ解決される場合の接続失敗を避けます。

## レシピビューア

今後の対話的な試遊環境では、対象版と連携MODに対応したEMIまたはJEIを用意します。現在のWindows試遊インスタンスには、Mekanismの同梱JEI連携を利用するため、[JEI 19.51.0.418 / Modrinth版ID ufHUqt9b](https://modrinth.com/mod/jei/version/ufHUqt9b)をクライアント側だけに追加しました。専用サーバーは従来の通常3 JARのままです。JEIは本アドオンの必須依存ではありません。2026-10-01 20:56〜20:57 JST、Windows起動ログでJEIの読み込み、初期設定処理、GUI atlas作成を確認しました。その後、ユーザーから5倍の正常動作報告を受けました。JEIの個別画面チェック結果は別途報告されていません。

- ファイル: `jei-1.21.1-neoforge-19.51.0.418.jar`、1,804,792 bytes。
- 取得元: `https://cdn.modrinth.com/data/u6dRKJwZ/versions/ufHUqt9b/jei-1.21.1-neoforge-19.51.0.418.jar`
- SHA-512: `a6ca3abc3080f6f41033a77cb955fefe9f9ebae795c33546227f555b2d31e8d4131cc705126d14a3fbabb31097fc0e876dc0bbeb05ad907a1b796f21e57527bf`

[JEI作者の操作説明](https://www.curseforge.com/minecraft/mc-mods/jei)に従い、アイテムにカーソルを合わせて **R** で作り方、**U** で用途を開きます。レシピ一覧が隠れている場合は **Ctrl+O** で切り替えます。まず「亜鉛の結晶」でRを押し、きれいなスラリー200 mBからの結晶化を調べると5倍工程をたどれます。

5倍の入口は **化学溶解室 + 硫酸** です。鉱石1個から汚れたスラリー1000 mBを作り、**化学洗浄機 + 水 → 化学結晶化装置 → 化学注入室 + 塩化水素 → 浄化室 + 酸素 → 粉砕機 → 濃縮室 → 精錬** とつなぎます。きれいなスラリー200 mBから結晶1個、以降の素材変換は各1対1です。最終収量は鉱石1個からインゴット5個、原石3個から10個、原石ブロック1個から30個です。装置の日本語名は固定版Mekanismの翻訳を照合しています。

## 試遊サーバー

このセッションでサーバーが起動済みの場合は、直接 `127.0.0.1:25566` へ接続します。以下の起動コマンドはサーバーが停止している場合だけ、親Minecraftリポジトリのルートで実行します。既存ユーザーワールドをコピー・指定しないでください。

```sh
cd .local/mod-development/create-mekanism-zinc/.local/verification/full-processing-server/instance
export JAVA_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2
export PATH="$JAVA_HOME/bin:$PATH"
./run.sh nogui
```

ワールド `cmz-full-processing` は保存確認用fixtureにより5アイテムとAdvanced Chemical Tank 2基を持ちます。ユーザー確認前に状態を変えずに見る場合は、コンソールで `op <プレイヤー名>` を実行し、クライアントで `/tp @s 1 81 2` を実行します。座標 (1,80,2) のチェストに5種類のアイテムが各1〜5個、(2,80,2)と(3,80,2)のAdvanced Chemical Tankに `dirty_zinc` 1111 mB、`clean_zinc` 2222 mBが保存されています。確認後はコンソールで `save-all flush`、次に `stop` を実行します。

この専用サーバーは `online-mode=false` です。サーバーはアカウント認証を行いません。認証済みPrismクライアントのログイン結果とGUIはユーザーが確認してください。サーバーの起動・接続確認を認証機能の検証とは扱いません。

## 登録素材と表示

クリエイティブの「素材」タブでアイコン、日本語名、英語名を確認します。

| ID | 日本語名 | 英語名 |
| --- | --- | --- |
| `create_mekanism_zinc:dust_zinc` | 亜鉛の粉 | Zinc Dust |
| `create_mekanism_zinc:dirty_dust_zinc` | 汚れた亜鉛の粉 | Dirty Zinc Dust |
| `create_mekanism_zinc:clump_zinc` | 亜鉛の凝塊 | Zinc Clump |
| `create_mekanism_zinc:shard_zinc` | 亜鉛の欠片 | Zinc Shard |
| `create_mekanism_zinc:crystal_zinc` | 亜鉛の結晶 | Zinc Crystal |

Mekanismのクリエイティブアイテム検索またはタンク画面で `dirty_zinc` と `clean_zinc` の登録と色も確認します。加工検収で使った `0.2.0-dev` の画像・色は仮のものです。`0.2.1-dev` では5アイテムの画像を新規制作しました。スラリーの配色は変更していません。新版アイコンの表示確認はまだ待ちです。

## 2〜5倍レシピ

JEI等で21件のレシピ登録を確認し、実加工では通常の電力・化学物質・水を使って入出力数を記録してください。入力は各 `c:` タグを満たす実アイテムを使用します。対象タグに該当する入力が見つからない場合は、そのことを記録して代用品を推測しないでください。

| 倍率・ライン | 加工の順序と入力 | 補助入力／期待出力 |
| --- | --- | --- |
| 2倍 | Enrichment Chamber: `c:ores/zinc` 1個 | Zinc Dust 2個 |
| 2倍 | Enrichment Chamber: `c:raw_materials/zinc` 3個 | Zinc Dust 4個 |
| 2倍 | Enrichment Chamber: `c:storage_blocks/raw_zinc` 1個 | Zinc Dust 12個 |
| 既存基礎 | Crusher: `c:ingots/zinc` 1個 | Zinc Dust 1個 |
| 3倍 | Purification Chamber: zinc ore 1個 → Crusher: Zinc Clump 3個 → Enrichment Chamber: Dirty Zinc Dust → Zinc Dust | 補助oxygen amount 1、`per_tick_usage=true`。各後段レシピは1入力→1出力 |
| 3倍 | Purification Chamber: raw zinc 1個 → 同じCrusher／Enrichment Chamber | 補助oxygen amount 1、`per_tick_usage=true`。原石1個からZinc Dust 2個 |
| 3倍 | Purification Chamber: raw zinc block 1個 → 同じ後段 | 補助oxygen amount 2、`per_tick_usage=true`。ブロックからZinc Dust 18個 |
| 4倍 | Chemical Injection Chamber: zinc ore 1個 → Purification Chamber: Zinc Shard 4個 → clumpへ変換し後段処理 | 補助hydrogen chloride amount 1、`per_tick_usage=true`。各ShardからClump 1個 |
| 4倍 | Chemical Injection Chamber: raw zinc 3個 → 同じ後段 | 補助hydrogen chloride amount 1、`per_tick_usage=true`。Shard 8個 |
| 4倍 | Chemical Injection Chamber: raw zinc block 1個 → 同じ後段 | 補助hydrogen chloride amount 2、`per_tick_usage=true`。Shard 24個 |
| 5倍 | Chemical Dissolution Chamber: ore 1個、またはraw zinc 3個、またはraw zinc block 1個 | 補助sulfuric acid amount 1／1／2、`per_tick_usage=true`。dirty slurryは順に1000／2000／6000 mB |
| 5倍 | Dirty Zinc slurryをWasherで洗浄し、Crystallizerでclean slurryを結晶化。Crystal 1個をInjection ChamberでShardへ、ShardをPurification ChamberでClumpへ変換 | 水5 mBでslurry 1 mB洗浄。clean slurry 200 mB→Zinc Crystal 1個。Crystal→Shardはhydrogen chloride amount 1、Shard→Clumpはoxygen amount 1、いずれも`per_tick_usage=true` |

補助化学物質なし、電力なし、水不足、結晶化に必要なclean slurry 200 mB未満の条件で入力が消費されないことも確認します。Enrichment Chamberでraw zinc 1個と2個をそれぞれ試し、入力が保持されて出力がないことを確認します。4倍Injection Chamberと5倍Dissolution Chamberでも原石1個・2個は保持され、出力がないことを確認します。Crushed Raw Zincと精錬済みZinc Ingotは3〜5倍入口に使えないことを確認します。生産機械の進捗値を手で変更して結果を早めないでください。

レシピの `per_tick_usage=true` と補助Chemical amountの基準はpurification / injection / dissolutionで通常1単位/tick、raw zinc blockでは2単位/tickです。実際の消費量、加工速度、電力は対象版Mekanismの機械設定・統計倍率・導入upgradeに従うため、常に固定量とは限りません。水とslurryの比率、crystallizerの200 mBごとの出力はレシピ量として確認します。

3〜5倍ラインの末端は次の共有手順です。CrystalはChemical Injection Chamberでhydrogen chloride amount 1を使ってShardへ、ShardはPurification Chamberでoxygen amount 1を使ってClumpへ（どちらも`per_tick_usage=true`）、CrusherでDirty Zinc Dustへ、Enrichment ChamberでZinc Dustへ加工し、FurnaceまたはBlast FurnaceでCreate Zinc Ingotにします。粉の精錬はFurnace 200 ticks／Blast Furnace 100 ticks、どちらも経験値0.35です。

| ラインと入力 | 最終インゴット期待数 |
| --- | ---: |
| 3倍: zinc ore / deepslate zinc ore 1個 | 3 |
| 3倍: raw zinc 1個 | 2 |
| 3倍: raw zinc block 1個 | 18 |
| 4倍: zinc ore / deepslate zinc ore 1個 | 4 |
| 4倍: raw zinc 3個 | 8 |
| 4倍: raw zinc block 1個 | 24 |
| 5倍: zinc ore / deepslate zinc ore 1個 | 5 |
| 5倍: raw zinc 3個 | 10 |
| 5倍: raw zinc block 1個 | 30 |

## 結果記録

日付、プロファイル名、構成、ログイン結果、素材名表示、各段階の実測入力／出力個数、補助入力の消費、再起動後の保存内容を記録してください。Windowsでの起動・専用サーバー参加は確認済みで、ユーザーから2〜5倍すべての加工ラインの動作確認報告を受けました。7素材すべての表示チェックは別途未記録で、元のzinc-v2キャンバスから切り出した0.2.3-devの5アイコンは2026-10-02のユーザー報告により見た目を検収済みです。専用サーバーのアカウント認証は検証対象外です。
