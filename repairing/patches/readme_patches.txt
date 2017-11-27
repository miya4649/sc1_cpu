Under Maintenance

* simd.patch

8bit整数のSIMD命令のテストです。
v8mv, v8ceq, v8cgt, v8cgta, v8add, v8sub, v8mul, v8sr, v8sl, v8sra が追加されます。
レジスタを8bitに区切り、各要素ごとに演算を行います。
sc1_cpuをWIDTH_D = 32, WIDTH_REG = 32でインスタンス化した場合は32bit / 8bit = 4要素を同時に処理できます。
256bitでインスタンス化した場合は32要素を同時に処理できます。（スカラ演算は256bit整数になります。）

SIMD命令一覧：

v8add
v8sub
v8mul
v8sr
v8sl
v8sra
v8mv
v8ceq
v8cgt
v8cgta


SIMD命令仕様：

※以下、[element] はソース、格納先のレジスタ、メモリを8bit区切りにした要素それぞれに対しての演算を表す。


V8ADD

解説: Vector 8bit Add : 8bit SIMD 加算

アセンブリ: v8add(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D[element] = A[element] + B[element];


V8SUB

解説: Vector 8bit Subtract : 8bit SIMD 減算

アセンブリ: v8sub(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D[element] = A[element] - B[element];


V8MUL

解説: Vector 8bit Multiply : 8bit SIMD 乗算

アセンブリ: v8mul(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D[element] = A[element] * B[element];


V8SR

解説: Vector 8bit Shift Right : 8bit SIMD 論理右シフト

アセンブリ: v8sr(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D[element] = A[element] >> B;


V8SL

解説: Vector 8bit Shift Left : 8bit SIMD 論理左シフト

アセンブリ: v8sl(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D[element] = A[element] << B;


V8SRA

解説: Vector 8bit Shift Right Arithmetic : 8bit SIMD 算術右シフト

アセンブリ: v8sra(reg_d, reg_a, reg_b, inc_d, inc_a, inc_b, mem_d, mem_a, mem_b)

機能: D[element] = A[element] >>> B;


V8MV

解説: Vector 8bit Move : もし(R1[element] != 0)ならばD[element]にA[element]を代入

アセンブリ: v8mv(reg_d, reg_a, inc_d, inc_a, mem_d, mem_a)

機能: if (R1[element] != 0) {D[element] = A[element];}


V8CEQ

解説: Vector 8bit Compare Equal : もし(A[element] == B[element])ならばR1[element] = 0xff、それ以外ならR1[element] = 0

アセンブリ: v8ceq(reg_a, reg_b, inc_a, inc_b, mem_a, mem_b)

機能: if (A[element] == B[element]) {R1[element] = 0xff;} else {R1[element] = 0;}


V8CGT

解説: Vector 8bit Compare Greater Than : もし(A[element] > B[element])ならばR1[element] = 0xff、それ以外ならR1[element] = 0

アセンブリ: v8cgt(reg_a, reg_b, inc_a, inc_b, mem_a, mem_b)

機能: if (A[element] > B[element]) {R1[element] = 0xff;} else {R1[element] = 0;}


V8CGTA

解説: Vector 8bit Compare Greater Than Arithmetic : A[element]、B[element]をsignedとして扱い、もし(A[element] > B[element])ならばR1[element] = 0xff、それ以外ならR1[element] = 0

アセンブリ: v8cgta(reg_a, reg_b, inc_a, inc_b, mem_a, mem_b)

機能: if (A[element](signed) > B[element](signed)) {R1[element] = 0xff;} else {R1[element] = 0;}


** パッチの当て方

cd sc1_cpu

patch -p1 < patches/simd.patch

パッチを当ててから各プロジェクトを開いて合成します。

通常版に戻すには、

patch -Rp1 < patches/simd.patch


* multicore_test.patch

複数のsc1_cpuコアを実装するテストです。DE0-CVとBemicroCV A9に対応しています。
DE0-CV：10コア実装して各コアに一つずつLEDを接続します。
BemicroCV A9：80コア実装して各コアの出力全てのxorを取った値で一つのLEDを点滅させます。
各コアのプログラムの入力に初期値を与えて周期を微妙に変化させています。

** パッチの当て方

cd sc1_cpu

patch -p1 < patches/multicore_test.patch

パッチを当ててから各プロジェクトを開いて合成します。


* icestorm.patch

iCE40HX-8K(開発環境: IceStorm)版のパッチです。これは通常版とは仕様が異なる特別バージョンです。詳しくは
sc1_cpu/ice40hx8k/readme.txt
を参照してください。
