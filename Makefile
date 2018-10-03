ROMS=$(wildcard *_code.bin *_data.bin *_code_rom.v *_data_rom.v *_code_mem.v *_data_mem.v *_code_tb.txt *_data_tb.txt)

all: tool

run: tool
	make -C tools run

tool: program
	make -C tools

program:
	make -C asm

clean:
	make -C tools clean
	make -C asm clean
	make -C testbench clean
	rm -f $(ROMS)
