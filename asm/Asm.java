/*
  Copyright (c) 2015-2016, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class Asm
{
  private int address;
  // オペコード
  // special type
  private static final int I_HALT = 0x00;
  private static final int I_NOP  = 0x01;
  private static final int I_MV   = 0x02;
  private static final int I_MVI  = 0x03;
  private static final int I_MVIH = 0x04;
  private static final int I_CEQ  = 0x05;
  private static final int I_CGT  = 0x06;
  private static final int I_CGTA = 0x07;
  private static final int I_BC   = 0x08;
  private static final int I_BL   = 0x09;
  private static final int I_BA   = 0x0a;
  private static final int I_LOOP = 0x0b;
  private static final int I_OUT  = 0x0c;
  // normal type
  private static final int I_ADD  = 0x40;
  private static final int I_SUB  = 0x41;
  private static final int I_AND  = 0x42;
  private static final int I_OR   = 0x43;
  private static final int I_XOR  = 0x44;
  private static final int I_NOT  = 0x45;
  private static final int I_SR   = 0x46;
  private static final int I_SL   = 0x47;
  private static final int I_SRA  = 0x48;
  private static final int I_MUL  = 0x49;
  private static final int I_IN   = 0x4a;


  private static final String header = "module rom\n"
    + "  (\n"
    + "   input             clk,\n"
    + "   input [7:0]       addr,\n"
    + "   output reg [31:0] data_out\n"
    + "   );\n"
    + "\n"
    + "  always @(posedge clk)\n"
    + "    begin\n"
    + "      case (addr)\n";

  private static final String footer = "      endcase\n"
    + "    end\n"
    + "\n"
    + "endmodule\n";


  public void do_asm()
  {
    address = 0;
    System.out.printf(header);


    // プログラム

    // 例1: 連続データの足し算テスト
    // 0番地はNOP
    as_nop(0,0,0,0,0,0,0,0,0);
    // mem[0]からmem[15]を0から15までの値でfill
    as_mvi(0); // r0 = 0
    as_add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
    as_mvi(1); // r0 = 1
    as_add(9,0,8, 0,0,0, 0,0,0); // r9 = r0 + r8 = 1
    as_mvi(16); // r0 = 16
    as_add(10,0,8, 0,0,0, 0,0,0); // r10 = r0 + r8 = 16
    as_add(11,8,8, 0,0,0, 0,0,0); // r11 = r8 + r8 = 0
    as_nop(8,8,8, 0,0,0, 1,1,1); // d_addr = r8; a_addr = r8; b_addr = r8
    as_add(9,11,8, 1,0,0, 1,0,0); // mem[d_addr] = r11 + r8; d_addr += r9;
    as_add(11,11,9, 0,0,0, 0,0,0); // r11 = r11 + r9
    as_cgt(10,11, 0,0, 0,0); // if (r10 > r11) r1 = 0xffffffff else r1 = 0
    as_bc(-3); // if (r1 != 0) pc += -3
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    // x = 16; y = 0; z = 8;
    // for (i = 0; i < 8; i++) {mem[x] = mem[y] + mem[z]; x+=1;y+=1;z+=1;}
    as_mvi(8); // r0 = 8
    as_add(11,0,8, 0,0,0, 0,0,0); // r11 = r0 + r8 = 8
    as_sub(3,0,9, 0,0,0, 0,0,0); // r3 = r0 - r9 = 7
    as_mvi(3); // r0 = 3
    as_add(4,0,8, 0,0,0, 0,0,0); // r4 = r0 + r8 = 3
    as_add(5,8,8, 0,0,0, 0,0,0); // r5 = r8 + r8 = 0
    as_loop(); // 3ステップ後の命令を7+1回繰り返す
    as_nop(10,8,11, 0,0,0, 1,1,1); // d_addr = r10; a_addr = r8; b_addr = r11
    as_nop(0,0,0,0,0,0,0,0,0);
    as_add(9,9,9, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r9; a+=r9; b+=r9;
    // x = 16;
    // for (i = 0; i < 8; i++) {r11 = r11 + mem[x]; x+=1;}
    as_loop(); // 3ステップ後の命令を7+1回繰り返す
    as_nop(8,8,10, 0,0,0, 1,1,1); // d_addr = r8; a_addr = r8; b_addr = r10
    as_add(11,8,8, 0,0,0, 0,0,0); // r11 = r8 + r8 = 0
    as_add(11,11,9, 0,0,1, 0,0,1); // r11 = r11 + mem[b]; b += r9;
    as_out(11, 0, 0); // out(r11) result = 120
    as_halt();
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);


    /*
    // 例2: カウントアップしてその値をI/Oポートに出力
    as_nop(0,0,0,0,0,0,0,0,0);
    as_mvi(0); // r0 = 0
    as_add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
    as_mvi(19); // r0 = 19 （実機では19、シミュレーション時は0にする）
    as_add(9,0,8, 0,0,0, 0,0,0); // r9 = r0 + r8 = 19
    as_mvi(1); // r0 = 1
    as_add(1,0,8, 0,0,0, 0,0,0); // r1 = r0 + r8 = 1
    // loop
    as_add(8,8,1, 0,0,0, 0,0,0); // r8 = r8 + r1 = 0
    as_sr(10,8,9, 0,0,0, 0,0,0); // r10 = r8 >> r9
    as_out(10, 0, 0);
    as_bc(-3); // if (r1 != 0) PC += -3
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    */


    while (address < 0x100)
    {
      print_binary(0);
    }
    System.out.printf(footer);
  }

  private void print_binary(int binary)
  {
    System.out.printf("        8'h%02x: data_out <= 32'h%08x;\n", address, binary);
    address++;
  }

  // アセンブラ
  private void as_halt()
  {
    print_binary(I_HALT);
  }
  
  private void as_nop(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_NOP);
  }

  private void as_mv(int reg_d, int reg_a, int add_d, int add_a, int is_mem_d, int is_mem_a)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (add_d << 12) | (add_a << 11) | (is_mem_d << 9) | (is_mem_a << 8) | I_MV);
  }

  private void as_mvi(int value)
  {
    print_binary(((value & 0xffff) << 10) | I_MVI);
  }

  private void as_mvih(int value)
  {
    print_binary(((value & 0xffff) << 10) | I_MVIH);
  }

  private void as_ceq(int reg_a, int reg_b, int add_a, int add_b, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_a << 20) | (reg_b << 14) | (add_a << 11) | (add_b << 10) | (is_mem_a << 8) | (is_mem_b << 7) | I_CEQ);
  }

  private void as_cgt(int reg_a, int reg_b, int add_a, int add_b, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_a << 20) | (reg_b << 14) | (add_a << 11) | (add_b << 10) | (is_mem_a << 8) | (is_mem_b << 7) | I_CGT);
  }

  private void as_cgta(int reg_a, int reg_b, int add_a, int add_b, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_a << 20) | (reg_b << 14) | (add_a << 11) | (add_b << 10) | (is_mem_a << 8) | (is_mem_b << 7) | I_CGTA);
  }

  private void as_bc(int offset)
  {
    print_binary(((offset & 0xffff) << 10) | I_BC);
  }

  private void as_bl(int offset)
  {
    print_binary(((offset & 0xffff) << 10) | I_BL);
  }

  private void as_ba()
  {
    print_binary(I_BA);
  }

  private void as_loop()
  {
    print_binary(I_LOOP);
  }

  private void as_in(int reg_d, int add_d, int is_mem_d)
  {
    print_binary((reg_d << 26) | (add_d << 12) | (is_mem_d << 9) | I_IN);
  }

  private void as_out(int reg_a, int add_a, int is_mem_a)
  {
    print_binary((reg_a << 20) | (add_a << 11) | (is_mem_a << 8) | I_OUT);
  }

  private void as_add(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_ADD);
  }

  private void as_sub(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_SUB);
  }

  private void as_and(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_AND);
  }

  private void as_or(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_OR);
  }

  private void as_xor(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_XOR);
  }

  private void as_not(int reg_d, int reg_a, int add_d, int add_a, int is_mem_d, int is_mem_a)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (add_d << 12) | (add_a << 11) | (is_mem_d << 9) | (is_mem_a << 8) | I_NOT);
  }

  private void as_sr(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_SR);
  }

  private void as_sl(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_SL);
  }

  private void as_sra(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_SRA);
  }

  private void as_mul(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_MUL);
  }
}
