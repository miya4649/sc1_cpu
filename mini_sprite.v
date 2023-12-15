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

module mini_sprite
  #(
    parameter SPRITE_WIDTH_BITS = 6,
    parameter SPRITE_HEIGHT_BITS = 7,
    parameter BPP = 8
    )
  (
   input wire                   clk,
   input wire                   reset,

   output wire signed [32-1:0]  bitmap_length,
   input wire [32-1:0]          bitmap_address,
   input wire signed [BPP-1:0]  bitmap_din,
   output wire signed [BPP-1:0] bitmap_dout,
   input wire                   bitmap_we,
   input wire                   bitmap_oe,

   input wire signed [32-1:0]   x,
   input wire signed [32-1:0]   y,
   input wire signed [32-1:0]   scale,

   output reg signed [BPP-1:0]  ext_color,
   input wire signed [32-1:0]   ext_count_h,
   input wire signed [32-1:0]   ext_count_v
   );

  localparam OFFSET_BITS = 16;
  localparam ADDR_BITS = (SPRITE_WIDTH_BITS + SPRITE_HEIGHT_BITS);
  localparam SCALE_BITS_BITS = 4;
  localparam SCALE_BITS = (1 << SCALE_BITS_BITS);
  localparam SCALE_DIV_BITS = 8;
  localparam SPRITE_WIDTH = (1 << SPRITE_WIDTH_BITS);
  localparam SPRITE_HEIGHT = (1 << SPRITE_HEIGHT_BITS);

  // return const value
  assign bitmap_length = 1 << ADDR_BITS;
  assign bitmap_dout = 1'd0;

  // inside the sprite
  wire                       vp_sprite_inside_d2;
  wire                       vp_sprite_inside_d4;
  assign vp_sprite_inside_d2 = ((dx1_d2 >= 0) &&
                                (dx1_d2 < SPRITE_WIDTH) &&
                                (dy1_d2 >= 0) &&
                                (dy1_d2 < SPRITE_HEIGHT));

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

  always @(posedge clk)
    begin
      dx0_d1 <= ext_count_h - x_sync;
      dy0_d1 <= ext_count_v - y_sync;
      scale_sync_d1 <= scale_sync;
      dx1_d2 <= (dx0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      dy1_d2 <= (dy0_d1 << scale_sync_d1) >> SCALE_DIV_BITS;
      bitmap_raddr_d3 <= {dy1_d2[SPRITE_HEIGHT_BITS-1:0], {SPRITE_WIDTH_BITS{1'b0}}} + dx1_d2[SPRITE_WIDTH_BITS-1:0];
      color_d5 <= vp_sprite_inside_d4 ? bitmap_color_d4 : 1'd0;
      color_d6 <= color_d5;
      ext_color <= color_d6; // ext_color: delay 7
    end

  // bitmap
  rw_port_ram
    #(
      .DATA_WIDTH (BPP),
      .ADDR_WIDTH (ADDR_BITS)
      )
  rw_port_ram_0
    (
     .clk (clk),
     .addr_r (bitmap_raddr_d3),
     .addr_w (bitmap_address),
     .data_in (bitmap_din),
     .we (bitmap_we),
     .data_out (bitmap_color_d4)
     );

  assign x_sync = x[OFFSET_BITS-1:0];
  assign y_sync = y[OFFSET_BITS-1:0];
  assign scale_sync = scale[OFFSET_BITS-1:0];

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (2)
      )
  shift_register_vector_vp_sprite_inside_d4
    (
     .clk (clk),
     .data_in (vp_sprite_inside_d2),
     .data_out (vp_sprite_inside_d4)
     );

endmodule
