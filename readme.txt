シンプルなCPU（メモリアクセスが速いタイプ）の実装です。
使用方法、最新版は下記のサイトを参照してください。
http://cellspe.matrix.jp/zerofpga/sc1_cpu.html

* ターゲットボードについて

このプロジェクトは以下のFPGAボードに対応しています。
Terasic DE0-CV
BeMicro CV A9
TinyFPGA BX
以下のボードでは周辺I/OはLEDとUARTのみの対応です。
BeMicro Max 10
MAX10-FB基板（FPGA電子工作スーパーキット付録基板）
iCE40HX-8K(開発環境: IceStorm)

* （BeMicro Max 10の場合）I/O電圧のジャンパ設定について

BeMicro Max 10ではボードのI/O電圧を3.3Vに設定することを前提にしています。
BeMicro Max 10 Getting Started User Guideのp.5を参照してVCCIO選択ジャンパ (J1,J9)が3.3V側に設定されていることを確認してください。
https://www.arrow.com/en/products/bemicromax10/arrow-development-tools/
現在周辺I/OはLED、UARTのみ対応しています。
UARTは以下のように接続します。
PC RXD0 ---- BeMicro Max 10:GPIO_J4_38
PC TXD0 ---- BeMicro Max 10:GPIO_J4_40
PC GND  ---- BeMicro Max 10:GPIO_J4_34

* （BeMicro CV A9の場合）I/O電圧のジャンパ設定について

このプロジェクトはボードのI/O電圧を3.3Vに設定することを前提にしています。
BeMicro CV A9 Hardware Reference Guide
http://www.alterawiki.com/wiki/BeMicro_CV_A9#Documentation
のp.23を参照してVCCIO選択ジャンパ (J11)のpin 1とpin 2が接続されていることを確認してください。

* （MAX10-FB基板の場合）I/O電圧のジャンパ設定について

MAX10-FB基板ではボードのI/O電圧を3.3Vに設定することを前提にしています。
書籍の標準設定では3.3Vになっています。

* マルチコア実装のテストについて
** アップデートに伴い改修中です。（2017/11/27現在）
DE0-CV、BeMicro CV A9向けのマルチコア実装のテストです。詳しくは
sc1_cpu/patches/readme_patches.txt
を参照してください。

* SIMD命令実装のテストについて
** アップデートに伴い改修中です。（2017/11/27現在）
INT8 SIMD命令の実装のテストです。詳しくは
sc1_cpu/patches/readme_patches.txt
を参照してください。

* ビルド・実行方法

ここではFPGAのビットストリーム転送までを解説します。
SC1-CPU用のプログラムを動かすためには後述のUARTの設定が必要です。

** Terasic DE0-CV、BeMicro Max 10、BeMicro CV A9の場合
Quartus Primeは「AlteraのFPGA開発ツール「Quartus Prime」をUbuntuにインストールする」の方法でインストールしているものとします。
ターミナルで、

tar xzf sc1_cpu.tar.gz

Quartus Ver.15.0以上 でプロジェクトファイルを開いて「Start Compilation」、「Programmer」で転送して実行します。

プロジェクトファイル:
Terasic DE0-CV: sc1_cpu/de0-cv/de0-cv_start.qpf
BeMicro Max 10: sc1_cpu/bemicro_max10/bemicro_max10_start.qpf
BeMicro CV A9: sc1_cpu/bemicro_cva9/bemicro_cva9_start.qpf

** MAX10-FB基板 の場合
MAX10-JB基板は初期状態ではWindows環境でしか仕様できない設定になっているため、まず「FPGA電子工作スーパーキット・サポートサイト」に書かれている方法でアップデートしてLinuxに対応させる必要があります。（もしくは書き込み時のみWindowsを使います。）

Quartus Ver.15.0以上 でプロジェクトファイル sc1_cpu/max10fb/max10fb_start.qpfを開いて「Start Compilation」、「Programmer」で転送して実行します。

** iCE40HX-8K(開発環境: IceStorm)の場合
オープンソースの開発環境IceStormでコンパイル、転送します。
詳しくは sc1_cpu/ice40hx8k/readme_icestorm.txt を参照してください。

