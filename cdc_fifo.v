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

module cdc_fifo
  #(
    parameter DATA_WIDTH = 8,
    parameter ADDR_WIDTH = 4,
    parameter MAX_ITEMS = 10 // MAX_ITEMS < ((1 << ADDR_WIDTH) - 5)
    )
  (
   // -------- clock domain: read  --------
   input wire                   clk_cr,
   input wire                   reset_cr,
   output wire [DATA_WIDTH-1:0] data_cr,
   input wire                   req_cr,
   output reg                   empty_cr,
   output reg                   valid_cr,
   // -------- clock domain: write --------
   input wire                   clk_cw,
   input wire                   reset_cw,
   input wire [DATA_WIDTH-1:0]  data_cw,
   input wire                   we_cw,
   output reg                   almost_full_cw
   );

  localparam SYNC_DEPTH = 4;
  localparam ZERO = 1'd0;
  localparam ONE = 1'd1;
  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;

  // gray-code conversion
  function [ADDR_WIDTH-1:0] bin2gray
    (
     input [ADDR_WIDTH-1:0] data_in
     );
    begin
      bin2gray = (data_in >> ONE) ^ data_in;
    end
  endfunction

  function [ADDR_WIDTH-1:0] gray2bin
    (
     input [ADDR_WIDTH-1:0] data_in
     );
    integer                 i;
    begin
      gray2bin = ZERO;
      for (i = 0; i < ADDR_WIDTH; i = i + ONE)
        begin
          gray2bin = gray2bin ^ (data_in >> i);
        end
    end
  endfunction

  // -------- clock domain: read  --------
  reg  [ADDR_WIDTH-1:0] addr_r_cr;
  reg [ADDR_WIDTH-1:0]  addr_r_gray_cr;
  reg [ADDR_WIDTH-1:0]  addr_r_next_cr;
  wire [ADDR_WIDTH-1:0] addr_w_sync_cr;
  wire                  empty;
  reg                   req_cr_d1;
  reg                   empty_d1;

  assign empty = (addr_r_gray_cr == addr_w_sync_cr) ? TRUE : FALSE;

  always @(posedge clk_cr)
    begin
      req_cr_d1 <= req_cr;
      empty_d1 <= empty;
      empty_cr <= empty_d1;
    end

  always @(posedge clk_cr)
    begin
      if (reset_cr == TRUE)
        begin
          addr_r_cr <= ZERO;
          addr_r_gray_cr <= ZERO;
          addr_r_next_cr <= ONE;
        end
      else
        begin
          if ((req_cr == TRUE) && (empty == FALSE))
            begin
              addr_r_cr <= addr_r_next_cr;
              addr_r_gray_cr <= bin2gray(addr_r_next_cr);
              addr_r_next_cr <= addr_r_next_cr + ONE;
            end
        end
    end

  always @(posedge clk_cr)
    begin
      if (reset_cr == TRUE)
        begin
          valid_cr <= FALSE;
        end
      else
        begin
          if ((req_cr_d1 == TRUE) && (empty_d1 == FALSE))
            begin
              valid_cr <= TRUE;
            end
          else
            begin
              valid_cr <= FALSE;
            end
        end
    end

  // -------- clock domain: write --------
  wire [ADDR_WIDTH-1:0] addr_r_sync_cw;
  reg [ADDR_WIDTH-1:0]  addr_w_cw;
  reg [ADDR_WIDTH-1:0]  addr_w_next_gray_cw;
  reg [ADDR_WIDTH-1:0]  addr_w_next_cw;
  reg [ADDR_WIDTH-1:0]  addr_w_gray_cw;
  reg [ADDR_WIDTH-1:0]  addr_r_sync_bin_cw;
  reg [ADDR_WIDTH-1:0]  item_count;

  always @(posedge clk_cw)
    begin
      if (reset_cw == TRUE)
        begin
          addr_w_cw <= ZERO;
          addr_w_next_cw <= ONE;
          addr_w_gray_cw <= ZERO;
        end
      else
        if (we_cw == TRUE)
          begin
            addr_w_cw <= addr_w_next_cw;
            addr_w_next_cw <= addr_w_next_cw + ONE;
            addr_w_gray_cw <= addr_w_next_gray_cw;
          end
    end

  always @(posedge clk_cw)
    begin
      addr_w_next_gray_cw <= bin2gray(addr_w_next_cw);
      addr_r_sync_bin_cw <= gray2bin(addr_r_sync_cw);
      item_count <= addr_w_cw - addr_r_sync_bin_cw;
      almost_full_cw <= (item_count > MAX_ITEMS) ? TRUE : FALSE;
    end

  shift_register_vector
    #(
      .WIDTH (ADDR_WIDTH),
      .DEPTH (SYNC_DEPTH)
      )
  shift_register_vector_0
    (
     .clk (clk_cw),
     .data_in (addr_r_gray_cr),
     .data_out (addr_r_sync_cw)
     );

  shift_register_vector
    #(
      .WIDTH (ADDR_WIDTH),
      .DEPTH (SYNC_DEPTH)
      )
  shift_register_vector_1
    (
     .clk (clk_cr),
     .data_in (addr_w_gray_cw),
     .data_out (addr_w_sync_cr)
     );

  dual_clk_ram
    #(
      .DATA_WIDTH (DATA_WIDTH),
      .ADDR_WIDTH (ADDR_WIDTH)
      )
  dual_clk_ram_0
    (
     .data_in (data_cw),
     .read_addr (addr_r_cr),
     .write_addr (addr_w_cw),
     .we (we_cw),
     .read_clock (clk_cr),
     .write_clock (clk_cw),
     .data_out (data_cr)
     );

endmodule
