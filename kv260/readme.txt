# AMD (Xilinx) Kria KV260, KR260 用プロジェクト

以下の説明はKV260に特化した部分のみの説明です。その他の説明はこの階層のひとつ上のディレクトリのreadme.txtを参照してください。

## 周辺回路の準備

ボードのPMOD J2ポートに以下のように抵抗とLEDを接続します。

(ピン番号はボード上、回路図上に記載されている番号とします。)

PMOD_J2_Pin1 --- 抵抗330Ω --- (+)LED(-) --- PMOD_J2_Pin9(GND)

RaspberryPiやFTDI TTL-232R-3V3など、3.3VのUARTインターフェースを使用できるデバイスを用意し、以下のように接続します。

PMOD_J2_Pin3 --- UART RX
PMOD_J2_Pin5 --- UART TX
PMOD_J2_Pin9 --- UART GND

ボードとホストPCをUSBケーブルで接続し、電源を入れます。

## 使用方法

デフォルトではKV260用のプロジェクトが生成されます。KR260の場合は、vivado.tclの「set board_type kv260」を「set board_type kr260」に書き換えます。

Linuxのターミナル(bash)で

$ source Vitisのインストールパス/settings64.sh

(例: $ source /opt/Xilinx/Vitis/2023.2/settings64.sh )

$ cd (解凍パス)/sc1_cpu/kv260

$ make

(sc1_cpuのアセンブラの実行、Vivadoでのビットストリーム生成、Vitisプロジェクトのビルドが連続して行われます。)

Vitis Unified IDEを起動し、

File: Open Workspaceで

(このディレクトリ)/vitis_workspace を選択してOK

View: Flow を選択して表示、

FLOW: Component で Project_1_appを選択、

FLOW: Run で実行します。

ここからの操作はこのディレクトリの一つ上のreadme.txtを参照してください。

例えばシンセサイザーのデモを起動するには以下のようにします。

UARTで接続しているマシンで、

$ export UART_DEVICE=/dev/ttyUSB0
（デバイス名は状況により変化する場合があります）

$ cd (解凍パス)/sc1_cpu/tools

$ ./launcher ../synth_code.bin ../synth_data.bin

## ライセンスについて

vitis_src以下のファイルはAMDのMITライセンスのプログラムです。(一部miyaが改造)
このディレクトリのpins.xdc, timings.xdc, vitisnew.py, vivado-run.tcl, vivado.tclはパブリックドメインとします。
その他のコードは2条項BSDライセンスとします。
