#include "targetver.h"
#include <stdio.h>
#include "compute-pi.h"


int main(int argc, char* argv[]) {
    printf("The approximate value of PI is %f\n", computePI(COMPUTE_PI_ITERATIONS));
    return 0;
}