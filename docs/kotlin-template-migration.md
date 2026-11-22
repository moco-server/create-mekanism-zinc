# Minecraft MODテンプレートへの移行仕様

2026-10-02に合意したJava版からKotlin版への移行仕様。現行の設計と固定版、移行時の実測結果は関連文書に記録している。[現在の設計](design.md)・[対応構成](compatibility.md)・[検証記録](verification.md)

## 目的と合意済みの方針

既存のCreate Mekanism Zincを、moco-server/minecraft-mod-templateをベースとした独立Kotlin MODとして保守できる構成へ移行する。ユーザーは2026-10-02に、Kotlin化を含める方針を承認した。

加工機能の追加・変更は行わず、受け入れ済みの2x〜5x加工と画像を維持する。履歴を置き換えるのではなく、テンプレートの必要な構成を取り込む。新規MOD向け初期化スクリプトを既存リポジトリへ直接実行しない。

この文書は実装前の仕様であり、以下の検証を実施済みとするものではない。

## 比較対象と固定版

- 移行前: このリポジトリの`243ec7f`（`0.2.3-dev`）。
- テンプレート: https://github.com/moco-server/minecraft-mod-template の`dfee13176ce3ec8e64d27dfdd12376a7cfd58b6b`、`template-version.txt`は`0.1.0`。
- Minecraft `1.21.1`、NeoForge `21.1.252`、Java `21`、ModDevGradle `2.0.148`、Create `6.0.10`（Modrinth `UjX6dr61`）、Mekanism `10.7.19.85`（`5KzzycBT`）を維持する。
- Gradleは現行`9.2.1`からテンプレートの`8.12.1`へ揃え、Wrapperと配布SHA256を取り込む。これはKotlin・静的解析を含むテンプレートの検証済み構成を再利用するため。
- Kotlin `2.0.21`、KotlinForForge NeoForge `5.5.0`、detekt `1.23.8`、ktlint plugin `12.1.2`／本体`1.5.0`、JUnit `5.11.4`を採用する。
- 移行版は`0.3.0-dev`とする。KFFという実行時依存が加わる版を、受け入れ済みの`0.2.3-dev`と区別する。通常JAR名の接頭辞`create-mekanism-zinc`は維持する。

## モジュールとKotlin実装

`common`を`core`へ変更し、本番Javaコード、単体テスト、GameTest、保存検証fixtureをKotlinへ移植する。Gradle設定はKotlin DSLへ統一する。画像制作・サーバー検証のPythonスクリプトは維持する。

### core

- ZincContent、ZincProcessingと既存の単体テストを移す。
- 現行のpackage `org.mocoserver.create_mekanism_zinc.content`／`processing`は維持する。テンプレートのサンプルpackageへ変更しない。
- Kotlin/JVMと標準ライブラリだけを本番依存とし、Minecraft、loader、KFF、Create、Mekanismの型や処理を持ち込まない。
- 公開APIには可視性と型を明示する。読み取り専用の公開型だけで不変性を保証したと扱わず、現在の変更不能な定義一覧を維持する。
- nullableな化学入力・流体入力は型で表し、使用するレシピ種別のJava API境界で契約を確認する。通常コードの`!!`を使用しない。
- 現行の定義クラスの役割を維持する。外部向けJavaバイナリAPI互換性は保証対象にしないが、登録IDや保存データの互換性は必須とする。
- テンプレートのbytecode／classpath境界検査を採用し、Create、Mekanismのpackage prefixを禁止対象へ追加する。

### neoforge

- 登録、イベント、creative tab、datagenは現行の責務と処理順を維持する。
- 本番MODのmetadataはKotlinForForge loaderへ移行する。fixtureのloaderも実際のKotlinクラス形態と対象版の読み込み仕様に合わせ、実起動で確認する。
- Create／Mekanismは固定版のcompileOnlyと開発runtimeを維持し、通常JARへ同梱しない。
- main、test、gametestの各source setでKotlinがコンパイル・検査される構成にする。
- 通常JARにはcoreの本番classesと必要resourceを含め、fixtureは別の`-gametests.jar`へ分離する。
- templateのサンプルitem、recipe、翻訳、GameTestは取り込まない。

### resource

- `common/src/main/resources`の本番resourceは`core/src/main/resources`へ移し、通常JARへの取り込みを維持する。
- アイコン生成スクリプトとCIの出力先も変更する。画像自体は移行前と全バイト一致を条件にする。
- 生成resourceは既存のdatagenで作る。生成JSONを手編集しない。

## ビルドと開発基盤

