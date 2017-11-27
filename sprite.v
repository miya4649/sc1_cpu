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

module sprite
  #(
    parameter SPRITE_SIZE_BITS = 6
    )
  (
   input                     clk,
   input                     reset,

   output signed [32-1:0]    bitmap_length,
   input signed [32-1:0]     bitmap_address,
   input signed [8-1:0]      bitmap_din,
   output signed [8-1:0]     bitmap_dout,
   input                     bitmap_we,
   input                     bitmap_oe,

   input signed [32-1:0]     x,
   input signed [32-1:0]     y,
   input signed [32-1:0]     scale,

   input                     ext_clkv,
   input                     ext_resetv,
   output reg signed [8-1:0] ext_color,
   input signed [32-1:0]     ext_count_h,
   input signed [32-1:0]     ext_count_v
   );

  localparam VGA_WIDTH = 640;
  localparam VGA_HEIGHT = 480;
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

  // inside the sprite
  wire                       vp_sprite_inside_d2;
  wire                       vp_sprite_inside_d4;
  assign vp_sprite_inside_d2 = ((dx1_d2 >= 0) &&
                                (dx1_d2 < SPRITE_SIZE) &&
                                (dy1_d2 >= 0) &&
                                (dy1_d2 < SPRITE_SIZE));

  // sprite logic
  reg [BPP-1:0]              color;
  reg signed [OFFSET_BITS+SCALE_BITS-1:0] dx0_d1;
  reg signed [OFFSET_BITS+SCALE_BITS-1:0] dy0_d1;
  reg signed [OFFSET_BITS+SCALE_BITS-1:0] dx1_d2;
  reg signed [OFFSET_BITS+SCALE_BITS-1:0] dy1_d2;
  reg [ADDR_BITS-1:0]                     bitmap_raddr_d3;
  wire [BPP-1:0]                          bitmap_color_d4;
  wire signed [OFFSET_BITS-1:0]           x_sync;
  wire signed [OFFSET_BITS-1:0]           y_sync;
  wire [SCALE_BITS_BITS-1:0]              scale_sync;
  reg [SCALE_BITS_BITS-1:0]               scale_sync_d1;
  reg signed [BPP-1:0]                    color_d5;
  reg signed [BPP-1:0]                    color_d6;

  always @(posedge ext_clkv)
    begin
      dx0_d1 <= ext_count_h - x_sync;
      dy0_d1 <= ext_count_v - y_sync;
      scale_sync_d1 <= scale_sync;
      dx1_d2 <= (dx0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      dy1_d2 <= (dy0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      bitmap_raddr_d3 <= (dy1_d2[SPRITE_SIZE_BITS-1:0] << SPRITE_SIZE_BITS) + dx1_d2[SPRITE_SIZE_BITS-1:0];
      color_d5 <= vp_sprite_inside_d4 ? bitmap_color_d4 : 1'd0;
      color_d6 <= color_d5;
      ext_color <= color_d6; // ext_color: delay 7
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
     .data_out (bitmap_color_d4)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (OFFSET_BITS)
      )
  cdc_synchronizer_x
    (
     .clk_in (clk),
     .clk_out (ext_clkv),
     .data_in (x[OFFSET_BITS-1:0]),
     .data_out (x_sync),
     .reset_in (reset)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (OFFSET_BITS)
      )
  cdc_synchronizer_y
    (
     .clk_in (clk),
     .clk_out (ext_clkv),
     .data_in (y[OFFSET_BITS-1:0]),
     .data_out (y_sync),
     .reset_in (reset)
     );

  cdc_synchronizer
    #(
      .DATA_WIDTH (SCALE_BITS)
      )
  cdc_synchronizer_scale
    (
     .clk_in (clk),
     .clk_out (ext_clkv),
     .data_in (scale[SCALE_BITS-1:0]),
     .data_out (scale_sync),
     .reset_in (reset)
     );

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (2)
      )
  shift_register_vector_vp_sprite_inside_d4
    (
     .clk (ext_clkv),
     .data_in (vp_sprite_inside_d2),
     .data_out (vp_sprite_inside_d4)
     );

endmodule
