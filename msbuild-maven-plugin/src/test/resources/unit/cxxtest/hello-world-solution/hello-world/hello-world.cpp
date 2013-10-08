#pragma once
#include <stdio.h>

int main(int argc, char* argv[])
{
    printf("HELLO WORLD!\n");
    fclose(fopen("cxxtest-report-hello-world.xml", "w"));

#ifdef NDEBUG
    return 0;
#else
    return 1;
#endif
}
