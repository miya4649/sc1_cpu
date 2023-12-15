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

module sprite
  #(
    parameter SPRITE_SIZE_BITS = 6
    )
  (
   input wire         clk,
   input wire         reset,

   output wire [31:0] bitmap_length,
   input wire [31:0]  bitmap_address,
   input wire [7:0]   bitmap_din,
   output wire [7:0]  bitmap_dout,
   input wire         bitmap_we,
   input wire         bitmap_oe,

   input wire [31:0]  x,
   input wire [31:0]  y,
   input wire [31:0]  scale,

   input wire         ext_clkv,
   input wire         ext_resetv,
   output wire [7:0]  ext_color,
   input wire [31:0]  ext_count_h,
   input wire [31:0]  ext_count_v
   );

  localparam BPP = 8;
  localparam OFFSET_BITS = 16;
  localparam ADDR_BITS = (SPRITE_SIZE_BITS * 2);
  localparam SCALE_BITS_BITS = 4;
  localparam SCALE_BITS = (1 << SCALE_BITS_BITS);
  localparam SCALE_DIV_BITS = 8;
  localparam SPRITE_SIZE = (1 << SPRITE_SIZE_BITS);

  // return const value
  assign bitmap_length = 1 << ADDR_BITS;
  assign bitmap_dout = 1'd0;

  // sprite logic
  reg [BPP-1:0]       color;
  reg [ADDR_BITS-1:0] bitmap_raddr_d3;
  wire [BPP-1:0]      bitmap_color_d5;
  wire [OFFSET_BITS-1:0] x_sync;
  wire [OFFSET_BITS-1:0] y_sync;
  wire [SCALE_BITS_BITS-1:0] scale_sync;
  reg [SCALE_BITS_BITS-1:0]  scale_sync_d1;
  reg [BPP-1:0]              color_d6;
  wire                       vp_sprite_inside_d2;
  wire                       vp_sprite_inside_d5;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dx0_d1;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dy0_d1;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dx1_d2;
  reg [OFFSET_BITS+SCALE_BITS-1:0] dy1_d2;
  
  // inside the sprite
  assign vp_sprite_inside_d2 = ((dx1_d2 >= 0) &&
                                (dx1_d2 < SPRITE_SIZE) &&
                                (dy1_d2 >= 0) &&
                                (dy1_d2 < SPRITE_SIZE));

  always @(posedge ext_clkv)
    begin
      dx0_d1 <= ext_count_h - x_sync;
      dy0_d1 <= ext_count_v - y_sync;
      scale_sync_d1 <= scale_sync;
      dx1_d2 <= (dx0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      dy1_d2 <= (dy0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      bitmap_raddr_d3 <= (dy1_d2[SPRITE_SIZE_BITS-1:0] << SPRITE_SIZE_BITS) + dx1_d2[SPRITE_SIZE_BITS-1:0];
      color_d6 <= vp_sprite_inside_d5 ? bitmap_color_d5 : 1'd0;
    end

  // bitmap
  dual_clk_ram
    #(
      .DATA_WIDTH (BPP),
      .ADDR_WIDTH (ADDR_BITS)
      )
  dual_clk_ram_0
    (
     .data_in (bitmap_din),
     .read_addr (bitmap_raddr_d3),
     .write_addr (bitmap_address),
     .we (bitmap_we),
     .read_clock (ext_clkv),
     .write_clock (clk),
     .data_out (bitmap_color_d5)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (OFFSET_BITS)
      )
  cdc_synchronizer_x
    (
     .clk (ext_clkv),
     .data_in (x[OFFSET_BITS-1:0]),
     .data_out (x_sync),
     .reset (ext_resetv)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (OFFSET_BITS)
      )
  cdc_synchronizer_y
    (
     .clk (ext_clkv),
     .data_in (y[OFFSET_BITS-1:0]),
     .data_out (y_sync),
     .reset (ext_resetv)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (SCALE_BITS)
      )
  cdc_synchronizer_scale
    (
     .clk (ext_clkv),
     .data_in (scale[SCALE_BITS-1:0]),
     .data_out (scale_sync),
     .reset (ext_resetv)
     );

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (3)
      )
  shift_register_vector_vp_sprite_inside_d5
    (
     .clk (ext_clkv),
     .data_in (vp_sprite_inside_d2),
     .data_out (vp_sprite_inside_d5)
     );

  // ext_color: delay 9
  shift_register_vector
    #(
      .WIDTH (BPP),
      .DEPTH (3)
      )
  shift_register_vector_ext_color
    (
     .clk (ext_clkv),
     .data_in (color_d6),
     .data_out (ext_color)
     );

endmodule
