# 現在の設計

Createの亜鉛をMekanismの加工系統へ接続する独立NeoForge MOD。MOD IDは`create_mekanism_zinc`、既存registry ID、タグ、recipe ID、画像、翻訳と加工係数を維持する。対応版は[compatibility.md](compatibility.md)、移行仕様は[承認済み設計](superpowers/specs/2026-10-02-template-migration-design.md)に記録する。

## 構成と境界

`core`はKotlin/JVMの純粋な定義層。`ZincContent`が5アイテムとdirty／clean slurryを定義し、`ZincProcessing`が21レシピの係数を定義する。公開collectionは変更不可とし、Minecraft、NeoForge、KFF、Create、Mekanism等の型への依存をclass pathとbytecode検査で拒否する。

`neoforge`は登録、イベント、datagenを担当する。生成JSONはGitで管理し、承認済みJava版の全34 JSONを独立した期待値としてテストする。mainのjarにはcoreのclassesとresourceを取り込み、外部依存・GameTest・開発資料は含めない。

GameTestは専用source setとfixture JARへ分け、ゲーム内の実機加工33件と保存fixtureを提供する。通常client／server／dataではfixtureを読み込まない。KFFをMOD loaderとして使用し、配布runtimeでは作者の5.5.0配布JARを別途配置する。

## 加工と保存互換

鉱石の2／3／4／5倍系統を提供する。原石は2倍で3個→dust 4個、3倍で1個→clump 2個、4倍で3個→shard 8個、5倍で3個→最終10個。原石ブロックでは12／18／24／30個となる。dirty slurryは鉱石／原石3個／ブロックから1000／2000／6000 mB、水はslurryの5倍量、clean slurry 200 mBからcrystal 1個。

本MOD独自の保存形式は増やさない。旧Java版で保存した5アイテムのstack数量と2種類のslurry量を、新Kotlin版の別JVMで読み戻して確認する。負例、出力満杯、加工連鎖は実GameTestで維持する。

## 開発運用

Gradle Kotlin DSL、Kotlin 2.0.21、型解決付きdetekt、ktlint、JUnitを使う。固定版とchecksumを維持し、通常ビルドは任意のskills導入に依存しない。開発clientにだけJEIを追加する。起動用directoryは`.local`内へ限定する。

画像の由来と検収は[assets.md](assets.md)と[client-acceptance.md](client-acceptance.md)、移行の実測と未実施項目は[verification.md](verification.md)に残す。歴史的設計の[milestone-1-design.md](milestone-1-design.md)と既存計画も保存する。
