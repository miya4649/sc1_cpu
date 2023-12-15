/*
  Copyright (c) 2017 miya
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
  private static final int CODE_ROM_DEPTH = 10;
  private static final int DATA_ROM_DEPTH = 9;

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
    lib_stop();
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
    lib_call("f_rand");
    lib_call("f_uart_hex_word_ln");
    lib_nop(1);
    lib_loop_end("loop0");
    lib_stop();

    // link library
    f_uart_char();
    f_uart_hex();
    f_uart_hex_word();
    f_uart_hex_word_ln();
    f_rand();
  }

  private void example_random_data()
  {
    datalib_rand_data();
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
    lib_set_im(256);
    as_add(R7,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_memory_dump");
    lib_stop();

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

    lib_set_im(3);
    as_add(8,0,6, 0,0,0); // r8 = r0 + r6 = 4
    lib_set_im32(SPRITE_X_ADDRESS);
    lib_wait_reg2addr();
    as_add(0,6,6, 1,0,0); // mem[d] = r6 + r6; (sprite.x = 0)
    as_add(1,6,6, 3,0,0); // mem[d+1] = r6 + r6; (sprite.y = 0)
    as_add(2,8,6, 3,0,0); // mem[d+2] = r8 + r6; (sprite.scale = 4)

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
    lib_stop();

    // link library
    f_copy_chr_bitmap();
    f_copy_small_data();
    f_wait_vsync();
  }

  private void example_ascii_data()
  {
    datalib_ascii_chr_data();
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

    lib_stop();

    // link library
    f_copy_chr_bitmap();
    f_copy_small_data();
    f_wait_vsync();
  }

  private void example_clear_data()
  {
    datalib_ascii_chr_data();
  }

  private void example_line()
  {
    lib_init_stack();
    lib_set_im32(SPRITE_X_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG); // sprite.x
    lib_set_im32(128);
    as_add(1,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG); // sprite.y
    as_add(2,reg_im(5),reg_im(0), AM_OFFSET,AM_REG,AM_REG); // sprite.scale

    label("L_example_line_0");

    // clear
    lib_set_im32(SPRITE_ADDRESS);
    lib_simple_mv(R6, SP_REG_MVI);
    lib_set_im32(16384);
    lib_simple_mv(R7, SP_REG_MVI);
    lib_set_im32(0);
    lib_simple_mv(R8, SP_REG_MVI);
    lib_call("f_memory_fill");

    // loop
    lib_set_im32(1000000);
    lib_simple_mv(R11, SP_REG_MVI);
    label("L_example_line_1");

    // set random line data
    lib_set_im32(addr_abs("d_example_line"));
    lib_simple_mv(R9, SP_REG_MVI);

    // x0 = nrand(128)
    lib_call("f_rand");
    lib_set_im(127);
    as_and(R9,R6,SP_REG_MVI, AM_SET,AM_REG,AM_REG);
    as_add(R9,R9,reg_im(1), AM_REG,AM_REG,AM_REG);
    // y0 = nrand(64)
    lib_call("f_rand");
    lib_set_im(63);
    as_and(R9,R6,SP_REG_MVI, AM_SET,AM_REG,AM_REG);
    as_add(R9,R9,reg_im(1), AM_REG,AM_REG,AM_REG);
    // x1 = nrand(128)
    lib_call("f_rand");
    lib_set_im(127);
    as_and(R9,R6,SP_REG_MVI, AM_SET,AM_REG,AM_REG);
    as_add(R9,R9,reg_im(1), AM_REG,AM_REG,AM_REG);
    // y1 = nrand(64)
    lib_call("f_rand");
    lib_set_im(63);
    as_and(R9,R6,SP_REG_MVI, AM_SET,AM_REG,AM_REG);
    as_add(R9,R9,reg_im(1), AM_REG,AM_REG,AM_REG);
    // color = nrand(256)
    lib_call("f_rand");
    lib_set_im(255);
    as_and(R9,R6,SP_REG_MVI, AM_SET,AM_REG,AM_REG);

    // line
    lib_set_im32(addr_abs("d_example_line"));
    lib_simple_mv(R6, SP_REG_MVI);
    lib_call("f_line");

    // loop end
    as_sub(R11,R11,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_cgta(R11,reg_im(0), AM_REG,AM_REG);
    lib_bc("L_example_line_1");
    lib_stop();
    lib_ba("L_example_line_0");

    // link library
    f_line();
    f_memory_fill();
    f_rand();
  }

  private void example_line_data()
  {
    datalib_rand_data();

    label("d_example_line");
    d32x4(0,0,63,63);
    d32x4(255,0,0,0);
  }

  private void example_timer()
  {
    // timer test
    int R_wait_count = R6;
    int R_led_addr = R9;
    int R_counter = R10;

    lib_init_stack();
    // blink LED
    // R_led_addr = LED_ADDRESS
    lib_set_im32(LED_ADDRESS);
    lib_simple_mv(R_led_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    // R_counter = 0
    as_add(R_counter,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    // reset timer
    lib_call("f_timer_reset");

    // loop
    label("L_example_timer_0");
    // LED = R_counter
    as_add(R_led_addr,R_counter,reg_im(0), AM_SET,AM_REG,AM_REG);
    // timer_wait(50000000); (1 second)
    lib_set32(R_wait_count, 50000000);
    lib_call("f_timer_wait");
    // R_counter++
    as_add(R_counter,R_counter,reg_im(1), AM_REG,AM_REG,AM_REG);
    lib_ba("L_example_timer_0");

    // link library
    f_timer_reset();
    f_timer_wait();
  }

  private void example_timer_data()
  {
    datalib_timer_data();
  }

  private void example_i2c()
  {
    // i2c test for Sunlike Display SO1602AWGB-UC-WB
    lib_init_stack();

    // reset timer
    lib_call("f_timer_reset");

    // loop start
    label("L_example_i2c_0");

    // timer_wait(50000000); (1 second)
    m_example_i2c_timer_wait(50000000);

    // display clear
    m_example_i2c_send_command(0x01);
    // cursor reset
    m_example_i2c_send_command(0x02);
    // display on
    m_example_i2c_send_command(0x0c);
    // display clear
    m_example_i2c_send_command(0x01);
    // puts
    lib_set32(R6, addr_abs("d_example_i2c_str"));
    lib_call("f_example_i2c_puts");

    m_example_i2c_timer_wait(50000000);

    // display clear
    m_example_i2c_send_command(0x01);
    // cursor reset
    m_example_i2c_send_command(0x02);

    lib_ba("L_example_i2c_0");

    // link library
    f_timer_reset();
    f_timer_wait();
    f_i2c_command();
    f_i2c_status();
    f_i2c_wait_command();
    f_i2c_write();
    f_i2c_read();
    f_example_i2c_puts();
  }

  private void f_example_i2c_puts()
  {
    // i2c print string
    // input: r6:text_start_address
    // output: none
    // modify: r6, d_addr, a_addr
    // use stack: 5
    int OLED_ADDR = 0x3c;
    int OLED_COMMAND = 0x00;
    int OLED_DATA = 0x40;
    int Rchar = R9;
    int Rnext = R10;
    int Raddr = R11;
    int Rshift = R12;
    label("f_example_i2c_puts");
    lib_push(SP_REG_LINK);
    /*
    addr = r6;
    shift = 24;
    do
    {
      char = mem[addr] >> shift;
      char = char & 0xff;
      if (char == 0) break;
      R6 = OLED_ADDR;
      R7 = OLED_DATA;
      R8 = char;
      call "f_i2c_write";
      shift -= 8;
      if (shift < 0)
      {
        shift = 24;
        addr++;
      }
    } while (1)
    */
    lib_push_regs(R7, R12);
    lib_simple_mv(Raddr, R6);
    as_nop(0,R6,0, AM_REG,AM_SET,AM_REG); // a_addr = r6
    as_add(Rshift,reg_im(12),reg_im(12), AM_REG,AM_REG,AM_REG);
    label("f_example_i2c_puts_L_0");
    as_sr(Rchar,Raddr,Rshift, AM_REG,AM_SET,AM_REG);
    lib_set_im(0xff);
    as_and(Rchar,Rchar,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_ceq(Rchar,reg_im(0), AM_REG,AM_REG);
    lib_bc("f_example_i2c_puts_L_1");
    lib_set32(R6, OLED_ADDR);
    lib_set32(R7, OLED_DATA);
    lib_simple_mv(R8, Rchar);
    lib_call("f_i2c_write");
    as_sub(Rshift,Rshift,reg_im(8), AM_REG,AM_REG,AM_REG);
    as_add(R6,reg_im(12),reg_im(12), AM_REG,AM_REG,AM_REG);
    as_add(Rnext,Raddr,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_cgta(reg_im(0),Rshift, AM_REG,AM_REG);
    as_mv(Rshift,R6, AM_REG,AM_REG);
    as_mv(Raddr,Rnext, AM_REG,AM_REG);
    lib_ba("f_example_i2c_puts_L_0");
    label("f_example_i2c_puts_L_1");
    lib_pop_regs(R12, R7);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void m_example_i2c_send_command(int command)
  {
    int OLED_ADDR = 0x3c;
    int OLED_COMMAND = 0x00;
    lib_set32(R6, OLED_ADDR);
    lib_set32(R7, OLED_COMMAND);
    lib_set32(R8, command);
    lib_call("f_i2c_write");
  }

  private void m_example_i2c_timer_wait(int count)
  {
    lib_set32(R6, count);
    lib_call("f_timer_wait");
  }

  private void example_i2c_data()
  {
    label("d_example_i2c_str");
    string_data("Hello, world!\n");

    datalib_timer_data();
  }

  @Override
  public void init()
  {
    set_rom_depth(CODE_ROM_DEPTH, DATA_ROM_DEPTH);
    set_stack_address((1 << DATA_ROM_DEPTH) - 1);
    // use default name
    //set_filename("example");
  }

  @Override
  public void program()
  {
    //example_counter();
    //example_helloworld();
    //example_random();
    //example_memory_dump();
    //example_vram_fill();
    //example_ascii();
    //example_clear();
    example_line();
    //example_timer();
    //example_i2c();
  }

  @Override
  public void data()
  {
    //example_helloworld_data();
    //example_random_data();
    //example_ascii_data();
    //example_clear_data();
    example_line_data();
    //example_timer_data();
    //example_i2c_data();
  }
}
