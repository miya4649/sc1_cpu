#!/bin/sh

EXE=main

iverilog -Wall -o $EXE ../testbench/testbench.v *.v ../rw_port_ram.v

if [ $? -eq 0 ]; then
  vvp $EXE
fi
