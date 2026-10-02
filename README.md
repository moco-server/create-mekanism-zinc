# Create Mekanism Zinc

Minecraft 1.21.1 / NeoForge 21.1.252向けの亜鉛加工連携MODです。Create 6.0.10とMekanism 10.7.19.85を対象に、5つの中間素材、2種類のスラリー、2〜5倍加工の21レシピを追加します。MITライセンス。現在版は`0.3.0-dev`です。

[minecraft-mod-template](https://github.com/moco-server/minecraft-mod-template) 0.1.0を基盤に、既存MODをKotlinへ移行しました。[採用履歴](docs/template-updates.md)に固定commitと差分を記録しています。

- `core`: ゲームAPIに依存しないKotlinの素材・レシピ定義と共通resource。
- `neoforge`: 登録・datagen・ゲームAPIとの接続。本番コードとGameTestを分離。
- `buildSrc`: coreの依存境界と配布JARの検査。
- `.agents/skills`: このリポジトリ用のMOD開発手順。[AGENTS.md](AGENTS.md)から参照。

配布時はCreate、Mekanismに加えて **Kotlin for Forge 5.5.0** が必要です。通常JARは`neoforge/build/libs/create-mekanism-zinc-0.3.0-dev.jar`。`-gametests.jar`は検証専用です。固定版・取得元は[対応構成](docs/compatibility.md)を参照してください。

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

既存の加工と画像のクライアント検収は[履歴](docs/client-acceptance.md)に保存しています。Kotlin版のGUI試遊、WindowsとGitHub上のCIは未実施です。公開配布や親packへの導入はこの移行に含めません。
