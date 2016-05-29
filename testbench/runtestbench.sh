#!/bin/sh

EXE=main

iverilog -Wall -o $EXE *.v ../*.v

if [ $? -eq 0 ]; then
  vvp $EXE
fi
