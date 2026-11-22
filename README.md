# Create Mekanism Zinc

Minecraft 1.21.1 / NeoForge 21.1.252向けの亜鉛加工連携MODです。Create 6.0.10とMekanism 10.7.19.85を対象に、5つの中間素材、2種類のスラリー、2〜5倍加工の21レシピを追加します。MITライセンス。

[minecraft-mod-template](https://github.com/moco-server/minecraft-mod-template) 0.1.0を基盤に、既存MODをKotlinへ移行しました。[採用履歴](docs/template-updates.md)に固定commitと差分を記録しています。

- `core`: ゲームAPIに依存しないKotlinの素材・レシピ定義と共通resource。
- `neoforge`: 登録・datagen・ゲームAPIとの接続。本番コードとGameTestを分離。
- `buildSrc`: coreの依存境界と配布JARの検査。
- `.agents/skills`: このリポジトリ用のMOD開発手順。[AGENTS.md](AGENTS.md)から参照。

配布時はCreate、Mekanismに加えて **Kotlin for Forge 5.5.0** が必要です。通常JARは`neoforge/build/libs/create-mekanism-zinc-<version>.jar`。`-gametests.jar`は検証専用です。固定版・取得元は[対応構成](docs/compatibility.md)を参照してください。

## 開発・検証

Java 21を用意し、Gradle 8.12.1はwrapperから実行します。通常ビルドにNodeや外部skillsの導入は不要です。

```sh
./gradlew check :neoforge:build :neoforge:verifyModJar :neoforge:gameTestJar
./gradlew :neoforge:runData
./gradlew :neoforge:runGameTestServer
./gradlew :neoforge:runClient
```

`runClient`には固定版のJEIを開発限定で追加します。サーバー、datagen、GameTestには追加しません。生成resourceの変更は追跡・未追跡の双方を確認してください。

CI補助のテストはNode 24で`node --test scripts/test/*.test.mjs`、サーバー検証scriptのテストは`python3 -m unittest discover -s scripts/test -p 'test_*.py'`です。外部skillsは[任意の導入手順](docs/external-skills.md)を使います。

配布JARの起動、2 JVM間の保存、旧Java版ワールドの移行を確認する手順と結果は[検証記録](docs/verification.md)、現在の設計は[docs/design.md](docs/design.md)を参照してください。

既存の加工と画像のクライアント検収は[履歴](docs/client-acceptance.md)を参照してください。

## リリース

1. 配布版に含める変更のPRで、Node 24と固定版pnpmを使い、`corepack pnpm --config.stateDir=.local/pnpm-state install --frozen-lockfile --ignore-scripts`、`corepack pnpm exec changeset`を実行します。`create-mekanism-zinc`の版上げ種別と利用者向けの変更内容を記入し、生成された`.changeset/*.md`をPRに含めます。
2. `main`に取り込むと、Changesetsが**Release Create Mekanism Zinc** PRを作成または更新します。`package.json`、`gradle.properties`、`CHANGELOG.md`を確認し、そのPRをマージします。GitHub ActionsがPRを作成できるよう、リポジトリの **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests** を有効にします。
3. マージされたコミットを`Release`ワークフローが検証します。通常JARをGitHub PackagesへMaven登録し、同じJARを`v<version>`のGitHub Releaseに添付します。Releaseの本文は`CHANGELOG.md`の該当版から生成されます。公開処理が途中で失敗した場合は、そのワークフローを再実行できます。

Maven座標は`org.mocoserver.create_mekanism_zinc:create-mekanism-zinc:<version>`、登録先は`https://maven.pkg.github.com/moco-server/create-mekanism-zinc`です。取得時の認証方法は[GitHub PackagesのGradleレジストリ](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)を参照してください。
