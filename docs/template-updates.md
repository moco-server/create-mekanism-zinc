# テンプレート採用履歴

## 2026-10-02: 既存Java MODをKotlin構成へ移行

当時の合意事項と受け入れ条件を参照できる。[Kotlinテンプレート移行仕様](kotlin-template-migration.md)

- 基盤: [moco-server/minecraft-mod-template](https://github.com/moco-server/minecraft-mod-template)
- template-version: `0.1.0`
- 固定commit: `dfee13176ce3ec8e64d27dfdd12376a7cfd58b6b`
- 採用: Gradle Kotlin DSL／wrapper、Kotlin・KFF・静的解析、pure core、buildSrc検査、Linux／Windows CI、通常JAR収集、GameTest結果検査、ローカルMOD skills、任意の外部skills導入、issue／PR template。
- 除外: 新規MOD用initializer、sample MODとsample resource、テンプレート自体の初期化テスト。既存のMOD ID／package／素材／レシピ／画像／翻訳を維持するため。
- 追加維持: 既存のSHA512依存検証、33実GameTests、保存fixture、Java版保存の移行検証、21レシピを含む独立JSON期待値、画像再生成検査。
- 適応: core禁止packageへ連携MODを追加、通常JARの実素材・classesを必須化、client専用JEI、シンボリックリンクのartifact出力先拒否、再現可能なJAR生成。

`template-version.txt`は採用基盤の版を表す。テンプレートの新規生成完了を偽装する`.template-initialized.json`は作成しない。今後の追随では対象commitの差分を比較し、ゲームの対応版やregistryを一括上書きせず、[検証記録](verification.md)の受け入れ条件を再確認する。

## 移行時の判断と費用

- 既存Java 21は読み取り専用で再利用し、キャッシュはworktree内へコピーした。環境差はbaseline試験で検出するが、約3 GBの検証領域を使う。
- テンプレートにない既存SHA512検証を保持・拡張した。依存更新時のchecksum保守が増える。
- packageのunderscoreを互換のため維持し、package規約と係数・色のMagicNumberのみ局所抑制した。互換部分に限る規約例外が残る。
- Java版生成JSONを独立期待値にした。将来の意図的なレシピ変更では期待値の再承認も必要になる。
- 大きいGameTest helperを用途別に分けた。helperの可視性が変わるため、コンパイルと実GameTestで検証した。
- artifact収集先のsymlinkを拒否し、JARのtimestampと順序を固定した。通常の空directoryへの収集と配布内容は維持する。

最終レビューでWindows native 24 JARのSHA512不足を修正した。Windows OS属性で実runtime configurationを解決して失敗→成功を確認し、Linux checksも再実行した。Windows固有依存のchecksum保守が追加されるが、strict検証は維持する。

## 最終レビューの判定と対象範囲

独立レビューは修正後の構成をマージ可能と判定した。Criticalなし、Windows checksum不足のImportant 1件は解消、未解決Minorなし。

次の保留判断を確認し、今回の検証範囲として採用する。

- Kotlin版GUI・操作・JEI画面は未実施。別profileでの試遊が必要で、Java版の検収を新版の実測として扱わない。
- Windows OS／GitHub Actionsの全工程は未実施。Windows属性の依存解決成功だけではOS上の成功を保証しない。
- 外部利用者向けJava binary API互換は仕様の保証対象外。registry／生成resource／保存互換は実測している。
- 固定版以外、Fabric、親packとの共存は対象外。対応を広げる場合は別途検証が必要。
- 検証専用runtime／cacheを作成後に手動改変する場合の包括的な耐改ざん性は対象外。所有範囲・symlink・hashの検査は通常の生成／再利用経路を保護する。
