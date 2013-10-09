#include "compute-pi.h"
#include <math.h>

double computePI(int n)  {
    double frac, denom;

    denom = (2 * n - 1) + pow((double) n, 2);

    for (int i = n - 1; i > 0; i--) {
        frac = pow((double) i, 2) / denom;
        denom = (2 * i - 1) + frac;
    }

    return 4.0 / (1.0 + frac);
}