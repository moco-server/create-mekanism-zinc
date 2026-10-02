# Kotlinテンプレート移行 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 受け入れ済みの亜鉛加工を維持し、既存Java MODをminecraft-mod-templateに基づくKotlin MODへ移行する。

**Architecture:** ゲーム非依存の定義をcore、本番登録・datagen・実機検証をneoforgeへ置く。テンプレートのビルド・品質・配布検査を実MODに適合させ、独立した旧版resourceと保存データで互換性を確認する。

**Tech Stack:** Java 21、Gradle 8.12.1、Kotlin 2.0.21、KFF 5.5.0、Minecraft 1.21.1、NeoForge 21.1.252。

**Spec:** [移行仕様](../specs/2026-10-02-template-migration-design.md)

## Global Constraints

- 移行前commitは`243ec7f`、テンプレートcommitは`dfee13176ce3ec8e64d27dfdd12376a7cfd58b6b`、template版は`0.1.0`。
- MOD ID `create_mekanism_zinc`、package `org.mocoserver.create_mekanism_zinc`、5アイテム・2slurry・21レシピの登録IDと内容を維持する。
- Create `6.0.10`／Modrinth `UjX6dr61`、Mekanism `10.7.19.85`／`5KzzycBT`、ModDevGradle `2.0.148`、JUnit `5.11.4`を維持する。
- 移行版は`0.3.0-dev`、JAR名は`create-mekanism-zinc-0.3.0-dev.jar`。fixtureは`-gametests.jar`として分離する。
- detekt `1.23.8`、ktlint plugin `12.1.2`／本体`1.5.0`、Wrapper checksumとcore／buildSrc lockを採用する。
- coreはpure Kotlin/JVM。公開APIの可視性・型を明示し、通常コードで`!!`を使わない。
- 生成resourceを手編集しない。画像・翻訳・21レシピと関連resourceは旧版と一致させる。
- 起動・保存検証は`.local`の専用runtimeで行う。ユーザーワールド、旧検証world、試遊profile、親packを変更しない。
- 外部MOD・KFF・Kotlin標準ライブラリ・fixture・開発資料を通常JARへ埋め込まない。
- Node／skills導入をGradleビルドの必須依存にしない。新規テンプレート初期化は実行しない。
- 各taskの確認済み変更をコミットする。push、公開、PR作成は行わない。

## Review Focus

1. Kotlin read-only ListをJavaから変更できてしまうケース: 定義一覧の変更を拒否し、後続datagenへ影響しないこと（Task 2）。
2. fixtureのKotlinメソッドがJVM staticにならずGameTest未発見: 33 required testsが実際に発見・成功すること（Task 4）。
3. Maven KFF aggregateを配布環境へ誤配置: 配布用JARのhashと通常JARのみの起動で確認すること（Task 5）。
4. 旧JARがbuild/libsに残り誤配布: 複数通常JARを収集scriptが拒否すること（Task 6）。
5. 新Kotlin版だけの保存試験で旧版互換を見落とすケース: Java版seed→Kotlin版checkを別JVMで実施すること（Task 5）。

## ファイルの役割

- root／core／neoforge `build.gradle.kts`、`settings.gradle.kts`、catalog／lock: コンパイル、依存、品質、runtime構成。
- `buildSrc/src/main/kotlin/CoreBoundaryTask.kt`／`VerifyModJarTask.kt`: 依存境界と配布JAR検査。対応するテンプレートtestを採用する。
- `core/src/main/kotlin/org/mocoserver/create_mekanism_zinc/{content/ZincContent,processing/ZincProcessing}.kt`: ゲームに依存しない登録識別子とレシピ係数。
- `neoforge/src/main/kotlin/org/mocoserver/create_mekanism_zinc/{CreateMekanismZinc,ZincRegistries,ZincDataGenerators}.kt`: 現在と同じ登録・イベント・datagen。
- `neoforge/src/gametest/kotlin/org/mocoserver/create_mekanism_zinc/tests/*.kt`: 既存GameTestと保存fixture。テスト期待値は定義一覧から生成しない。
- `scripts/verify-server.py`: 配布runtime、保存・移行検証。`scripts/test/test_verify_server.py`で選択と所有範囲を検査する。
- CIと`scripts/{collect-mod-jar,check-gametest-result}.mjs`: 通常artifactの選択と正のGameTest結果の確認。
- `AGENTS.md`、`.agents/skills`、docs: このMOD向けの開発規約、現在の設計、実測、template更新記録。

