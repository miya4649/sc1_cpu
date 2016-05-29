シンプルなCPU（メモリアクセスが速いタイプ）の実装です。
使用方法は下記のサイトを参照してください。

http://cellspe.matrix.jp/zerofpga/sc1_cpu.html


* ターゲットボードについて

このプロジェクトは以下のFPGA開発ボードに対応しています。
BeMicro Max 10
BeMicro CV A9
Terasic DE0-CV
MAX10-FB基板（CQ出版社「FPGA電子工作スーパーキット」付録基板）
Lattice iCE40HX-8K Breakout Board (開発環境: IceStorm)

* （BeMicro Max 10の場合）I/O電圧のジャンパ設定について

BeMicro Max 10ではボードのI/O電圧を3.3Vに設定することを前提にしています。
BeMicro Max 10 Getting Started User Guideのp.5を参照してVCCIO選択ジャンパ (J1,J9)が3.3V側に設定されていることを確認してください。
https://www.arrow.com/en/products/bemicromax10/arrow-development-tools/

* （BeMicro CV A9の場合）I/O電圧のジャンパ設定について

このプロジェクトはボードのI/O電圧を3.3Vに設定することを前提にしています。
BeMicro CV A9 Hardware Reference Guide
http://www.alterawiki.com/wiki/BeMicro_CV_A9#Documentation
のp.23を参照してVCCIO選択ジャンパ (J11)のpin 1とpin 2が接続されていることを確認してください。

* （MAX10-FB基板の場合）I/O電圧のジャンパ設定について

MAX10-FB基板ではボードのI/O電圧を3.3Vに設定することを前提にしています。
書籍の標準設定では3.3Vになっています。

* iCE40HX-8K用のプロジェクトについて
iCE40HX-8K(開発環境: IceStorm)版は通常版とは仕様が異なる特別バージョンです。詳しくは
sc1_cpu/sc1_cpu/ice40hx8k/readme.txt
を参照してください。

* ビルド・実行方法

Quartus II Ver.15.0以上 でプロジェクトファイルを開いて「Start Compilation」、「Programmer」で転送して実行します。
プロジェクトファイル:
BeMicro Max 10: sc1_cpu/bemicro_max10/bemicro_max10_start.qpf
BeMicro CV A9:  sc1_cpu/bemicro_cva9/bemicro_cva9_start.qpf
Terasic DE0-CV: sc1_cpu/de0-cv/de0-cv_start.qpf
MAX10-FB基板: sc1_cpu/max10fb/max10fb_start.qpf

テスト用にカウンターのプログラムが入っています。実行するとLEDが光ります。

* このCPUでプログラミングする方法

sc1_cpu/asm 以下に簡易アセンブラが入っています。
Program.java にアセンブリを記述し、 sc1_cpu/asm に cd して ./run.sh を実行すると一つ上のディレクトリにバイナリ（rom.v）が出力されます。
（Windowsではrun.batを実行）
これがCPUの起動時に読み込まれて実行されます。
実行にはJDK 8.0以上のインストールが必要です。


* 命令セット・アーキテクチャ

特徴:
以下のようなロードx2、演算、ストア、アドレス・インクリメントx3を含む操作を1サイクルで連続して実行することができる。
mem[d_addr] = mem[a_addr] (演算x) mem[b_addr]; d_addr += Rx; a_addr += Ry; b_addr += Rz; (1 cycle)

31-------------------------------------0
add,sub,and,or,xor,not,sr,sl,sra,mul,halt,nop,mv,ceq,cgt,cgta,ba,in,out
reg_d:6 reg_a:6 reg_b:6 none:1 inc_flag:3 mem_flag:3 op:7

mvi,mvih
none:6 im:16 none:3 op:7

bc,bl
none:6 ims:16 none:3 op:7

op: オペコード

reg_d (4/6bit): オペランドDのレジスタ番号
reg_a (4/6bit): オペランドAのレジスタ番号
reg_b (4/6bit): オペランドBのレジスタ番号

im (16bit): 即値
ims (16bit signed): 即値(signed)

mem_d (1bit): オペランドDは レジスタ(0) or メモリ(1)
mem_a (1bit): オペランドAは レジスタ(0) or メモリ(1)
mem_b (1bit): オペランドBは レジスタ(0) or メモリ(1)

