# テンプレート採用履歴

## 2026-10-02: 既存Java MODをKotlin構成へ移行

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
