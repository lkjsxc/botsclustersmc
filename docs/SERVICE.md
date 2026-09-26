# 共有 Linux 環境での常駐運用

`host/systemd/` は共有 Coder 環境で実測した、任意導入の systemd 設定例です。
通常の `./start.sh`、学習本体、報酬、昇級条件は変更しません。
[実測記録](verification/20260926-shared-workspace.md)も確認してください。

## 前提と設定

JDK 21 の `/usr/bin/java`、ユーザー `coder`、リポジトリ
`/home/coder/workspace/botsclustersmc`、`ACADEMY=academy` が前提です。
別の環境では両 unit の `User`、`Group`、`WorkingDirectory`、実行パスと、
学習 unit の `ReadWritePaths` を適合させます。
**監視 unit は `.env` を読みません。** `ACADEMY` を変更する場合は、
監視の `ExecStart` にあるデータディレクトリも必ず合わせてください。

共有 12 CPU / 8 GiB 環境での主要設定は次のとおりです。
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

評価は追加 JVM とメモリを使います。共有環境の余裕を確認して単発で実行します。
[評価手順](EVALUATION.md)に従い、例えば `--heap-gb 1 --tasks 0,1,2 --cases 32`
を指定します。固定モデルの試験成績と学習中の成績、過去の昇級記録は別物です。
常駐 evaluator や定期バックアップはこの例に含めていません。

端末接続に依存しない常駐とサービスの正常停止・復元・再開を確認しました。
自動起動設定は、**Coder のコンテナ再作成やワークスペース・ホーム削除からの
復旧保証ではありません。** OS が置き換わると JDK と `/etc/systemd/system/` の
再導入が必要になり得ます。コンテナ全体の再起動・再作成は試験していません。