* Raspberry Pi、PCとの接続

Raspberry Pi、もしくはUSBシリアルケーブルを接続したPCからFPGAにUARTで接続して、プログラムの転送、実行を行えるようにしました。

このプロジェクトにおける各ボードごとのUARTピン配置
ボード	UART_TXD	UART_RXD	GND
Raspberry Pi	8番ピン	10番ピン	6番ピン
DE0-CV	GPIO-0の35番ピン	GPIO-0の37番ピン	GPIO-0の30番ピン
Bemicro Max10	GPIO_J4の35番ピン	GPIO_J4の37番ピン	GPIO_J4の33番ピン
Bemicro CVA9	GPIO_J4の35番ピン	GPIO_J4の37番ピン	GPIO_J4の33番ピン
Lattice iCE40HX-8K	D16ピン(J2-35番ピン)	C16ピン(J2-37番ピン)	GND(J2-39番ピン)
MAX10-FB基板	P140(CN2-8番ピン)	P141(CN2-9番ピン)	GND(CN2-20番ピン)
TinyFPGA BX	PIN_1	PIN_2	G

** Raspberry Pi 3の場合

以下のように接続します。TXDとRXDはクロス接続となっていることに注意してください。

RPi RXD0 ---- FPGA UART_TXD
RPi TXD0 ---- FPGA UART_RXD
RPi GND ---- FPGA GND

Raspberry PiのUART端子をRaspberry Pi側から外部デバイスに向けて使用できるように設定します。
ターミナルで、

sudo raspi-config

Interfacing Options: Serial: Would you like a login shell to be accessible over serial?: No

Would you like the serial port hardware to be enabled?: Yes

設定を保存、raspi-configを閉じて、

sudo rnano /boot/config.txt

以下の設定をファイル末尾に追加して保存します。
dtoverlay=pi3-miniuart-bt

SC1-CPUのプログラム転送ツールなどでデバイス名の指定を省略できるようにする設定です。

rnano ~/.bashrc

ファイル末尾に追加
export UART_DEVICE=/dev/ttyAMA0

sudo reboot


** PCの場合

PCに接続する場合、USBシリアルケーブルが別途必要です。（FTDI TTL-232R-3V3など。必ずTTL 3.3V 仕様のものを使ってください。電圧が異なるものを使うと最悪FPGAが壊れます。）
FTDI TTL-232R-3V3 にもVCC 5Vのピンが1本あるので、これを間違えて接続しないよう注意してください。

これを以下のように接続します。
TXDとRXDはクロス接続となっていることに注意してください。

シリアルケーブル RXD ---- FPGA UART_TXD
シリアルケーブル TXD ---- FPGA UART_RXD
シリアルケーブル GND ---- FPGA GND

FTDI TTL-232R-3V3の場合、以下のようにudevのパーミッションを設定します。他機種の場合は、idVendor、idProductを読み替えてください。（USBで接続してからlsusbコマンドを打つと調べられます。ID idVendor:idProductの順です。）

sudo rnano /etc/udev/rules.d/99-ft232.rules
KERNEL=="ttyUSB*", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", GROUP="plugdev", MODE="0666"
sudo udevadm control --reload-rules

rnano ~/.bashrc

ファイル末尾に追加
export UART_DEVICE=/dev/ttyUSB0

* その他のI/Oの接続

DE0-CV版では、音声出力インターフェイスを接続します。「DE0-CV向けオーディオ・アダプタの製作」と同じものです。DE0-CVのGPIOとの接続は単線のジャンパーワイヤで行います。
BeMicro CV A9版では、「映像・音声・コントローラー・インターフェースの製作」の拡張ボードを接続します。

* UART経由でのプログラムの転送、実行

上記のように設定したRaspberry PiまたはPCで、

cd sc1_cpu

make run

これでツールのコンパイル、プログラムのコンパイル、転送、実行が行われます。
デフォルトではLEDが点滅するサンプル・プログラムが起動するようになっています。

* このCPUでプログラミングする方法

