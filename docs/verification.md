# 検証記録

## 初回配布 `v0.3.0`（2026-11-22）

- リリースPR #3でGradleとChangesetsの版を`0.3.0`にそろえた。リリース候補のLinux／Windows CIが成功し、Linuxでデータ・画像の再生成差分が0、GameTest 33件が成功した。[リリースPR](https://github.com/moco-server/create-mekanism-zinc/pull/3)・[候補版の検証](https://github.com/moco-server/create-mekanism-zinc/actions/runs/45014086580)
- マージコミット`b0babc2`で`check`、通常JARのビルドと内容検査、GameTest 33件が成功した。GitHub Packagesの`org.mocoserver.create_mekanism_zinc:create-mekanism-zinc:0.3.0`からJARとPOMを取得して公開前のファイルと照合し、同じJARをGitHub Releaseに添付した。[配布ワークフロー](https://github.com/moco-server/create-mekanism-zinc/actions/runs/45014370709)・[リリース](https://github.com/moco-server/create-mekanism-zinc/releases/tag/v0.3.0)
- Releaseからダウンロードした`create-mekanism-zinc-0.3.0.jar`は候補版CIとローカルで検証した通常JARにバイト単位で一致した。NeoForge metadataの版は`0.3.0`、SHA-256は`112c4078bd7bdca97b058d27c1db17adac1f079eab4b3d4d2324f1cfff3c03e6`。
- Temurin 21.0.13.1／Gradle 8.12.1で通常JARをビルドし、Create、Mekanism、配布KFFとともに新規ワールドの専用サーバーを起動した。`python3 scripts/verify-server.py clean-boot`で`CLEAN_PRODUCTION_JAR_BOOT_PASS`、`migration`で`LEGACY_WORLD_MIGRATION_PASS Java=0.2.3-dev Kotlin=0.3.0`を確認した。旧Java版で保存した5アイテムと2種類のスラリーのID・数量が一致し、全サーバーJVMが終了コード0で停止した。GUIクライアントの接続と画面表示は未実施。[対応構成](compatibility.md)

## Changesetsによる配布（2026-11-22）

- Node 24.20.0／pnpm 12.8.1／Changesets CLI 3.0.3で固定lockfileから導入した。隔離した作業コピーで試験用patch changesetを消費し、`package.json`と`gradle.properties`が`0.3.0-dev`から`0.3.0`へそろい、当該版のRelease本文が生成された。Nodeテスト23件、Pythonテスト10件が成功した。
- 変更前の`:neoforge:test`は、Kotlin for Forge 5.5.0の版範囲`[3,5)`から選ばれたloader 4.0.45の検証で失敗した。GameTest起動では同loaderとNeoForge 21.1.252付属の4.0.44が衝突した。KFFからのloader解決を除外し、NeoForgeの提供するloaderで動作を確認した。[KFF 5.5.0の依存情報](https://thedarkcolour.github.io/KotlinForForge/thedarkcolour/kfflang-neoforge/5.5.0/kfflang-neoforge-5.5.0.module)・[NeoForge 21.1.252の依存情報](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.252/neoforge-21.1.252.module)
- Temurin 21.0.13.1／Gradle 8.12.1で`runData`後の生成resource差分は0。厳格な依存検証で`check`、通常JARのビルドと内容検査、GameTest 33件、ローカルMaven登録が成功した。MavenのJARとRelease用に収集したJARは同一バイト列で、NeoForge metadataの版は`0.3.0`だった。JARのSHA-256は`112c4078bd7bdca97b058d27c1db17adac1f079eab4b3d4d2324f1cfff3c03e6`。
- ローカルMaven登録と同じ通常JAR、Create、Mekanism、配布KFFで新規ワールドの専用サーバーを起動した。`CLEAN_PRODUCTION_JAR_BOOT_PASS`とサーバーJVMの終了コード0を確認した。
- GitHub Packages向けGradleタスクはdry-runで確認した。公開スクリプトの模擬テストでは、Packages登録直後にRelease作成が失敗した場合の再実行、同一JAR/POMの再利用、内容が異なる既存JARの拒否を確認した。`actionlint` 1.7.12は`concurrency.queue`に未対応のため、このキーはGitHubの構文資料と照合し、残りを`actionlint`で検証した。[GitHub Actionsの同時実行制御](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency)

ローカルログは`.local/release-verify/`。GitHub ActionsでのPR作成と実際の公開、Windows、GUI接続は未実施。

## CI依存検証の登録漏れ修正（2026-10-02）

GitHub Actions run `36992278786`ではLinux／Windowsとも、`buildSrc`設定時に`kotlinx-coroutines-bom:1.6.4`のPOMが検証メタデータ未登録で停止した。Java 21（Eclipse Adoptium）、Gradle Wrapper 8.12.1、`JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2`と、新規`GRADLE_USER_HOME=.local/gradle-ci-clean`で`./gradlew help`を実行し、同じ失敗を再現した。既存キャッシュの`check`は修正前にも成功したため、既存キャッシュだけではこの登録漏れを検出できなかった。

- [Maven Centralのcoroutines BOM 1.6.4 POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-bom/1.6.4/kotlinx-coroutines-bom-1.6.4.pom)と[Gradle Plugin Portalの同POM](https://plugins.gradle.org/m2/org/jetbrains/kotlinx/kotlinx-coroutines-bom/1.6.4/kotlinx-coroutines-bom-1.6.4.pom)を取得し、SHA-512一致を確認して登録した。
- 続く新規キャッシュの`check`で、JUnit BOM [5.7.1](https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.7.1/junit-bom-5.7.1.module)／[5.9.0](https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.9.0/junit-bom-5.9.0.module)のGradle module metadataにも登録漏れを確認した。Maven Centralから取得したSHA-512を登録した。3ファイルともハッシュは`gradle/verification-metadata.xml`に記録した。依存版と検証設定は変更していない。
- 修正後、同じ新規キャッシュで`./gradlew check :neoforge:build :neoforge:verifyModJar`が終了code 0。既存タスク出力は一部UP-TO-DATEだが、依存取得と検証は新規キャッシュで行った。
- 別呼出しの`./gradlew :neoforge:runData`が終了code 0。`git status --porcelain --untracked-files=all -- neoforge/src/generated/resources`の差分0。
- 続く`./gradlew :neoforge:build :neoforge:verifyModJar :neoforge:runGameTestServer`が終了code 0、`All 33 required tests passed`。`node scripts/check-gametest-result.mjs`も33件成功を確認した。

ログと取得ファイルは`.local/verification/ci-dependencies/`へ保存した。今回Windows実機、GitHub Actions再実行、GUI、新たな通常JAR専用サーバー起動は未実施。

## Kotlinテンプレート移行（0.3.0-dev、2026-10-02）

Minecraft 1.21.1／NeoForge 21.1.252／Create 6.0.10／Mekanism 10.7.19.85は維持した。Gradle 8.12.1、Kotlin 2.0.21、KFF 5.5.0を採用した。Java 21の検証済みJDKを読み取りで利用し、Gradleキャッシュは今回のworktreeの`.local/gradle`へ複製した。ローカル検証では`JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2`、通常サーバーでは追加で`-Xmx2G`を指定した。

- 旧Javaソース`243ec7f`から別ディレクトリでビルドし、単体8件とrequired GameTest 33件が成功した。本番／fixture JARと46 resourceのSHA256 manifestを`.local/template-migration/baseline`へ保持した。
- Kotlin版のroot `check`が成功した。core単体7件、NeoForge単体2件、buildSrc単体8件、ktlint、型解決付きdetekt（main／test／gametest）、core境界を含む。境界検査は一時的な`mekanism/probe/BoundaryProbe.class`を実際に拒否し、probe削除後に成功した。
- 本番Kotlin datagenが成功し、全46 resource（21レシピ、関連タグ／data map／advancement、画像／モデル／翻訳）が移行前とSHA256一致した。追跡・未追跡の生成resource差分は0。
- Kotlin版も`All 33 required tests passed`。旧Java版とテスト名・template・timeoutが一致する。実機の2x〜5x、電力不足、補助素材不足・枯渇、無効入力、199 mBでの無出力などの契約を維持した。
- 通常JARの必須class／resource検査が成功した。外部class、fixture、依存JAR、生成cache、開発資料を拒否する。依存JARとcache混入の追加回帰は修正前に失敗し、修正後に全8 buildSrcテストが成功した。
- サーバー検証scriptの10テストが成功した。現在版JARの選択、欠落・危険な版文字列・symlink、runtime所有範囲・未所有ディレクトリ、hash不一致、migration CLIを検査した。
- `.local/verification/template-migration-server`で配布KFFを使い、本番JARのみの`CLEAN_PRODUCTION_JAR_BOOT_PASS`を確認した。KFFは作者配布`kotlinforforge-5.5.0-all.jar`で、Maven aggregateではない。
- Kotlin版の保存・停止・別JVM読み込みは`CMZ_PERSIST_SEED_PASS`、`CMZ_PERSIST_CHECK_PASS items=5 chemicals=2`、`TWO_JVM_WORLD_PERSISTENCE_PASS`を確認した。
- 別の`.local/verification/template-migration-legacy-server`を旧Java 0.2.3-devと旧fixtureでseedした。Kotlin 0.3.0-dev＋配布KFFへ交換し、新版fixtureではseedせずcheckだけを実行した。`LEGACY_WORLD_MIGRATION_PASS Java=0.2.3-dev Kotlin=0.3.0-dev`と5アイテム／2化学物質の実数量一致を確認した。その保存worldをfixture無しでも起動できた。全サーバーJVMは終了code 0で停止した。

通常runtimeのmodsは本番addon、Create、Mekanism、KFFの4 JARのみ。fixtureは検証後に除去された。サーバーはloopback `127.0.0.1:25566`、world名`cmz-template-migration`。EULAは以前に受諾済みの親pack内検証ファイルを読み取り、今回のruntimeへコピーした。ユーザーワールドや旧検証worldは使用していない。

ローカルログは`.local/logs/{baseline-build,baseline-gametest,core-check,adapter-check,kotlin-datagen,kotlin-gametest,clean-boot,persistence,migration}.log`。この時点の通常JAR SHA512: `1446073655907570ce7fffe52f64563d74098d04e5f6e47a2f296323f2289596c056ededf28cb34f7fd0d4510753c4381a9b8fe2405c6e549370aa4f0b148e2d`。最終成果物とCI相当の結果は後続の節で記録する。

### 最終構成の検証

テンプレートのCI補助・ローカルskills・issue／PR templateを採用し、Linux／Windows workflowを設定した。Linux相当のコマンドをローカルで実行し、Nodeテスト21件、Pythonテスト10件、root check、通常JAR build／verify、fixture JAR buildが成功した。再実行したdatagenと画像生成でも全46 baseline hash一致、追跡・未追跡差分0。最終ソースで実行したGameTestも33 required成功。

JEI 19.51.0.418はModDevGradle 2.0.148の`RunModel.getAdditionalRuntimeClasspathConfiguration()`へclientだけ追加した。作者公開の固定版sources JARとplugin JARの署名を`javap`で確認し、生成したclient classpathにJEIが存在、server／data／GameTest classpathに不存在を確認した。API参照: [作者公開の2.0.148 sources JAR内RunModel.java](https://maven.neoforged.net/releases/net/neoforged/moddev-gradle/2.0.148/moddev-gradle-2.0.148-sources.jar)。GUIは起動していない。

JARはtimestampを除去しentry順序を固定した。最終通常JAR SHA512: `a1e94fd8dcccbd892643a0ac4305c7509b38d6ff2f8bd99d017466f06e1fb9ee0c7ec259af743665f72b16af2fb1540d831bd79a9d53e6d7df03d9ad39b59354`。通常JAR収集scriptはclassifierを除外し、0／複数候補、symlink、空でない出力先を拒否する。Linux／Windows CIはGitHub上では未実行。公開配布、親packと既存試遊profileへの導入は未実施。

### 最終レビューとWindows依存検証

CodeRabbit CLI 0.8.2は作者配布ZIPと公開SHA256SUMSを照合し、インストール済みbinaryの一致を確認した。全実装commitのレビューは指摘0件。独立レビューでWindows向けLWJGL nativeのchecksum不足を指摘されたため修正した。

Linux上で固定ModDevGradleの`net.neoforged.minecraftdependencies.OperatingSystem.ATTRIBUTE`を`windows`へ設定し、実際の`neoFormRuntimeDependenciesRuntimeClasspath`と`testRuntimeClasspath`を解決した。修正前はstrict検証でWindows native 24 JARの未記録が原因で失敗した。作者repoの対象版artifactを取得してSHA512を追加後、同じconfigurationを通常のstrict検証で解決できた。Linuxのroot check／build／verify／fixtureも再成功し、通常JARの最終hashは変わらない。

この確認はWindows向け依存解決の検証であり、Windows OS上のビルド実行ではない。ローカルログは`.local/logs/windows-runtime-{red,metadata,green}.log`と`review-fix-check.log`。対象はLWJGL 3.3.3の8 modules（lwjgl／freetype／glfw／jemalloc／openal／opengl／stb／tinyfd）、各windows／windows-x86／windows-arm64。検証時の一時init scriptは`.local/windows-runtime.init.gradle`に保持する。

### 現行の再現手順

Java 21へ`JAVA_HOME`を指定し、`GRADLE_USER_HOME`は必要ならrepo内へ置く。`CMZ_EULA_FILE`には既存の受諾済みファイルを指定する。

```sh
./gradlew :neoforge:runData
./gradlew check :neoforge:build :neoforge:verifyModJar :neoforge:gameTestJar
./gradlew :neoforge:runGameTestServer
python3 -m unittest discover -s scripts/test -p 'test_*.py'
python3 scripts/verify-server.py clean-boot
python3 scripts/verify-server.py persistence
python3 scripts/verify-server.py migration
```

migrationには`.local/template-migration/baseline`のJava 0.2.3-dev本番／fixture JARが必要。`CMZ_BASELINE_DIR`で別の読取り場所を指定できる。旧sourceは`git archive 243ec7f`から別ディレクトリへ展開し、旧Wrapperで`:neoforge:build :neoforge:gameTestJar`を実行して取得する。現在のKotlin版や同名のfixtureで旧版seedを代用しない。

GitHub CIの実行、Windows上の実行、Kotlin版GUI／実操作のユーザー再検収はまだ実施していない。以下の旧版検証・検収は履歴として維持する。

---

## Pinned runtime for accepted processing build (`0.2.0-dev`)

- Minecraft 1.21.1, NeoForge 21.1.252, Java 21 (`21.0.12.1+1-LTS`), Gradle 9.2.1, ModDevGradle 2.0.148.
- Create 6.0.10, pinned Modrinth file `UjX6dr61`; Mekanism 10.7.19.85, pinned Modrinth file `5KzzycBT` (installed metadata version 10.7.19).
- Version `0.2.0-dev` registers 21 recipes. The production JAR is `neoforge/build/libs/create-mekanism-zinc-0.2.0-dev.jar`; the separate `-gametests.jar` contains fixtures and is not part of the client/ordinary server artifact.
- `scripts/verify-server.py` owns only `.local/verification/full-processing-server/instance`, guarded by `.cmz-owned-runtime`; it refuses a different `CMZ_GAME_DIR`. It binds the private offline server to `127.0.0.1:25566` and names its world `cmz-full-processing`. The existing `.local/verification/clean-server/instance/eula.txt` was read as the already accepted EULA source and copied to the isolated runtime. The script verifies the NeoForge 21.1.252 installer URL and pinned SHA-512 `11addafebd1dbb583252230c7ff97bf80c8b5f6d980a6ae8ffd409ba302e8beaee87e51c35fd8daf8a589b1c9fce9845dd0406a7b7113185ad349748a3461de5`, copies hash-verified Create/Mekanism cache files, and invokes the selected Java 21 binary directly.

## Reproduction commands

From the standalone addon repository, configure the same Java/Gradle cache/EULA paths used for this verification:

```sh
export JAVA_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle
export CMZ_EULA_FILE=/home/osamu/develop/moco-server/mc/.local/mod-development/create-mekanism-zinc/.local/verification/clean-server/instance/eula.txt
./gradlew :neoforge:runData
./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar
./gradlew :neoforge:runGameTestServer
python3 scripts/verify-server.py prepare
python3 scripts/verify-server.py clean-boot
python3 scripts/verify-server.py persistence
```

Data generation remains a separate Gradle invocation. Persistence adds the fixture JAR for the seed and check JVMs only; a `finally` cleanup removes it on both normal and error exits. The production JAR does not contain fixture classes or dependency JARs. Runtime logs are in `.local/verification/full-processing-server/logs/`, with command transcripts `clean-boot-run.log` and `persistence-run.log` in the same ignored runtime directory.

## Observed results (2026-10-01)

- Current `0.2.0-dev` unit tests: common 6 tests (content 4, processing 2) and NeoForge 2 recipe-generation tests; all 8 passed with zero failures/errors. Two separate `:neoforge:runData` invocations completed; the second reported `written: 0`, and SHA-256 manifests of every generated resource file before/after were identical. Controller compared all 21 generated recipes with the pinned-version iron recipe JSON after substituting the ore/material IDs and zinc tags, and confirmed exact JSON equality. The original iron recipes remain unchanged.
- The final ordinary-JVM run of `./gradlew :neoforge:runGameTestServer` passed all 33 required tests: `33 GAME TESTS COMPLETE IN 1.130 s`, `All 33 required tests passed :)`, `BUILD SUCCESSFUL`. During the sequence of reruns, one intermediate launch had a native C2 compiler SIGSEGV in `PhaseIFG::effective_degree` while compiling vanilla `StateDefinition$$Lambda.accept`, before recipe loading/tests. The same source, Java 21 build, and ordinary JVM settings passed on the following run; no C2 workaround was used or persisted.
- Task3 build command `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` completed `BUILD SUCCESSFUL` (18 tasks, 2 executed); common boundary check passed with `0 artifacts`. Python syntax compilation for `scripts/verify-server.py` passed.
- Production JAR SHA-512: `707b8672c82a5483cb6cb775e4899a9488faf1f6452d729efe9585547dfdd278f7f99b53190cd9fe405dd5484f7f0b251d59c691bff29d64fd8b6133e7c86dfd`. The ordinary JAR has production metadata and no `tests/` classes, GameTest fixture, or embedded dependency JAR. The separate fixture JAR SHA-512 is `231696a4684d9cb05f8f9440483ea02c8b327279fe72b5fdd055efd5ded06874228b7cf53e227883a1f3f70520ad96801d42f0d8e6077fb036d477efc56b9f1f`.
- `python3 scripts/verify-server.py prepare` passed with the pinned NeoForge installer hash and created only the marked `full-processing-server` runtime. `python3 scripts/verify-server.py clean-boot` passed with JVM PID 27 and `fixture=False`; the server log listed Create 6.0.10, addon 0.2.0-dev and Mekanism 10.7.19, loaded recipes and started on `127.0.0.1:25566`, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, then exited 0.
- `python3 scripts/verify-server.py persistence` used two Java 21 JVMs sequentially against the same new disposable world. PID 27 loaded the fixture, wrote actual chest/tank contents and logged `CMZ_PERSIST_SEED_PASS world=cmz-full-processing`, saved and exited 0. PID 214 loaded the same world in a separate process, read its saved contents and logged `CMZ_PERSIST_CHECK_PASS items=5 chemicals=2` and `TWO_JVM_WORLD_PERSISTENCE_PASS`, then exited 0. The fixture verifies five actual item stacks and two actual Mekanism chemical-tank stacks; it does not synthesize expected output. After the run, the `mods` directory contained only the ordinary addon, Create, and Mekanism JARs; the fixture was removed.
- GameTest evidence for 3x routes: normal and deepslate zinc ore each yielded 3 Clumps and 3 final ingots; one raw zinc yielded 2 Clumps and 2 ingots; one raw zinc block yielded 18 Clumps and 18 ingots. For 4x, normal/deepslate ore yielded 4 Shards→4 Clumps→4 ingots; 3 raw zinc yielded 8→8→8; a raw zinc block yielded 24→24→24. For 5x, normal/deepslate ore produced 1000 mB dirty/clean slurry→5 crystals→5 Clumps/ingots; 3 raw zinc produced 2000 mB→10→10; a raw block produced 6000 mB→30→30. Actual machine outputs were carried between stages.
- Negative real-machine checks: missing power or oxygen/hydrogen chloride/sulfuric acid retained inputs and produced no output. For per-tick chemical stages, tests observed actual chemical consumption, then removed supply and confirmed stall with retained input/no output. Washer consumed water at exactly 5 mB per slurry mB (5,000/10,000/30,000 mB); 4 mB water retained dirty slurry, and the 30,000 mB route replenished a real 10,000 mB tank. Crystallizer retained 199 mB with no crystal and consumed slurry in exact 200 mB units. All addon purification/injection/dissolution recipes rejected 64 `create:crushed_raw_zinc` and 64 `create:zinc_ingot`; 1–2 raw zinc items were retained at 4x injection and 5x dissolution entrances.
- After persistence and automatic fixture removal, `python3 scripts/verify-server.py clean-boot` was run again against the same saved `cmz-full-processing` world. It started PID 27 with `fixture=False`, loaded only the ordinary addon, Create and Mekanism JARs, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, and exited 0. Transcript: `.local/verification/full-processing-server/clean-boot-after-persistence.log`. NeoForge logged that saved metadata referenced the now-removed test-fixture mod (`create_mekanism_zinc_tests -> MISSING`); production startup and world load succeeded. This is expected for a test-seeded world; the fixture JAR remained absent.
- The verified production JAR was copied to the parent project's ignored `dist/cmz-trial/create-mekanism-zinc-0.2.0-dev.jar`; the prior 0.1.0-dev JAR remains there unchanged. This is a local trial handoff, not a public release or parent-pack installation.
- The dedicated server uses `online-mode=false` for this private loopback test. It does not authenticate clients. GUI, authenticated user login, and user-side 3x–5x processing remain unverified; see [client-acceptance.md](client-acceptance.md). The user previously reported 2x processing worked.

## Final controller review (2026-10-01)

The controller accepted each Luna implementation task and its review corrections before proceeding. Final `:neoforge:runData` at 19:33 JST succeeded with `written: 0` and no tracked or untracked generated-resource diff. A separate `:common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` invocation succeeded; cached JUnit reports matched the unchanged source (6 common + 2 NeoForge, zero failures/errors), and the boundary check again reported `0 artifacts`. A controller production-only boot at 19:35 JST used JVM 26, reopened `cmz-full-processing`, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, and exited 0. Its transcript is in the parent project's `.local/issue7-control/milestone-2/controller-final-clean-boot.log`.

The final ordinary JAR in source build output, parent `dist/cmz-trial/`, isolated dedicated server, and fresh Windows Prism instance has the identical SHA-512 recorded above. All three trial/runtime JARs (the addon and two pinned dependencies) were verified against their recorded hashes; no fixture remains in either ordinary server or client. The controller then started the ordinary trial server at 19:38 JST; its bind is `127.0.0.1:25566`, and Windows PowerShell TCP reachability was confirmed. This proves transport reachability only. At that point Windows GUI startup and 3x–5x user acceptance were pending; see the later native-client and user-acceptance observations below.

## Native client startup (2026-10-01 20:37–20:39 JST)

At the user's request, Windows Prism launched `moco-cmz-trial-0.2.0-dev-client` with `--server 127.0.0.1:25566`. The ordinary client log confirmed addon `0.2.0-dev` and NVIDIA GeForce RTX 4060 Ti / OpenGL 4.6.0 NVIDIA 572.60 initialization. The client logged connection at 20:39:05, and the dedicated server logged a loopback player joining its world at 20:39:07. The player later disconnected normally. This establishes native startup and dedicated-server joining; at that point seven-material appearance and 3x–5x user processing acceptance were pending. The offline server does not authenticate accounts. Evidence is the new Prism instance's `minecraft/logs/latest.log` and the isolated server's `logs/latest.log`; no account data or raw player identifiers are copied into this record.

## User acceptance through 4x and recipe viewer (2026-10-01)

The user reported “4倍まで確認した”. Record successful client processing through 4x, without treating this as a per-input measured-results checklist or acceptance of all seven material displays. At that point the 5x user check was pending because they could not find its recipe chain without a recipe viewer.

After confirming the native trial client was closed, the controller added only JEI 19.51.0.418 (Modrinth `ufHUqt9b`) to `moco-cmz-trial-0.2.0-dev-client`. Existing three JAR hashes were checked before the addition; the fourth JAR's size and SHA-512 matched the pinned Modrinth metadata. No save or existing configuration was edited. The dedicated server retains three JARs. JEI is an optional trial-client tool, not an addon dependency. Its complete pin, controls, and 5x guide are in [client acceptance](client-acceptance.md).

Native Windows startup at 20:56–20:57 JST discovered JEI alongside the fixed addon/Create/Mekanism versions, initialized JEI ConfigManager and created the JEI GUI atlas without an ERROR line. At startup, world-dependent recipe registration, the displayed zinc chain, and actual 5x client processing were still pending.

## User acceptance of all processing lines (2026-10-01)

Following their earlier successful 2x and through-4x reports, the user reported “5倍ラインも正常動作を確認した”. Client processing-line acceptance for 2x–5x is complete based on these user observations. This report supplies the previously missing 5x client result; it does not provide measured results for each input variant or a separate seven-material appearance/translation checklist. The JEI screen itself was not separately reported as checked.

The accepted ordinary addon remains `0.2.0-dev` with the previously recorded artifact hash. This update changes acceptance records only; no source, recipes, resources, dependencies, worlds or live runtime settings were changed. No new Java test run was needed for this documentation update. Formal artwork and parent-pack installation/coexistence verification remain follow-up work. No public release or Issue #7 closure was performed.

## Original item artwork revision (`0.2.1-dev`, 2026-10-01)

At the user's request the controller created five original concept images with built-in `image_gen`, without third-party image inputs, then separately authored editable 16×16 pixel grids. Luna implemented the native-grid PNG writer and generated the production images; the controller reviewed the generator, all five silhouettes at enlarged/actual size, and the final JAR. Provenance, prompts and fixed-source license review are in [assets](assets.md).

`python3 tools/generate_icons.py` regenerated all five production PNGs from `artwork/zinc-v1.json`. Independent controller regeneration into a temporary directory produced byte-identical files. The controller JAR inspection confirmed five new 16×16 RGBA textures matching source, all 21 recipe JSONs unchanged from the accepted `0.2.0-dev` JAR, all production class files byte-identical, metadata `0.2.1-dev`, and no fixture/test classes or nested JARs.

Luna's `:common:test :neoforge:test :neoforge:build` completed successfully. The same controller command also completed `BUILD SUCCESSFUL`; 13 tasks were up-to-date and the common dependency boundary executed with `0 artifacts`. The unchanged-code JUnit reports have 6 common + 2 NeoForge tests, zero failures/errors. GameTests and server persistence were not repeated for this image/metadata-only revision; their recorded results describe the accepted `0.2.0-dev` build.

The ordinary JAR is 49,681 bytes, SHA-512 `40d1bbd634d23173a77295db484f6a5b7fedba744723e30862b99d7f006e69b6de54ed2a2b15d2b5114e2e4870e6447d9c891f562d62a8386ddf993d2022c8e8`. A byte-identical copy is in the parent project's ignored `dist/cmz-trial/create-mekanism-zinc-0.2.1-dev.jar`. The prior `0.2.0-dev` JAR, live trial server and native Windows instance were not replaced. In-game texture rendering and user visual acceptance of zinc-v1 remain pending. No public release or main-pack installation was performed.

## Native artwork-client startup (2026-10-02 07:20 JST)

The user requested a singleplayer icon check and explicitly did not need a dedicated server. After checking that Windows Java/Prism processes were stopped, the controller verified all four existing client JAR hashes, backed up the old addon outside mods, and installed the hash-verified `0.2.1-dev` ordinary JAR. Exactly one addon and four total JARs remain. Create, Mekanism, JEI, save files and settings were preserved. The Prism profile ID remains `moco-cmz-trial-0.2.0-dev-client`.

Windows Prism launched that profile without a server argument. At 07:20 JST the native client log confirmed addon `0.2.1-dev`, JEI 19.51.0.418, Microsoft Java21, NVIDIA rendering and JEI GUI atlas creation, with no ERROR line in the inspected startup log. No dedicated-server startup command was issued. This confirms native startup; in-game item appearance still awaits the user's singleplayer check. The original processing acceptance remains the `0.2.0-dev` record.

## Reference-style artwork and Japanese labels (`0.2.2-dev`, 2026-10-02)

The user found two Japanese names inconsistent with comparable items and reported that zinc-v1 stood out when viewed beside other materials. They supplied five screenshots in dust/dirty dust/clump/shard/crystal order and requested similarity in visual characteristics, explicitly without processing the existing images. The controller generated a new style-study board with built-in image_gen, labeled the screenshots only as style/category references, then separately authored zinc-v2 native pixel grids. The five textures have subtler contours/highlights, broad granular dust mounds, a solid elongated clump, a short jagged shard and one oblique crystal. The enlarged/16px native preview was reviewed by the controller; user in-game acceptance remains pending.

Luna first changed only the two requested Japanese keys, which the controller reviewed, then generated the five approved pixel grids, changed the generator default input and set `mod_version=0.2.2-dev`. The exact labels are `汚れた亜鉛の粉` and `亜鉛の凝塊`; the other five translations, including both slurry names, remain unchanged.

Both Luna's and the controller's `:common:test :neoforge:test :neoforge:build` completed successfully. The controller repeat had 13 up-to-date tasks; common dependency boundary executed with 0 artifacts. Existing unchanged-code JUnit reports show common6/NeoForge2, zero failures/errors. Independent PNG regeneration produced byte difference0. Controller JAR inspection confirmed all five new 16×16 RGBA textures match source, both exact labels, all production classes and all data JSON byte-identical to0.2.1-dev, 21 recipes, metadata0.2.2-dev, no fixture/test classes or nestedJARs. GameTests/persistence were not repeated for this display-only revision.

The ordinary JAR is49,610bytes, SHA-512 `f4950702e971728bba8bf1e5538e4cce59b7e854465830746ac99df7b761884f776c925ede42ea67e4d023c6d08e0460d6092a58c613013de0fd6c2c31d205d5`; a byte-identical copy is at parent `dist/cmz-trial/create-mekanism-zinc-0.2.2-dev.jar`. At artifact preparation, the Windows client, live server and saved worlds had not been updated. The subsequent client-only installation is recorded below. No public release or parent-pack installation occurred.

## Native reference-style client launch (2026-10-02 07:39 JST)

Windows Java was stopped, so the controller backed up the old0.2.1-dev addon outside mods and installed the hash-verified0.2.2-dev JAR into the existing trial profile. All four client JAR hashes matched their pins; exactly one addon remains. Create, Mekanism, JEI, world saves and settings were retained. Prism launched without a server argument, and no dedicated server was started.

The native log confirmed0.2.2-dev, JEI19.51.0.418, Java21, NVIDIA rendering and JEI GUI atlas creation at07:39 JST, with no ERROR line in the inspected startup log. This confirms client startup; translated item labels and icon appearance in-world still await user confirmation.

## Canvas-crop artwork (`0.2.3-dev`, 2026-10-02)

The user clarified that the five icons shown on the generated zinc-v2 canvas were the intended game artwork. The controller had instead authored a different 16×16 grid for0.2.2-dev. This was an asset-selection error, not a loader transformation. The user requested extracting the actual canvas, so the controller fixed five crop rectangles and delegated only extraction, versioning and build work to the existing Luna agent. The controller performed the review and acceptance checks.

`python3 tools/generate_icons.py --output .local/artwork/zinc-v2/root-candidates` was run twice independently by the controller. All five PNGs matched production bytes and repeated generation produced identical hashes. Independent PNG decoding confirmed every RGBA value within each prescribed source rectangle is preserved, all remaining512×512 padding is transparent, and the2560×512 preview consists of the five actual production PNGs. A deliberately wrong source was rejected with exit2 before the output directory was created. The source SHA256 is `3c27148393b58faf8b5a77aa121ab3498d351677333bf9315063d1b0171f928c`; boxes are recorded in `artwork/zinc-v2-crops.json`. No resize, recoloring, alpha threshold or separate redraw is used.

Both Luna and the controller ran `:common:test :neoforge:test :neoforge:build` successfully with the documented Java21 and Gradle environment. The controller repeat reported14 actionable tasks,1 executed and13 up-to-date; the common boundary passed with0 artifacts. Unchanged-code JUnit reports contain common6/NeoForge2 tests with0 failures/errors. Controller JAR inspection confirmed five512×512 RGBA textures identical to production, metadata0.2.3-dev,21 recipes, no test fixture or nestedJAR, and all96 non-image production entries (classes, data and other assets) byte-identical to0.2.2-dev. GameTests and world persistence were not repeated for this image-only correction.

The ordinary JAR is533,864bytes, SHA512 `c0a6a8a979d8e30fcff9bc2631ec80ed630c11b1d6df3450c8691a9a2e5468259d5aad7f2edc1810b64be88b16a71bdd2765a389d0ed6884270ee2690ec38fe7`; an identical copy is at parent `dist/cmz-trial/create-mekanism-zinc-0.2.3-dev.jar`. CI now checks icon regeneration against tracked textures and uploads the current ordinary JAR rather than the obsolete0.2.0-dev filename. The previous native-grid renderer is preserved with an isolated default output under`.local/`.

Windows Java and Prism were stopped immediately before installation. The controller checked all four pinned JARs, backed up0.2.2-dev and the instance metadata outside mods, installed0.2.3-dev and rechecked every hash; exactly one addon remains. Create, Mekanism, JEI, saves and settings were retained. The existing Prism profile was launched without a server argument; no dedicated server was started.

At08:00 JST the first native startup ended in `EXCEPTION_ACCESS_VIOLATION` in Microsoft Java21.0.7 `jvm.dll`, current thread `GC Thread#11`, after Datafixer bootstrap. No cause is inferred from this single native failure. The same JARs, Java and settings were retried. At08:03 JST the native log confirmed0.2.3-dev, JEI19.51.0.418, NVIDIA rendering, Sound engine startup and JEI GUI atlas creation, with no ERROR or missing-texture line in the inspected startup log. The Java process remained running and responsive; the first failure was not reproduced by this retry. The later user visual acceptance is recorded below. Luna also observed one SIGSEGV in an initial auxiliary Python subprocess verification harness; its standalone generation and direct checks, and the controller's independent repeated generation/pixel/JAR checks, passed. Crash logs remain local and are not distributed.

## Cropped-icon client acceptance (2026-10-02)

The user checked the installed `0.2.3-dev` crop artwork in the Windows singleplayer trial and reported: 「確認した。十分実用的なので良さそう」. Record the five item icons as visually accepted and retain this exact artwork/JAR as the baseline. Earlier user reports already accepted all2x–5x processing lines. This report does not supply a separate seven-material display checklist or per-input measured processing counts.

The verified implementation commit is `c87f105`. [CI run36939017225](https://github.com/moco-server/create-mekanism-zinc/actions/runs/36939017225) succeeded, including unit tests, data-generation diff checks, build, GameTests, icon regeneration checks and ordinary JAR upload. This acceptance-record update changes documentation only; the JAR, source artwork, translations, recipes and runtime remain unchanged.

## Historical observations — addon 0.1.0-dev at commit `f5f3024`

The following is a historical snapshot of the earlier six-recipe implementation, recorded 2026-10-01 at commit `f5f3024c1ee0f0a3430b8ee9c9f9b863d4f46b8a`. Its runtime directory, server port, JAR names, and GameTest count differ from the current `0.2.0-dev` procedure above. These results are retained for history and do not describe the current runtime.

- Target then: Minecraft 1.21.1, NeoForge 21.1.252, Java 21 (`21.0.12.1+1-LTS`), Gradle 9.2.1, ModDevGradle 2.0.148, Create 6.0.10 (`UjX6dr61`), Mekanism 10.7.19.85 (`5KzzycBT`). Artifact `create-mekanism-zinc-0.1.0-dev.jar`; six recipes.
- Runtime then: `.local/verification/clean-server/instance/cmz-verification`, loopback `127.0.0.1:25565`, `online-mode=false`. The previously accepted EULA file was copied; no saved user world was used. Runtime output was under `.local/verification/clean-server/`.
- At that revision `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` passed; 5 common tests and 2 NeoForge tests had zero failures/errors, and common-boundary inspection found `0 artifacts`. `./gradlew :neoforge:runGameTestServer` reported all 13 required GameTests passed.
- Historical generated-resource determinism also passed: `:neoforge:runData` was run in two separate Gradle invocations; both ended `BUILD SUCCESSFUL`, the second reported `written: 0`, and SHA-256 manifests of all generated resource files before/after were identical.
- Historical ordinary JAR SHA-512 was `ad22d5220015dfa09ec94ed7439f3f517ea89d1c2178b050b0dcfaf4fd7d11047a4378ef07085fef53bc515d273e18328128b1a88637afc71781dbc6f4b4b4d3`. Inspection at that stage found one production `META-INF/neoforge.mods.toml`, no fixture classes, no embedded dependency JARs, five textures, five models, and two language files. A production-only dedicated-server boot logged `CLEAN_PRODUCTION_JAR_BOOT_PASS` and exited 0.
- The old persistence harness used separate seed/check Java server processes. Controller reruns against the same historical world at 16:05 and 16:06 JST passed: `CMZ_PERSIST_SEED_PASS world=cmz-verification`, then `CMZ_PERSIST_CHECK_PASS items=5 chemicals=2` and `TWO_JVM_WORLD_PERSISTENCE_PASS`. The fixture checked actual chest slots and Advanced Chemical Tank stacks. The seed was later fixed to clear tank contents before insertion; repeated checks stored 1111 mB dirty and 2222 mB clean slurry correctly.
- The comparison of all 21 recipes against pinned-version iron JSON equivalents is a current `0.2.0-dev` check; it was not part of this six-recipe historical snapshot.
