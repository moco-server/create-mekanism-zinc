# クライアント受け入れ確認

この確認は、認証済みクライアントで実際にログインして行います。現在の状態は「一部ユーザー確認済み・クライアント検収待ち」です。

## 対象と準備

- Minecraft 1.21.1、NeoForge 21.1.252、Java 21
- Create 6.0.10、Mekanism 10.7.19.85（配布ファイル build 85）
- クライアントに production jar `neoforge/build/libs/create-mekanism-zinc-0.1.0-dev.jar` を追加
- 専用サーバーにも同じ production jar と上記依存MODを追加
- GameTest用 `create-mekanism-zinc-0.1.0-dev-gametests.jar` はクライアント・配布サーバーに追加しない

## 起動と停止

以下のパスは独立したMODリポジトリ (`.local/mod-development/create-mekanism-zinc`) を基準にしています。まず親のMinecraftプロジェクトからそこへ移動し、検証済み使い捨てワールドを起動します。このワールドは確認専用です。保存済みワールドをこの手順で使わないでください。

```sh
cd .local/mod-development/create-mekanism-zinc
cd .local/verification/clean-server/instance
export JAVA_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2
export PATH="$JAVA_HOME/bin:$PATH"
./run.sh nogui
```

この隔離サーバーは `127.0.0.1:25565` のみにbindし、`online-mode=false` です。外部接続を受けず、サーバー側のアカウント認証を行いません。いつもの認証済みランチャーから `localhost:25565` へ接続し、クライアントの起動・MOD表示・GUIを確認してください。認証済み接続をサーバーが検証したとは扱いません。

再起動確認済みワールドには、チェストに5種類のアイテムが各1〜5個、`2 80 2` と `3 80 2` のAdvanced Chemical Tankに両スラリーが各1111 mB／2222 mB入っています。コンソールで `op <プレイヤー名>` を実行し、ゲーム内で `/tp @s 1 81 2` と入力すると配置場所に移動できます。確認後、コンソールで `save-all flush`、続けて `stop` を実行します。

## 登録内容

クリエイティブの「素材」タブで次の5アイテムが表示され、各アイコンと日本語・英語名を確認します。

| ID | 日本語名 | 英語名 |
| --- | --- | --- |
| `create_mekanism_zinc:dust_zinc` | 亜鉛の粉 | Zinc Dust |
| `create_mekanism_zinc:dirty_dust_zinc` | 不純な亜鉛の粉 | Dirty Zinc Dust |
| `create_mekanism_zinc:clump_zinc` | 亜鉛の塊 | Zinc Clump |
| `create_mekanism_zinc:shard_zinc` | 亜鉛の欠片 | Zinc Shard |
| `create_mekanism_zinc:crystal_zinc` | 亜鉛の結晶 | Zinc Crystal |

Mekanismのクリエイティブアイテム検索または化学タンク画面で `dirty_zinc` と `clean_zinc` が登録され、色が表示されることも確認します。色と画像は暫定です。

## レシピと処理

JEI等のレシピ表示がある場合、以下を確認します。実機で確認する場合は、通常の電力・燃料を使い、入力と出力の個数を数えます。

| 入力 | 装置 | 期待出力 |
| --- | --- | --- |
| `c:ores/zinc` 1個 | Enrichment Chamber | Zinc Dust 2個 |
| `c:raw_materials/zinc` 3個 | Enrichment Chamber | Zinc Dust 4個 |
| `c:storage_blocks/raw_zinc` 1個 | Enrichment Chamber | Zinc Dust 12個 |
| `c:ingots/zinc` 1個 | Crusher | Zinc Dust 1個 |
| Zinc Dust 1個 | Furnace | Create Zinc Ingot 1個、200 ticks |
| Zinc Dust 1個 | Blast Furnace | Create Zinc Ingot 1個、100 ticks |

Enrichment ChamberにRaw Zincを1個、次に2個入れ、どちらも入力が消費されずDustが出ないことも確認します。入力は `c:ores/zinc`、`c:raw_materials/zinc` 等のタグを満たす実アイテムを使用してください。タグに該当する入力が見つからない場合は、その旨を記録します。

## ワールド保存

テストハーネスでは実際に専用サーバーJVMを2回起動し、この使い捨てワールドのチェスト／タンク内容を再読み込み後に照合済みです。GUIでも画面で確認したい場合は、上記の座標で5アイテムと両スラリーを確認してください。

## 結果記録

確認日、構成、ログイン結果、アイテム表示、各レシピの実測個数、ワールド再起動後の内容を記録します。GUI確認と認証済みログインは自動検証の対象外です。

### 2026-10-01：ユーザーによる試遊報告

- Windows版Prismの専用試遊インスタンスを用意した後、ユーザーから「一応2倍は動作してた」と報告を受けた。2倍加工の動作確認として記録する。個別の入力種類・実測収量・全6レシピの確認範囲は未報告。
- 粉のアイコンについて、ユーザーから見た目の品質に問題があるとの指摘を受けた。現在の5アイテムの画像は `assets.md` に記載した仮画像であり、正式画像の制作は未完了。
- 7種類すべての表示、日英表示名、認証済みログインの確認を含むクライアント検収は引き続き保留する。