### Task 1: 移行前の独立した比較対象を確保する

**Files:** `.local/template-migration/baseline/`のみ。追跡対象の本番コードは変更しない。

**Interfaces:** 次taskへ旧版の本番／fixture JAR、resource hash manifest、旧版検証ログを渡す。

- [ ] using-git-worktreesで隔離した作業場所を確保する。元のユーザー変更を保持し、計画書と仕様書がそのcheckoutに存在することを確認する。
- [ ] Java 21、Gradle cache、受諾済みEULAを探索し、書き込み用cacheは作業repoの`.local`へ置く。テンプレートの取得commitを照合する。
- [ ] `git archive 243ec7f`から`.local/template-migration/baseline/source`へ旧ソースを展開し、全本番resource／生成resourceのSHA256 manifestを作る。
- [ ] 旧ソースで`:common:test :neoforge:test :neoforge:build :neoforge:gameTestJar`を実行する。期待値はJUnit計8件のfailure/errorゼロとビルド成功。本番／fixture JARをbaselineへコピーする。
- [ ] 旧版の`:neoforge:runGameTestServer`を実行し、33 required tests成功を記録する。前提不足・失敗があれば原因を切り分けて記録する。

### Task 2: coreとビルド基盤をテンプレートへ移行する

**Files:** root／core／neoforgeのGradle設定、Wrapper、`gradle/libs.versions.toml`、`buildSrc/**`、`config/detekt/detekt.yml`、`.editorconfig`、`.gitattributes`、`gradle.properties`。commonのmain／test／resourceをcoreへ移す。`tools/generate_icons.py`の出力先を変更する。

**Interfaces:** `ZincContent`はobjectとしてMOD_ID、ITEMS、CHEMICALSとnested定義型を公開する。`ZincProcessing.recipes(): List<ProcessingRecipe>`とnested Kind／ChemicalInput／FluidInput／ProcessingRecipeを公開する。data classの値とnullable入力は旧recordと同じ。中間段階の既存Java callerには必要な`@JvmField`／`@JvmStatic`とrecord accessor互換メソッドを一時的に用意し、全Kotlin化後に不要な橋渡しを除去する。

- [ ] 既存のcontent／processing testをKotlinへ移植し、同じ独立期待値・順序・nullable入力・21件をassertする。
- [ ] 定義一覧の変更拒否testを追加する。`assertThrows(UnsupportedOperationException::class.java) { (ZincContent.ITEMS as MutableList<ZincContent.ItemDefinition>).clear() }`と、recipes／CHEMICALSにも各要素型を指定して同じ確認を行う。新coreタスクが未定義の段階で失敗を確認する。
- [ ] テンプレートからKotlin DSL、buildSrcとtest、catalog、Wrapper、品質設定を移し、sample固有entryと依存をMOD向けに置き換える。root名は`create-mekanism-zinc`、groupは現行値、versionは`0.3.0-dev`。
- [ ] core定義をKotlin化し、公開一覧は変更不能な実体にする。main resourceをcoreへ移し、アイコンscriptの出力先を更新する。
- [ ] core境界の禁止prefixへ`com/simibubi/create/`、`mekanism/`、`com/tterrag/registrate/`、`net/createmod/`を追加する。templateのbuildSrcテストに連携MOD参照拒否を加え、偽class検査が失敗するredと通るgreenを確認する。
- [ ] `:core:check`とbuildSrc checkを実行し、coreの単体・型解決付き解析・境界検査が成功することを確認する。core／buildSrcで指定classpathのlockを生成する。template lockを無条件にコピーしない。
- [ ] `git diff --check`とTask 1 manifestで移動済みresourceを確認し、検証済み変更をコミットする。

