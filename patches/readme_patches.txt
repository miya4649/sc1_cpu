* multicore_test.patch

複数のsc1_cpuコアを実装するテストです。DE0-CVとBemicroCV A9に対応しています。
DE0-CV：10コア実装して各コアに一つずつLEDを接続します。
BemicroCV A9：80コア実装して各コアの出力全てのxorを取った値で一つのLEDを点滅させます。
各コアのプログラムの入力に初期値を与えて周期を微妙に変化させています。

** パッチの当て方

cd sc1_cpu

patch -p1 < patches/multicore_test.patch

パッチを当ててから各プロジェクトを開いて合成します。

通常版に戻すには、

patch -Rp1 < patches/multicore_test.patch

* icestorm.patch

iCE40HX-8K(開発環境: IceStorm)版のパッチです。これは通常版とは仕様が異なる特別バージョンです。詳しくは
sc1_cpu/ice40hx8k/readme.txt
を参照してください。