sc1_cpu/asm 以下にJava上で動作する簡易アセンブラが入っています。
実行にはOpenJDK 8.0以上のインストールが必要です。
AsmLibクラスを継承したクラスを作り、init()で初期化設定、program()にプログラム、data()にデータを記述します。AsmTop.javaも修正します。
sc1_cpuディレクトリに移動して make を実行するとプログラム・バイナリが出力されます。
PCとFPGAをUARTで接続し、 make run を実行するとバイナリがFPGAに転送されて実行されます。
詳しくはExamples.javaのソースコードを参照してください。
SC1-SoCからのUART出力を使用するプログラムでは、あらかじめ別のターミナル上で、
cd sc1_cpu/tools
./reciever
を実行してください。

* SC1-SOC 仕様

現在の仕様では、外部I/OへのアクセスはDestination, Source_A のみ接続しています。
そのため、b_addr にローカルメモリ以外の外部デバイスのアドレスを指定しても値を読むことはできません。Source_Bに外部デバイスから読んだ値を入れたい場合は、一旦Source_Aを使って汎用レジスタに読み込んでから使用してください。

* 命令セット・アーキテクチャ (2017/11/17)

特徴:
以下のようなロードx2、演算、ストア、アドレス・インクリメントx3を含む操作を1サイクルで連続して実行することができる。
mem[d_addr] = mem[a_addr] (演算x) mem[b_addr]; d_addr += Rx; a_addr += Ry; b_addr += Rz; (1 cycle)

31-------------------------------------0
add,sub,and,or,xor,not,sr,sl,sra,mul,halt,nop,mv,ceq,cgt,cgta,ba,bc,bl,loop
operand_d:6 operand_a:6 operand_b:6 none:1 am_d:2 am_a:2 am_b:2 op:7

mvi,mvih
im:16 none:9 op:7

op (7bit): オペコード

* オペランド

operand_d (6bit): オペランドD
operand_a (6bit): オペランドA
operand_b (6bit): オペランドB

im (16bit): 即値

am_d (2bit): オペランドDのアドレッシング・モード
am_a (2bit): オペランドAのアドレッシング・モード
am_b (2bit): オペランドBのアドレッシング・モード

* アドレッシング・モード

（am:アドレッシング・モード, o:オペランド, addr:命令実行時に使用されるアドレス, addr_s:内部的に保存されるアドレス）

am:0
対象はレジスタ。
オペランドの最上位ビットが0の時、オペランドを4bitのレジスタ番号とみなして処理する。
operand = reg[o]
オペランドA,Bにおいて、オペランドの最上位ビットが1の時、オペランドを5bit Signedの即値として処理する。
operand = o

am:1
対象はメモリ。アドレスに指定レジスタの値を代入して命令を実行。そのアドレスは内部的に保存される。
operand = mem[addr]; addr = reg[o];          addr_s = reg[o];

am:2
対象はメモリ。保存されているアドレスを使用して命令を実行し、実行後に指定レジスタの値をアドレスに加算して保存。（post increment）
operand = mem[addr]; addr = addr_s;          addr_s = addr_s + reg[o];

am:3
対象はメモリ。アドレスに即値を一時的に加算（オフセット値）して命令を実行。保存されているアドレスは変化しない。
operand = mem[addr]; addr = addr_s + o;      addr_s = addr_s;

* レジスタ

専用レジスタ:

PC: プログラムカウンタ
d_addr: オペランドDのメモリのアドレス・レジスタ
a_addr: オペランドAのメモリのアドレス・レジスタ
b_addr: オペランドBのメモリのアドレス・レジスタ

汎用レジスタ:

R0 〜 R15
（R0 〜 R5は特定命令で使用される）
R0: MVI, BA
R1: CEQ, CGT, CGTA
R2: BL
R3~5: LOOP

* 命令配置制約

** 分岐命令(BC,BL,BA)、HALTの後の3命令は遅延スロットとなる。分岐命令の後3ステップの命令はそのまま実行され、4サイクル後に分岐先の命令が実行される。条件分岐の有無に関わらず遅延スロットの命令は実行される。混乱を避けるため通常は遅延スロットにはNOPを置く場合が多い。
例：
bc(); // if (R1 != 0) PC += R0
nop(0,0,0,0,0,0);
nop(0,0,0,0,0,0);
nop(0,0,0,0,0,0); // この命令まで実行されてから分岐先に飛ぶ