### Task 3: 本番NeoForgeアダプタとdatagenをKotlin化する

**Files:** 3つの本番Kotlinファイル、`neoforge/src/test/kotlin/org/mocoserver/create_mekanism_zinc/ZincRecipeGenerationTest.kt`、main metadata、`neoforge/build.gradle.kts`。

**Interfaces:** `CreateMekanismZinc(IEventBus)`、`ZincRegistries.ITEMS`／`CHEMICALS`／登録済みmaps、`ZincDataGenerators.gatherData(GatherDataEvent): Unit`。NeoForgeアダプタ以外からゲーム操作を行わない。

- [ ] 既存recipe-generation testをKotlinへ移し、独立した21レシピのinput／output／chemical／fluid／perTickUsage／時間／経験値とdata mapの期待値を維持する。
- [ ] 本番3クラスを移植する。Java generics・登録supplier・イベントlistenerを固定版APIに合わせ、null契約を確認する。mapsの登録順を維持する。
- [ ] main metadataを`kotlinforforge`／`[5.5,)`へ変更し、固定版KFF開発依存を加える。Minecraft／NeoForge／Create／Mekanismの従来の対応範囲を維持する。
- [ ] GameTest source setのcompile／runtime継承とloadedMods設定を維持し、client／server／dataにはfixtureを読み込ませない。
- [ ] `:neoforge:compileKotlin :neoforge:test`で移植を確認する。datagenを別invocationで実行し、全生成resourceをbaselineと比較する。追跡・未追跡差分ゼロを確認する。
- [ ] `verifyModJar` requiredEntriesへ実MODのclasses、画像、model、翻訳、全レシピ、タグ、data mapを設定する。`:neoforge:build :neoforge:verifyModJar`の成功と依存／fixture非混入を確認してコミットする。

### Task 4: GameTestと保存fixtureをKotlin化する

**Files:** gametestの`RegistrySmokeGameTests.kt`、`ZincMachineGameTests.kt`、`ZincAdvancedMachineGameTests.kt`、`PersistenceFixtureMod.kt`、gametest metadata、静的解析task設定。

**Interfaces:** GameTestメソッドは旧版と同名、引数`GameTestHelper`、戻り値UnitのJVM static。fixtureの`cmz_verify seed`／`check`と成功／失敗markerを維持する。

- [ ] 既存の33個の`@GameTest`を同じannotation値で移植し、Java reflection用に`@JvmStatic`を付ける。連鎖・tick待機・実機outputの引き継ぎ・無効入力判定を維持する。
- [ ] fixtureをKotlinへ移し、5つのchest slotと1111／2222 mB slurryの実保存検査を維持する。fixture metadataをKotlin loaderへ適合させる。
- [ ] gametestへktlintと型解決付きdetektを適用し、root checkの依存に含める。静的解析の広い無効化は行わない。
- [ ] `check :neoforge:gameTestJar`を実行する。`:neoforge:runGameTestServer`で33 required tests成功を確認する。annotation数だけで成功と扱わない。
- [ ] 通常JARにfixture class／metadataが混入していないことを再確認し、検証済み変更をコミットする。

### Task 5: 配布runtimeと旧版保存互換を検証する

**Files:** `scripts/verify-server.py`、`scripts/test/test_verify_server.py`、互換性／検証記録。runtimeは`.local/verification/template-migration-server`、旧版互換試験は別の`template-migration-legacy-server`。

**Interfaces:** `mod_version(root: Path) -> str`でGradle propertiesから版を取得する。`mod_artifact(root: Path, version: str, fixture: bool) -> Path`で通常／fixture JARを選ぶ。CLIのprepare／clean-boot／persistenceを維持し、migrationを追加する。migrationは`.local/template-migration/baseline`の固定旧版JARを使う。

