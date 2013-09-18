#pragma once
#include <stdio.h>

int main(int argc, char* argv[])
{
    printf("HELLO WORLD!\n");

#ifdef NDEBUG
    return 0;
#else
    return 1;
#endif
}