** アドレッシング・モードがSet(1)もしくはIncrement(2)の命令で参照されるレジスタを操作する命令は、その命令の3サイクル以上前に置かなければいけない。
例：
mvi(16); // MVIレジスタR0に値16を代入
nop(0,0,0,0,0,0); // 2サイクル待つ
nop(0,0,0,0,0,0);
nop(0,0,0, 1,1,1); // d_addr, a_addr, b_addrにR0の値を代入

** 書き込み先がメモリの場合、書き換え後の値が読み込み可能となるのは3サイクル後である。ゆえにデータ依存性のある操作を連続して行う場合、2サイクル以上他の命令を挟まなければならない。
例：
R6=1; d_addr=8; a_addr=8; であるとして、
add(0,0,6, 3,3,0); // mem[8] = mem[8] + 1;
nop(0,0,0,0,0,0); // 2サイクル待つ
nop(0,0,0,0,0,0); // ここにはNOP以外に無関係な他の命令を入れても良い
add(0,0,6, 3,3,0); // mem[8] = mem[8] + 1;

** 無条件のMV命令はないのでADDで0を加算する操作等で代用する。
例：
mvi(0); // r0 = 0
add(8,0,0, 0,0,0); // r8 = r0 + r0 = 0
mvi(1); // r0 = 1
add(9,0,8, 0,0,0); // r9 = r0 + r8 = 1

** LOOP命令のループ終了位置オフセットはLOOP命令の4サイクル後以降に設定しなければいけない。
例：
mvi(0); // r0 = 0
add(8,0,0, 0,0,0); // r8 = r0 + r0 = 0
mvi(7); // r0 = 7
add(3,0,8, 0,0,0); // r3 = r0 + r8 = 7 : ループ回数 = 7 + 1
mvi(3); // r0 = 4
add(4,0,8, 0,0,0); // r4 = r0 + r8 = 4 : ループ終了位置はLOOP命令の4サイクル後
add(5,8,8, 0,0,0); // r5 = r8 + r8 = 0 : ループ終了位置からのジャンプ先オフセットは0 （同じ命令を繰り返す）
loop(); // ループ実行（予約）
nop(0,0,0,0,0,0); // ウェイト（他の無関係な命令を置いても良い）
nop(0,0,0,0,0,0);
nop(0,0,0,0,0,0);
add(9,9,9, 2,2,2); // mem[d]=mem[a]+mem[b]; d+=r9; a+=r9; b+=r9; // この命令が8回繰り返される

** LOOP命令を使ったループの中の最後の命令が遅延スロット内の命令であってはいけない。（最後がBL命令+3xNOPの場合など）その後にNOPかその他の命令を1つ配置すること。

** LOOP命令はネスト不可だが通常の条件分岐を使ったループの中にLOOP命令のループを置いたり、LOOP命令のループの中に通常の条件分岐を使ったループを置くことは可能。

** 遅延スロットに置かれた命令は複数回実行される場合がある。（ループの終了地点で使われた場合など）遅延スロットを利用する場合はそのことを考慮する必要がある。


* Application Binary Interface (ABI)

** スタック・ポインター

R15はスタック・ポインター格納用に予約する。
プログラムは開始時にデータメモリの最終アドレスをスタック・ポインターにセットしなければならない。
スタックは最終アドレスから開始アドレスに向かって成長させる。
関数はスタック・ポインターを呼び出し時点の値に復元してからリターンしなければならない。

** リンク・レジスターの保存

関数は入り口でリンク・レジスター（R2）の値を保存し、リターンする前にこれを復元しなければならない。リンク・レジスターはBL命令実行時に変更される。

** レジスター割り当て

R6からR9までは関数の引数や戻り値として使用する。順序は引数リストの左から、番号の若い順に割り当てる。揮発性（復元責任は呼び出し側）。

R10からR11は揮発性の汎用レジスター。

R12からR14は非揮発性（呼び出された側が復元責任を持つ）の汎用レジスター。


* 各命令の解説

** ADD

