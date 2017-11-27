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

/*
 Video Audio Controller Interface Board
 http://cellspe.matrix.jp/zerofpga/avinterface.html
 
 KEY5: GPIO_Pin24: DIFF_RX_N8
 KEY4: GPIO_Pin26: DIFF_RX_N7
 KEY3: GPIO_Pin28: DIFF_RX_N6
 KEY2: GPIO_Pin32: DIFF_RX_N5
 KEY1: GPIO_Pin34: DIFF_RX_N4
 KEY0: GPIO_Pin36: DIFF_RX_N3
 
 AUDIO_R: GPIO_Pin38: DIFF_RX_N2
 AUDIO_L: GPIO_Pin40: DIFF_RX_N1
 
 VGA_R0: GPIO_Pin:2 GPIO2
 VGA_R1: GPIO_Pin:4 GPIO4
 VGA_G0: GPIO_Pin:6 GPIO6
 VGA_G1: GPIO_Pin:8 GPIO8
 VGA_B0: GPIO_Pin:10 GPIO_D
 VGA_B1: GPIO_Pin:14 DIFF_TX_N9
 VGA_HS: GPIO_Pin:16 LVDS_TX_O_P3
 VGA_VS: GPIO_Pin:18 LVDS_TX_O_P0
*/

`include "../topinclude.v"

module top
  (
   // KEY
   /*
   input        DIFF_RX_N8,
   input        DIFF_RX_N7,
   input        DIFF_RX_N6,
   input        DIFF_RX_N5,
   input        DIFF_RX_N4,
   input        DIFF_RX_N3,
    */
`ifdef USE_UART
   output       LVDS_TX_O_N2,
   input        LVDS_TX_O_N1,
`endif
`ifdef USE_AUDIO
   // AUDIO
   output       DIFF_RX_N2,
   output       DIFF_RX_N1,
`endif
`ifdef USE_VGA
   // VGA
   output       GPIO2,
   output       GPIO4,
   output       GPIO6,
   output       GPIO8,
   output       GPIO_D,
   output       DIFF_TX_N9,
   output       LVDS_TX_O_P3,
   output       LVDS_TX_O_P0,
`endif
   input        CLK_24MHZ,
   output [7:0] USER_LED,
   input [0:0]  TACT
   );

  localparam UART_CLK_HZ = 50400000;
  localparam UART_SCLK_HZ = 115200;
  localparam UART_COUNTER_WIDTH = 9;
  localparam WIDTH_D = 32;
  localparam DEPTH_I = 12;
  localparam DEPTH_D = 12;

  wire          clk;
  wire          pll_locked;

  // generate reset signal
  wire       RESET_N;
  reg        reset;
  reg        reset1;
  reg        resetpll;
  reg        resetpll1;
  assign RESET_N = TACT[0];

  always @(posedge CLK_24MHZ)
    begin
      resetpll1 <= ~RESET_N;
      resetpll <= resetpll1;
    end

  always @(posedge clk)
    begin
      reset1 <= ~pll_locked;
      reset <= reset1;
    end


  av_pll av_pll_0
    (
     .refclk (CLK_24MHZ),
     .rst (resetpll),
`ifdef USE_VGA
     .outclk_0 (clkv),
`endif
`ifdef USE_AUDIO
     .outclk_1 (clka),
`endif
     .outclk_2 (clk),
     .locked (pll_locked)
     );

  sc1_soc
    #(
      .UART_CLK_HZ (UART_CLK_HZ),
      .UART_SCLK_HZ (UART_SCLK_HZ),
      .UART_COUNTER_WIDTH (UART_COUNTER_WIDTH),
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
     .clk (clk),
     .reset (reset),
     .led (led)
   );

  // LED
  wire [WIDTH_D-1:0]   led;
  assign USER_LED = ~led[7:0];

`ifdef USE_UART

  // uart
  wire          uart_txd;
  wire          uart_rxd;
  assign LVDS_TX_O_N2 = uart_txd;
  assign uart_rxd = LVDS_TX_O_N1;

`endif

`ifdef USE_AUDIO

  wire          clka;
  reg           reseta;
  reg           reseta1;
  // audio output port
  wire          audio_r;
  wire          audio_l;
  assign DIFF_RX_N2 = audio_r;
  assign DIFF_RX_N1 = audio_l;

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
  // VGA port
  wire [7:0] VGA_R_in;
  wire [7:0] VGA_G_in;
  wire [7:0] VGA_B_in;
  wire       VGA_HS;
  wire       VGA_VS;
  assign GPIO2 = VGA_R_in[2];
  assign GPIO4 = VGA_R_in[3];
  assign GPIO6 = VGA_G_in[2];
  assign GPIO8 = VGA_G_in[3];
  assign GPIO_D = VGA_B_in[2];
  assign DIFF_TX_N9 = VGA_B_in[3];
  assign LVDS_TX_O_P3 = VGA_HS;
  assign LVDS_TX_O_P0 = VGA_VS;

  always @(posedge clkv)
    begin
      resetv1 <= ~pll_locked;
      resetv <= resetv1;
    end

`endif

endmodule
