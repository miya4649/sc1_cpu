/*
  Copyright (c) 2016 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`include "../topinclude.v"
`define USE_I2C

/*
 GPIO Pinout:
 UART TXD: GPIO_0 Pin35 (GPIO_0[30])
 UART RXD: GPIO_0 Pin37 (GPIO_0[32])
 I2C SCL : GPIO_0 Pin36 (GPIO_0[31])
 I2C SDA : GPIO_0 Pin38 (GPIO_0[33])
 AUDIO R : GPIO_1 Pin38 (GPIO_1[33])
 AUDIO L : GPIO_1 Pin40 (GPIO_1[35])
*/

module top
  (
   input        CLOCK_50,
   input        RESET_N,
   output [6:0] HEX0,
   output [6:0] HEX1,
   output [6:0] HEX2,
   output [6:0] HEX3,
   output [6:0] HEX4,
   output [6:0] HEX5,
`ifdef USE_VGA
   output       VGA_HS,
   output       VGA_VS,
   output [3:0] VGA_R,
   output [3:0] VGA_G,
   output [3:0] VGA_B,
`endif
   output [9:0] LEDR,
   inout [35:0] GPIO_0,
   inout [35:0] GPIO_1
   );

  localparam UART_CLK_HZ = 50000000;
  localparam UART_SCLK_HZ = 115200;
  localparam I2C_CLK_HZ = 50000000;
  localparam I2C_SCLK_HZ = 100000;
  localparam WIDTH_D = 32;
  localparam DEPTH_I = 12;
  localparam DEPTH_D = 12;

  wire          pll_locked;

  // unused GPIO
  assign GPIO_0[29:0] = 30'bz;
  assign GPIO_0[35:34] = 2'bz;
  assign GPIO_1[32:0] = 33'bz;
  assign GPIO_1[34] = 1'bz;

`ifndef USE_UART
  assign GPIO_0[30] = 1'bz;
  assign GPIO_0[32] = 1'bz;
`endif

`ifndef USE_I2C
  assign GPIO_0[31] = 1'bz;
  assign GPIO_0[33] = 1'bz;
`endif

`ifndef USE_AUDIO
  assign GPIO_1[33] = 1'bz;
  assign GPIO_1[35] = 1'bz;
`endif

  // generate reset signal (push button 1)
  reg           reset;
  reg           reset1;
  reg           resetpll;
  reg           resetpll1;

  always @(posedge CLOCK_50)
    begin
      resetpll1 <= ~RESET_N;
      resetpll <= resetpll1;
    end

  always @(posedge CLOCK_50)
    begin
      reset1 <= ~pll_locked;
      reset <= reset1;
    end

  av_pll av_pll_0
    (
     .refclk (CLOCK_50),
     .rst (resetpll),
`ifdef USE_VGA
     .outclk_0 (clkv),
`endif
`ifdef USE_AUDIO
     .outclk_1 (clka),
`endif
     .locked (pll_locked)
     );

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
     .vga_color_out (VGA_COLOR_in),
`endif
`ifdef USE_I2C
     .i2c_scl (GPIO_0[31]),
     .i2c_sda (GPIO_0[33]),
`endif
     .clk (CLOCK_50),
     .reset (reset),
     .led (LEDR)
   );

`ifdef USE_UART
  // uart
  wire          uart_txd;
  wire          uart_rxd;
  assign GPIO_0[30] = uart_txd;
  assign uart_rxd = GPIO_0[32];
`endif

`ifdef USE_AUDIO
  wire          clka;
  reg           reseta;
  reg           reseta1;
  // audio output port
  wire          audio_r;
  wire          audio_l;
  assign GPIO_1[33] = audio_r;
  assign GPIO_1[35] = audio_l;

  always @(posedge clka)
    begin
      reseta1 <= ~pll_locked;
      reseta <= reseta1;
    end
`endif

`ifdef USE_VGA
  wire          clkv;
  reg           resetv;
  reg           resetv1;
  // truncate RGB data
  wire [7:0]    VGA_COLOR_in;
  assign VGA_R = {VGA_COLOR_in[7:5], VGA_COLOR_in[5]};
  assign VGA_G = {VGA_COLOR_in[4:2], VGA_COLOR_in[2]};
  assign VGA_B = {VGA_COLOR_in[1:0], {2{VGA_COLOR_in[0]}}};

  always @(posedge clkv)
    begin
      resetv1 <= ~pll_locked;
      resetv <= resetv1;
    end
`endif

`ifdef USE_HEX_LED
  wire [23:0]    hex_led;
  assign HEX5 = get_hex(hex_led[23:20]);
  assign HEX4 = get_hex(hex_led[19:16]);
  assign HEX3 = get_hex(hex_led[15:12]);
  assign HEX2 = get_hex(hex_led[11:8]);
  assign HEX1 = get_hex(hex_led[7:4]);
  assign HEX0 = get_hex(hex_led[3:0]);

  function [6:0] get_hex
    (
     input [3:0] count
     );
    begin
      case (count)
        4'h0: get_hex = 7'b1000000;
        4'h1: get_hex = 7'b1111001;
        4'h2: get_hex = 7'b0100100;
        4'h3: get_hex = 7'b0110000;
        4'h4: get_hex = 7'b0011001;
        4'h5: get_hex = 7'b0010010;
        4'h6: get_hex = 7'b0000010;
        4'h7: get_hex = 7'b1011000;
        4'h8: get_hex = 7'b0000000;
        4'h9: get_hex = 7'b0010000;
        4'ha: get_hex = 7'b0001000;
        4'hb: get_hex = 7'b0000011;
        4'hc: get_hex = 7'b1000110;
        4'hd: get_hex = 7'b0100001;
        4'he: get_hex = 7'b0000110;
        4'hf: get_hex = 7'b0001110;
        default: get_hex = 7'bx;
      endcase
    end
  endfunction
`else
  // turn off hex leds
  assign HEX0 = 7'b1111111;
  assign HEX1 = 7'b1111111;
  assign HEX2 = 7'b1111111;
  assign HEX3 = 7'b1111111;
  assign HEX4 = 7'b1111111;
  assign HEX5 = 7'b1111111;
`endif

endmodule
