/*
  Copyright (c) 2015 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`include "../topinclude.v"
`timescale 1ns / 1ps

module testbench;

  localparam STEP  = 20; // 20 ns: 50MHz
  localparam STEPA = 56; // 56 ns: 18MHz
  localparam STEPV = 40; // 40 ns: 25MHz
  localparam TICKS = 5000000;

  localparam WIDTH_D = 32;
  localparam DEPTH_D = 12;
  localparam DEPTH_I = 12;
  localparam DEPTH_V = 17;
  localparam DEPTH_REG = 4;
  localparam DEPTH_IO_REG = 5;
  localparam CLK_HZ  = 50000000;
  localparam SCLK_HZ = 25000000;
  localparam WAIT = (CLK_HZ / SCLK_HZ - 1);
  localparam COUNTER_WIDTH = 9;
  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam ONE = 1'd1;
  localparam ZERO = 1'd0;
  localparam FFFF = {WIDTH_D{1'b1}};
  localparam VGA_BPP = 8;

  reg clk;
  reg reset;
  wire [9:0] led;
  integer    i;
`ifdef USE_UART
  wire       txd;
  reg        rxd;
`endif
`ifdef USE_AUDIO
  reg        clka;
  reg        reseta;
  wire       audio_r;
  wire       audio_l;
`endif
`ifdef USE_MINI_AUDIO
  wire       audio_r;
  wire       audio_l;
`endif
`ifdef USE_VGA
  reg        clkv;
  reg        resetv;
  wire       vga_hs;
  wire       vga_vs;
  wire [VGA_BPP-1:0] vga_color_out;
`endif
`ifdef USE_MINI_VGA
  wire       vga_hs;
  wire       vga_vs;
  wire [VGA_BPP-1:0] vga_color_out;
`endif
`ifdef USE_I2C
  wire       i2c_sda;
  wire       i2c_scl;
`endif

    initial
      begin
        $dumpfile("wave.vcd");
        $dumpvars(10, testbench);
        for (i = 0; i < (1 << DEPTH_REG); i = i + 1)
          begin
            $dumpvars(0, testbench.sc1_soc_0.sc1_cpu_0.reg_file[i]);
            $dumpvars(0, testbench.sc1_soc_0.mem_d.rw_port_ram_a.ram[i]);
            $dumpvars(0, testbench.sc1_soc_0.mem_i.ram[i]);
`ifdef USE_VGA
            $dumpvars(0, testbench.sc1_soc_0.sprite_0.dual_clk_ram_0.gen.ram[i]);
`endif
          end
        for (i = 0; i < (1 << DEPTH_IO_REG); i = i + 1)
          begin
            $dumpvars(0, testbench.sc1_soc_0.io_reg_w[i]);
            $dumpvars(0, testbench.sc1_soc_0.io_reg_r[i]);
          end
        $monitor("time: %d reset: %d led: %d vsync: %d", $time, reset, led, testbench.sc1_soc_0.vga_iface_0.vsync);
      end

  // generate clk
  initial
    begin
      clk = 1'b1;
      forever
        begin
          #(STEP / 2) clk = ~clk;
        end
    end

  // generate reset signal
  initial
    begin
      reset = 1'b0;
      repeat (2) @(posedge clk) reset <= 1'b1;
      @(posedge clk) reset <= 1'b0;
    end

  // stop simulation after TICKS
  initial
    begin
      repeat (TICKS) @(posedge clk);
      $finish;
    end

  sc1_soc
    #(
      .UART_CLK_HZ (CLK_HZ),
      .UART_SCLK_HZ (SCLK_HZ),
      .WIDTH_D (WIDTH_D),
      .DEPTH_I (DEPTH_I),
      .DEPTH_D (DEPTH_D),
      .DEPTH_V (DEPTH_V)
      )
  sc1_soc_0
    (
     .clk (clk),
     .reset (reset),
`ifdef USE_UART
     .uart_rxd (rxd),
     .uart_txd (txd),
`endif
`ifdef USE_AUDIO
     .clka (clka),
     .reseta (reseta),
     .audio_r (audio_r),
     .audio_l (audio_l),
`endif
`ifdef USE_MINI_AUDIO
     .audio_r (audio_r),
     .audio_l (audio_l),
`endif
`ifdef USE_VGA
     .clkv (clkv),
     .resetv (resetv),
     .vga_hs (vga_hs),
     .vga_vs (vga_vs),
     .vga_color_out (vga_color_out),
`endif
`ifdef USE_MINI_VGA
     .vga_hs (vga_hs),
     .vga_vs (vga_vs),
     .vga_color_out (vga_color_out),
`endif
`ifdef USE_I2C
     .i2c_sda (i2c_sda),
     .i2c_scl (i2c_scl),
`endif
     .led (led)
     );

`ifdef USE_UART

  task uart_data(input [7:0] value);
    integer i;
    begin
      @(posedge clk) rxd <= FALSE; // start bit
      repeat (WAIT) @(posedge clk);

      for (i = 0; i < 8; i = i + 1)
        begin
          @(posedge clk) rxd <= value[i];
          repeat (WAIT) @(posedge clk);
        end

      @(posedge clk) rxd <= TRUE; // stop bit
      repeat (WAIT) @(posedge clk);
    end
  endtask

  task uart_word(input [31:0] address, input [31:0] data);
    begin
      uart_data(8'haa); // start byte
      uart_data(address[7:0]);
      uart_data(address[15:8]);
      uart_data(address[23:16]);
      uart_data(address[31:24]);
      uart_data(data[7:0]);
      uart_data(data[15:8]);
      uart_data(data[23:16]);
      uart_data(data[31:24]);
      uart_data(8'h55); // end byte
    end
  endtask

  // uart write
  initial
    begin
      @(posedge clk) rxd <= TRUE; // idle
      repeat (10) @(posedge clk);

      uart_word(32'h00005000, 32'h00000001); // cpu_reset
      uart_word(32'h00005002, 32'h00000000); // soc master

      // program start here --------
      uart_word(32'h00004000, 32'h00000001);
      uart_word(32'h00004001, 32'h00000003);
      uart_word(32'h00004002, 32'h18000040);
      uart_word(32'h00004003, 32'h00010003);
      uart_word(32'h00004004, 32'h1c018040);
      uart_word(32'h00004005, 32'h00030003);
      uart_word(32'h00004006, 32'h20018040);
      uart_word(32'h00004007, 32'h30010003);
      uart_word(32'h00004008, 32'h00000004);
      uart_word(32'h00004009, 32'h00000001);
      uart_word(32'h0000400a, 32'h00000001);
      uart_word(32'h0000400b, 32'h00618840);
      uart_word(32'h0000400c, 32'h04619840);
      uart_word(32'h0000400d, 32'h08819840);
      uart_word(32'h0000400e, 32'h20000040);
      uart_word(32'h0000400f, 32'h80000003);
      uart_word(32'h00004010, 32'h00000004);
      uart_word(32'h00004011, 32'h00000001);
      uart_word(32'h00004012, 32'h00000001);
      uart_word(32'h00004013, 32'h00000a81);
      uart_word(32'h00004014, 32'h0fff0003);
      uart_word(32'h00004015, 32'h0c018040);
      uart_word(32'h00004016, 32'h00050003);
      uart_word(32'h00004017, 32'h00000004);
      uart_word(32'h00004018, 32'h10018040);
      uart_word(32'h00004019, 32'hffff0003);
      uart_word(32'h0000401a, 32'hffff0004);
      uart_word(32'h0000401b, 32'h14018040);
      uart_word(32'h0000401c, 32'h0000000b);
      uart_word(32'h0000401d, 32'h00000001);
      uart_word(32'h0000401e, 32'h00000001);
      uart_word(32'h0000401f, 32'h00000001);
      uart_word(32'h00004020, 32'h1c621040);
      uart_word(32'h00004021, 32'h2081c040);
      uart_word(32'h00004022, 32'h2081c040);
      uart_word(32'h00004023, 32'h20010003);
      uart_word(32'h00004024, 32'h00000004);
      uart_word(32'h00004025, 32'h00000001);
      uart_word(32'h00004026, 32'h00000001);
      uart_word(32'h00004027, 32'h0001c205);
      uart_word(32'h00004028, 32'hfff90003);
      uart_word(32'h00004029, 32'hffff0004);
      uart_word(32'h0000402a, 32'h00000008);
      uart_word(32'h0000402b, 32'h00000001);
      uart_word(32'h0000402c, 32'h00000001);
      uart_word(32'h0000402d, 32'h00000001);
      uart_word(32'h0000402e, 32'h20010003);
      uart_word(32'h0000402f, 32'h00000004);
      uart_word(32'h00004030, 32'h00000001);
      uart_word(32'h00004031, 32'h00000001);
      uart_word(32'h00004032, 32'h00018205);
      uart_word(32'h00004033, 32'hfff90003);
      uart_word(32'h00004034, 32'hffff0004);
      uart_word(32'h00004035, 32'h00000008);
      uart_word(32'h00004036, 32'h00000001);
      uart_word(32'h00004037, 32'h00000001);
      uart_word(32'h00004038, 32'h00000001);
      uart_word(32'h00004039, 32'h0461c040);
      uart_word(32'h0000403a, 32'hffd30003);
      uart_word(32'h0000403b, 32'hffff0004);
      uart_word(32'h0000403c, 32'h00000008);
      uart_word(32'h0000403d, 32'h00000001);
      uart_word(32'h0000403e, 32'h00000001);
      uart_word(32'h0000403f, 32'h00000001);
      // program end here --------

      // data start here --------
      uart_word(32'h00000000, 32'h00000000);
      // data end here --------

      uart_word(32'h00005002, 32'h00000001); // cpu master
      uart_word(32'h00005000, 32'h00000000); // cpu reset off
      uart_word(32'h00005001, 32'h00000001); // resume on
      uart_word(32'h00005001, 32'h00000000); // resume off

    end

`endif

`ifdef USE_AUDIO

  // generate clka
  initial
    begin
      clka = 1'b1;
      forever
        begin
          #(STEPA / 2) clka = ~clka;
        end
    end

  // generate reseta signal
  initial
    begin
      reseta = 1'b0;
      repeat (2) @(posedge clka) reseta <= 1'b1;
      @(posedge clka) reseta <= 1'b0;
    end

`endif

`ifdef USE_VGA

  // generate clkv
  initial
    begin
      clkv = 1'b1;
      forever
        begin
          #(STEPV / 2) clkv = ~clkv;
        end
    end

  // generate resetv signal
  initial
    begin
      resetv = 1'b0;
      repeat (2) @(posedge clkv) resetv <= 1'b1;
      @(posedge clkv) resetv <= 1'b0;
    end

`endif

endmodule
