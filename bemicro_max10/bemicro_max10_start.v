/*
  Copyright (c) 2015-2018, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`default_nettype none
`define USE_UART
`define USE_TIMER
`define USE_I2C

module bemicro_max10_start
  (
   input        SYS_CLK,
   output [7:0] USER_LED,
`ifdef USE_UART
   // UART
   output       GPIO_J4_35,
   input        GPIO_J4_37,
`endif
`ifdef USE_I2C
   // I2C
   inout        I2C_SCL,
   inout        I2C_SDA,
`endif
   input [3:0]  PB
   );

  localparam UART_CLK_HZ = 40000000;
  localparam UART_SCLK_HZ = 115200;
  localparam UART_COUNTER_WIDTH = 9;
  localparam I2C_CLK_HZ = 40000000;
  localparam I2C_SCLK_HZ = 100000;
  localparam WIDTH_D = 32;
  // for small FPGAs
  localparam DEPTH_I = 10;
  localparam DEPTH_D = 9;

  // LED
  wire [7:0]    led;
  assign USER_LED = ~led;

  // generate reset signal
  wire         RESET_N;
  reg          reset;
  reg          reset1;
  reg          resetpll;
  reg          resetpll1;
  assign RESET_N = PB[0];

  always @(posedge SYS_CLK)
    begin
      resetpll1 <= ~RESET_N;
      resetpll <= resetpll1;
    end

  always @(posedge SYS_CLK)
    begin
      reset1 <= ~pll_locked;
      reset <= reset1;
    end

  wire          clk;
  wire          pll_locked;

  simple_pll simple_pll_0
    (
     .areset (resetpll),
     .inclk0 (SYS_CLK),
     .c0 (clk),
     .locked (pll_locked)
     );

  sc1_soc
    #(
      .UART_CLK_HZ (UART_CLK_HZ),
      .UART_SCLK_HZ (UART_SCLK_HZ),
      .UART_COUNTER_WIDTH (UART_COUNTER_WIDTH),
      .I2C_CLK_HZ (I2C_CLK_HZ),
      .I2C_SCLK_HZ (I2C_SCLK_HZ),
      .WIDTH_D (WIDTH_D),
      .DEPTH_I (DEPTH_I),
      .DEPTH_D (DEPTH_D)
      )
  sc1_soc_0
    (
`ifdef USE_UART
     .uart_rxd (uart_rxd),
     .uart_txd (uart_txd),
`endif
`ifdef USE_AUDIO
     .clka (clka),
     .reseta (reseta),
     .audio_r (audio_r),
     .audio_l (audio_l),
`endif
`ifdef USE_VGA
     .clkv (clkv),
     .resetv (resetv),
     .vga_hs (VGA_HS),
     .vga_vs (VGA_VS),
     .vga_r (VGA_R_in),
     .vga_g (VGA_G_in),
     .vga_b (VGA_B_in),
`endif
`ifdef USE_I2C
     .i2c_scl (I2C_SCL),
     .i2c_sda (I2C_SDA),
`endif
     .clk (clk),
     .reset (reset),
     .led (led)
   );

`ifdef USE_UART

  // uart
  wire          uart_txd;
  wire          uart_rxd;
  assign GPIO_J4_35 = uart_txd;
  assign uart_rxd = GPIO_J4_37;

`endif

endmodule
