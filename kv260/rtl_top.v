/*
  Copyright (c) 2023, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

// ver.4

module rtl_top
  (
   input wire         clk,
   input wire         clkv,
   input wire         clka,
   output wire        video_de,
   output wire        video_hsyncn,
   output wire        video_vsyncn,
   output wire [35:0] video_color,
`ifdef USE_XAUDIO
   output wire [31:0] audio_data,
   output wire        audio_id,
   output wire        audio_valid,
   input wire         audio_ready,
`endif
`ifdef USE_UART
   input wire         uart_rxd,
   output wire        uart_txd,
`endif
`ifdef USE_LED
   output wire        led,
`endif
   input wire         resetn
   );

  localparam UART_CLK_HZ = 100000000;
  localparam UART_SCLK_HZ = 115200;
  localparam WIDTH_D = 32;
  localparam DEPTH_I = 12;
  localparam DEPTH_D = 12;

  localparam ZERO = 1'd0;
  localparam ONE = 1'd1;
  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam AUDIO_WIDTH = 16;
  localparam BUFFER_DEPTH = 4;

  wire           vga_hs;
  wire           vga_vs;
  wire [7:0]     vga_color_out;

  // reset
  wire reset;
  wire resetv;
  wire reseta;
  wire resetp;

  assign resetp = ~resetn;

  shift_register
    #(
      .DELAY (3)
      )
  shift_register_reset
    (
     .clk (clk),
     .din (resetp),
     .dout (reset)
     );

  shift_register
    #(
      .DELAY (3)
      )
  shift_register_reseta
    (
     .clk (clka),
     .din (resetp),
     .dout (reseta)
     );

  shift_register
    #(
      .DELAY (3)
      )
  shift_register_resetv
    (
     .clk (clkv),
     .din (resetp),
     .dout (resetv)
     );

  sc1_soc
    #(
      .UART_CLK_HZ (UART_CLK_HZ),
      .UART_SCLK_HZ (UART_SCLK_HZ),
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
`ifdef USE_XAUDIO
     .clka (clka),
     .reseta (reseta),
     .audio_data (audio_data),
     .audio_id (audio_id),
     .audio_valid (audio_valid),
     .audio_ready (audio_ready),
`endif
`ifdef USE_VGA
     .clkv (clkv),
     .resetv (resetv),
     .vga_hs (vga_hs),
     .vga_vs (vga_vs),
     .vga_de (video_de),
     .vga_color_out (vga_color_out),
`endif
     .clk (clk),
     .reset (reset),
     .led (led)
   );

`ifdef USE_VGA
  assign video_hsyncn = ~vga_hs;
  assign video_vsyncn = ~vga_vs;
  assign video_color = {vga_color_out[1:0], {10{vga_color_out[0]}}, vga_color_out[7:5], {9{vga_color_out[5]}}, vga_color_out[4:2], {9{vga_color_out[2]}}};
`endif

endmodule
