/*
  Copyright (c) 2015-2017, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`define USE_UART

module top
  (
   input        CLK,
`ifdef USE_UART
   input        UART_RXD,
   output       UART_TXD,
`endif
   output [7:0] LED
   );

  localparam UART_CLK_HZ = 12000000;
  localparam UART_SCLK_HZ = 115200;
  localparam UART_COUNTER_WIDTH = 9;
  localparam WIDTH_D = 32;
  // for small FPGAs
  localparam DEPTH_I = 10;
  localparam DEPTH_D = 10;
  localparam DEPTH_V = 15;
  localparam RESET_TIMER_BIT = 24;

  wire [WIDTH_D-1:0] led;
  assign LED = led[7:0];

  reg                  reset = 1'b0;
  reg [31:0]           reset_counter = 32'd0;
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

  // uart
  wire          uart_txd;
  wire          uart_rxd;
  assign UART_TXD = uart_txd;
  assign uart_rxd = UART_RXD;

`endif

  sc1_soc
    #(
      .UART_CLK_HZ (UART_CLK_HZ),
      .UART_SCLK_HZ (UART_SCLK_HZ),
      .UART_COUNTER_WIDTH (UART_COUNTER_WIDTH),
      .WIDTH_D (WIDTH_D),
      .DEPTH_I (DEPTH_I),
      .DEPTH_V (DEPTH_V),
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
     .clk (CLK),
     .reset (reset),
     .led (led)
   );

endmodule
