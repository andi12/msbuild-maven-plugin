#include "targetver.h"
#include <stdio.h>


int main(int argc, char* argv[]) {
    printf("HELLO WORLD!\n");

#ifdef _DEBUG
    int i = 0; // could be declared later
    if ( argc == 3 )
    {
        i = argc;
        printf("i=%d", i);
    }
#endif

    int var1 = 10; // unused

    return 0;
}