解説：加算

アセンブリ：add(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A + B;


** SUB

解説：減算

アセンブリ：sub(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A - B;


** AND

解説：論理AND

アセンブリ：and(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A & B;


** OR

解説：論理OR

アセンブリ：or(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A | B;


** XOR

解説：論理XOR

アセンブリ：xor(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A ^ B;


** NOT

解説：論理NOT

アセンブリ：not(reg_d, reg_a, am_d, am_a)

機能：D = ~A;


** SR

解説：Shift Right。論理右シフト

アセンブリ：sr(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A >> B;


** SL

解説：Shift Left。論理左シフト

アセンブリ：sl(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A << B;


** SRA

解説：Shift Right Arithmetic。算術右シフト

アセンブリ：sra(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A >>> B;


** MUL

解説：乗算

アセンブリ：mul(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：D = A * B;


** HALT

解説：プログラム実行停止。Resume信号で再開される。

アセンブリ：halt()

機能：if (Resume signal) {PC = PC + 1;}
else {PC = PC;}


** NOP

解説：何もしない。ただし、アドレッシング・モードに0以外を指定すればアドレス操作だけを行うことが可能。アドレス操作も行わない場合はam_d, am_a, am_bを0にしなければいけない。

アセンブリ：nop(reg_d, reg_a, reg_b, am_d, am_a, am_b)

機能：None


** MV

解説：Move。もし(R1 != 0)ならばDにAを代入。

アセンブリ：mv(reg_d, reg_a, am_d, am_a)

機能：if (R1 != 0) {D = A;}


** MVI

解説：Move Immediate。16bit即値をR0に代入。上位16bitは0

アセンブリ：mvi(im)

機能：R0 = im


** MVIH

解説：Move Immediate High。16bit即値をR0の上位16bitに代入。

アセンブリ：mvih(im)

機能：R0 = (R0 & 0xffff) | (im << 16)


** CEQ

解説：Compare Equal。もし(A == B)ならばR1 = 0xffffffff、それ以外ならR1 = 0

アセンブリ：ceq(reg_a, reg_b, am_a, am_b)

機能：if (A == B) {R1 = 0xffffffff;} else {R1 = 0;}


** CGT

解説：Compare Greater Than。もし(A > B)ならばR1 = 0xffffffff、それ以外ならR1 = 0

アセンブリ：cgt(reg_a, reg_b, am_a, am_b)

機能：if (A > B) {R1 = 0xffffffff;} else {R1 = 0;}


** CGTA

解説：Compare Greater Than Arithmetic。A、Bをsignedとして扱い、もし(A > B)ならばR1 = 0xffffffff、それ以外ならR1 = 0

アセンブリ：cgta(reg_a, reg_b, am_a, am_b)

機能：if (A(signed) > B(signed)) {R1 = 0xffffffff;} else {R1 = 0;}


** BC

解説：Branch Conditional。もし(R1 != 0)なら現在のPCにR0を加算した命令アドレスに分岐する。それ以外なら次の命令に進む。

アセンブリ：bc()

機能：if (R1 != 0) {PC += R0;}


** BL

解説：Branch and Link。現在のPC+ディレイ・スロット(3)+1をR2にコピーし、現在のPCにR0を加算した命令アドレスに分岐する。このリンク・アドレスを用いてリターン(BA)した場合、この命令のディレイ・スロットを飛び越してその次の命令に戻ってくる。

アセンブリ：bl()

機能：{R2 = PC + 1 + DelaySlot(3); PC += R0;}


** BA

解説：Branch Absolute。R0の値の命令アドレスにジャンプする。

アセンブリ：ba()

機能：PC = R0


** LOOP

解説：ループ命令。指定範囲の命令を指定回数繰り返し実行する。
ループ回数：(R3 + 1)
ループ終了位置オフセット：R4 (この命令のPC + R4の命令を実行後にジャンプする)
ジャンプ先オフセット：R5（ループ終了位置のPC + R5にジャンプ）
R4 >= 4, R5 <= 0 でなければならない。

アセンブリ：loop()

機能：R3, R4, R5を内部レジスタにコピーし、ループ実行を予約する。
