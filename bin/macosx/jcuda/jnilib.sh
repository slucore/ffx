#!/bin/sh

rm libcudafft.jnilib
nvcc -g -m32 -c convolution.cu -I/System/Library/Frameworks/JavaVM.framework/Headers -I/Developer/GPU\ Computing/C/common/inc
g++ -g -arch i386 -dynamiclib convolution.o -o libcudafft.jnilib -L/usr/local/cuda/lib -lcudart -lcuda -lcufft
rm convolution.o

