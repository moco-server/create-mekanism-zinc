# クライアント受け入れ確認

3〜5倍ラインのGUI・加工結果はユーザー確認待ちです。ユーザーからは2倍加工について「2倍は問題なく動作してた」と報告済みです。今回の確認は Issue #7 の全体完了や公開配布を意味しません。

## 対象プロファイル

- 新規Prismインスタンス `moco-cmz-trial-0.2.0-dev-client` はコントローラーが作成し、Minecraft 1.21.1 / NeoForge 21.1.252 / LWJGL 3.3.3、通常3 JARのSHA-512、21レシピ、fixture非同梱を独立確認済みです。保存先は `C:\Users\wwwdd\AppData\Roaming\PrismLauncher\instances\moco-cmz-trial-0.2.0-dev-client` です。ワールド、既存設定、認証情報はコピーしていません。
- 2026-10-01 20:37 JST、Windows版Prismから新版を起動し、0.2.0-devの読み込みとNVIDIAによる描画初期化をログで確認しました。20:39 JSTにクライアントと専用サーバー両側のログで接続・ワールド参加も確認しました。7素材の表示と3〜5倍の実加工はユーザー確認待ちです。旧0.1.0-devインスタンスのワールド／MOD構成は変更していません。
- Minecraft 1.21.1、NeoForge 21.1.252、Java 21、Create 6.0.10、Mekanism 10.7.19.85 (Modrinth build 85)。対応範囲は[互換性表](compatibility.md)の固定対象のみです。
- クライアントへ入れるaddonは `dist/cmz-trial/create-mekanism-zinc-0.2.0-dev.jar`（独立MOD内では `neoforge/build/libs/create-mekanism-zinc-0.2.0-dev.jar`）です。GameTest fixtureの `-gametests.jar` は入れません。
- 試遊サーバーは専用の新規ワールドで、IPv4 `127.0.0.1:25566` に接続します。`localhost` ではなくIPv4を直接指定してください。Windowsで `localhost` がIPv6へ解決される場合の接続失敗を避けます。

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
| `create_mekanism_zinc:dirty_dust_zinc` | 不純な亜鉛の粉 | Dirty Zinc Dust |
| `create_mekanism_zinc:clump_zinc` | 亜鉛の塊 | Zinc Clump |
| `create_mekanism_zinc:shard_zinc` | 亜鉛の欠片 | Zinc Shard |
| `create_mekanism_zinc:crystal_zinc` | 亜鉛の結晶 | Zinc Crystal |

Mekanismのクリエイティブアイテム検索またはタンク画面で `dirty_zinc` と `clean_zinc` の登録と色も確認します。現在の画像・色は暫定です。粉のアイコン品質は既に課題として報告されています。

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

日付、プロファイル名、構成、ログイン結果、素材名表示、各段階の実測入力／出力個数、補助入力の消費、再起動後の保存内容を記録してください。Windowsでの起動・専用サーバー参加は確認済みですが、7素材の表示、3〜5倍のユーザー確認、画像品質の改善はまだ完了していません。専用サーバーのアカウント認証は検証対象外です。
