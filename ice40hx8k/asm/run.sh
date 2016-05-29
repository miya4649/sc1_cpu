#!/bin/sh

javac Asm.java AsmTop.java Program.java
java AsmTop > ../rom.v
