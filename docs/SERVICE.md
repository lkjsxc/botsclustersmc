# 共有 Linux 環境での常駐運用

`host/systemd/` は共有 Coder 環境で実測した、任意導入の systemd 設定例です。
通常の `./start.sh`、学習本体、報酬、昇級条件は変更しません。
[初回の実測記録](verification/20260926-shared-workspace.md)と
[継続稼働・定期評価の検証](verification/20260927-shared-workspace-continuity.md)も確認してください。

## 前提と設定

JDK 21 の `/usr/bin/java`、ユーザー `coder`、リポジトリ
`/home/coder/workspace/botsclustersmc`、`ACADEMY=academy` が前提です。
別の環境では両 unit の `User`、`Group`、`WorkingDirectory`、実行パスと、
学習 unit の `ReadWritePaths` を適合させます。
**監視 unit は `.env` を読みません。** `ACADEMY` を変更する場合は、
監視の `ExecStart` にあるデータディレクトリも必ず合わせてください。

初回に検証した共有 12 CPU / 8 GiB 環境での主要設定は次のとおりです。
2026-09-27 の継続確認時は 12 GiB に増えていましたが、学習設定は維持しています。
既存 `.env` の無条件上書きや、既存 Academy の人口変更には使いません。

```text
EULA=false
ACADEMY=academy
BOTS=512
HEAP_GB=4
REGION_THREADS=4
INFERENCE_THREADS=1
LEARNER_THREADS=2
PORT=25565
BIND_ADDRESS=0.0.0.0
ONLINE_MODE=true
OFFLINE_ACCESS_ACK=false
SEED=7
JAVA_BIN=/usr/bin/java
```

EULA を読んで同意した場合だけ `EULA=true` にします。
人間の接続認証は有効のままです。NPC のログインは不要です。
人口はチェックポイントに固定され、別の体数には別の空の Academy が必要です。

学習プロセス群は最大 5.5 GiB、CPU 時間は最大 6 コア相当、監視は最大 512 MiB。
ヒープ以外のメモリも制限対象です。開発作業より低い優先度で実行します。
他の処理を含む OOM 回避や性能の保証ではありません。スワップも観測しています。
余裕と実際の行動数・学習数を確認し、CPU 使用率だけで判断しないでください。
試験中の actor は学習しないため、試験人数によって学習サンプル毎秒は変動します。

## 導入

パスと `.env` を確認し、リポジトリ直下で実行します。

```sh
./test.sh
sudo systemd-analyze verify host/systemd/botsclustersmc-training.service \
  host/systemd/botsclustersmc-monitor.service
sudo install -m 0644 -o root -g root host/systemd/botsclustersmc-training.service \
  /etc/systemd/system/botsclustersmc-training.service
sudo install -m 0644 -o root -g root host/systemd/botsclustersmc-monitor.service \
  /etc/systemd/system/botsclustersmc-monitor.service
sudo systemctl daemon-reload
sudo systemctl enable --now botsclustersmc-training.service botsclustersmc-monitor.service
```

学習本体は root ではなく `coder` で動きます。
同じ Academy を手動 `start.sh` と常駐サービスで同時起動しないでください。
外部公開のポート転送やファイアウォール変更は行いません。

## 状態確認・停止・再開

```sh
systemctl status botsclustersmc-training.service botsclustersmc-monitor.service
./status.sh
./console.sh bots progress
curl --fail http://127.0.0.1:8765/api/status
sudo journalctl -u botsclustersmc-training.service -n 60 --no-pager
```

`active` だけでなく、新しい status 時刻、全 actor の行動、学習数・更新数の増加、
失敗・拒否件数を確認します。監視画面はループバックにのみ待ち受けます。
Coder の認証付きポート転送か SSH トンネルを使い、無認証で公開しないでください。
制御ファイル、モデル、チェックポイントも外部公開しません。

```sh
sudo systemctl stop botsclustersmc-training.service
systemctl show botsclustersmc-training.service -p ActiveState -p Result
./export.sh
```

正常停止と最終保存を確認してから `academy/` 全体を別の保存先へバックアップします。
推論用 `policy.bcmc` は、学習状態 `training.bcmc` の代わりにはなりません。
同じホーム内のコピーだけではホーム削除やストレージ故障に備えられません。

```sh
sudo systemctl start botsclustersmc-training.service
./status.sh
```

保存済み学習では `startup_restored_checkpoint=true` と学習数の増加を確認します。
新規開始なら `false` です。壊れた状態を自動削除・初期化する設定ではありません。
終了時は supervisor の保存処理を待ちます。`SuccessExitStatus=143` は正常停止試験で
観測した Java の終了値です。異常終了時の再起動には回数制限があります。

## 更新・評価・復旧の範囲

