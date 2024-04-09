/*
  Copyright (c) 2018 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`define USE_UART
//`define USE_INIT_RAM
//`define USE_TIMER
//`define USE_I2C

`define DISABLE_MUL
`define USE_MINI_AUDIO
`define USE_MINI_VGA

`define MINI_AUDIO_FACTOR_MUL 3
`define MINI_AUDIO_FACTOR_DIV 500
// 1344 - SCALE_H
`define MINI_VGA_MAX_H 1340
// 806 - 1
`define MINI_VGA_MAX_V 805
`define MINI_VGA_WIDTH 1024
`define MINI_VGA_HEIGHT 768
`define MINI_VGA_SYNC_H_START 1048
`define MINI_VGA_SYNC_V_START 771
`define MINI_VGA_SYNC_H_END 1184
`define MINI_VGA_SYNC_V_END 777
`define MINI_VGA_LINE_BITS 12
`define MINI_VGA_COUNTER_BITS 2
`define MINI_VGA_FACTOR_MUL 1
`define MINI_VGA_FACTOR_DIV 1
`define MINI_VGA_SCALE_H 4
`define MINI_VGA_BPP 3
`define MINI_VGA_SPRITE_WIDTH_BITS 7
`define MINI_VGA_SPRITE_HEIGHT_BITS 6
`define MINI_VGA_ENABLE_FRAC_CLK 0

module top
  (
   input  CLK,
   output LED,
`ifdef USE_UART
   output PIN_1,
   input  PIN_2,
`endif
`ifdef USE_MINI_AUDIO
   output PIN_12,
   output PIN_13,
`endif
`ifdef USE_MINI_VGA
   output PIN_18,
   output PIN_19,
   output PIN_21,
   output PIN_22,
   output PIN_23,
`endif
`ifdef USE_I2C
   inout  PIN_14,
   inout  PIN_15,
`endif
   output USBPU
   );

  localparam UART_CLK_HZ = 16000000;
  localparam UART_SCLK_HZ = 115200;
  localparam I2C_CLK_HZ = 16000000;
  localparam I2C_SCLK_HZ = 100000;
  localparam WIDTH_D = 32;
  // for small FPGAs
  localparam DEPTH_I = 10;
  localparam DEPTH_D = 9;

  assign USBPU = 1'b0;

  wire [WIDTH_D-1:0] led_data;
  assign LED = led_data[0];

  // soft reset
  parameter RESET_TIMER_BIT = 24;
  reg           reset = 1'b0;
  reg [31:0]    reset_counter = 32'd0;
  always @(posedge CLK)
    begin
      if (reset_counter[RESET_TIMER_BIT] == 1'b1)
        begin
          reset <= 1'b0;
        end
      else
        begin
          reset <= 1'b1;
          reset_counter <= reset_counter + 1'd1;
        end
    end

`ifdef USE_UART
  wire          uart_txd;
  wire          uart_rxd;
  assign PIN_1 = uart_txd;
  assign uart_rxd = PIN_2;
`endif

`ifdef USE_MINI_AUDIO
  wire          audio_r;
  wire          audio_l;
  assign PIN_13 = audio_r;
  assign PIN_12 = audio_l;
`endif

`ifdef USE_MINI_VGA
  wire          VGA_HS;
  wire          VGA_VS;
  wire [`MINI_VGA_BPP-1 : 0] VGA_COLOR_in;
  assign PIN_18 = VGA_VS;
  assign PIN_19 = VGA_HS;
  assign PIN_21 = VGA_COLOR_in[0];
  assign PIN_22 = VGA_COLOR_in[1];
  assign PIN_23 = VGA_COLOR_in[2];
`endif

  sc1_soc
    #(
      .UART_CLK_HZ (UART_CLK_HZ),
      .UART_SCLK_HZ (UART_SCLK_HZ),
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
`ifdef USE_MINI_AUDIO
     .audio_r (audio_r),
     .audio_l (audio_l),
`endif
`ifdef USE_MINI_VGA
     .vga_hs (VGA_HS),
     .vga_vs (VGA_VS),
     .vga_color_out (VGA_COLOR_in),
`endif
`ifdef USE_I2C
     .i2c_scl (PIN_15),
     .i2c_sda (PIN_14),
`endif
     .clk (CLK),
     .reset (reset),
     .led (led_data)
   );

endmodule