- [ ] unittestで`0.3.0-dev`の通常／fixture選択、欠落JAR・危険なversion値の拒否、runtime外／未所有ディレクトリ拒否、hash不一致拒否を追加する。実installerやserverを起動しないtestとしてredを確認する。
- [ ] 旧`0.2.0-dev`固定名を除去し、propertiesから版を決める。新runtimeでも所有marker・直下instance限定・loopback bind・fixtureのfinally除去を維持する。
- [ ] KFF `5.5.0`配布JARを作者公開先から取得し、一次資料URLとSHA512を固定する。Maven開発aggregateを使用しない。
- [ ] `python3 -m unittest discover -s scripts/test -p 'test_*.py'`を実行する。配布KFFのみのruntimeでprepare、clean-boot、persistenceを実行し、正常起動と2 JVM保存確認markerを記録する。
- [ ] migration CLIは別runtimeで旧Java本番／fixture JARを使ってseed・save・stopし、その後Kotlin本番／fixtureとKFFへ交換して別JVMでcheckする。`LEGACY_WORLD_MIGRATION_PASS`と実際の5 items／2 chemicals markerを確認する。
- [ ] fixture除去後の通常JARのみでも保存済みワールドを起動できることを確認する。hash、構成、marker、前提不足を記録してコミットする。

### Task 6: CI・開発手順・テンプレート由来を統一する

**Files:** `.github/workflows/build.yml`をverify workflowへ統合、issue／PR template、`scripts/{collect-mod-jar,check-gametest-result}.mjs`、`scripts/test/ci-artifacts.test.mjs`、`AGENTS.md`、`.agents/skills/**`、skills manifest／導入script、`package.json`とpnpm設定、README、`docs/{design,compatibility,verification,template-updates,external-skills}.md`、`template-version.txt`。

**Interfaces:** Node補助はtemplateと同じ`collectModJar(root)`、`checkGameTestResult(text)`。NodeはCI補助／任意skills導入でのみ使用する。

- [ ] templateのartifact script/testを採用し、0／複数通常JAR、sources／fixture除外、symlink、空でない出力先、GameTestゼロ件／失敗／成功の判定を確認する。`node --test scripts/test/*.test.mjs`を実行する。
- [ ] Linux CIへcheck、独立datagen、追跡・未追跡差分確認、ビルド／通常JAR検査／GameTest、結果script、画像再生成差分、通常JAR収集を設定する。Windows CIはcheckとビルド／JAR検査を実行する。版固定のartifact名を使用しない。
- [ ] templateのローカルMOD skillsと参照資料を採用し、AGENTSのdesign／対応版／検証参照を実MODへ合わせる。外部skill manifest／導入補助は任意として提供し、グローバル導入や外部skillsの自動インストールは行わない。
- [ ] READMEと現行designにKotlin構成、KFF配布依存、現行コマンド、通常JAR場所を記載する。古い検証／検収記録は歴史として維持する。template版・commit・除外した初期化／sampleを更新記録へ残す。
- [ ] JEIの既存固定版と取得元／hash／client sideを維持し、開発client限定で設定する。実試遊profileは変更しない。
- [ ] 最終的に`check :neoforge:build :neoforge:verifyModJar :neoforge:gameTestJar`と必要なscript testを実行し、datagenと画像をbaselineと比較する。コード変更後に必要なruntime検証のみ再実行する。
- [ ] diffと仕様の完了条件をレビューし、修正後の必要な検証を行う。CI未実行・GUI未検収は未実施として報告する。最終JAR hashとKFF追加を記録してコミットする。

## 計画の自己確認

仕様のcore／Kotlin／resourceはTask 2〜4、配布検査はTask 3、旧版保存互換はTask 1・5、skills／CI／文書／由来はTask 6が担当する。全体のレビュー観点は各担当taskの検証へ含めた。通常MODの新機能、sample初期化、外部環境への導入は含めない。
