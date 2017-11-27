* iCE40HX-8K(開発環境: IceStorm)版について
iCE40HX-8K版では現時点で周辺I/OはLEDとUARTのみ対応しています。

* UARTの接続
PCのUSBシリアルケーブル(3.3V仕様)やRaspberry Piを以下のピンに接続します。
iCE40:D16ピン(J2-35番ピン,TXD) --- PCシリアルケーブル:RXD
iCE40:C16ピン(J2-37番ピン,RXD) --- PCシリアルケーブル:TXD
iCE40:GND(J2-39番ピン) --- PCシリアルケーブル:GND

* 実行方法
ターゲットボードはLattice iCE40HX-8K Breakout Board、開発環境はIceStorm用です。

Project IceStorm
http://www.clifford.at/icestorm/

IceStormのRaspberry Piへのインストール例
http://cellspe.matrix.jp/zerofpga/sjr_ice.html

まずiCE40HX-8Kのプログラミングモード設定を確認してください。
ボード上のジャンパ設定でSPI Flashに書き込むかCRAMに書き込むかを選択できます。

ターミナルで、

cd sc1_cpu/ice40hx8k

make

FPGAボードに書き込みます。（モードを間違えないように気をつけてください）

（CRAMに書き込む設定の場合）
make prog-ram

（SPI Flashに書き込む設定の場合）
make prog
