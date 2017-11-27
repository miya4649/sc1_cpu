/*
  Copyright (c) 2017, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.lang.Math;

public class Examples extends AsmLib
{
  private static final int ROM_DEPTH = 12; // ROM_DEPTH = 10 for small FPGAs

  private void example_counter()
  {
    // simple counter (register)
    lib_set_im32(LED_ADDRESS);
    lib_simple_mv(R8, SP_REG_MVI);
    as_add(R6,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG); // r6 = 0
    lib_set_im(19); // r0 = 19 (19 on the real machine, 0 on the simulation)
    lib_simple_mv(R7, SP_REG_MVI);
    // loop
    label("L_0");
    as_sr(R8,R6,R7, AM_SET,AM_REG,AM_REG); // mem[LED_ADDRESS] = r6 >> r7
    as_add(R6,R6,reg_im(1), AM_REG,AM_REG,AM_REG); // r6++
    lib_ba("L_0"); // goto L_0
  }

  private void example_helloworld()
  {
    lib_init_stack();

    lib_set_im32(addr_abs("d_helloworld"));
    lib_simple_mv(R6, SP_REG_MVI);
    lib_call("f_uart_print");
    as_halt();
    lib_wait_delay_slot();
    // link library
    f_uart_char();
    f_uart_print();
  }

  private void example_helloworld_data()
  {
    label("d_helloworld");
    string_data("Hello, world!\n");
  }

  private void example_random()
  {
    lib_init_stack();
    lib_loop_start("loop0", 100, LOOP_MODE_IM);
    lib_set(R6, 7);
    lib_call("f_nrand");
    lib_call("f_uart_hex_word_ln");
    lib_nop(1);
    lib_loop_end("loop0");
    as_halt();
    lib_wait_delay_slot();

    // link library
    f_uart_char();
    f_uart_hex();
    f_uart_hex_word();
    f_uart_hex_word_ln();
    f_nrand();
  }

  private void d_rand_data()
  {
    label("d_rand");
    d32(0xfc720c27);
  }

  private void example_memory_dump()
  {
    // memory dump test
    /*
    init stack
    r6 = 0;
    r7 = 256;
    call f_uart_memory_dump
     */
    lib_init_stack();

    as_add(R6,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_set_im(64);
    as_add(R7,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_memory_dump");
    as_halt();
    lib_wait_delay_slot();

    // link library
    f_uart_char();
    f_uart_hex();
    f_uart_hex_word();
    f_uart_memory_dump();
  }

  private void example_vram_fill()
  {
    // fill sprite memory
    lib_nop(1);
    lib_set_im(0); // r0 = 0
    as_add(6,0,0, 0,0,0); // r6 = r0 + r0 = 0
    lib_set_im(1); // r0 = 1
    as_add(7,0,6, 0,0,0); // r7 = r0 + r6 = 1

    lib_set_im(4);
    as_add(8,0,6, 0,0,0); // r8 = r0 + r6 = 4
    lib_set_im32(SPRITE_X_ADDRESS);
    lib_wait_reg2addr();
    as_add(0,6,6, 1,0,0); // mem[d] = r6 + r6; (sprite.x = 0)
    as_add(1,6,6, 3,0,0); // mem[d+1] = r6 + r6; (sprite.y = 0)
    as_add(2,8,6, 3,0,0); // mem[d+2] = r6 + r6; (sprite.scale = 4)

    as_add(8,0,0, 0,0,0); // r8 = r0 + r0 = 0

    label("L_0");

    lib_set_im32(SPRITE_ADDRESS); // r0 = SPRITE_ADDRESS
    lib_wait_reg2addr();
    as_nop(0,0,0, 1,1,1); // d_addr=r0; a_addr=r0; b_addr=r0;

    // loop
    lib_set_im(4095); // loop count - 1
    as_add(3,0,6, 0,0,0); // r3 = r0 + r6;
    lib_set_im32(addr_abs("L_3") - addr_abs("L_1")); // the offset of the end of the loop
    as_add(4,0,6, 0,0,0); // r4 = r0 + r6;
    lib_set_im32(addr_abs("L_2") - addr_abs("L_3")); // jump offset
    as_add(5,0,6, 0,0,0); // r5 = r0 + r6;
    label("L_1"); // loop inst
    as_loop();
    lib_wait_loop();
    label("L_2"); // loop start
    as_add(7,6,8, 2,0,0); // mem[d]=r6+r8; d_addr+=r7;
    label("L_3"); // loop end
    as_add(8,8,7, 0,0,0); // r8=r8+r7

    as_add(8,8,7, 0,0,0); // r8=r8+r7

    // vsync wait
    label("L_4");
    lib_set_im32(VSYNC_ADDRESS);
    lib_wait_reg2addr();
    as_ceq(0,7, 1,0); // if (mem[r0] == r7) r1 = true
    lib_bc("L_4"); // if (r1 != 0) goto L_4
    label("L_5");
    lib_set_im32(VSYNC_ADDRESS);
    lib_wait_reg2addr();
    as_ceq(0,6, 1,0); // if (mem[r0] == r6) r1 = true
    lib_bc("L_5"); // if (r1 != 0) goto L_5

    as_add(1,6,7, 0,0,0); // r1 = r6 + r7 = 1
    lib_bc("L_0"); // if (r1 != 0) goto L_0
  }

  private void example_ascii()
  {
    lib_init_stack();
    lib_push(SP_REG_LINK);
    lib_call("f_copy_chr_bitmap");

    // chr reg init
    lib_set_im32(CHR_X_ADDRESS);
    lib_wait_reg2addr();
    // CHR_X
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);
    // CHR_Y
    lib_set_im32(-48);
    as_add(1,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // CHR_SCALE
    as_add(2,reg_im(2),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // CHR_PALETTE0
    lib_set_im32(0xffffff00);
    as_add(3,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // CHR_PALETTE1
    lib_set_im32(0xe0e0e000);
    as_add(4,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // CHR_PALETTE2
    lib_set_im32(0x1c1c1c00);
    as_add(5,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // CHR_PALETTE3
    lib_set_im32(0x03030300);
    as_add(6,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    lib_set_im32(CHR_CHR_ADDRESS);
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,0,0, AM_SET,AM_REG,AM_REG);
    lib_set_im32(4094);
    lib_simple_mv(R8, SP_REG_MVI);
    lib_set(R6, 1);
    lib_set(R7, 0);
    lib_nop(1);
    lib_loop_start("example_ascii_loop0", R8, LOOP_MODE_REG);
    as_add(R6,R7,reg_im(0), AM_INC,AM_REG,AM_REG);
    as_add(R7,R7,reg_im(1), AM_REG,AM_REG,AM_REG);
    lib_loop_end("example_ascii_loop0");

    lib_set(R6, 0);
    label("example_ascii_L_0");
    lib_call("f_wait_vsync");
    lib_set_im32(CHR_X_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,R6,reg_im(0), AM_SET,AM_REG,AM_REG);
    as_sub(R6,R6,reg_im(8), AM_REG,AM_REG,AM_REG);
    lib_set_compare_reg(true);
    lib_bc("example_ascii_L_0");

    lib_pop(SP_REG_LINK);
    lib_stop(R7);

    // link library
    f_copy_chr_bitmap();
    f_copy_small_data();
    f_wait_vsync();
  }

  private void example_clear()
  {
    lib_init_stack();

    // clear LED
    lib_set_im32(LED_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);

    // chr reg init
    lib_set_im32(CHR_X_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);
    as_add(1,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(2,reg_im(2),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(3,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(4,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(5,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(6,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // clear chr data
    lib_set_im32(CHR_CHR_ADDRESS);
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,0,0, AM_SET,AM_REG,AM_REG);
    as_add(R6,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_loop_start("example_clear_loop0", 4095, LOOP_MODE_IM);
    as_add(R6,reg_im(0),reg_im(0), AM_INC,AM_REG,AM_REG);
    lib_loop_end("example_clear_loop0");

    // clear sprite data
    lib_set_im32(SPRITE_ADDRESS);
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,0,0, AM_SET,AM_REG,AM_REG);
    as_add(R6,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_loop_start("example_clear_loop1", 4095, LOOP_MODE_IM);
    as_add(R6,reg_im(0),reg_im(0), AM_INC,AM_REG,AM_REG);
    lib_loop_end("example_clear_loop1");

    // clear audio data
    lib_set_im32(0x80008000);
    lib_simple_mv(R6, SP_REG_MVI);
    lib_set_im32(AUDIO_DATA_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,R6,reg_im(0), AM_SET,AM_REG,AM_REG);
    lib_set_im32(AUDIO_VALID_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);
    as_add(SP_REG_MVI,reg_im(1),reg_im(0), AM_SET,AM_REG,AM_REG);

    as_halt();
    lib_wait_delay_slot();

    // link library
    f_copy_chr_bitmap();
    f_copy_small_data();
    f_wait_vsync();
  }

  @Override
  public void init()
  {
    set_rom_depth(ROM_DEPTH);
    set_stack_address((1 << ROM_DEPTH) - 1);
    // use default name
    //set_filename("example");
  }

  @Override
  public void program()
  {
    example_counter();
    //example_helloworld();
    //example_random();
    //example_memory_dump();
    //example_vram_fill();
    //example_ascii();
    //example_clear();
  }

  @Override
  public void data()
  {
    datalib_ascii_chr_data();
    d_rand_data();
    example_helloworld_data();
  }
}