inc_d (1bit): d_addrにこのレジスタの値を 代入する(0) or 加算する(1)
inc_a (1bit): a_addrにこのレジスタの値を 代入する(0) or 加算する(1)
inc_b (1bit): b_addrにこのレジスタの値を 代入する(0) or 加算する(1)

mem_x=0の時はオペランドのレジスタの値がそのまま使われる。inc_xは無視される。
mem_x=1, inc_x=0の時はオペランドのレジスタの値がx_addrに代入される。
mem_x=1, inc_x=1の時はオペランドのレジスタの値（負の値を使用可能）がx_addrに加算される。
mem_x=1の場合のアドレスへの代入、加算は演算実行後に有効になる。新しいアドレスの値は次の命令以降で使われる。


* レジスタ

専用レジスタ:

PC: プログラムカウンタ
d_addr: オペランドDのメモリのアドレス
a_addr: オペランドAのメモリのアドレス
b_addr: オペランドBのメモリのアドレス

汎用レジスタ:

R0 〜 R15
（R0 〜 R5は特定命令で使用される）

* 命令配置制約

** 分岐命令(BC,BL,BA)、HALTの後の2命令は遅延スロットとなる。分岐命令の後2ステップの命令はそのまま実行され、3サイクル後に分岐先の命令が実行される。条件分岐の有無に関わらず遅延スロットの命令は実行される。混乱を避けるため通常は遅延スロットにはNOPを置く場合が多い。
例：
bc(-2); // if (R1 != 0) PC += -2
nop(0,0,0,0,0,0,0,0,0);
nop(0,0,0,0,0,0,0,0,0); // この命令まで実行されてから分岐先に飛ぶ

** インストラクションメモリのアドレス0の位置にはNOPを1つ置かなければいけない。アドレス0からプログラム実行開始後、NOPが3回実行される。

** mem_flag=1の命令で参照されるレジスタを操作する命令は、その命令の2サイクル以上前に置かなければいけない。
例：
mvi(16); // MVIレジスタR0に値16を代入
nop(0,0,0,0,0,0,0,0,0); // 1サイクル待つ
nop(0,0,0, 0,0,0, 1,1,1); // d_addr, a_addr, b_addrにR0の値を代入

** 書き込み先がメモリの場合、書き換え後の値が読み込み可能となるのは3サイクル後である。ゆえにデータ依存性のある操作を連続して行う場合、2サイクル以上他の命令を挟まなければならない。
例：
R3=0; R4=1; d_addr=8; a_addr=8; であるとして、
add(3,3,4, 1,1,0, 1,1,0); // mem[8] = mem[8] + 1;
nop(0,0,0,0,0,0,0,0,0); // 2サイクル待つ
nop(0,0,0,0,0,0,0,0,0); // ここにはNOP以外に無関係な他の命令を入れても良い
add(3,3,4, 1,1,0, 1,1,0); // mem[8] = mem[8] + 1;

** 無条件のMV命令はないのでADDで0を加算する操作等で代用する。
例：
mvi(0); // r0 = 0
add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
mvi(1); // r0 = 1
add(9,0,8, 0,0,0, 0,0,0); // r9 = r0 + r8 = 1

