/*
  Copyright (c) 2016, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class Program extends Asm
{
  // label
  // value: 0 <= X < LABEL_SIZE (unique)
  private static final int L_0 = 0;
  private static final int L_1 = 1;
  private static final int L_2 = 2;
  private static final int L_3 = 3;
  private static final int L_4 = 4;
  private static final int L_5 = 5;
  private static final int L_6 = 6;
  private static final int L_7 = 7;
  private static final int L_8 = 8;
  private static final int L_9 = 9;
  private static final int L_10 = 10;
  private static final int L_11 = 11;
  private static final int L_12 = 12;

  private void example1()
  {
    // example1: adding test of consecutive data sets
    // address 0 must be NOP
    as_nop(0,0,0,0,0,0,0,0,0);
    // for(i=0;i<16;i++){mem[i]=i;}
    as_mvi(0); // r0 = 0
    as_add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
    as_mvi(1); // r0 = 1
    as_add(9,0,8, 0,0,0, 0,0,0); // r9 = r0 + r8 = 1
    as_mvi(16); // r0 = 16
    as_add(10,0,8, 0,0,0, 0,0,0); // r10 = r0 + r8 = 16
    as_add(11,8,8, 0,0,0, 0,0,0); // r11 = r8 + r8 = 0
    as_nop(8,8,8, 0,0,0, 1,1,1); // d_addr = r8; a_addr = r8; b_addr = r8

    label(L_0);

    as_add(9,11,8, 1,0,0, 1,0,0); // mem[d_addr] = r11 + r8; d_addr += r9;
    as_add(11,11,9, 0,0,0, 0,0,0); // r11 = r11 + r9
    as_cgt(10,11, 0,0, 0,0); // if (r10 > r11) r1 = 0xffffffff else r1 = 0
    as_bc(addr_rel(L_0)); // if (r1 != 0) goto L_0
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    // x = 16; y = 0; z = 8;
    // for (i = 0; i < 8; i++) {mem[x] = mem[y] + mem[z]; x+=1;y+=1;z+=1;}
    as_mvi(8); // r0 = 8
    as_add(11,0,8, 0,0,0, 0,0,0); // r11 = r0 + r8 = 8
    as_sub(3,0,9, 0,0,0, 0,0,0); // r3 = r0 - r9 = 7 (loop count-1)
    as_mvi(addr_abs(L_3) - addr_abs(L_1)); // r0 = 3
    as_add(4,0,8, 0,0,0, 0,0,0); // r4 = r0 + r8 = 3 (the offset of the end of the loop)
    as_mvi(addr_abs(L_2) - addr_abs(L_3) + 1); // r0 = 0
    as_add(5,0,8, 0,0,0, 0,0,0); // r5 = r0 + r8 = 0 (jump offset)
    as_loop();

    label(L_1);

    as_nop(10,8,11, 0,0,0, 1,1,1); // d_addr = r10; a_addr = r8; b_addr = r11
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);

    label(L_2);

    as_add(9,9,9, 1,1,1, 1,1,1); // mem[d]=mem[a]+mem[b]; d+=r9; a+=r9; b+=r9;

    label(L_3);

    // x = 16;
    // for (i = 0; i < 8; i++) {r11 = r11 + mem[x]; x+=1;}
    as_mvi(addr_abs(L_6) - addr_abs(L_4)); // r0 = 3
    as_add(4,0,8, 0,0,0, 0,0,0); // r4 = r0 + r8 = 3
    as_mvi(addr_abs(L_5) - addr_abs(L_6) + 1); // r0 = 0
    as_add(5,0,8, 0,0,0, 0,0,0); // r5 = r0 + r8 = 0
    as_loop();

    label(L_4);

    as_nop(8,8,10, 0,0,0, 1,1,1); // d_addr = r8; a_addr = r8; b_addr = r10
    as_add(11,8,8, 0,0,0, 0,0,0); // r11 = r8 + r8 = 0
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);

    label(L_5);

    as_add(11,11,9, 0,0,1, 0,0,1); // r11 = r11 + mem[b]; b += r9;

    label(L_6);

    as_out(11, 0, 0); // out(r11) result = 120
    as_halt();
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
  }

  private void example2()
  {
    // example2: simple counter (register)
    as_nop(0,0,0,0,0,0,0,0,0);
    as_mvi(0); // r0 = 0
    as_add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
    as_mvi(19); // r0 = 19 (19 on the real machine, 0 on the simulation)
    as_add(9,0,8, 0,0,0, 0,0,0); // r9 = r0 + r8 = 19
    as_mvi(1); // r0 = 1
    as_add(1,0,8, 0,0,0, 0,0,0); // r1 = r0 + r8 = 1
    // loop
    label(L_0);
    as_add(8,8,1, 0,0,0, 0,0,0); // r8 = r8 + r1 = 0
    as_sr(10,8,9, 0,0,0, 0,0,0); // r10 = r8 >> r9
    as_out(10, 0, 0);
    as_bc(addr_rel(L_0)); // if (r1 != 0) PC += -3
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
  }

  private void example3()
  {
    // example3: simple counter (RAM, loop instruction)
    as_nop(0,0,0,0,0,0,0,0,0);

    as_mvi(0); // r0 = 0
    as_add(8,0,0, 0,0,0, 0,0,0); // r8 = r0 + r0 = 0
    as_mvi(1); // r0 = 1
    as_add(9,0,8, 0,0,0, 0,0,0); // r9 = r0 + r8 = 1
    as_add(10,8,8, 0,0,0, 0,0,0); // r10 = r8 + r8 = 0

    // loop (write data)
    // for(i=0;i<256;i++){mem[i]=i;}
    as_mvi(255); // loop count - 1
    as_add(3,0,8, 0,0,0, 0,0,0); // r3 = loop count-1
    as_mvi(addr_abs(L_3) - addr_abs(L_1));
    as_add(4,0,8, 0,0,0, 0,0,0); // r4 = the offset of the end of the loop
    as_mvi(addr_abs(L_2) - addr_abs(L_3) + 1);
    as_add(5,0,8, 0,0,0, 0,0,0); // r5 = jump offset
    as_loop();
    label(L_1);
    as_nop(8,8,8, 0,0,0, 1,1,1); // d_addr = r8; a_addr = r8; b_addr = r8
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    label(L_2);
    as_add(9,10,8, 1,0,0, 1,0,0); // mem[d] = r10 + r8; d+=r9;
    as_add(10,10,9, 0,0,0, 0,0,0); // r10 = r10 + r9
    label(L_3);

    label(L_9);

    as_add(10,8,8, 0,0,0, 0,0,0); // r10 = r8 + r8 = 0
    as_mvi(256);
    as_add(11,0,8, 0,0,0, 0,0,0); // r11 = r0 + r8 = 256

    label(L_4);

    // loop(wait)
    as_mvi(0x8d80); // loop - 1 (low)
    as_mvih(0x5b); // loop - 1 (high)
    as_add(3,0,8, 0,0,0, 0,0,0); // r3 = loop count-1
    as_mvi(addr_abs(L_7) - addr_abs(L_5));
    as_add(4,0,8, 0,0,0, 0,0,0); // r4 = the offset of the end of the loop
    as_mvi(addr_abs(L_6) - addr_abs(L_7) + 1);
    as_add(5,0,8, 0,0,0, 0,0,0); // r5 = jump offset
    as_loop();

    label(L_5);

    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);

    label(L_6);

    as_nop(0,0,0,0,0,0,0,0,0);

    label(L_7);

    as_nop(8,10,8, 0,0,0, 1,1,1); // d_addr = r8; a_addr = r10; b_addr = r8
    as_out(8, 1, 1);
    as_add(10,10,9, 0,0,0, 0,0,0); // r10 = r10 + r9
    as_cgt(11,10, 0,0, 0,0);
    as_bc(addr_rel(L_4)); // if r11 > r10 goto L_4
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_add(1,8,9, 0,0,0, 0,0,0); // r1 = r8 + r9
    as_bc(addr_rel(L_9)); // else goto L_9
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
    as_nop(0,0,0,0,0,0,0,0,0);
  }

  @Override
  public void program()
  {
    //example1();
    //example2();
    example3();
  }
}