自動 `git pull` はしません。評価を終了し、正常停止・未コミット変更確認の後に、
fast-forward 更新とテストを行って再開します。unit を変更した場合は再導入と
`daemon-reload` が必要で、実行中サービスへの適用には再起動が必要です。

評価は追加 JVM とメモリを使います。共有環境の余裕を確認してください。
単発では[評価手順](EVALUATION.md)に従い、例えば
`--heap-gb 1 --tasks 0,1,2 --cases 32` を指定します。
固定モデルの試験成績と学習中の成績、過去の昇級記録は別物です。
定期評価は次節の任意導入です。定期バックアップは含めていません。

端末接続に依存しない常駐とサービスの正常停止・復元・再開を確認しました。
自動起動設定は、**Coder のコンテナ再作成やワークスペース・ホーム削除からの
復旧保証ではありません。** OS が置き換わると JDK と `/etc/systemd/system/` の
再導入が必要になり得ます。2026-09-27 は既に再起動した環境で保存状態からの
自動復元を確認しましたが、こちらからのコンテナ削除・再作成試験ではありません。

## 任意の定期評価

`botsclustersmc-evaluation.timer` は、起動から約5分後に最初の評価を行い、
各評価の終了から約30分後に次を実行します。導入時点で起動から5分以上経過していると、
最初の評価は直ちに始まります。実行時刻には30秒の許容幅があります。
評価を待つ間は追加の Minecraft JVM を残しません。

各回は現在のソースをビルドし、保存済みチェックポイントを一度だけ読み、
その固定モデルで到達済みの全段階（練習中の最先端を含む）を各32ケース測ります。
試験ごとに乱数条件を変えます。学習、報酬、昇級判定、モデル選択・配置には介入せず、
結果は既存の読み取り専用ダッシュボードに表示します。自動 `git pull` は行いません。

先に学習を一度起動し、所有済み `academy/` と正常な `training.bcmc`、
`.build/`、`.cache/`、`dist/` が存在することを確認します。
EULA は既存の明示的な同意を読み、サービスから勝手に有効化しません。
別の Academy やパスでは評価 unit の `WorkingDirectory` と `ReadWritePaths` も変更します。
`academy/` は結果を書き込むため書き込み可能ですが、評価コードは学習用状態を書きません。
チェックポイント単体を読み取り専用 bind mount にしないでください。
学習側がファイルを原子的に置換するため、古いファイルを固定してしまうおそれがあります。

```sh
# 手動の evaluate.sh が実行中なら、その正常終了を先に確認する。
sudo systemd-analyze verify host/systemd/*.service host/systemd/*.timer
sudo install -m 0644 -o root -g root host/systemd/botsclustersmc-evaluation.service \
  /etc/systemd/system/botsclustersmc-evaluation.service
sudo install -m 0644 -o root -g root host/systemd/botsclustersmc-evaluation.timer \
  /etc/systemd/system/botsclustersmc-evaluation.timer
sudo systemctl daemon-reload
sudo systemctl enable --now botsclustersmc-evaluation.timer
```

評価プロセス群には CPU 最大2コア相当、メモリ最大2.5 GiB、低い実行優先度を設定し、
試験用サーバーのヒープは1 GiBです。将来の全段階・他の負荷での容量保証ではありません。
systemd の同じ service が重複起動されることはなく、手動評価との競合も既存の
Academy 排他ロックが拒否します。ビルド中の競合やチェックポイント不在・破損は失敗として扱い、
最後に完了した結果を残します。失敗を技能の0点と混同しないでください。

```sh
systemctl list-timers botsclustersmc-evaluation.timer
systemctl show botsclustersmc-evaluation.service -p ActiveState -p Result -p ExecMainStatus
sudo journalctl -u botsclustersmc-evaluation.service -n 60 --no-pager
```

実行中の one-shot service は `activating`、正常終了後は `inactive` になります。
`inactive` だけで停止故障と判断せず、タイマーの次回時刻と `Result=success`、
ダッシュボードの新しい結果時刻・固定モデル番号・試験成功数を確認してください。
終了コード143を正常停止として扱っていても、完了した試験とは限りません。

ソース更新、手動再評価、停止バックアップの前には、次の両方を停止します。
タイマーだけの停止では既に進行中の評価を止めません。

```sh
sudo systemctl stop botsclustersmc-evaluation.timer botsclustersmc-evaluation.service
# ソース更新・検証や手動評価の終了後
sudo systemctl start botsclustersmc-evaluation.timer
```

結果ファイルは最新の完了結果に置き換わります。失敗したケースも含む完全な比較記録や
正確に試験した重みを保存する場合は、タイマーを止めて単発の `--export NEW.zip` を使います。
同一モデルの別 seed 検証には `--from` を使います。このタイマーは学習バックアップや
履歴モデルの自動保存ではなく、学習中の技能を定期的に測る仕組みです。
