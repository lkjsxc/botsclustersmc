# botsclustersmc 0.4.0 — git cloneから学習開始

このリポジトリのルートにMinecraftの実行本体を収録しました。以前の「研究部品だけのZIP」とは異なり、ソースからRustクライアントをビルドし、Folia上で32体のBotを動かして強化学習できます。旧ZIP・GitHub Actionsの成果物・手動でのファイル移植は不要です。

## 1. Linuxサーバーで準備

推奨割り当ては16論理CPU・12GiB RAM・120GBです。初回ビルド前に20GiB以上の空きが必要です。Java 21のJDKを使用します。macOSやWindowsの端末から、LinuxサーバーにSSH接続して実行してください。Linux x86_64で検証します。aarch64のソースビルド経路はありますが、実機検証対象ではありません。

Ubuntu 24.04での例です。Python・GPU・APIキーは通常運用に不要です。

```bash
sudo apt-get update
sudo apt-get install -y git curl ca-certificates build-essential pkg-config cmake unzip util-linux openjdk-21-jdk

git clone https://github.com/lkjsxc/botsclustersmc.git
cd botsclustersmc
cp .env.example .env
```

既定値は`BOTS=32`、`BOT_PREFIX=bcmc`、`SERVER_PORT=25565`、`BIND_ADDRESS=0.0.0.0`、`OFFLINE_ACCESS_ACK=true`、`JAVA_HEAP_GB=6`、`FOLIA_THREADS=6`です。`.env`を変更する場合は初回起動前に編集してください。

**オフラインモードにはアカウントの本人確認がありません。** TCP 25565をLAN・VPN・信頼できる接続元に制限してください。`OFFLINE_ACCESS_ACK=true`は認証やファイアウォールを設定しません。自分のPC内だけで使用するときは`BIND_ADDRESS=127.0.0.1`にします。既存のMinecraftサーバーが25565を使用中なら、そちらを先に停止してください。

## 2. 起動

Minecraft EULA（https://aka.ms/MinecraftEULA）を読み、自分で同意した場合だけ次を実行してください。

```bash
EULA=true ./start.sh
```

初回はネット接続が必要です。固定版Rust・Azalea・Gson・Foliaを取得し、Rustのテストとビルド、実際のFolia APIを使ったJavaブリッジのコンパイル、新しい専用ワールドと32区画の作成を自動で行います。途中で止めずにコンパイルログと起動ログを確認してください。ダウンロード・ビルドの失敗時は、原因を解消して同じコマンドを再実行できます。フォルダーの削除は不要です。

全区画と32体の行動開始を確認すると、次のメッセージが表示されます。

```text
All 32 enclosed training cells verified.
All 32 bots are online, spawned and making policy decisions.
```

これだけでは学習更新の証明にはなりません。別端末で`./status.sh`を実行し、`policy_version`と`trained_samples`が増えることを確認してください。停止しない限り、学習と評価を繰り返します。

## 3. Minecraftから見学

**Minecraft Java Edition 1.21.11**で`サーバーIP:25565`に接続します。DNSとルーターの転送を別途設定済みなら`lkjsxc.com:25565`も使用できます。このソフトはDNS・ポート転送の設定を行いません。

人間の参加者はスペクテイターになります。チャットで`/academy watch 0`〜`/academy watch 31`を実行すると各訓練区画を見学できます。Bot名は`bcmc00`〜`bcmc31`です。

## 4. 状態・停止・再開

起動端末は開いたままにし、別の端末で同じリポジトリに移動して操作します。

```bash
./status.sh
./console.sh "list"
cat academy/state/academy-status.json
./stop.sh
```

`stop.sh`は停止要求です。起動端末に`Clean shutdown: model, curriculum and world saved.`と表示され、プロセスが終了してからバックアップや再起動を行います。Ctrl+Cも保存後に停止します。

```bash
# 同じワールド・重み・最適化状態・進級記録から再開
./start.sh
```

初回の同意は`academy/server/eula.txt`に保存されるので、同じAcademyを再開する際に毎回`EULA=true`は不要です。ワールドは`academy/server/`、学習状態は`academy/state/`、ログは`academy/logs/`です。バックアップは停止後の`academy/`全体を保存します。初回起動後に人数を変えると所有マーカーと不一致になるため、勝手にリセットせず停止します。

古いインストールに上書きせず、新しいcloneを別フォルダーに作ってください。最初から学習するなら旧データをコピーする必要はありません。旧学習を継続するときは、停止・バックアップ後に互換性のある`academy/`全体をコピーし、人数を維持します。重みだけをコピーしないでください。

## 実装されている学習の範囲

Minecraft内で実行できるのは、前進・停止、旋回して到達・停止、視線合わせ、平面移動、1ブロック段差、指定原木の破壊の6段階です。操作はランダム初期化されたニューラル方策が選び、CPU上のPPOが学習します。経路探索・自動照準・模倣学習・LLM・操作の台本ではありません。目標座標などの数値情報と設計済み報酬を使用するため、映像だけで学ぶ方式でもありません。

各Botが現在段階を40試行した後、方策を固定して試験します。各Botに現段階16回中14回以上の成功、過去の各技能4回中3回以上の成功を要求します。時間が経つだけでは進級しません。起動・PPO更新の検証と、全段階習得・人間らしい動きの達成は別です。

**`experimental/rl-next/`の18課題定義は、すべてMinecraft本体に統合済みではありません。** クラフト・鉄精錬・建築・共同生活は、この実行版の学習段階ではありません。現在の検証範囲は`docs/VALIDATION.md`に記載します。

## 問題が起きた場合

```bash
./scripts/doctor.sh
./scripts/diagnostics.sh
```

初回ビルドのログは端末と`logs/`、起動後のログは`academy/logs/`です。診断アーカイブは`.env`・ワールド・重みを含めませんが、ログ中の名前・IP・パスは共有前に確認してください。詳しくは`docs/TROUBLESHOOTING.md`を参照してください。