** LOOP命令のループ終了位置オフセットはLOOP命令の3サイクル後以降に設定しなければいけない。
例：
mvi(0); // r0 = 0
add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
mvi(7); // r0 = 7
add(3,0,8, 0,0,0, 0,0,0); // r3 = r0 + r8 = 7 : ループ回数 = 7 + 1
mvi(3); // r0 = 3
add(4,0,8, 0,0,0, 0,0,0); // r4 = r0 + r8 = 3 : ループ終了位置はLOOP命令の3サイクル後
add(5,8,8, 0,0,0, 0,0,0); // r5 = r8 + r8 = 0 : ループ終了位置からのジャンプ先オフセットは0 （同じ命令を繰り返す）
loop(); // ループ実行（予約）
nop(0,0,0,0,0,0,0,0,0); // ウェイト（他の無関係な命令を置いても良い）
nop(0,0,0,0,0,0,0,0,0);
add(9,9,9, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r9; a+=r9; b+=r9; // この命令が8回繰り返される

** LOOP命令はネスト不可だが通常の条件分岐を使ったループの中にLOOP命令のループを置いたり、LOOP命令のループの中に通常の条件分岐を使ったループを置くことは可能。



各命令の解説:

ADD

解説: 加算

アセンブリ: add(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A + B;


SUB

解説: 減算

アセンブリ: sub(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A - B;


AND

解説: 論理AND

アセンブリ: and(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A & B;


OR

解説: 論理OR

アセンブリ: or(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A | B;


XOR

解説: 論理XOR

アセンブリ: xor(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A ^ B;


NOT

解説: 論理NOT

アセンブリ: not(reg_d, reg_a, inc_d, inc_a, mem_d, mem_a)

機能: D = ~A;


SR

解説: Shift Right。論理右シフト

アセンブリ: sr(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A >> B;


SL

解説: Shift Left。論理左シフト

アセンブリ: sl(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A << B;


SRA

解説: Shift Right Arithmetic。算術右シフト

アセンブリ: sra(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A >>> B;


MUL

解説: 乗算

アセンブリ: mul(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D = A * B;


HALT

解説: プログラム実行停止

アセンブリ: halt()

機能: PC = PC;


NOP

解説: 何もしない。ただし、オペランドを使用すればアドレス操作だけを行うことが可能。アドレス操作も行わない場合はmem_d, mem_a, mem_bを0にしなければいけない

アセンブリ: nop(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: None


MV

解説: Move。もし(R1 != 0)ならばDにAを代入

アセンブリ: mv(reg_d, reg_a, inc_d, inc_a, mem_d, mem_a)

機能: if (R1 != 0) {D = A;}


MVI

解説: Move Immediate。16bit即値をR0に代入。上位16bitは0

アセンブリ: mvi(im)

機能: R0 = im


MVIH

解説: Move Immediate High。16bit即値をR0の上位16bitに代入。

アセンブリ: mvih(im)

機能: R0 = (R0 & 0xffff) | (im << 16)


CEQ

解説: Compare Equal。もし(A == B)ならばR1 = 0xffffffff、それ以外ならR1 = 0

アセンブリ: ceq(reg_a, reg_b, inc_a, inc_b, mem_a, mem_b)

機能: if (A == B) {R1 = 0xffffffff;} else {R1 = 0;}


CGT

解説: Compare Greater Than。もし(A > B)ならばR1 = 0xffffffff、それ以外ならR1 = 0

アセンブリ: cgt(reg_a, reg_b, inc_a, inc_b, mem_a, mem_b)

機能: if (A > B) {R1 = 0xffffffff;} else {R1 = 0;}


CGTA

解説: Compare Greater Than Arithmetic。A、Bをsignedとして扱い、もし(A > B)ならばR1 = 0xffffffff、それ以外ならR1 = 0

アセンブリ: cgta(reg_a, reg_b, inc_a, inc_b, mem_a, mem_b)

機能: if (A(signed) > B(signed)) {R1 = 0xffffffff;} else {R1 = 0;}


BC

解説: Branch Conditional。もし(R1 != 0)なら現在のPCに即値を加算した命令アドレスに分岐する。それ以外なら次の命令に進む。

アセンブリ: bc(ims)

機能: if (R1 != 0) {PC += ims;}


BL

解説: Branch and Link。現在のPC+1をR2にコピーし、現在のPCに即値を加算した命令アドレスに分岐する。

アセンブリ: bl(ims)

機能: {R2 = PC + 1; PC += ims;}


BA

解説: Branch Absolute。R0の値の命令アドレスにジャンプする。

アセンブリ: ba()

機能: PC = R0


LOOP

解説: ループ命令。指定範囲の命令を指定回数繰り返し実行する。
ループ回数：(R3 + 1)
ループ終了位置オフセット：R4 (この命令のPC + R4の命令を実行後にジャンプする)
ジャンプ先オフセット：R5（ループ終了位置のPC + R5にジャンプ）
R4 >= 3, R5 <= 0 でなければならない。

アセンブリ: loop()

機能: R3, R4, R5を内部レジスタにコピーし、ループ実行を予約する。


IN

解説: 入力ポートの値をDにコピーする

アセンブリ: in(reg_d, inc_d, mem_d)

機能: D = Port_IN


OUT

解説: Aの値を出力ポートに出力する

アセンブリ: out(reg_a, inc_a, mem_a)

機能: Port_OUT = A
