# 現在の設計

Createの亜鉛をMekanismの加工系統へ接続する独立NeoForge MOD。MOD IDは`create_mekanism_zinc`。互換性を保つため、登録ID、レシピのIDと内容、タグ、slurry data map、画像、翻訳、クリエイティブタブの表示内容を維持する。固定版は[対応構成](compatibility.md)に記録する。

## 構成と境界

`core`はKotlin/JVMの純粋な定義層。`ZincContent`が5アイテムとdirty／clean slurryを定義し、`ZincProcessing`が21レシピの係数を定義する。公開APIは可視性と型を明示し、公開コレクションはJava呼び出しからも変更不可とする。Minecraft、NeoForge、KFF、Create、Mekanism等の型への依存をclasspathとbytecode検査で拒否する。

`neoforge`は登録、イベント、datagenを担当する。生成JSONはGitで管理し、承認済みJava版の全34 JSONを独立した期待値としてテストする。通常JARには本番クラスと必要なリソースを収録する。`verifyModJar`で素材画像、モデル、翻訳、全21レシピ、タグ、slurry data mapなどの必須項目と、外部依存、GameTest、開発資料の混入を検査する。

GameTestは専用source setと`-gametests.jar`へ分け、登録と実機加工の必須テスト33件、保存fixtureを提供する。fixtureを読み込む開発起動はGameTest serverに限定する。KFFをMOD loaderとして使用し、配布環境には作者の5.5.0配布JARを別途配置する。

## 加工と保存互換

鉱石の2／3／4／5倍系統を提供する。原石は2倍で3個→dust 4個、3倍で1個→clump 2個、4倍で3個→shard 8個、5倍で3個→最終10個。原石ブロックでは12／18／24／30個となる。dirty slurryは鉱石／原石3個／ブロックから1000／2000／6000 mB、水はslurryの5倍量、clean slurry 200 mBからcrystal 1個。

保存にはMinecraftとMekanismの既存形式を使う。同じ版での保存と再読み込みに加え、旧Java版で保存したワールドを新Kotlin版の別JVMで読み込み、5アイテムと2種類のslurryのID・数量が維持されることを確認する。旧版互換の検証には旧Java版で実際に保存したワールドを使う。負例、出力満杯、加工連鎖は実GameTestで維持する。

## 開発運用

Gradle Kotlin DSL、Kotlin 2.0.21、型解決付きdetekt、ktlint、JUnitを使う。固定版とchecksumを維持し、通常ビルドは任意のskills導入に依存しない。開発clientにだけJEIを追加する。起動用directoryは`.local`内へ限定する。

画像の由来と検収を記録する。[画像資料](assets.md)・[クライアント検収](client-acceptance.md)

Kotlin移行時に合意した要件と、その実測結果を参照できる。[Kotlinテンプレート移行仕様](kotlin-template-migration.md)・[検証記録](verification.md)

初期設計と加工系統の拡張は履歴を参照できる。[マイルストーン1の設計](milestone-1-design.md)・[3〜5倍加工の実装計画](plans/2026-10-01-full-processing.md)
