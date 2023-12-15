create_clock -period "12.0 MHz" [get_ports CLK12M]

derive_pll_clocks

derive_clock_uncertainty
