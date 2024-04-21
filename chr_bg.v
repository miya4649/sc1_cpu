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

module chr_bg
  #(
    parameter CHR_SIZE_BITS = 6,
    parameter BITMAP_BITS = 2
    )
  (
   input wire         clk,
   input wire         reset,

   output wire [31:0] chr_length,
   input wire [31:0]  chr_address,
   input wire [7:0]   chr_din,
   output wire [7:0]  chr_dout,
   input wire         chr_we,
   input wire         chr_oe,

   output wire [31:0] bitmap_length,
   input wire [31:0]  bitmap_address,
   input wire [7:0]   bitmap_din,
   output wire [7:0]  bitmap_dout,
   input wire         bitmap_we,
   input wire         bitmap_oe,

   input wire [31:0]  x,
   input wire [31:0]  y,
   input wire [31:0]  scale,
   input wire [31:0]  palette0,
   input wire [31:0]  palette1,
   input wire [31:0]  palette2,
   input wire [31:0]  palette3,

   input wire         ext_clkv,
   input wire         ext_resetv,
   output reg [7:0]   ext_color,
   input wire [31:0]  ext_count_h,
   input wire [31:0]  ext_count_v
   );

  localparam INT_BITS = 32;
  localparam BPP = 8;
  localparam CHR_BITS = 8;
  localparam OFFSET_BITS = 16;
  localparam ADDR_BITS = (CHR_SIZE_BITS * 2);
  localparam SCALE_BITS_BITS = 4;
  localparam SCALE_BITS = (1 << SCALE_BITS_BITS);
  localparam SCALE_DIV_BITS = 8;
  localparam CHR_SIZE = (1 << CHR_SIZE_BITS);
  localparam BITMAP_ADDR_BITS = 12;
  localparam BITMAP_DATA_BITS = 2;
  localparam BITMAP_SIZE_BITS = 3;

  // return const value
  assign chr_length = 1 << ADDR_BITS;
  assign chr_dout = 1'd0;
  assign bitmap_length = 1 << ADDR_BITS;
  assign bitmap_dout = 1'd0;

  // logic
  reg [OFFSET_BITS+SCALE_BITS-1:0] dx0_d1;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dy0_d1;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dx1_d2;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dy1_d2;
  reg [ADDR_BITS-1:0]              chr_raddr_d3;
  wire [CHR_BITS-1:0]              chr_name_d5;
  wire [CHR_BITS-1:0]              chr_name_d8;
  wire [OFFSET_BITS-1:0]           x_sync;
  wire [OFFSET_BITS-1:0]           y_sync;
  wire [SCALE_BITS_BITS-1:0]       scale_sync;
  wire [INT_BITS-1:0]              palette0_sync;
  wire [INT_BITS-1:0]              palette1_sync;
  wire [INT_BITS-1:0]              palette2_sync;
  wire [INT_BITS-1:0]              palette3_sync;
  reg [SCALE_BITS_BITS-1:0]        scale_sync_d1;
  reg [BITMAP_ADDR_BITS-1:0]       bitmap_addr0_d3;
  wire [BITMAP_ADDR_BITS-1:0]      bitmap_addr0_d5;
  reg [BITMAP_ADDR_BITS-1:0]       bitmap_addr1_d6;
  wire [BITMAP_DATA_BITS-1:0]      bitmap_data_d8;

  always @(posedge ext_clkv)
    begin
      dx0_d1 <= ext_count_h - x_sync;
      dy0_d1 <= ext_count_v - y_sync;
      scale_sync_d1 <= scale_sync;
      dx1_d2 <= (dx0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      dy1_d2 <= (dy0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      chr_raddr_d3 <= (dy1_d2[CHR_SIZE_BITS+BITMAP_SIZE_BITS-1:BITMAP_SIZE_BITS] << CHR_SIZE_BITS) + dx1_d2[CHR_SIZE_BITS+BITMAP_SIZE_BITS-1:BITMAP_SIZE_BITS];
      bitmap_addr0_d3 <= (dy1_d2[BITMAP_SIZE_BITS-1:0] << BITMAP_SIZE_BITS) + dx1_d2[BITMAP_SIZE_BITS-1:0];
      bitmap_addr1_d6 <= (chr_name_d5 << (BITMAP_SIZE_BITS << 1)) + bitmap_addr0_d5;
      case ({chr_name_d8[7:6], bitmap_data_d8})
        // ext_color: 9 cycle delay
        4'b0000: ext_color <= palette0_sync[7:0];
        4'b0001: ext_color <= palette0_sync[15:8];
        4'b0010: ext_color <= palette0_sync[23:16];
        4'b0011: ext_color <= palette0_sync[31:24];

        4'b0100: ext_color <= palette1_sync[7:0];
        4'b0101: ext_color <= palette1_sync[15:8];
        4'b0110: ext_color <= palette1_sync[23:16];
        4'b0111: ext_color <= palette1_sync[31:24];

        4'b1000: ext_color <= palette2_sync[7:0];
        4'b1001: ext_color <= palette2_sync[15:8];
        4'b1010: ext_color <= palette2_sync[23:16];
        4'b1011: ext_color <= palette2_sync[31:24];

        4'b1100: ext_color <= palette3_sync[7:0];
        4'b1101: ext_color <= palette3_sync[15:8];
        4'b1110: ext_color <= palette3_sync[23:16];
        4'b1111: ext_color <= palette3_sync[31:24];
      endcase
    end

  // Display Data RAM
  dual_clk_ram
    #(
      .DATA_WIDTH (CHR_BITS),
      .ADDR_WIDTH (ADDR_BITS)
      )
  dual_clk_ram_0
    (
     .data_in (chr_din),
     .read_addr (chr_raddr_d3),
     .write_addr (chr_address),
     .we (chr_we),
     .read_clock (ext_clkv),
     .write_clock (clk),
     .data_out (chr_name_d5)
     );

  // Character Generator RAM
  dual_clk_ram
    #(
      .DATA_WIDTH (BITMAP_BITS),
      .ADDR_WIDTH (ADDR_BITS)
      )
  dual_clk_ram_1
    (
     .data_in (bitmap_din),
     .read_addr (bitmap_addr1_d6),
     .write_addr (bitmap_address),
     .we (bitmap_we),
     .read_clock (ext_clkv),
     .write_clock (clk),
     .data_out (bitmap_data_d8)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (OFFSET_BITS)
      )
  cdc_synchronizer_x
    (
     .clk (ext_clkv),
     .data_in (x[OFFSET_BITS-1:0]),
     .data_out (x_sync)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (OFFSET_BITS)
      )
  cdc_synchronizer_y
    (
     .clk (ext_clkv),
     .data_in (y[OFFSET_BITS-1:0]),
     .data_out (y_sync)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (SCALE_BITS)
      )
  cdc_synchronizer_scale
    (
     .clk (ext_clkv),
     .data_in (scale[SCALE_BITS-1:0]),
     .data_out (scale_sync)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (INT_BITS)
      )
  cdc_synchronizer_palette0
    (
     .clk (ext_clkv),
     .data_in (palette0),
     .data_out (palette0_sync)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (INT_BITS)
      )
  cdc_synchronizer_palette1
    (
     .clk (ext_clkv),
     .data_in (palette1),
     .data_out (palette1_sync)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (INT_BITS)
      )
  cdc_synchronizer_palette2
    (
     .clk (ext_clkv),
     .data_in (palette2),
     .data_out (palette2_sync)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (INT_BITS)
      )
  cdc_synchronizer_palette3
    (
     .clk (ext_clkv),
     .data_in (palette3),
     .data_out (palette3_sync)
     );

  shift_register_vector
    #(
      .WIDTH (CHR_BITS),
      .DEPTH (3)
      )
  shift_register_vector_chr_name_d8
    (
     .clk (ext_clkv),
     .data_in (chr_name_d5),
     .data_out (chr_name_d8)
     );

  shift_register_vector
    #(
      .WIDTH (BITMAP_ADDR_BITS),
      .DEPTH (2)
      )
  shift_register_vector_bitmap_addr0_d5
    (
     .clk (ext_clkv),
     .data_in (bitmap_addr0_d3),
     .data_out (bitmap_addr0_d5)
     );

endmodule
