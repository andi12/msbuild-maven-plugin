#pragma once
#include <stdio.h>

int main(int argc, char* argv[])
{
    char c;

    for (int i = 0; i < argc; i++)
    {
        printf("%s\n", argv[i]);
    }

    while ((c = fgetc(stdin)) != EOF)
    {
        printf("%c", c);
    }
}
