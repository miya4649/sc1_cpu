#------------------------------------------------------------
create_clock -period "50.0 MHz" [get_ports SYS_CLK]
create_clock -period "24.0 MHz" [get_ports USER_CLK]

#------------------------------------------------------------
derive_pll_clocks

#------------------------------------------------------------
derive_clock_uncertainty

#------------------------------------------------------------
set_clock_groups -asynchronous -group SYS_CLK -group simple_pll_0|altpll_component|auto_generated|pll1|clk[0]
