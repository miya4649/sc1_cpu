/*
  Copyright (c) 2015 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

// FACTOR MUL:DIV = (video_clock / scale_h):clk
// ex. (25.2MHz / 4) * 2:16MHz = 42:53
// COUNTER_BITS >= factor_mul,div bits + 1

module mini_vga
  #(
    // whole line h
    parameter VGA_MAX_H = 800-1,
    // whole line v
    parameter VGA_MAX_V = 525-1,
    // visible area h
    parameter VGA_WIDTH = 640,
    // visible area v
    parameter VGA_HEIGHT = 480,
    // 640 + front porch(16)
    parameter VGA_SYNC_H_START = 656,
    // 480 + front porch(10)
    parameter VGA_SYNC_V_START = 490,
    // 656 + sync pulse(96)
    parameter VGA_SYNC_H_END = 752,
    // 490 + sync pulse(2)
    parameter VGA_SYNC_V_END = 492,
    parameter LINE_BITS = 10,
    parameter COUNTER_BITS = 7,
    parameter BPP = 3,
    parameter ENABLE_FRAC_CLK = 0
    )
  (
   input wire                           clk,
   input wire                           reset,
   input wire signed [COUNTER_BITS-1:0] ext_factor_mul,
   input wire signed [COUNTER_BITS-1:0] ext_factor_div,
   input wire signed [LINE_BITS-1:0]    scale_h,
   output wire                          vsync,
   output wire signed [32-1 : 0]        vcount,
   input wire signed [BPP-1 : 0]        ext_color,
   output wire                          ext_vga_hs,
   output wire                          ext_vga_vs,
   output wire                          ext_vga_de,
   output wire                          ext_vga_clk,
   output wire signed [BPP-1 : 0]       ext_vga_color,
   output wire signed [32-1 : 0]        ext_count_h,
   output wire signed [32-1 : 0]        ext_count_v
   );

  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam ONE = 1'd1;
  localparam ZERO = 1'd0;
  localparam PIXEL_DELAY = 7;

  reg [LINE_BITS-1:0] count_h;
  reg [LINE_BITS-1:0] count_v;
  wire                vga_hs;
  wire                vga_vs;
  wire                pixel_valid;
  wire                vga_hs_delay;
  wire                vga_vs_delay;
  wire                pixel_valid_delay;
  wire                en;
  wire                frac_clk_en;
  wire                frac_clk_out;

  assign en = ENABLE_FRAC_CLK ? frac_clk_en : TRUE;

  // H counter
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          count_h <= ZERO;
        end
      else
        begin
          if (en == TRUE)
            begin
              if (count_h >= VGA_MAX_H)
                begin
                  count_h <= ZERO;
                end
              else
                begin
                  count_h <= count_h + scale_h;
                end
            end
        end
    end

  // V counter
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          count_v <= ZERO;
        end
      else
        begin
          if (en == TRUE)
            begin
              if (count_h == VGA_MAX_H)
                begin
                  if (count_v == VGA_MAX_V)
                    begin
                      count_v <= ZERO;
                    end
                  else
                    begin
                      count_v <= count_v + ONE;
                    end
                end
            end
        end
    end

  // H sync
  assign vga_hs = ((count_h >= VGA_SYNC_H_START) && (count_h < VGA_SYNC_H_END)) ? FALSE : TRUE;
  // V sync
  assign vga_vs = ((count_v >= VGA_SYNC_V_START) && (count_v < VGA_SYNC_V_END)) ? FALSE : TRUE;
  // Pixel valid
  assign pixel_valid = ((count_h < VGA_WIDTH) && (count_v < VGA_HEIGHT)) ? TRUE : FALSE;

  // ext out
  assign ext_vga_color = pixel_valid_delay ? ext_color : {BPP{1'b0}};
  assign ext_vga_hs = vga_hs_delay;
  assign ext_vga_vs = vga_vs_delay;
  assign ext_vga_de = pixel_valid_delay;
  assign ext_count_h = count_h;
  assign ext_count_v = count_v;

  assign vsync = ext_vga_vs;
  assign vcount = count_v;

  frac_clk
    #(
      .COUNTER_BITS (COUNTER_BITS)
      )
  frac_clk_0
    (
     .clk (clk),
     .reset (reset),
     .factor_mul (ext_factor_mul),
     .factor_div (ext_factor_div),
     .clk_out (frac_clk_out),
     .en (frac_clk_en)
   );

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (PIXEL_DELAY)
      )
  shift_register_vector_vga_hs
    (
     .clk (clk),
     .data_in (vga_hs),
     .data_out (vga_hs_delay)
     );

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (PIXEL_DELAY)
      )
  shift_register_vector_vga_vs
    (
     .clk (clk),
     .data_in (vga_vs),
     .data_out (vga_vs_delay)
     );

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (PIXEL_DELAY)
      )
  shift_register_vector_pixel_valid
    (
     .clk (clk),
     .data_in (pixel_valid),
     .data_out (pixel_valid_delay)
     );

  shift_register_vector
    #(
      .WIDTH (1),
      .DEPTH (PIXEL_DELAY)
      )
  shift_register_vector_frac_clk_out
    (
     .clk (clk),
     .data_in (frac_clk_out),
     .data_out (ext_vga_clk)
     );

endmodule