- root `check`から両モジュール、ktlint、型解決付きdetekt、buildSrcの検査を実行する。
- gametestの整形・型解決付き解析も対象にし、全体的な解析無効化やbaselineで移植違反を隠さない。必要な抑制には具体的な理由を添え、範囲を限定する。
- バージョンcatalog、core／buildSrcの依存lock、Wrapper checksum、`.editorconfig`、`.gitattributes`を導入する。
- templateのbuildSrcを採用し、通常JAR検査の必須entryを実MODのclasses、5アイテムの画像・モデル、翻訳、21レシピ、タグ、slurry data mapへ置き換える。
- `AGENTS.md`とMOD固有skills・補助資料を採用し、設計／検証記録の参照をこのMODへ適合させる。外部skillsの導入は通常ビルドへ依存させず、導入手順・固定manifestとして提供する。グローバル設定を変更しない。
- templateの初期化・サンプル用script/testは移行後のMODの検証に必要ないため取り込まない。CIのJAR収集・GameTest結果検査など実MODにも必要な補助を採用する。
- `template-version.txt`と移行記録に元template版、commit、取り込み内容、除外内容を残す。新規生成を装う初期化記録は作らない。

## 互換性と実行環境

維持するのはMOD ID `create_mekanism_zinc`、5アイテムと2化学物質の登録名、21レシピのID・内容、タグ、slurry data map、翻訳、画像、creative tabでの表示内容。Minecraft／NeoForge／Create／Mekanismの対応版を移行に便乗して変更しない。

KFF `5.5.0`の配布用JARを新たな実行時依存として記録する。Gradle用Maven aggregate JARを配布用JARとして扱わない。取得元、hash、sideを対応構成と実測に残す。通常JARへKFFやKotlin標準ライブラリを埋め込まない。

ローカルclient／server／datagen／GameTest環境は`.local`へ置く。開発clientでは既存の固定版JEIを開発限定で利用可能にし、通常MODの必須依存にしない。受け入れ済みの試遊profileやユーザーの保存ワールドは更新しない。

サーバー検証scriptはmod_versionから本番／fixture JARを選び、古い版の固定名を除去する。既存の管理対象runtime識別・loopback bind・hash照合・fixture除去は維持する。移行検証専用の隔離runtimeを使用し、過去の検証ワールドを上書きしない。EULAは既存の受諾済みファイルを使い、見つからない場合はその検証の前提不足として記録する。

## 検証と受け入れ条件

1. 移行前の本番resourceと生成resourceのhash、および旧Java版の本番／fixture JARを隔離した場所へ保存する。移行前のテスト結果を確認し、移行による失敗と環境不足を区別する。
2. `./gradlew check`でcore／NeoForge単体テスト、型解決付き静的解析、境界検査、buildSrc検査が成功する。既存の独立した21レシピ期待値を維持し、実装と同じ一覧から期待値を作り直さない。
3. datagenを独立したinvocationで実行し、生成resourceの追跡・未追跡差分がないことを確認する。全21レシピと関連resource、画像・翻訳が移行前と一致する。
4. 通常JAR、fixture JARをビルドし、`verifyModJar`で必要classes／resourceの存在、外部MOD・テスト・開発資料の非混入を確認する。
5. 実際のGameTest serverで既存の登録・2x〜5x加工・無効入力等のrequired testがすべて成功し、正の件数が記録される。
6. 配布用KFF入りの独立NeoForge serverで通常JARのみを起動し、fixtureがなくても正常起動することを確認する。
7. Kotlin版で保存・停止・別JVMで読み込みを実施し、5アイテムと2slurryのID・数量を維持することを確認する。
8. 別の隔離ワールドを旧Java版のfixtureでseedして保存し、Kotlin版へ置き換えた別JVMでcheckする。新規保存検証だけで旧版互換を証明したと扱わない。
9. Linux CIで品質検査、datagen差分、通常JAR検査、GameTest結果、画像再生成差分、通常JAR artifact収集を実施する。Windows CIでは品質検査と通常ビルド／JAR検査を実施する。CI定義の追加とremote CIの実行成功は区別する。

GUIの見た目と実操作の再検収は、起動やサーバー自動検証から推測しない。過去の受け入れ記録は残し、新版で未実施の項目を明記する。公開、packへの導入、push、PR、既存試遊環境の更新はこの移行の完了条件に含めない。

## 文書と完了時の報告

README、`docs/design.md`、互換性、検証、テンプレート更新記録、開発手順を新構成へ適合させる。古い検証・検収記録は履歴として維持し、現行手順と区別する。

完了時には、実際の変更、成功した検証、未実施の検証、通常JARの場所とhash、KFF追加による導入方法の変更を報告する。環境の前提不足で未実施の必須検証が残った場合は、移行を全面検証済みと報告しない。
