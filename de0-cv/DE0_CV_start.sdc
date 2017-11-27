create_clock -period "50.0 MHz" [get_ports CLOCK_50]
create_clock -period "50.0 MHz" [get_ports CLOCK2_50]
create_clock -period "50.0 MHz" [get_ports CLOCK3_50]
create_clock -period "50.0 MHz" [get_ports CLOCK4_50]

derive_pll_clocks

derive_clock_uncertainty

set_clock_groups -asynchronous -group CLOCK_50 -group av_pll_0|av_pll_inst|altera_pll_i|general[0].gpll~PLL_OUTPUT_COUNTER|divclk

set_clock_groups -asynchronous -group CLOCK_50 -group av_pll_0|av_pll_inst|altera_pll_i|general[1].gpll~PLL_OUTPUT_COUNTER|divclk
