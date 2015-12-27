/*
  Copyright (c) 2015, miya
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
  private static final int I_IN   = 0x0b;
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
    as_mvi(0, 0); // r0 = 0
    as_mvi(1, 1); // r1 = 1
    as_mvi(2, 16); // r2 = 16
    as_mvi(3, 0); // r3 = 0
    as_nop(0,0,0, 0,0,0, 1,1,1); // d_addr = r0; a_addr = r0; b_addr = r0
    as_mv(1,3,1, 1,0,0, 1,0,0); // if (r1 != 0) mem[d_addr] = r3; d_addr += r1;
    as_add(3,3,1, 0,0,0, 0,0,0); // r3 = r3 + r1
    as_cgt(4,2,3, 0,0, 0,0); // if (r2 > r3) r4 = 0xffffffff else r4 = 0
    as_bc(4, -3); // if (r4 != 0) pc += -3
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    // x = 16; y = 0; z = 8;
    // for (i = 0; i < 8; i++) {mem[x] = mem[y] + mem[z]; x+=1;y+=1;z+=1;}
    as_mvi(3, 8); // r3 = 8
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(2,0,3, 0,0,0, 1,1,1); // d_addr = r2; a_addr = r0; b_addr = r3
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    as_add(1,1,1, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r1; a+=r1; b+=r1;
    // x = 16;
    // for (i = 0; i < 8; i++) {r0 = r0 + mem[x]; x+=1;}
    as_nop(0,0,2, 0,0,0, 1,1,1); // d_addr = r0; a_addr = r0; b_addr = r2
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_add(0,0,1, 0,0,1, 0,0,1); // r0 = r0 + mem[b]; b += r1;
    as_out(0, 0, 0); // out(r0) result = 120
    as_halt();
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);


    /*
    // 例2: カウントアップしてその値をI/Oポートに出力
    as_nop(0,0,0,0,0,0,0,0,0);
    as_mvi(0, 0);
    as_mvi(1, 1);
    as_add(0,0,1, 0,0,0, 0,0,0);
    as_out(0, 0, 0);
    as_bc(1, -2);
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

  private void as_mv(int reg_d, int reg_a, int reg_b, int add_d, int add_a, int add_b, int is_mem_d, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_d << 12) | (add_a << 11) | (add_b << 10) | (is_mem_d << 9) | (is_mem_a << 8) | (is_mem_b << 7) | I_MV);
  }

  private void as_mvi(int reg_d, int value)
  {
    print_binary((reg_d << 26) | ((value & 0xffff) << 10) | I_MVI);
  }

  private void as_mvih(int reg_d, int value)
  {
    print_binary((reg_d << 26) | ((value & 0xffff) << 10) | I_MVIH);
  }

  private void as_ceq(int reg_d, int reg_a, int reg_b, int add_a, int add_b, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_a << 11) | (add_b << 10) | (is_mem_a << 8) | (is_mem_b << 7) | I_CEQ);
  }

  private void as_cgt(int reg_d, int reg_a, int reg_b, int add_a, int add_b, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_a << 11) | (add_b << 10) | (is_mem_a << 8) | (is_mem_b << 7) | I_CGT);
  }

  private void as_cgta(int reg_d, int reg_a, int reg_b, int add_a, int add_b, int is_mem_a, int is_mem_b)
  {
    print_binary((reg_d << 26) | (reg_a << 20) | (reg_b << 14) | (add_a << 11) | (add_b << 10) | (is_mem_a << 8) | (is_mem_b << 7) | I_CGTA);
  }

  private void as_bc(int reg_d, int offset)
  {
    print_binary((reg_d << 26) | ((offset & 0xffff) << 10) | I_BC);
  }

  private void as_bl(int reg_d, int offset)
  {
    print_binary((reg_d << 26) | ((offset & 0xffff) << 10) | I_BL);
  }

  private void as_ba(int reg_a)
  {
    print_binary((reg_a << 20) | I_BA);
  }

  private void as_in(int reg_d)
  {
    print_binary((reg_d << 26) | I_IN);
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
