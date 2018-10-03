module default_data_mem_pair
  #(
    parameter DATA_WIDTH=8,
    parameter ADDR_WIDTH=12
    )
  (
   input                     clk,
   input [(ADDR_WIDTH-1):0]  addr_r_a,
   input [(ADDR_WIDTH-1):0]  addr_r_b,
   input [(ADDR_WIDTH-1):0]  addr_w,
   input [(DATA_WIDTH-1):0]  data_in,
   input                     we,
   output [(DATA_WIDTH-1):0] data_out_a,
   output [(DATA_WIDTH-1):0] data_out_b
   );

  default_data_mem
    #(
      .DATA_WIDTH (DATA_WIDTH),
      .ADDR_WIDTH (ADDR_WIDTH)
      )
  default_data_mem_a
    (
     .clk (clk),
     .addr_r (addr_r_a),
     .addr_w (addr_w),
     .data_in (data_in),
     .we (we),
     .data_out (data_out_a)
     );

  default_data_mem
    #(
      .DATA_WIDTH (DATA_WIDTH),
      .ADDR_WIDTH (ADDR_WIDTH)
      )
  default_data_mem_b
    (
     .clk (clk),
     .addr_r (addr_r_b),
     .addr_w (addr_w),
     .data_in (data_in),
     .we (we),
     .data_out (data_out_b)
     );

endmodule
